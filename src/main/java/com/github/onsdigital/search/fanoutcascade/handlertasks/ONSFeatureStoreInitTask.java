package com.github.onsdigital.search.fanoutcascade.handlertasks;

import com.github.onsdigital.fanoutcascade.handlertasks.HandlerTask;

import java.util.Date;

/**
 * @author sullid (David Sullivan) on 26/12/2017
 * @project dp-search-service
 */
public class ONSFeatureStoreInitTask extends HandlerTask {

    private String featureStore;
    private String featureSetName;
    private Date date;

    public ONSFeatureStoreInitTask(String featureStore, String featureSetName, Date date) {
        super(ONSFeatureStoreInitTask.class);
        this.featureStore = featureStore;
        this.featureSetName = featureSetName;
        this.date = date;
    }

    public String getFeatureStore() {
        return featureStore;
    }

    public String getFeatureSetName() {
        return featureSetName;
    }

    public Date getDate() {
        return date;
    }
}
