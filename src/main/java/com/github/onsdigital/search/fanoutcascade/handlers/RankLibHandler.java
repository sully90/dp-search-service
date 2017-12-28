package com.github.onsdigital.search.fanoutcascade.handlers;

import com.github.onsdigital.fanoutcascade.handlers.Handler;
import com.github.onsdigital.fanoutcascade.handlertasks.HandlerTask;
import com.github.onsdigital.search.fanoutcascade.handlertasks.ModelUploadTask;
import com.github.onsdigital.search.fanoutcascade.handlertasks.RankLibTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

/**
 * @author sullid (David Sullivan) on 25/12/2017
 * @project dp-search-service
 */
public class RankLibHandler implements Handler {

    private static final Logger LOGGER = LoggerFactory.getLogger(RankLibHandler.class);

    @Override
    public Object handleTask(HandlerTask handlerTask) {
        // Runs RankLib for a given training set
        RankLibTask task = (RankLibTask) handlerTask;

        String trainingSetFileName = TrainingSetHandler.getFileName(task.getDate());
        int model = task.getModel();

        if (LOGGER.isDebugEnabled()) LOGGER.debug(String.format("Got task %s for training set %s.", task, trainingSetFileName));

        String outputFileName = getModelFileName(trainingSetFileName, model);

        if (LOGGER.isDebugEnabled()) LOGGER.debug(String.format("Running RankLib for model %d. Output filename is %s.", model, outputFileName));

        final Process p;
        try {
            p = run(model, trainingSetFileName, outputFileName);
            p.waitFor();
        } catch (IOException | InterruptedException e) {
            LOGGER.error("Exception whilst trying to run RankLib, exiting.", e);
            return null;
        }

        int exitCode = p.exitValue();
        if (exitCode != 0) {
            if (LOGGER.isDebugEnabled()) LOGGER.debug(String.format("RankLib exited with code %d, returning null", exitCode));
            return null;
        }

        // Return a model upload task
        String name = String.format("ons_model_%d", model);
        return new ModelUploadTask(task, name, outputFileName);
    }

    private static String getModelFileName(String trainingSetFileName, int model) {
        return trainingSetFileName.replace(".txt", "") + "_model_" + model + ".txt";
    }

    public Process run(int model, String judgementWithFeaturesFile, String modelOutput) throws IOException, InterruptedException {
        String cmd = getCmd(model, judgementWithFeaturesFile, modelOutput);
        if (LOGGER.isDebugEnabled()) LOGGER.debug("Command: " + cmd);
        final Process p = Runtime.getRuntime().exec(cmd);

        new Thread(() -> {
            BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;

            try {
                while ((line = input.readLine()) != null) {
                    if (LOGGER.isDebugEnabled()) LOGGER.debug(line);
                }
            } catch (IOException e) {
                LOGGER.error("Caught IOException whilst attempting to run RankLib", e);
            }
        }).start();

        return p;
//        p.waitFor();
//        return p.exitValue();
    }

    private static String getCmd(int model, String judgementWithFeaturesFile, String modelOutput) throws IOException {
        String cmd = String.format("java -jar %s -ranker %s -train %s -save %s -frate 1.0",
                getPathToRankLib(),
                model,
                judgementWithFeaturesFile,
                modelOutput);
        return cmd;
    }

    private static String getPathToRankLib() throws IOException {
        URL url = RankLibHandler.class.getResource("/lib/RankLib-2.8.jar");
        if (url == null) {
            throw new IOException("Unable to locate RankLib-2.8.jar in the classpath");
        }
        return url.getPath();
    }
}
