package com.github.onsdigital.search.util;

import com.github.onsdigital.elasticutils.client.bulk.configuration.BulkProcessorConfiguration;
import com.github.onsdigital.elasticutils.client.generic.ElasticSearchClient;
import com.github.onsdigital.elasticutils.client.generic.RestSearchClient;
import com.github.onsdigital.elasticutils.client.generic.TransportSearchClient;
import com.github.onsdigital.elasticutils.client.http.SimpleRestClient;
import com.github.onsdigital.elasticutils.util.ElasticSearchHelper;
import org.elasticsearch.client.transport.TransportClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.UnknownHostException;

/**
 * @author sullid (David Sullivan) on 13/12/2017
 * @project dp-search-service
 */
public class SearchClientUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(SearchClientUtils.class);

    private static final String HOST_NAME = "localhost";

    public static ElasticSearchClient getSearchClient() {
        return getSearchClient(ClientType.HTTP);
    }

    public static ElasticSearchClient getSearchClient(ClientType clientType) {

        BulkProcessorConfiguration configuration = ElasticSearchHelper.getDefaultBulkProcessorConfiguration();
        // Default to the Http com.github.onsdigital.search.client
        switch (clientType) {
            case TCP:
                try {
                    TransportClient client = ElasticSearchHelper.getTransportClient(HOST_NAME, 9300);
                    return new TransportSearchClient(client, configuration);
                } catch (UnknownHostException e) {
                    LOGGER.error("Unable to load TCP com.github.onsdigital.search.client", e);
                    throw new RuntimeException(e);
                }
            default:
                SimpleRestClient client = ElasticSearchHelper.getRestClient(HOST_NAME, 9200);
                return new RestSearchClient(client, configuration);
        }
    }

}
