package com.github.onsdigital.search.server;

import com.github.onsdigital.elasticutils.client.bulk.configuration.BulkProcessorConfiguration;
import com.github.onsdigital.elasticutils.client.generic.ElasticSearchClient;
import com.github.onsdigital.elasticutils.client.generic.RestSearchClient;
import com.github.onsdigital.elasticutils.client.generic.TransportSearchClient;
import com.github.onsdigital.elasticutils.client.http.SimpleRestClient;
import com.github.onsdigital.elasticutils.util.ElasticSearchHelper;
import com.github.onsdigital.search.elasticsearch.indicies.ElasticSearchIndex;
import com.github.onsdigital.search.exceptions.NoSuchIndexException;
import com.github.onsdigital.search.nlp.opennlp.OpenNlpService;
import com.github.onsdigital.search.util.ClientType;
import org.elasticsearch.client.transport.TransportClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.UnknownHostException;

import static com.github.onsdigital.search.response.HttpResponse.internalServerError;
import static com.github.onsdigital.search.response.HttpResponse.ok;
import static com.github.onsdigital.search.util.SearchClientUtils.getSearchClient;

/**
 * @author sullid (David Sullivan) on 22/11/2017
 * @project dp-search-service
 */

@Path("/search")
public class SearchEngineService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SearchEngineService.class);

    private static final OpenNlpService OPENNLP_SERVICE = OpenNlpService.getInstance();

    @GET
    @Path("/{index}/{query}")
    @Produces({ MediaType.APPLICATION_JSON })
    public Response searchIndex(@PathParam("index") String index, @PathParam("query") String query) {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("searchIndex received request on index '{}' with query '{}'.", index, query);
        }

        ElasticSearchIndex elasticSearchIndex;
        try {
            elasticSearchIndex = ElasticSearchIndex.forIndexName(index);
        } catch (NoSuchIndexException e) {
            LOGGER.error("searchIndex unable to locate index '{}' in ElasticSearchIndex enum.", index, e);
            return internalServerError(e);
        }

        ElasticSearchClient searchClient = getSearchClient();

//        System.out.println(index);
        System.out.println(elasticSearchIndex + " : " + OPENNLP_SERVICE.getNamedEntities(query));
        return ok();
    }

}
