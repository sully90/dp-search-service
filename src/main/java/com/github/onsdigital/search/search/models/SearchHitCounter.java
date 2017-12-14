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
    private List<Integer> ranks;

    public SearchHitCounter() {
        this.urlCountMap = new LinkedHashMap<>();
        this.ranks = new LinkedList<>();
    }

    public Integer add(String url, int rank) {
        if (!urlCountMap.containsKey(url)) {
            this.ranks.add(rank);
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

    public List<Judgement> getJudgementList(String queryTerm, int queryId) {
        List<Judgement> judgementList = new ArrayList<>();

        // First get the max number of counts
        int maxCount = 0;
        for (String url : this.urlCountMap.keySet()) {
            if (this.urlCountMap.get(url) > maxCount) {
                maxCount = this.urlCountMap.get(url);
            }
        }

        // Max count scores a 3 (perfect), normalise down to 1 (irrelevant)
        Set<String> urlKeySet = this.urlCountMap.keySet();
        List<String> urlList = new LinkedList<>(urlKeySet);
        for (int i = 0; i < urlKeySet.size(); i++) {
            String url = urlList.get(i);
            int rank = this.ranks.get(i);

            int count = this.urlCountMap.get(url);
            float judgementValue = normalise(count, maxCount, MAX_SCORE);
            // Create a new judgement and append to the list
            Judgement judgement = new Judgement(judgementValue, queryId, rank);
            judgement.setComment(String.format("%s:%s", queryTerm, url));
            judgementList.add(judgement);
        }

        return judgementList;
    }

    private static float normalise(int count, int maxCount, int maxScore) {
        return (Float.valueOf(count) / Float.valueOf(maxCount)) * Float.valueOf(maxScore);
    }

    @Override
    public String toString() {
        List<String> strings = new LinkedList<>();

        Set<String> urlKeySet = this.urlCountMap.keySet();
        List<String> urlList = new LinkedList<>(urlKeySet);
        for (int i = 0; i < this.urlCountMap.size(); i++) {
            strings.add(String.format("%d:%s", this.ranks.get(i), urlList.get(i)));
        }

        return strings.toString();
    }
}
