package com.github.onsdigital.search.fanoutcascade.handlers;

import com.github.onsdigital.elasticutils.ml.client.http.LearnToRankClient;
import com.github.onsdigital.elasticutils.ml.client.response.features.models.FeatureSet;
import com.github.onsdigital.elasticutils.ml.client.response.sltr.SltrHit;
import com.github.onsdigital.elasticutils.ml.client.response.sltr.SltrResponse;
import com.github.onsdigital.elasticutils.ml.client.response.sltr.models.Rankable;
import com.github.onsdigital.elasticutils.ml.query.SltrQueryBuilder;
import com.github.onsdigital.elasticutils.ml.ranklib.Exporter;
import com.github.onsdigital.elasticutils.ml.ranklib.models.Judgement;
import com.github.onsdigital.elasticutils.ml.ranklib.models.Judgements;
import com.github.onsdigital.elasticutils.ml.requests.FeatureSetRequest;
import com.github.onsdigital.elasticutils.ml.requests.LogQuerySearchRequest;
import com.github.onsdigital.elasticutils.ml.util.LearnToRankHelper;
import com.github.onsdigital.fanoutcascade.handlers.Handler;
import com.github.onsdigital.fanoutcascade.handlertasks.HandlerTask;
import com.github.onsdigital.search.configuration.SearchEngineProperties;
import com.github.onsdigital.search.fanoutcascade.handlertasks.RankLibTask;
import com.github.onsdigital.search.fanoutcascade.handlertasks.TrainingSetTask;
import com.github.onsdigital.search.search.models.SearchHitCounter;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @author sullid (David Sullivan) on 22/12/2017
 * @project dp-search-service
 */
public class TrainingSetHandler implements Handler {

    private static final Logger LOGGER = LoggerFactory.getLogger(TrainingSetHandler.class);

    private static final String HOSTNAME = "localhost";

    @Override
    public Object handleTask(HandlerTask handlerTask) throws Exception {

        TrainingSetTask task = (TrainingSetTask) handlerTask;
        Map<String, SearchHitCounter> uniqueHits = task.getUniqueHits();

        // Init Judgement - Rankable map
        Map<Judgement, Rankable> queryFeatureMap = new LinkedHashMap<>();

        // Init Learn to rank client and generate a training set
        try (LearnToRankClient learnToRankClient = LearnToRankHelper.getLTRClient(HOSTNAME)) {

            // Init the feature store
            initFeatureStore(task, learnToRankClient);

            // For each search term, compute judgeemts and log features
            for (String term : uniqueHits.keySet()) {
//                System.out.println("Term: " + term);

                Judgements judgements = uniqueHits.get(term).getJudgements(term);
                List<Judgement> judgementList = judgements.getJudgementList();
                // Sort the judgements by the original rank they were displayed to the user as
                Collections.sort(judgementList);

                // Loop over judgements and get feature scores
                for (int i = 0; i < judgementList.size(); i++) {
                    Judgement judgement = judgementList.get(i);
                    Object obj = judgement.getAttr("url");
                    if (null != obj && obj instanceof String) {
                        // Pages are stored in ES with _id as their uri
                        // So we perform a sltr query with an _id filter to get the feature scores
                        String url = String.valueOf(obj);

                        // Construct the LogQuerySearchRequest
                        LogQuerySearchRequest logQuerySearchRequest = getLogQuerySearchRequest(task.getFeatureStore(),
                                task.getFeatureSet(), url, term);

                        LOGGER.info("Query: " + logQuerySearchRequest.toJson());

                        // Perform the sltr search request
                        SltrResponse sltrResponse = learnToRankClient.search("ons_*", logQuerySearchRequest);

                        // Get the sltr hits from the response
                        List<SltrHit> sltrHits = sltrResponse.getHits().getHits();
                        if (sltrHits.size() > 0) {
                            // The page was found, so we have logged feature scores as Fields
                            Rankable rankable = sltrHits.get(0);
                            queryFeatureMap.put(judgement, rankable);
                        } else {
                            LOGGER.warn(String.format("TrainingSetHandler: No hits for term:url pair \t %s:%s", term, url));
                        }
                    }
                }
            }

            Date now = task.getDate();

            // Write the training data
            String fileName = getFileName(now);

            // Run the exporter
            Exporter.export(fileName, queryFeatureMap);

            // It's now safe to record the task to mongo (all databases connections are live)
            task.writer().save();

            // Return the RankLib tasks
            List<RankLibTask> tasks = new ArrayList<>();
            // Return a RankLibTask
            for (int i = 0; i <= 9; i++) {
                RankLibTask rankLibTask = new RankLibTask(task.getFeatureStore(), task.getFeatureSet(), task.getDate(), i);
                tasks.add(rankLibTask);
            }
            return tasks;
        } catch (Exception e) {
            LOGGER.error("Error in LearnToRankClient", e);
            // rethrow to be dealt with by exception handler
            throw e;
        }
    }

    private static void initFeatureStore(TrainingSetTask task, LearnToRankClient client) throws IOException {

        // Get name on disk
        String nameOnDisk = task.getFeatureStore();

        String featureStoreName = getFeatureStoreNameWithDate(task.getFeatureStore(), new Date());

        Map<String, List<FeatureSet>> featureSets = loadFeatureSet(nameOnDisk);

        if (null == featureSets) {
            IOException e = new IOException(String.format("No feature store found on disk with name %s, exiting.", nameOnDisk));
            LOGGER.error(e.getMessage(), e);
            throw e;
        }

        // Create a new feature set
        List<FeatureSet> featureSetList = featureSets.get(nameOnDisk);

        // Init the store
        if (!client.featureStoreExists(featureStoreName)) {
            LOGGER.info("Creating feature store: " + featureStoreName);
            client.initFeatureStore(featureStoreName);
        }

        for (FeatureSet featureSet : featureSetList) {
            if (client.featureSetExists(featureStoreName, featureSet.getName())) {
                client.deleteFeatureSet(featureStoreName, featureSet.getName());
            }
            FeatureSetRequest request = new FeatureSetRequest(featureSet);

            if (LOGGER.isDebugEnabled()) LOGGER.debug(String.format("Creating feature set %s in store %s", request.getName(), featureStoreName));
            client.createFeatureSet(featureStoreName, request);
        }
    }

    public static String getFileName(Date date) {
        File directory = new File(String.format("src/main/resources/elastic.ltr/models/ons_%s/", date.getTime()));
        if (!directory.isDirectory()) {
            directory.mkdir();
        }
        String fileName = String.format("%s/ons_train.txt", directory.getAbsolutePath());
        return fileName;
    }

    private static LogQuerySearchRequest getLogQuerySearchRequest(String store, String featureSet,
                                                                  String id, String keywords) {
        // Placeholder log_name
        String logName = "logged_featureset";

        // Build the sltr query with keyword template injection
        SltrQueryBuilder sltrQueryBuilder = new SltrQueryBuilder(logName, featureSet);
        sltrQueryBuilder.setStore(store);
        sltrQueryBuilder.setParam("keywords", keywords);

        // Build the elasticsearch query, which performs a term filter on the _id field
        QueryBuilder qb = QueryBuilders.boolQuery()
                .must(QueryBuilders.termQuery("_id", id))
                .should(sltrQueryBuilder);

        // Build the LogQuerySearchRequest and return
        LogQuerySearchRequest logQuerySearchRequest = LogQuerySearchRequest.getRequestForQuery(qb, sltrQueryBuilder);
        return logQuerySearchRequest;
    }

    private static String getFeatureStoreNameWithDate(String featureStore, Date date) {
        return String.format("%s_%d", featureStore, date.getTime());
    }

    private static Map<String, List<FeatureSet>> loadFeatureSet(String featureStore) throws IOException {
        // Get the location of the featureSet store
        String path = SearchEngineProperties.getProperty("elastic.ltr.featureSets.store");

        // Get from classpath
        File storeDirectory = new File(path);

        if (!storeDirectory.isDirectory()) {
            throw new IOException("Unable to locate featureStore directory.");
        }

        Map<String, List<FeatureSet>> featureStoreToSetMap = new HashMap<>();

        // First directory name is the featureStore name

        // Each sub directory is a featureStore
        File[] featureStoreDirectorites = storeDirectory.listFiles(File::isDirectory);
        for (File featureStoreDirectory : featureStoreDirectorites) {
            String featureStoreName = featureStoreDirectory.getName();

            if (featureStoreName.equals(featureStore)) {
                // Each sub-directory from here is a featureSet
                List<FeatureSet> featureSets = new LinkedList<>();
                File[] featureSetDirectories = featureStoreDirectory.listFiles(File::isDirectory);
                for (File featureSetDirectory : featureSetDirectories) {
                    // Each sub-directory is a featureSet
                    FeatureSet featureSet = FeatureSet.readFromDirectory(featureSetDirectory);
                    featureSets.add(featureSet);
                }
                featureStoreToSetMap.put(featureStore, featureSets);
                return featureStoreToSetMap;
            }
        }

        return null;
    }
}
