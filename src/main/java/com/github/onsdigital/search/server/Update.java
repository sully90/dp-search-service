package com.github.onsdigital.search.server;

import com.github.onsdigital.elasticutils.client.bulk.configuration.BulkProcessorConfiguration;
import com.github.onsdigital.elasticutils.client.generic.ElasticSearchClient;
import com.github.onsdigital.elasticutils.client.http.SimpleRestClient;
import com.github.onsdigital.elasticutils.util.ElasticSearchHelper;
import com.github.onsdigital.search.elasticsearch.OpenNlpSearchClient;
import com.github.onsdigital.search.mongo.Movie;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author sullid (David Sullivan) on 28/11/2017
 * @project dp-search-service
 */
public class Update {

    private static ElasticSearchClient getClient(String hostName, String indexName) {
        SimpleRestClient client = ElasticSearchHelper.getRestClient(hostName, 9200);
        BulkProcessorConfiguration configuration = ElasticSearchHelper.getDefaultBulkProcessorConfiguration(200);
        return new OpenNlpSearchClient(client, indexName, configuration, Object.class);
    }

    public static void main(String[] args) {
        try (ElasticSearchClient client = getClient("localhost", "movies")) {

            Iterable<Movie> it = Movie.finder().find();
            List<Movie> movies = new ArrayList<>();
            it.forEach(movies::add);

            client.bulk(movies);
            client.flush();
            client.awaitClose(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
