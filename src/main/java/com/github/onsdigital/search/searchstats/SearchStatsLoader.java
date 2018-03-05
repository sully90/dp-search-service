package com.github.onsdigital.search.searchstats;

import com.github.onsdigital.search.search.models.SearchStat;

import java.util.List;

/**
 * @author sullid (David Sullivan) on 05/03/2018
 * @project dp-search-service
 */
public interface SearchStatsLoader {

    List<SearchStat> getSearchStats();

}
