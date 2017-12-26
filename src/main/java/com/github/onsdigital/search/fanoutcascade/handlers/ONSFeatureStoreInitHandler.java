package com.github.onsdigital.search.fanoutcascade.handlers;

import com.github.onsdigital.elasticutils.ml.client.http.LearnToRankClient;
import com.github.onsdigital.elasticutils.ml.client.response.features.models.FeatureSet;
import com.github.onsdigital.elasticutils.ml.requests.FeatureSetRequest;
import com.github.onsdigital.elasticutils.ml.util.LearnToRankHelper;
import com.github.onsdigital.fanoutcascade.handlers.Handler;
import com.github.onsdigital.fanoutcascade.handlertasks.HandlerTask;
import com.github.onsdigital.search.configuration.SearchEngineProperties;
import com.github.onsdigital.search.fanoutcascade.handlertasks.ONSFeatureStoreInitTask;
import com.github.onsdigital.search.fanoutcascade.handlertasks.RankLibTask;
import com.github.onsdigital.search.server.LearnToRankService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @author sullid (David Sullivan) on 26/12/2017
 * @project dp-search-service
 */
public class ONSFeatureStoreInitHandler implements Handler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ONSFeatureStoreInitHandler.class);

    @Override
    public Object handleTask(HandlerTask handlerTask) throws Exception {
        ONSFeatureStoreInitTask task = (ONSFeatureStoreInitTask) handlerTask;

        // Get name on disk
        String nameOnDisk = task.getFeatureStore();

        String featureStoreName = getFeatureStoreNameWithDate(task, new Date());
        String featureSetName = task.getFeatureSetName();

        Map<String, List<FeatureSet>> featureSets = loadFeatureSet(nameOnDisk);

        if (null == featureSets) {
            LOGGER.info(String.format("No feature store found on disk with name %s, exiting.", nameOnDisk));
            return null;
        }

        // Create a new feature set
        try (LearnToRankClient client = LearnToRankHelper.getLTRClient(LearnToRankService.HOSTNAME)) {
            List<FeatureSet> featureSetList = featureSets.get(nameOnDisk);

            // Init the store
            if (!client.featureStoreExists(featureStoreName)) {
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

        // Return the RankLib tasks
        List<RankLibTask> tasks = new ArrayList<>();
        // Return a RankLibTask
        for (int i = 0; i <= 9; i++) {
            RankLibTask rankLibTask = new RankLibTask(featureStoreName, featureSetName, task.getDate(), i);
            tasks.add(rankLibTask);
        }
        return tasks;
    }

    private static String getFeatureStoreNameWithDate(ONSFeatureStoreInitTask task, Date date) {
        return String.format("%s_%d", task.getFeatureStore(), date.getTime());
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
//        for (File featureSetDirectory : featureSetDirectories) {

//        }

        return null;
    }
}
