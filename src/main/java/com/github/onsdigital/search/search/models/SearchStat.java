package com.github.onsdigital.search.search.models;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Date;

/**
 * @author sullid (David Sullivan) on 12/12/2017
 * @project dp-search-service
 */
public class SearchStat {

    private Date created;
    private String url;
    private String term;
    private String listType;
    private String gaID;
    private int pageIndex;
    private int linkIndex;
    private int pageSize;

    protected SearchStat() {
        // For Jackson
    }

    public Date getCreated() {
        return created;
    }

    public String getUrl() {
        return url;
    }

    public String getTerm() {
        return term;
    }

    public String getListType() {
        return listType;
    }

    public String getGaID() {
        return gaID;
    }

    public int getPageIndex() {
        return pageIndex;
    }

    public int getLinkIndex() {
        return linkIndex;
    }

    public int getPageSize() {
        return pageSize;
    }

    @JsonIgnore
    public int getRank() {
        return (this.pageIndex - 1) * this.pageSize + this.linkIndex;
    }
}
