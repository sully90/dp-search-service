package com.github.onsdigital.search.server;

import com.github.onsdigital.elasticutils.ml.client.http.LearnToRankClient;
import com.github.onsdigital.elasticutils.ml.client.response.features.LearnToRankGetResponse;
import com.github.onsdigital.elasticutils.ml.client.response.features.LearnToRankListResponse;
import com.github.onsdigital.elasticutils.ml.client.response.features.models.FeatureSet;
import com.github.onsdigital.elasticutils.ml.client.response.sltr.SltrResponse;
import com.github.onsdigital.elasticutils.ml.query.SltrQueryBuilder;
import com.github.onsdigital.elasticutils.ml.ranklib.models.ModelType;
import com.github.onsdigital.elasticutils.ml.ranklib.models.RankLibModel;
import com.github.onsdigital.elasticutils.ml.requests.FeatureSetRequest;
import com.github.onsdigital.elasticutils.ml.requests.LogQuerySearchRequest;
import com.github.onsdigital.elasticutils.ml.util.JsonUtils;
import com.github.onsdigital.elasticutils.ml.util.LearnToRankHelper;
import com.github.onsdigital.search.configuration.SearchEngineProperties;
import org.apache.commons.io.IOUtils;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.WrapperQueryBuilder;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.glassfish.jersey.server.mvc.Viewable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
        Runtime.getRuntime().addShutdownHook(new LearnToRankClient.ShutDownClientThread(client));
    }

    @PUT
    @Path("/featuresets/init/")
    @Produces({ MediaType.APPLICATION_JSON })
    public Response initFeatureStore() {
        try {
            Map<String, List<FeatureSet>> featureSets = loadFeatureSets();

            for (String featureStore : featureSets.keySet()) {
                for (FeatureSet featureSet : featureSets.get(featureStore)) {
                    if (!client.featureStoreExists(featureStore)) {
                        client.initFeatureStore(featureStore);
                    }

                    if (client.featureSetExists(featureStore, featureSet.getName())) {
                        client.deleteFeatureSet(featureStore, featureSet.getName());
                    }
                    FeatureSetRequest request = new FeatureSetRequest(featureSet);
                    client.createFeatureSet(featureStore, request);
                }
            }
            return ok();
        } catch (IOException e) {
            return internalServerError(e);
        }
    }

    @GET
    @Path("/featuresets/list/{featurestore}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response listFeatureSets(@PathParam("featurestore") String featureStore) {
        Map<String, String> params = new HashMap<String, String>() {{
           put("keywords", "rpi");
        }};
        try {
            LearnToRankListResponse<FeatureSetRequest> response = client.listFeatureSets(featureStore);
            return ok(response);
        } catch (IOException e) {
            LOGGER.error("Error listing feature sets", e);
            return internalServerError(e);
        }
    }

    @GET
    @Path("/featuresets/list/{featurestore}/{name}")
    @Produces({ MediaType.APPLICATION_JSON })
    public Response getFeatureByName(@PathParam("featurestore") String featureStore, @PathParam("name") String name) {
        try {
            LearnToRankGetResponse<FeatureSetRequest> response = client.getFeatureSet(featureStore, name);
            return ok(response);
        } catch (IOException e) {
            LOGGER.error("Error retrieving featureset with name: " + name, e);
            return internalServerError(e);
        }
    }

    @POST
    @Path("/sltr/{index}/{featureset}/")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public Response sltr(@PathParam("index") String index,
                         @PathParam("featureset") String featureSet,
                         Map<String, Object> qbMap) {

        if (!qbMap.containsKey("params") || !(qbMap.get("params") instanceof Map)) {
            return internalServerError("Must supply keywords Map in query map");
        }

        // Construct the base query object

        if (!qbMap.containsKey("query")) {
            return internalServerError("Must supply query string");
        }

        WrapperQueryBuilder qb;
        try {
            String queryString = JsonUtils.toJson(qbMap.get("query"));
            qb = QueryBuilders.wrapperQuery(queryString);
        } catch (IOException e) {
            return internalServerError(e);
        }

        // Construct the LoggingQuery
        String store = "";
        if (qbMap.containsKey("store") && qbMap.get("store") instanceof String) {
            store = (String) qbMap.get("store");
        }

        String logName = "logged_featureset";
        if (qbMap.containsKey("logName") && qbMap.get("logName") instanceof String) {
            logName = (String) qbMap.get("logName");
        }
        SltrQueryBuilder sltrQueryBuilder = new SltrQueryBuilder(logName, featureSet);
        if (!store.isEmpty()) {
            sltrQueryBuilder.setStore(store);
        }
        Map<String, String> keywordsMap = (Map<String, String>) qbMap.get("params");

        for (String key : keywordsMap.keySet()) {
            sltrQueryBuilder.setParam(key, keywordsMap.get(key));
        }

        LogQuerySearchRequest logQuerySearchRequest = LogQuerySearchRequest.getRequestForQuery(qb, sltrQueryBuilder);

        try {
            SltrResponse response = client.search(index, logQuerySearchRequest);
            return ok(response);
        } catch (IOException e) {
            String message = String.format("Error performing sltr on index: %s, featureset: %s", index, featureSet);
            LOGGER.error(message, e);
            return internalServerError(e);
        }
    }

    @GET
    @Path("/model/")
    @Produces({ MediaType.TEXT_HTML })
    public Viewable uploadPage() {
        return new Viewable("/upload", null);
    }

    @POST
    @Path("/model/upload")
    @Consumes({ MediaType.MULTIPART_FORM_DATA })
//    @Produces({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.TEXT_HTML })
    public Viewable uploadModel(@FormDataParam("model_name") String modelName,
                                @FormDataParam("model_type") String modelType,
                                @FormDataParam("file") InputStream inputStream) throws IOException {
        String content = IOUtils.toString(inputStream, "utf-8");

        ModelType type = ModelType.fromString(String.format("model/%s", modelType));
        RankLibModel model = new RankLibModel(modelName, type, content);
        return new Viewable("/done", null);
    }

    private static Map<String, List<FeatureSet>> loadFeatureSets() throws IOException {
        // Get the location of the featureSet store
        String path = SearchEngineProperties.getProperty("elastic.ltr.featureSets.store");

        // Get from classpath
        File storeDirectory = new File(path);

        if (!storeDirectory.isDirectory()) {
            throw new IOException("Unable to locate featureStore directory.");
        }

        Map<String, List<FeatureSet>> featureStoreToSetMap = new HashMap<>();

        // First directory name is the featureStore name

        // Each sub directory is a featureStore
        File[] featureStoreDirectorites = storeDirectory.listFiles(File::isDirectory);
        for (File featureStoreDirectory : featureStoreDirectorites) {
            String featureStore = featureStoreDirectory.getName();
            // Each sub-directory from here is a featureSet
            List<FeatureSet> featureSets = new LinkedList<>();
            File[] featureSetDirectories = featureStoreDirectory.listFiles(File::isDirectory);
            for (File featureSetDirectory : featureSetDirectories) {
                // Each sub-directory is a featureSet
                FeatureSet featureSet = FeatureSet.readFromDirectory(featureSetDirectory);
                featureSets.add(featureSet);
            }
            featureStoreToSetMap.put(featureStore, featureSets);
        }
//        for (File featureSetDirectory : featureSetDirectories) {

//        }

        return featureStoreToSetMap;
    }

    public static void main(String[] args) {
        String keywords = "rpi";
        QueryBuilder qb = QueryBuilders.boolQuery()
                .should(QueryBuilders.matchPhraseQuery("description.summary", keywords))
                .should(QueryBuilders.matchPhraseQuery("description.keywords", keywords))
                .should(QueryBuilders.multiMatchQuery(keywords, "entities.persons",
                        "entities.organizations",
                        "entities.dates",
                        "entities.locations"));

        SltrQueryBuilder sltrQueryBuilder = new SltrQueryBuilder("logged_featureset", "page_features");
        sltrQueryBuilder.setParam("keywords", keywords);

        LogQuerySearchRequest request = LogQuerySearchRequest.getRequestForQuery(qb, sltrQueryBuilder);
        try {
            System.out.println(request.toJson(20));
            client.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
