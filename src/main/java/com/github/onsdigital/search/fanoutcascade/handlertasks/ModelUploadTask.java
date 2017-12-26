package com.github.onsdigital.search.fanoutcascade.handlertasks;

import com.github.onsdigital.fanoutcascade.handlertasks.HandlerTask;

/**
 * @author sullid (David Sullivan) on 26/12/2017
 * @project dp-search-service
 */
public class ModelUploadTask extends HandlerTask {

    private String featureStore;
    private String featureSet;
    private String name;
    private String modelFileName;

    public ModelUploadTask(RankLibTask rankLibTask, String name, String modelFileName) {
        super(ModelUploadTask.class);
        this.featureStore = rankLibTask.getFeatureStore();
        this.featureSet = rankLibTask.getFeatureSet();
        this.name = name;
        this.modelFileName = modelFileName;
    }

    public String getFeatureStore() {
        return featureStore;
    }

    public String getFeatureSet() {
        return featureSet;
    }

    public String getName() {
        return name;
    }

    public String getModelFileName() {
        return modelFileName;
    }
}
