package com.github.onsdigital.search.fanoutcascade.handlers;

import com.github.onsdigital.elasticutils.ml.ranklib.models.Judgements;
import com.github.onsdigital.fanoutcascade.handlers.Handler;
import com.github.onsdigital.fanoutcascade.handlertasks.HandlerTask;
import com.github.onsdigital.fanoutcascade.pool.FanoutCascade;
import com.github.onsdigital.search.fanoutcascade.handlertasks.ModelTrainingTask;
import com.github.onsdigital.search.search.PerformanceChecker;
import com.github.onsdigital.search.search.models.SearchHitCounter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author sullid (David Sullivan) on 22/12/2017
 * @project dp-search-service
 */
public class PerformaceCheckerHandler implements Handler {

    private static final Logger LOGGER = LoggerFactory.getLogger(PerformaceCheckerHandler.class);

    private static final float NDCG_THRESHOLD = 0.5f;

    @Override
    public Object handleTask(HandlerTask handlerTask) {

        // TODO - remove
        int count = 0;

        while (!FanoutCascade.getInstance().isShutdown()) {
            PerformanceChecker performanceChecker = new PerformanceChecker();

            double sumNdcg = 0.0f;
            int tmpCount = 0;

            // Count the unique URL hits
            Map<String, SearchHitCounter> uniqueHits = performanceChecker.getUniqueHitCounts();

            // For each search term, compute judgeemts and log features
            for (String term : uniqueHits.keySet()) {
                Judgements judgements = uniqueHits.get(term).getJudgements(term);

                // Compute normalised discounted cumulative gain as a measure of current performance
                float[] ndcg = judgements.normalisedDiscountedCumulativeGain();

                double sum = 0.0d;
                for (float val : ndcg) {
                    sum += (double) val;
                }
                sumNdcg += sum;
                count += ndcg.length;
            }

            double meanNdcg = sumNdcg / (double) count;
            LOGGER.info("Mean NGDC: " + meanNdcg);

            // TODO - remove
            tmpCount++;

            // TODO - remove || clause
            if (meanNdcg < NDCG_THRESHOLD || tmpCount > 3) {
                tmpCount = 0;
                LOGGER.info("Submitting ModelTrainingTask");
                // Submit a ModelTrainingTask
                ModelTrainingTask modelTrainingTask = new ModelTrainingTask(uniqueHits);
                FanoutCascade.getInstance().getLayerForTask(ModelTrainingTask.class).submit(modelTrainingTask);
            }

            try {
                Thread.sleep(TimeUnit.SECONDS.toMillis(10));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
        return null;
    }
}
