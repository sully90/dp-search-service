package com.github.onsdigital.search.fanoutcascade.handlertasks;

import com.github.onsdigital.fanoutcascade.handlertasks.HandlerTask;

import java.util.Date;

/**
 * @author sullid (David Sullivan) on 25/12/2017
 * @project dp-search-service
 */
public class RankLibTask extends HandlerTask {

    private String featureStore;
    private String featureSet;
    private Date date;
    private int model;

    public RankLibTask(String featureStore, String featureSet, Date date, int model) {
        super(RankLibTask.class);
        this.featureStore = featureStore;
        this.featureSet = featureSet;
        this.date = date;
        this.model = model;
    }

    public String getFeatureStore() {
        return featureStore;
    }

    public String getFeatureSet() {
        return featureSet;
    }

    public Date getDate() {
        return date;
    }

    public int getModel() {
        return model;
    }
}
