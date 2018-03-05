package com.github.onsdigital.search.searchstats;

import com.github.onsdigital.search.mongo.models.MongoSearchStat;
import com.github.onsdigital.search.search.models.SearchStat;

import java.util.LinkedList;
import java.util.List;

/**
 * @author sullid (David Sullivan) on 05/03/2018
 * @project dp-search-service
 */
public class MongoSearchStatsLoader implements SearchStatsLoader {
    @Override
    public List<SearchStat> getSearchStats() {
        // Loads all search stats from mongoDB
        Iterable<MongoSearchStat> it = MongoSearchStat.finder().find();

        List<SearchStat> searchStats = new LinkedList<>();
        it.forEach(searchStats::add);
        return searchStats;
    }
}
