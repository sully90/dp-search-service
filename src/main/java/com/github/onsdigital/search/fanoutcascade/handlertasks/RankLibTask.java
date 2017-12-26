package com.github.onsdigital.search.fanoutcascade.handlertasks;

import com.github.onsdigital.fanoutcascade.handlertasks.HandlerTask;

/**
 * @author sullid (David Sullivan) on 25/12/2017
 * @project dp-search-service
 */
public class RankLibTask extends HandlerTask {

    private String featureStore;
    private String featureSet;
    private String trainingSetFileName;
    private int model;

    public RankLibTask(String featureStore, String featureSet, String trainingSetFileName, int model) {
        super(RankLibTask.class);
        this.featureStore = featureStore;
        this.featureSet = featureSet;
        this.trainingSetFileName = trainingSetFileName;
        this.model = model;
    }

    public String getFeatureStore() {
        return featureStore;
    }

    public String getFeatureSet() {
        return featureSet;
    }

    public String getTrainingSetFileName() {
        return trainingSetFileName;
    }

    public int getModel() {
        return model;
    }
}
