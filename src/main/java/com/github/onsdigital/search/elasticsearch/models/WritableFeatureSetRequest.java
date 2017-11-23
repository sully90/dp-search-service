package com.github.onsdigital.search.elasticsearch.models;

import com.github.onsdigital.elasticutils.ml.features.Feature;
import com.github.onsdigital.elasticutils.ml.features.FeatureSet;
import com.github.onsdigital.elasticutils.ml.features.Template;
import com.github.onsdigital.elasticutils.ml.requests.FeatureSetRequest;
import com.github.onsdigital.elasticutils.ml.requests.Validation;
import com.github.onsdigital.mongo.WritableObject;
import com.github.onsdigital.mongo.util.DatabaseConnection;
import com.github.onsdigital.mongo.util.DatabaseType;
import com.github.onsdigital.mongo.util.ObjectFinder;
import com.github.onsdigital.mongo.util.ObjectWriter;
import com.github.onsdigital.search.mongo.CollectionNames;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author sullid (David Sullivan) on 23/11/2017
 * @project dp-search-service
 */
public class WritableFeatureSetRequest extends FeatureSetRequest implements WritableObject {

    private ObjectId _id;

    public WritableFeatureSetRequest(FeatureSet featureSet) {
        super(featureSet);
    }

    public WritableFeatureSetRequest(FeatureSet featureSet, Validation validation) {
        super(featureSet, validation);
    }

    private WritableFeatureSetRequest() {
        // For Jackson
    }

    @Override
    public ObjectWriter writer() {
        return new ObjectWriter(DatabaseType.LOCAL, CollectionNames.LTR_FEATURE_SET_REQUESTS, this);
    }

    public static ObjectFinder<WritableFeatureSetRequest> finder() {
        return new ObjectFinder<>(DatabaseType.LOCAL, CollectionNames.LTR_FEATURE_SET_REQUESTS, WritableFeatureSetRequest.class);
    }

    @Override
    public ObjectId getObjectId() {
        return _id;
    }

    public static void main(String[] args) {
        Map<String, String> templateMatch = new HashMap<String, String>() {{
            put("title", "{{keywords}}");
            put("overview", "{{keywords}}");
        }};
        Template template = new Template(templateMatch);

        List<String> params = new ArrayList<String>() {{
            add("keywords");
        }};
        Feature feature = new Feature("title_query", params, template);

        List<Feature> featureList = new ArrayList<Feature>() {{
            add(feature);
        }};

        FeatureSet featureSet = new FeatureSet("java_test_feature_set", featureList);

//        WritableFeatureSetRequest request = new WritableFeatureSetRequest(featureSet);
//        request.writer().save();
        ObjectId id = new ObjectId("5a16d1843c6ebd69ae6b9bab");
        WritableFeatureSetRequest request = WritableFeatureSetRequest.finder().findOne(id);
        System.out.println(request.getFeatureSet().getName());
    }
}
