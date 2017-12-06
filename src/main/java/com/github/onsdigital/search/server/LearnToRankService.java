package com.github.onsdigital.search.server;

import com.github.onsdigital.elasticutils.ml.client.http.LearnToRankClient;
import com.github.onsdigital.elasticutils.ml.client.response.features.LearnToRankGetResponse;
import com.github.onsdigital.elasticutils.ml.client.response.features.LearnToRankListResponse;
import com.github.onsdigital.elasticutils.ml.client.response.features.models.FeatureSet;
import com.github.onsdigital.elasticutils.ml.requests.FeatureSetRequest;
import com.github.onsdigital.elasticutils.ml.util.LearnToRankHelper;
import com.github.onsdigital.search.configuration.SearchEngineProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import static com.github.onsdigital.search.response.HttpResponse.internalServerError;
import static com.github.onsdigital.search.response.HttpResponse.ok;

/**
 * @author sullid (David Sullivan) on 06/12/2017
 * @project dp-search-service
 */

@Path("/ltr")
public class LearnToRankService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LearnToRankService.class);

    private static final String HOSTNAME = "localhost";
    private static final LearnToRankClient client;

    static {
        client = LearnToRankHelper.getLTRClient(HOSTNAME);
    }

    @PUT
    @Path("/featuresets/init")
    @Produces({ MediaType.APPLICATION_JSON })
    public Response initFeatureStore() {
        try {
            List<FeatureSet> featureSets = loadFeatureSets();

            for (FeatureSet featureSet : featureSets) {
                FeatureSetRequest request = new FeatureSetRequest(featureSet);
                client.addFeatureSet(request);
            }
            return ok();
        } catch (IOException e) {
            return internalServerError(e);
        }
    }

    @GET
    @Path("/featuresets/list/")
    @Produces({MediaType.APPLICATION_JSON})
    public Response listFeatureSets() {
        try {
            LearnToRankListResponse response = client.listFeatureSets();
            return ok(response);
        } catch (IOException e) {
            LOGGER.error("Error listing feature sets", e);
            return internalServerError(e);
        }
    }

    @GET
    @Path("/featuresets/list/{name}")
    @Produces({ MediaType.APPLICATION_JSON })
    public Response getFeatureByName(@PathParam("name") String name) {
        try {
            LearnToRankGetResponse response = client.getFeatureSetByName(name);
            return ok(response);
        } catch (IOException e) {
            return internalServerError(e);
        }
    }

    private static List<FeatureSet> loadFeatureSets() throws IOException {
        // Get the location of the featureSet store
        String path = SearchEngineProperties.getProperty("elastic.ltr.featureSets.store");

        // Get from classpath
        File storeDirectory = new File(path);

        if (!storeDirectory.isDirectory()) {
            throw new IOException("Unable to locate featureStore directory.");
        }

        List<FeatureSet> featureSets = new LinkedList<>();

        // Each sub directory is a featureStore, containing a list of features as json files
        File[] featureSetDirectories = storeDirectory.listFiles(File::isDirectory);
        for (File featureSetDirectory : featureSetDirectories) {
            FeatureSet featureSet = FeatureSet.readFromDirectory(featureSetDirectory);
            featureSets.add(featureSet);
        }

        return featureSets;
    }

}
