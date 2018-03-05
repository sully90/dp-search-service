package com.github.onsdigital.search.mongo.models;

import com.github.onsdigital.mongo.util.FindableObject;
import com.github.onsdigital.mongo.util.ObjectFinder;
import com.github.onsdigital.search.mongo.CollectionNames;
import com.github.onsdigital.search.search.models.SearchStat;
import org.bson.types.ObjectId;

/**
 * @author sullid (David Sullivan) on 05/03/2018
 * @project dp-search-service
 */
public class MongoSearchStat extends SearchStat implements FindableObject {

    private ObjectId _id;

    private MongoSearchStat() {
        super();
    }

    @Override
    public ObjectId getObjectId() {
        return this._id;
    }

    public static ObjectFinder<MongoSearchStat> finder() {
        return new ObjectFinder<MongoSearchStat>(CollectionNames.SEARCH_STATS, MongoSearchStat.class);
    }
}
