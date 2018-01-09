package com.github.onsdigital.search.fanoutcascade.handlertasks;

import com.github.onsdigital.fanoutcascade.handlertasks.HandlerTask;
import com.github.onsdigital.search.search.models.SearchHitCounter;

import java.util.Date;
import java.util.Map;

/**
 * @author sullid (David Sullivan) on 26/12/2017
 * @project dp-search-service
 */
public class ONSFeatureStoreInitTask extends HandlerTask {

    private String featureStore;
    private String featureSetName;
    private Map<String, SearchHitCounter> uniqueHits;
    private Date date;

    public ONSFeatureStoreInitTask(String featureStore, String featureSetName, Map<String, SearchHitCounter> uniqueHits, Date date) {
        super(ONSFeatureStoreInitTask.class);
        this.featureStore = featureStore;
        this.featureSetName = featureSetName;
        this.uniqueHits = uniqueHits;
        this.date = date;
    }

    public String getFeatureStore() {
        return featureStore;
    }

    public String getFeatureSetName() {
        return featureSetName;
    }

    public Map<String, SearchHitCounter> getUniqueHits() {
        return uniqueHits;
    }

    public Date getDate() {
        return date;
    }
}
