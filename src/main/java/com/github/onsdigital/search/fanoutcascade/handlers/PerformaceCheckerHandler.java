package com.github.onsdigital.search.fanoutcascade.handlers;

import com.github.onsdigital.elasticutils.ml.ranklib.models.Judgements;
import com.github.onsdigital.fanoutcascade.handlers.Handler;
import com.github.onsdigital.fanoutcascade.handlertasks.HandlerTask;
import com.github.onsdigital.fanoutcascade.pool.FanoutCascade;
import com.github.onsdigital.search.configuration.SearchEngineProperties;
import com.github.onsdigital.search.fanoutcascade.handlertasks.ModelTrainingTask;
import com.github.onsdigital.search.mongo.models.Duration;
import com.github.onsdigital.search.search.PerformanceChecker;
import com.github.onsdigital.search.search.models.SearchHitCounter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Iterator;
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
        int tmpCount = 0;

        while (!FanoutCascade.getInstance().isShutdown()) {
            try {
                PerformanceChecker performanceChecker = new PerformanceChecker();

                double sumNdcg = 0.0f;
                int count = 0;

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
                LOGGER.info("Mean NDCG: " + meanNdcg);

                // TODO - remove
                tmpCount++;

                // TODO - remove || clause
                if (meanNdcg < NDCG_THRESHOLD || tmpCount >= 3) {
                    tmpCount = 0;

                    // Check if we've submitted in the last allowed time frame
                    Date now = new Date();

                    TimeUnit timeUnit = SearchEngineProperties.FANOUTCASCADE.getSubmitTimeUnit();
                    long value = SearchEngineProperties.FANOUTCASCADE.getSubmitValue();

                    boolean canSubmit = true;
                    Iterable<ModelTrainingTask> modelTrainingTasks = ModelTrainingTask.finder().find();
                    Iterator<ModelTrainingTask> it = modelTrainingTasks.iterator();
                    while (it.hasNext()) {
                        ModelTrainingTask modelTrainingTask = it.next();
                        Duration duration = new Duration(now, modelTrainingTask.getDate());
                        long longDuration = duration.getDuration(timeUnit);
                        if (longDuration < value) {
                            canSubmit = false;
                        }
                    }

                    if (canSubmit) {
                        LOGGER.info("Submitting ModelTrainingTask");
                        // Submit a ModelTrainingTask
                        ModelTrainingTask modelTrainingTask = new ModelTrainingTask(uniqueHits, now);
                        // Save a copy of the task
                        modelTrainingTask.writer().save();

                        // Submit
                        FanoutCascade.getInstance().getLayerForTask(ModelTrainingTask.class).submit(modelTrainingTask);
                    } else {
                        LOGGER.info("Already submitted this window, skipping");
                    }
                }
            } catch (Exception e) {
                // Thread must stay alive, so catch any exception raised
                e.printStackTrace();
            }

            try {
                TimeUnit sleepTimeUnit = SearchEngineProperties.FANOUTCASCADE.getPerformanceCheckerSleepTimeUnit();
                long sleepTime = SearchEngineProperties.FANOUTCASCADE.getPerformanceCheckerSleepValue();
                Thread.sleep(sleepTimeUnit.toMillis(sleepTime));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
        return null;
    }
}
