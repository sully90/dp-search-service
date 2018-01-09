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
import java.util.*;

import static com.github.onsdigital.search.response.HttpResponse.internalServerError;
import static com.github.onsdigital.search.response.HttpResponse.ok;

/**
 * @author sullid (David Sullivan) on 06/12/2017
 * @project dp-search-service
 */

@Path("/ltr")
public class LearnToRankService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LearnToRankService.class);

    private static final String HOSTNAME_KEY = "elastic.hostname";

    public static final String HOSTNAME;

    static {
        if (SearchEngineProperties.getProperty(HOSTNAME_KEY) != null) {
            HOSTNAME = SearchEngineProperties.getProperty(HOSTNAME_KEY);
        } else {
            // Default to localhost
            HOSTNAME = "localhost";
        }
    }

    @PUT
    @Path("/featuresets/init/")
    @Produces({ MediaType.APPLICATION_JSON })
    public Response initFeatureStore() {
        try (LearnToRankClient client = LearnToRankHelper.getLTRClient(HOSTNAME)) {
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
        } catch (Exception e) {
            return internalServerError(e);
        }
    }

    @GET
    @Path("/featuresets/list")
    @Produces({MediaType.APPLICATION_JSON})
    public Response listFeatureSets() {
        // Lists feature sets for all feature stores
        Map<String, LearnToRankListResponse<FeatureSetRequest>> featureSetMapping = new LinkedHashMap<>();

        try (LearnToRankClient client = LearnToRankHelper.getLTRClient(HOSTNAME)) {
            Set<String> featureStores = client.listFeatureStores();

            for (String featureStore : featureStores) {
                if ("_default_".equals(featureStore)) {
                    continue;
                }
                LearnToRankListResponse<FeatureSetRequest> learnToRankListResponse = client.listFeatureSets(featureStore);
                featureSetMapping.put(featureStore, learnToRankListResponse);
            }

            return ok(featureSetMapping);
        } catch (Exception e) {
            LOGGER.error("Error listing feature sets", e);
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
        try (LearnToRankClient client = LearnToRankHelper.getLTRClient(HOSTNAME)) {
            LearnToRankListResponse<FeatureSetRequest> response = client.listFeatureSets(featureStore);
            return ok(response);
        } catch (Exception e) {
            LOGGER.error("Error listing feature sets", e);
            return internalServerError(e);
        }
    }

    @GET
    @Path("/featuresets/list/{featurestore}/{name}")
    @Produces({ MediaType.APPLICATION_JSON })
    public Response getFeatureByName(@PathParam("featurestore") String featureStore, @PathParam("name") String name) {
        try (LearnToRankClient client = LearnToRankHelper.getLTRClient(HOSTNAME)) {
            LearnToRankGetResponse<FeatureSetRequest> response = client.getFeatureSet(featureStore, name);
            return ok(response);
        } catch (Exception e) {
            LOGGER.error("Error retrieving featureset with name: " + name, e);
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
        try (LearnToRankClient client = LearnToRankHelper.getLTRClient(HOSTNAME)) {
            System.out.println(request.toJson(20));
            client.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
