package com.github.onsdigital.search.fanoutcascade.handlertasks;

import com.github.onsdigital.fanoutcascade.handlertasks.HandlerTask;
import com.github.onsdigital.mongo.WritableObject;
import com.github.onsdigital.mongo.util.ObjectFinder;
import com.github.onsdigital.mongo.util.ObjectWriter;
import com.github.onsdigital.search.mongo.CollectionNames;
import com.github.onsdigital.search.search.models.SearchHitCounter;
import org.bson.types.ObjectId;

import java.util.Date;
import java.util.Map;

/**
 * @author sullid (David Sullivan) on 22/12/2017
 * @project dp-search-service
 */
public class TrainingSetTask extends HandlerTask implements WritableObject {

    private ObjectId _id;
    private Map<String, SearchHitCounter> uniqueHits;
    private Date date;

    public TrainingSetTask(Map<String, SearchHitCounter> uniqueHits, Date date) {
        super(TrainingSetTask.class);
        this.uniqueHits = uniqueHits;
        this.date = date;
    }

    private TrainingSetTask() {
        super(TrainingSetTask.class);
    }

    public Map<String, SearchHitCounter> getUniqueHits() {
        return uniqueHits;
    }

    public Date getDate() {
        return date;
    }

    @Override
    public ObjectWriter writer() {
        return new ObjectWriter(CollectionNames.MODEL_TRAINING_TASKS, this);
    }

    @Override
    public ObjectId getObjectId() {
        return this._id;
    }

    public static ObjectFinder<TrainingSetTask> finder() {
        return new ObjectFinder<>(CollectionNames.MODEL_TRAINING_TASKS, TrainingSetTask.class);
    }
}
