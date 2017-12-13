package com.github.onsdigital.search.elasticsearch;

import com.github.onsdigital.elasticutils.util.search.ElasticSearchIndex;

/**
 * @author sullid (David Sullivan) on 13/12/2017
 * @project dp-search-service
 */
public enum SearchIndicies implements ElasticSearchIndex {
    SEARCH_STATS("searchstats");

    private String indexName;

    SearchIndicies(String indexName) {
        this.indexName = indexName;
    }

    @Override
    public String getIndexName() {
        return this.indexName;
    }
}
