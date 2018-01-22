package com.github.onsdigital.search.search.models;

import com.github.onsdigital.elasticutils.ml.ranklib.models.Judgements;
import com.github.onsdigital.mongo.WritableObject;
import com.github.onsdigital.mongo.util.ObjectFinder;
import com.github.onsdigital.mongo.util.ObjectWriter;
import com.github.onsdigital.search.mongo.CollectionNames;
import org.bson.types.ObjectId;

import java.util.Date;

/**
 * @author sullid (David Sullivan) on 18/12/2017
 * @project dp-search-service
 */
public class WritableJudgements implements WritableObject {

    private ObjectId _id;
    private String term;
    private Judgements judgements;
    private Date timeStamp;

    public WritableJudgements(String term, Judgements judgements, Date timeStamp) {
        this.term = term;
        this.judgements = judgements;
        this.timeStamp = timeStamp;
    }

    private WritableJudgements() {
        // For jackson
    }

    public String getTerm() {
        return term;
    }

    public Judgements getJudgements() {
        return judgements;
    }

    public Date getTimeStamp() {
        return timeStamp;
    }

    @Override
    public ObjectWriter writer() {
        return new ObjectWriter(CollectionNames.JUDGEMENTS, this);
    }

    @Override
    public ObjectId getObjectId() {
        return this._id;
    }

    public static ObjectFinder<WritableJudgements> finder() {
        return new ObjectFinder<>(CollectionNames.JUDGEMENTS, WritableJudgements.class);
    }
}
