package com.github.onsdigital.search.search.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.onsdigital.elasticutils.ml.ranklib.models.Judgement;
import com.github.onsdigital.elasticutils.ml.ranklib.models.Judgements;

import java.util.*;

/**
 * @author sullid (David Sullivan) on 13/12/2017
 * @project dp-search-service
 */
public class SearchHitCounter {

    @JsonIgnore
    private static final float MAX_SCORE = Judgements.MAX_SCORE;

    private int queryId;
    private Map<String, SearchHitCount> urlCountMap;

    public SearchHitCounter(int queryId) {
        this.queryId = queryId;
        this.urlCountMap = new HashMap<>();
    }

    public void add(String url, int rank) {
        if (!urlCountMap.containsKey(url)) {
            SearchHitCount hitCount = new SearchHitCount(rank, 1);
            urlCountMap.put(url, hitCount);
        } else {
            this.increment(url);
        }
    }

    public SearchHitCount get(String url) {
        return this.urlCountMap.get(url);
    }

    public void increment(String url) {
        this.urlCountMap.get(url).increment();
    }

    public Integer getTotalHits() {
        int total = 0;
        for (String url : this.urlCountMap.keySet()) {
            total += this.urlCountMap.get(url).getCount();
        }
        return total;
    }

    public List<Judgement> getJudgementList(String queryTerm) {
        List<Judgement> judgementList = new ArrayList<>();

        // First get the max number of counts
        int maxCount = 0;
        for (String url : this.urlCountMap.keySet()) {
            if (this.urlCountMap.get(url).getCount() > maxCount) {
                maxCount = this.urlCountMap.get(url).getCount();
            }
        }

        // Max count scores a 3 (perfect), normalise down to 1 (irrelevant)
        SortedSet<Map.Entry<String, SearchHitCount>> sortedSet = this.sort();
        Iterator<Map.Entry<String, SearchHitCount>> it = sortedSet.iterator();

        while (it.hasNext()) {
            Map.Entry<String, SearchHitCount> entry = it.next();
            String url = entry.getKey();
            SearchHitCount searchHitCount = entry.getValue();

            int rank = searchHitCount.getRank();
            int count = searchHitCount.getCount();

            float judgementValue = normalise(count, maxCount, MAX_SCORE);
            // Create a new judgement and append to the list
            Judgement judgement = new Judgement(judgementValue, queryId, rank);
            judgement.setComment(String.format("%s:%s", queryTerm, url));
            judgementList.add(judgement);
        }

        Collections.sort(judgementList);
        return judgementList;
    }

    public Judgements getJudgements(String queryTerm) {
        return new Judgements(queryId, this.getJudgementList(queryTerm));
    }

    private static float normalise(int count, int maxCount, float maxScore) {
        return (Float.valueOf(count) / Float.valueOf(maxCount)) * maxScore;
    }

    public SortedSet<Map.Entry<String, SearchHitCount>> sort() {
        return entriesSortedByValues(this.urlCountMap);
    }

    private static <K, V extends Comparable<? super V>> SortedSet<Map.Entry<K, V>> entriesSortedByValues(Map<K, V> map) {
        SortedSet<Map.Entry<K, V>> sortedEntries = new TreeSet<Map.Entry<K, V>>(
                new Comparator<Map.Entry<K, V>>() {
                    @Override
                    public int compare(Map.Entry<K, V> e1, Map.Entry<K, V> e2) {
                        return e1.getValue().compareTo(e2.getValue());
                    }
                });
        sortedEntries.addAll(map.entrySet());
        return sortedEntries;
    }

    @Override
    public String toString() {
        SortedSet<Map.Entry<String, SearchHitCount>> sortedSet = this.sort();
        Iterator<Map.Entry<String, SearchHitCount>> it = sortedSet.iterator();

        List<String> strings = new LinkedList<>();

        while (it.hasNext()) {
            Map.Entry<String, SearchHitCount> entry = it.next();
            SearchHitCount searchHitCount = entry.getValue();
            strings.add(String.format("%d:%d", searchHitCount.getRank(), searchHitCount.getCount()));
        }

        return strings.toString();
    }
}
