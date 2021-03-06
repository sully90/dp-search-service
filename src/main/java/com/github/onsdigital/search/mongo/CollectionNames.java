package com.github.onsdigital.search.mongo;

import com.github.onsdigital.mongo.util.MongoCollectionNames;

/**
 * @author sullid (David Sullivan) on 23/11/2017
 * @project dp-search-service
 */
public enum CollectionNames implements MongoCollectionNames {
    LTR_FEATURE_SET_REQUESTS("ltr_feature_set_requests"),
    SEARCH_STATS("searchstats"),
    JUDGEMENTS("judgements"),
    MODEL_TRAINING_TASKS("model_training_tasks"),
    TOPICS("topics");

    private String name;

    CollectionNames(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
