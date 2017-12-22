package com.github.onsdigital.search.fanoutcascade.handlers;

import com.github.onsdigital.elasticutils.ml.client.http.LearnToRankClient;
import com.github.onsdigital.elasticutils.ml.client.response.sltr.SltrHit;
import com.github.onsdigital.elasticutils.ml.client.response.sltr.SltrResponse;
import com.github.onsdigital.elasticutils.ml.client.response.sltr.models.Fields;
import com.github.onsdigital.elasticutils.ml.query.SltrQueryBuilder;
import com.github.onsdigital.elasticutils.ml.ranklib.models.Judgement;
import com.github.onsdigital.elasticutils.ml.ranklib.models.Judgements;
import com.github.onsdigital.elasticutils.ml.requests.LogQuerySearchRequest;
import com.github.onsdigital.elasticutils.ml.util.LearnToRankHelper;
import com.github.onsdigital.fanoutcascade.handlers.Handler;
import com.github.onsdigital.fanoutcascade.handlertasks.HandlerTask;
import com.github.onsdigital.search.fanoutcascade.handlertasks.ModelTrainingTask;
import com.github.onsdigital.search.search.models.SearchHitCounter;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author sullid (David Sullivan) on 22/12/2017
 * @project dp-search-service
 */
public class ModelTrainingHandler implements Handler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModelTrainingHandler.class);

    private String store = "ons_featurestore";
    private String featureSet = "ons_features";

    @Override
    public Object handleTask(HandlerTask handlerTask) {
        ModelTrainingTask task = (ModelTrainingTask) handlerTask;
        Map<String, SearchHitCounter> uniqueHits = task.getUniqueHits();
        // Init Learn to rank client and generate a training set
        try (LearnToRankClient learnToRankClient = LearnToRankHelper.getLTRClient("localhost")) {

            // For each search term, compute judgeemts and log features
            for (String term : uniqueHits.keySet()) {
                System.out.println("Term: " + term);

                Judgements judgements = uniqueHits.get(term).getJudgements(term);
                List<Judgement> judgementList = judgements.getJudgementList();
                // Sort the judgements by the original rank they were displayed to the user as
                Collections.sort(judgementList);

                // Loop over judgements and get feature scores
                for (int i = 0; i < judgementList.size(); i++) {
                    Object obj = judgementList.get(i).getAttr("url");
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
                            Fields fields = sltrHits.get(0).getFields();

                            // Print scores to the console
                            System.out.println(fields.getValues().toString());
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error closing LearnToRankClient", e);
        }
        return null;
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
