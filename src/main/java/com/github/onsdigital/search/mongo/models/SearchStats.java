package com.github.onsdigital.search.mongo.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.onsdigital.mongo.WritableObject;
import com.github.onsdigital.mongo.util.ObjectFinder;
import com.github.onsdigital.mongo.util.ObjectWriter;
import com.github.onsdigital.search.mongo.CollectionNames;
import org.bson.types.ObjectId;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author sullid (David Sullivan) on 12/12/2017
 * @project dp-search-service
 */
public class SearchStats implements WritableObject {

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

    private SearchStats() {
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
    public int getAbsoluteIndex() {
        return (this.pageIndex - 1) * this.pageSize + this.linkIndex;
    }

    @Override
    public ObjectWriter writer() {
        return new ObjectWriter(CollectionNames.SEARCH_STATS, this);
    }

    public static ObjectFinder<SearchStats> finder() {
        return new ObjectFinder<>(CollectionNames.SEARCH_STATS, SearchStats.class);
    }

    @Override
    public ObjectId getObjectId() {
        return this._id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getTerm(), this.getUrl());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SearchStats) {
            SearchStats other = (SearchStats) obj;
            return  (other.getTerm().equals(this.getTerm()) && other.getUrl().equals(this.getUrl()));
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("term:%s : index:%d", this.getTerm(), this.getAbsoluteIndex());
    }

    public static Map<String, Map<SearchStats, Integer>> countClicks() {
        Iterable<SearchStats> it = SearchStats.finder().find();
        return countClicks(it);
    }

    public static Map<String, Map<SearchStats, Integer>> countClicks(Iterable<SearchStats> it) {
        // Init the map
        Map<String, Map<SearchStats, Integer>> countMap = new HashMap<>();

        // Collect all available records from mongo and count how many times a link is clicked for each search term
        for (SearchStats searchStats : it) {
            String term = searchStats.getTerm();
            if (!countMap.containsKey(term)) {
                Map<SearchStats, Integer> counters = new HashMap<>();
                countMap.put(term, counters);
            }

            Map<SearchStats, Integer> counters = countMap.get(term);
            if (!counters.containsKey(searchStats)) {
                counters.put(searchStats, 1);
            } else {
                counters.replace(searchStats, counters.get(searchStats) + 1);
            }
        }

        return countMap;
    }

    public static void main(String[] args) {
        Map<String, Map<SearchStats, Integer>> counts = SearchStats.countClicks();

        String term = "claimant rate wales";
        Map<SearchStats, Integer> claimantCounts = counts.get(term);
//        System.out.println(claimantCounts);

        for (SearchStats key : claimantCounts.keySet()) {
            System.out.println(key + " ; " + key.getPageIndex() + " ; " + key.getLinkIndex() + " : " + claimantCounts.get(key));
        }
    }
}
