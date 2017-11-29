package com.github.onsdigital.search.elasticsearch;

import com.github.onsdigital.elasticutils.client.bulk.configuration.BulkProcessorConfiguration;
import com.github.onsdigital.elasticutils.client.generic.RestSearchClient;
import com.github.onsdigital.elasticutils.client.http.SimpleRestClient;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.common.xcontent.XContentType;

/**
 * @author sullid (David Sullivan) on 28/11/2017
 * @project dp-search-service
 */
public class OpenNlpSearchClient<T> extends RestSearchClient<T> {

    public OpenNlpSearchClient(SimpleRestClient client, String index, BulkProcessorConfiguration configuration, Class<T> returnClass) {
        super(client, index, configuration, returnClass);
    }

    @Override
    public IndexRequest createIndexRequest(byte[] messageBytes, XContentType xContentType) {
        return super.createIndexRequestWithPipeline(messageBytes, Pipeline.OPENNLP.getPipelineName(), xContentType);
    }

    public enum Pipeline {
        OPENNLP("opennlp-pipeline");

        private String pipelineName;

        Pipeline(String pipelineName) {
            this.pipelineName = pipelineName;
        }

        public String getPipelineName() {
            return pipelineName;
        }
    }
}
