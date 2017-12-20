package com.github.onsdigital.search.elasticsearch;

import com.github.onsdigital.elasticutils.client.bulk.configuration.BulkProcessorConfiguration;
import com.github.onsdigital.elasticutils.client.generic.TransportSearchClient;
import com.github.onsdigital.elasticutils.client.pipeline.Pipeline;
import com.github.onsdigital.elasticutils.client.type.DocumentType;
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

    public IndexRequest createIndexRequest(String index, DocumentType documentType, byte[] messageBytes) {
        return super.createIndexRequestWithPipeline(index, documentType, Pipeline.OPENNLP, messageBytes, XContentType.JSON);
    }

    @Override
    public IndexRequest createIndexRequest(String index, DocumentType documentType, byte[] messageBytes, XContentType xContentType) {
        return super.createIndexRequestWithPipeline(index, documentType, Pipeline.OPENNLP, messageBytes, xContentType);
    }

    public enum Pipeline implements com.github.onsdigital.elasticutils.client.pipeline.Pipeline {
        OPENNLP("opennlp-pipeline");

        private String pipelineName;

        Pipeline(String pipelineName) {
            this.pipelineName = pipelineName;
        }

        public String getPipeline() {
            return pipelineName;
        }
    }
}
