package com.github.onsdigital.search.elasticsearch.indicies;

import com.github.onsdigital.elasticutils.indicies.ElasticIndexNames;
import com.github.onsdigital.search.exceptions.NoSuchIndexException;

/**
 * @author sullid (David Sullivan) on 22/11/2017
 * @project dp-search-service
 */
public enum ElasticSearchIndex implements ElasticIndexNames {
    TEST("test");

    private String indexName;

    ElasticSearchIndex(String indexName) {
        this.indexName = indexName;
    }

    public String getIndexName() {
        return this.indexName;
    }

    public static ElasticSearchIndex forIndexName(String indexName) throws NoSuchIndexException {
        for (ElasticSearchIndex index : ElasticSearchIndex.values()) {
            if (index.getIndexName().equals(indexName)) {
                return index;
            }
        }
        throw new NoSuchIndexException(indexName);
    }
}
