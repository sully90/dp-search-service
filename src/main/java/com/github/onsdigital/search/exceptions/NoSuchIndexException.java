package com.github.onsdigital.search.exceptions;

/**
 * @author sullid (David Sullivan) on 22/11/2017
 * @project dp-search-service
 */
public class NoSuchIndexException extends Exception {

    public NoSuchIndexException(String indexName) {
        super(String.format("Unable to find Elasticsearch index with name %s", indexName));
    }

}
