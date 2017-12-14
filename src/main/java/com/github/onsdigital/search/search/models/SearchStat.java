package com.github.onsdigital.search.search.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.onsdigital.elasticutils.client.generic.ElasticSearchClient;
import com.github.onsdigital.elasticutils.client.type.DefaultDocumentTypes;
import com.github.onsdigital.elasticutils.util.search.ElasticSearchIndex;
import com.github.onsdigital.elasticutils.util.search.ObjectSearcher;
import com.github.onsdigital.elasticutils.util.search.Searchable;
import com.github.onsdigital.mongo.util.FindableObject;
import com.github.onsdigital.mongo.util.ObjectFinder;
import com.github.onsdigital.search.elasticsearch.SearchIndicies;
import com.github.onsdigital.search.mongo.CollectionNames;
import com.github.onsdigital.search.util.SearchClientUtils;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @author sullid (David Sullivan) on 12/12/2017
 * @project dp-search-service
 */
public class SearchStat implements FindableObject, Searchable {

    @JsonIgnore
    private static final SearchIndicies index = SearchIndicies.SEARCH_STATS;

    private ObjectId _id;
    private String url;
    private String term;
    @JsonProperty("listtype")
    private String listType;
    @JsonProperty("pageindex")
    private int pageIndex;
    @JsonProperty("linkindex")
    private int linkIndex;
    @JsonProperty("pagesize")
    private int pageSize;
    @JsonProperty("timestamp")
    private Date timeStamp;

    private SearchStat() {
        // For Jackson
    }

    public String getUrl() {
        return url;
    }

    public String getTerm() {
        return term;
    }

    @JsonProperty("listtype")
    public String getListType() {
        return listType;
    }

    @JsonProperty("pageindex")
    public int getPageIndex() {
        return pageIndex;
    }

    @JsonProperty("linkindex")
    public int getLinkIndex() {
        return linkIndex;
    }

    @JsonProperty("pagesize")
    public int getPageSize() {
        return pageSize;
    }

    @JsonProperty("timestamp")
    public Date getTimeStamp() {
        return timeStamp;
    }

    @JsonIgnore
    public int getRank() {
        return (this.pageIndex - 1) * this.pageSize + this.linkIndex;
    }

    @Override
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public ObjectId getObjectId() {
        return this._id;
    }

    @Override
    public ElasticSearchIndex getIndex() {
        return index;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getTerm(), this.getUrl());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SearchStat) {
            SearchStat other = (SearchStat) obj;
            return  (other.getTerm().equals(this.getTerm()) && other.getUrl().equals(this.getUrl()));
        }
        return false;
    }

    public static ObjectFinder<SearchStat> finder() {
        return new ObjectFinder<>(CollectionNames.SEARCH_STATS, SearchStat.class);
    }

    public static ObjectSearcher<SearchStat> search(ElasticSearchClient<SearchStat> client) {
        return new ObjectSearcher<>(client, SearchIndicies.SEARCH_STATS, SearchStat.class);
    }

    public static void syncElasticSearchAndMongo() {
        // Syncs mongoDB and Elasticsearch, treating mongo as the master
        Iterable<SearchStat> it = SearchStat.finder().find();
        List<SearchStat> searchStats = new ArrayList<>();
        it.forEach(searchStats::add);

        if (searchStats.size() == 0) {
            return;
        }

        String indexName = searchStats.get(0).getIndex().getIndexName();

        try (ElasticSearchClient<SearchStat> searchClient = SearchClientUtils.getSearchClient()) {
            // Drop the existing index, if it exists
            if (searchClient.indexExists(indexName)) {
                searchClient.dropIndex(indexName);
            }
            // Index the documents
            searchClient.bulk(indexName, DefaultDocumentTypes.DOCUMENT, searchStats);
            searchClient.awaitClose(1, TimeUnit.MINUTES);
        } catch (Exception e) {
            throw new RuntimeException(e);  // Throw as RuntimeException
        }
    }

    public static void main(String[] args) {
        SearchStat.syncElasticSearchAndMongo();
    }

}
