package com.github.onsdigital.search.fanoutcascade.handlertasks;

import com.github.onsdigital.fanoutcascade.handlertasks.HandlerTask;
import com.github.onsdigital.search.search.SortBy;

/**
 * @author sullid (David Sullivan) on 22/12/2017
 * @project dp-search-service
 */
public class PerformanceCheckerTask extends HandlerTask {

    private SortBy sortBy;

    public PerformanceCheckerTask(SortBy sortBy) {
        super(PerformanceCheckerTask.class);
        this.sortBy = sortBy;
    }

    public SortBy getSortBy() {
        return sortBy;
    }
}
