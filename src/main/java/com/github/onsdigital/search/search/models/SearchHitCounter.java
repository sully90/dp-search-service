package com.github.onsdigital.search.search.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.onsdigital.elasticutils.ml.ranklib.models.Judgement;

import java.util.*;

/**
 * @author sullid (David Sullivan) on 13/12/2017
 * @project dp-search-service
 */
public class SearchHitCounter {

    @JsonIgnore
    private static final int MAX_SCORE = 3;

    private Map<String, Integer> urlCountMap;

    public SearchHitCounter() {
        this.urlCountMap = new HashMap<>();
    }

    public Integer add(String url) {
        if (!urlCountMap.containsKey(url)) {
            return urlCountMap.put(url, 1);
        } else {
            return this.increment(url);
        }
    }

    public Integer get(String url) {
        return this.urlCountMap.get(url);
    }

    public Integer increment(String url) {
        return this.urlCountMap.replace(url, this.urlCountMap.get(url) + 1);
    }

    public Integer getTotalHits() {
        int total = 0;
        for (String url : this.urlCountMap.keySet()) {
            total += this.urlCountMap.get(url);
        }
        return total;
    }

    public List<Judgement> getJudgementList(int queryId) {
        List<Judgement> judgementList = new ArrayList<>();

        // First get the max number of counts
        int maxCount = 0;
        for (String url : this.urlCountMap.keySet()) {
            if (this.urlCountMap.get(url) > maxCount) {
                maxCount = this.urlCountMap.get(url);
            }
        }

        // Max count scores a 3 (perfect), normalise down to 1 (irrelevant)
        for (String url : this.urlCountMap.keySet()) {
            int count = this.urlCountMap.get(url);
            float judgement = normalise(count, maxCount, MAX_SCORE);
            // Create a new judgement and append to the list
            judgementList.add(new Judgement(judgement, queryId));
        }

        return judgementList;
    }

    private static float normalise(int count, int maxCount, int maxScore) {
        return (Float.valueOf(count) / Float.valueOf(maxCount)) * Float.valueOf(maxScore);
    }

    @Override
    public String toString() {
        return this.urlCountMap.toString();
    }
}
