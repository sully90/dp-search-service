package com.github.onsdigital.search.fanoutcascade.handlertasks;

import com.github.onsdigital.fanoutcascade.handlertasks.HandlerTask;
import com.github.onsdigital.search.search.models.SearchHitCounter;

import java.util.Map;

/**
 * @author sullid (David Sullivan) on 22/12/2017
 * @project dp-search-service
 */
public class ModelTrainingTask extends HandlerTask {

    private Map<String, SearchHitCounter> uniqueHits;

    public ModelTrainingTask(Map<String, SearchHitCounter> uniqueHits) {
        super(ModelTrainingTask.class);
        this.uniqueHits = uniqueHits;
    }

    public Map<String, SearchHitCounter> getUniqueHits() {
        return uniqueHits;
    }
}
