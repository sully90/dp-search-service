package com.github.onsdigital.search.client;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import org.apache.http.HttpStatus;
import org.elasticsearch.common.Strings;
import org.glassfish.jersey.client.ClientConfig;

import javax.ws.rs.Consumes;
import javax.ws.rs.client.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.StringJoiner;

/**
 * @author sullid (David Sullivan) on 08/12/2017
 * @project dp-search-service
 */
public class JerseyClient {

    private Client client;

    private String hostName;
    private int port;
    private String applicationName;

    public JerseyClient(String hostName, int port, String applicationName) {
        this.hostName = hostName;
        this.port = port;
        this.applicationName = applicationName;

        this.client = ClientBuilder.newClient(new ClientConfig().register(JacksonJsonProvider.class));
    }

    @Consumes({MediaType.APPLICATION_JSON})
    public Response get(String path) {
        WebTarget webTarget = this.getWebResource(path);

        Invocation.Builder request = webTarget.request();
        request.header("Content-type", MediaType.APPLICATION_JSON);

        Response response = request.get();
        if (response.getStatus() != HttpStatus.SC_OK) {
            throw new RuntimeException("JerseyClient: Got non 200 status code: " + response.getStatus());
        }

        return response;
    }

    @Consumes({ MediaType.APPLICATION_JSON })
    public Response post(String path, Object input) throws IOException {
        WebTarget webTarget = this.getWebResource(path);

        Invocation.Builder request = webTarget.request();
        request.header("Content-type", MediaType.APPLICATION_JSON);

        Response response = request.post(Entity.entity(input, MediaType.APPLICATION_JSON));

        if (response.getStatus() != HttpStatus.SC_OK) {
            throw new RuntimeException("JerseyClient: Got non 201 status code: " + response.getStatus());
        }

        return response;
    }

    private WebTarget getWebResource(String path) {
        String api = endpoint(path);
        System.out.println("API for path " + path + " : " + api);
        return client.target(api);

    }

    private String getUrl() {
        StringBuilder sb = new StringBuilder("http://")
                .append(this.hostName)
                .append(":")
                .append(this.port)
                .append("/")
                .append(this.applicationName)
                .append("/");
        return sb.toString();
    }

    /**
     * Utility method to build request's endpoint.
     */
    private String endpoint(String... parts) {
        StringJoiner joiner = new StringJoiner("/", this.getUrl(), "");
        for (String part : parts) {
            if (Strings.hasLength(part)) {
                joiner.add(part);
            }
        }
        return joiner.toString();
    }

}
