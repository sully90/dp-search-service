package com.github.onsdigital.search.search.models;

/**
 * @author sullid (David Sullivan) on 18/12/2017
 * @project dp-search-service
 */
public class SearchHitCount implements Comparable<SearchHitCount> {

    private int rank;
    private int count;

    public SearchHitCount(int rank) {
        this(rank, 0);
    }

    public SearchHitCount(int rank, int count) {
        this.rank = rank;
        this.count = count;
    }

    private SearchHitCount() {
        // For Jackson
    }

    public void increment() {
        this.count++;
    }

    public int getRank() {
        return rank;
    }

    public int getCount() {
        return count;
    }

    @Override
    public int compareTo(SearchHitCount o) {
        return this.rank - o.getRank();
    }
}
