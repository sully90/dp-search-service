package com.github.onsdigital.search.fanoutcascade.handlers;

import com.github.onsdigital.elasticutils.ml.client.http.LearnToRankClient;
import com.github.onsdigital.elasticutils.ml.client.response.sltr.SltrHit;
import com.github.onsdigital.elasticutils.ml.client.response.sltr.SltrResponse;
import com.github.onsdigital.elasticutils.ml.client.response.sltr.models.Rankable;
import com.github.onsdigital.elasticutils.ml.query.SltrQueryBuilder;
import com.github.onsdigital.elasticutils.ml.ranklib.Exporter;
import com.github.onsdigital.elasticutils.ml.ranklib.models.Judgement;
import com.github.onsdigital.elasticutils.ml.ranklib.models.Judgements;
import com.github.onsdigital.elasticutils.ml.requests.LogQuerySearchRequest;
import com.github.onsdigital.elasticutils.ml.util.LearnToRankHelper;
import com.github.onsdigital.fanoutcascade.handlers.Handler;
import com.github.onsdigital.fanoutcascade.handlertasks.HandlerTask;
import com.github.onsdigital.search.fanoutcascade.handlertasks.ModelTrainingTask;
import com.github.onsdigital.search.fanoutcascade.handlertasks.ONSFeatureStoreInitTask;
import com.github.onsdigital.search.search.models.SearchHitCounter;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

/**
 * @author sullid (David Sullivan) on 22/12/2017
 * @project dp-search-service
 */
public class TrainingSetHandler implements Handler {

    private static final Logger LOGGER = LoggerFactory.getLogger(TrainingSetHandler.class);

    private static final String HOSTNAME = "localhost";

    private String store = "ons_featurestore";
    private String featureSet = "ons_features";

    @Override
    public Object handleTask(HandlerTask handlerTask) throws Exception {

        ModelTrainingTask task = (ModelTrainingTask) handlerTask;
        Map<String, SearchHitCounter> uniqueHits = task.getUniqueHits();

        // Init Judgement - Rankable map
        Map<Judgement, Rankable> queryFeatureMap = new LinkedHashMap<>();

        // Init Learn to rank client and generate a training set
        try (LearnToRankClient learnToRankClient = LearnToRankHelper.getLTRClient(HOSTNAME)) {

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
                    if (obj instanceof String) {
                        // Pages are stored in ES with _id as their uri
                        // So we perform a sltr query with an _id filter to get the feature scores
                        String url = String.valueOf(obj);

                        // Construct the LogQuerySearchRequest
                        LogQuerySearchRequest logQuerySearchRequest = getLogQuerySearchRequest(store,
                                featureSet, url, term);

                        // Perform the sltr search request
                        SltrResponse sltrResponse = learnToRankClient.search("ons_*", logQuerySearchRequest);

                        // Get the sltr hits from the response
                        List<SltrHit> sltrHits = sltrResponse.getHits().getHits();
                        if (sltrHits.size() > 0) {
                            // The page was found, so we have logged feature scores as Fields
                            Rankable rankable = sltrHits.get(0);
                            queryFeatureMap.put(judgement, rankable);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error in LearnToRankClient", e);
            // rethrow to be dealt with by exception handler
            throw e;
        }
        Date now = task.getDate();

        // Create a FeatureStoreInitTask
        ONSFeatureStoreInitTask initTask = new ONSFeatureStoreInitTask(store, featureSet, now);

        // Write the training data
        String fileName = getFileName(now);

        // Run the exporter
        Exporter.export(fileName, queryFeatureMap);

        // Return the store init task
        return initTask;
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
}
