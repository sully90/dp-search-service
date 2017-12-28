package com.github.onsdigital.search.fanoutcascade.handlers;

import com.github.onsdigital.elasticutils.ml.ranklib.models.Judgements;
import com.github.onsdigital.fanoutcascade.handlers.Handler;
import com.github.onsdigital.fanoutcascade.handlertasks.HandlerTask;
import com.github.onsdigital.fanoutcascade.pool.FanoutCascade;
import com.github.onsdigital.search.configuration.SearchEngineProperties;
import com.github.onsdigital.search.fanoutcascade.handlertasks.ModelTrainingTask;
import com.github.onsdigital.search.search.PerformanceChecker;
import com.github.onsdigital.search.search.models.SearchHitCounter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author sullid (David Sullivan) on 22/12/2017
 * @project dp-search-service
 */
public class PerformanceCheckerHandler implements Handler {

    private static final Logger LOGGER = LoggerFactory.getLogger(PerformanceCheckerHandler.class);

    private static final DateFormat ISO_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    private static final float NDCG_THRESHOLD = 0.5f;

    // TODO - remove
    private static final boolean FORCE_SUBMIT = true;

    @Override
    public Object handleTask(HandlerTask handlerTask) {

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

                // TODO - remove || clause
                if (meanNdcg < NDCG_THRESHOLD || FORCE_SUBMIT) {

                    // Check if we've submitted in the last allowed time frame
                    Date now = new Date();
                    long nowMillis = now.getTime();

                    TimeUnit timeUnit = SearchEngineProperties.FANOUTCASCADE.getSubmitTimeUnit();
                    long value = SearchEngineProperties.FANOUTCASCADE.getSubmitValue();

                    long withinWindow = nowMillis - timeUnit.toMillis(value);
                    Date then = new Date(withinWindow);

                    final String query = getDateQuery(then, now);
                    LOGGER.info("Query: " + query);

                    long taskCount = ModelTrainingTask.finder().count(query);
                    LOGGER.info(String.format("Found %d tasks which match query", taskCount));

                    if (taskCount == 0) {
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

                // Sleep the thread
                TimeUnit sleepTimeUnit = SearchEngineProperties.FANOUTCASCADE.getPerformanceCheckerSleepTimeUnit();
                long sleepTime = SearchEngineProperties.FANOUTCASCADE.getPerformanceCheckerSleepValue();
                Thread.sleep(sleepTimeUnit.toMillis(sleepTime));

            } catch (Exception e) {
                // Thread must stay alive, so catch any exception raised
                e.printStackTrace();
            }

        }
        return null;
    }

    static String getDateQuery(Date then, Date now) {
        return String.format("{date: {$gte : {$date : \"%s\"}, $lte : {$date : \"%s\"}}}", ISO_DATE_FORMAT.format(then), ISO_DATE_FORMAT.format(now));
    }
}