package com.github.onsdigital.search.elasticsearch;

import com.github.onsdigital.elasticutils.client.ElasticSearchRESTClient;
import com.github.onsdigital.elasticutils.client.bulk.configuration.BulkProcessorConfiguration;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;

/**
 * @author sullid (David Sullivan) on 28/11/2017
 * @project dp-search-service
 */
public class OpenNlpElasticSearchRESTClient<T> extends ElasticSearchRESTClient<T> {


    public OpenNlpElasticSearchRESTClient(String hostName, String indexName, Class<T> returnClass) {
        super(hostName, indexName, returnClass);
    }

    public OpenNlpElasticSearchRESTClient(String hostName, int http_port, String indexName, Class<T> returnClass) {
        super(hostName, http_port, indexName, returnClass);
    }

    public OpenNlpElasticSearchRESTClient(String hostName, int http_port, String indexName, BulkProcessorConfiguration bulkProcessorConfiguration, Class<T> returnClass) {
        super(hostName, http_port, indexName, bulkProcessorConfiguration, returnClass);
    }

    public OpenNlpElasticSearchRESTClient(String indexName, RestHighLevelClient client, BulkProcessorConfiguration bulkProcessorConfiguration, Class<T> returnClass) {
        super(indexName, client, bulkProcessorConfiguration, returnClass);
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
