package com.github.onsdigital.search.server;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.onsdigital.elasticutils.ml.client.response.features.LearnToRankListResponse;
import com.github.onsdigital.elasticutils.ml.client.response.sltr.SltrResponse;
import com.github.onsdigital.search.client.JerseyClient;
import com.github.onsdigital.search.configuration.SearchEngineProperties;
import org.apache.http.HttpStatus;
import org.junit.Assert;
import org.junit.Test;

import javax.ws.rs.core.Response;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author sullid (David Sullivan) on 08/12/2017
 * @project dp-search-service
 */
public class TestClient {

    private static final String HOSTNAME = "localhost";
    private static final int PORT = 8080;
    private static final String APPNAME = "SearchEngine/api";
    private static final JerseyClient client;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        client = new JerseyClient(HOSTNAME, PORT, APPNAME);
    }

    @Test
    public void testGet() {
        String path = "ltr/featuresets/list/";
        Response response = client.get(path);
        assertEquals(response.getStatus(), HttpStatus.SC_OK);

        LearnToRankListResponse listResponse = response.readEntity(LearnToRankListResponse.class);
        assertFalse(listResponse.isTimedOut());
    }

    private Map<String, Object> getQueryJsonFilename() throws IOException {
        String lrtConfigPath = SearchEngineProperties.getProperty("elastic.ltr.store");
        String fileName = "queries/example_query_rpi.json";

        File file = new File(lrtConfigPath + "/" + fileName);
        Map<String, Object> qbMap = MAPPER.readValue(file, new TypeReference<Map<String, Object>>(){});
        return qbMap;
    }

    @Test
    public void testPost() {
        String path = "ltr/sltr/bulletin/page_featureset/rpi";

        Map<String, Object> qbMap = null;
        try {
            qbMap = getQueryJsonFilename();
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }

        Response response = null;
        try {
            response = client.post(path, qbMap);
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
        assertEquals(response.getStatus(), HttpStatus.SC_OK);

        SltrResponse sltrResponse = response.readEntity(SltrResponse.class);
        assertFalse(sltrResponse.isTimedOut());
    }

}
