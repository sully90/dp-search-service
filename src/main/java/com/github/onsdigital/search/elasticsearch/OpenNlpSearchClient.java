package com.github.onsdigital.search.elasticsearch;

import com.github.onsdigital.elasticutils.client.bulk.configuration.BulkProcessorConfiguration;
import com.github.onsdigital.elasticutils.client.generic.TransportSearchClient;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.xcontent.XContentType;

/**
 * @author sullid (David Sullivan) on 28/11/2017
 * @project dp-search-service
 */
public class OpenNlpSearchClient extends TransportSearchClient {

    public OpenNlpSearchClient(TransportClient client, BulkProcessorConfiguration configuration) {
        super(client, configuration);
    }

    public IndexRequest createIndexRequest(String index, byte[] messageBytes) {
        return super.createIndexRequestWithPipeline(index, messageBytes, Pipeline.OPENNLP.getPipelineName(), XContentType.JSON);
    }

    @Override
    public IndexRequest createIndexRequest(String index, byte[] messageBytes, XContentType xContentType) {
        return super.createIndexRequestWithPipeline(index, messageBytes, Pipeline.OPENNLP.getPipelineName(), xContentType);
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
