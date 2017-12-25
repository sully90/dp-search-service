package com.github.onsdigital.search.fanoutcascade.handlertasks;

import com.github.onsdigital.fanoutcascade.handlertasks.HandlerTask;

/**
 * @author sullid (David Sullivan) on 25/12/2017
 * @project dp-search-service
 */
public class RankLibTask extends HandlerTask {

    private String trainingSetFileName;
    private int model;

    public RankLibTask(String trainingSetFileName, int model) {
        super(RankLibTask.class);
        this.trainingSetFileName = trainingSetFileName;
        this.model = model;
    }

    public String getTrainingSetFileName() {
        return trainingSetFileName;
    }

    public int getModel() {
        return model;
    }
}
