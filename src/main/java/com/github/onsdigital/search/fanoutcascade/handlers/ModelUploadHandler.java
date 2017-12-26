package com.github.onsdigital.search.fanoutcascade.handlers;

import com.github.onsdigital.elasticutils.ml.client.http.LearnToRankClient;
import com.github.onsdigital.elasticutils.ml.ranklib.models.ModelType;
import com.github.onsdigital.elasticutils.ml.ranklib.models.RankLibModel;
import com.github.onsdigital.elasticutils.ml.util.LearnToRankHelper;
import com.github.onsdigital.fanoutcascade.handlers.Handler;
import com.github.onsdigital.fanoutcascade.handlertasks.HandlerTask;
import com.github.onsdigital.search.fanoutcascade.handlertasks.ModelUploadTask;
import com.github.onsdigital.search.server.LearnToRankService;
import org.apache.http.HttpStatus;
import org.elasticsearch.client.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author sullid (David Sullivan) on 26/12/2017
 * @project dp-search-service
 */
public class ModelUploadHandler implements Handler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModelUploadHandler.class);

    private static final String HOSTNAME = LearnToRankService.HOSTNAME;

    @Override
    public Object handleTask(HandlerTask handlerTask) throws Exception {
        ModelUploadTask task = (ModelUploadTask) handlerTask;

        String featureStore = task.getFeatureStore();
        String featureSet = task.getFeatureSet();
        String name = task.getName();
        String modelFileName = task.getModelFileName();

        try (LearnToRankClient client = LearnToRankHelper.getLTRClient(HOSTNAME)) {
            // First, check if model exists
            Response getResponse = client.getModel(featureStore, name);
            if (getResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                // Found, delete the model
                LOGGER.info(String.format("Deleting model with name %s from feature store %s", name, featureStore));
                client.deleteModel(featureStore, name);
            }

            RankLibModel rankLibModel = RankLibModel.fromFile(name, ModelType.RANKLIB, modelFileName);
            LOGGER.info(String.format("Uploading model with name %s and feature set %s to feature store %s",
                    name, featureSet, featureStore));
            client.createModel(featureStore, featureSet, rankLibModel);

            LOGGER.info(String.format("Clearing cache on feature store %s", featureStore));
            // Clear model cache
            client.clearCache(featureStore);
        } catch (Exception e) {
            LOGGER.error("Caught exception during model upload", e);
        }
        return null;
    }
}
