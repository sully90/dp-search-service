package com.github.onsdigital.search.search;

/**
 * @author sullid (David Sullivan) on 19/01/2018
 * @project dp-search-service
 */
public enum SortBy {
    RELEVANCE("relevance"),
    RELEASE_DATE("release_date"),
    TITLE("title"),
    LTR("ltr"),
    NONE("none");

    private String sortBy;

    SortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public String getSortBy() {
        return sortBy;
    }
}
