package com.github.onsdigital.search.search;

import com.github.onsdigital.elasticutils.client.generic.ElasticSearchClient;
import com.github.onsdigital.elasticutils.ml.ranklib.models.Judgement;
import com.github.onsdigital.search.search.models.SearchHitCounter;
import com.github.onsdigital.search.search.models.SearchStat;
import com.github.onsdigital.search.util.SearchClientUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @author sullid (David Sullivan) on 13/12/2017
 * @project dp-search-service
 *
 * Scores the performance of the Search Engine using the searchstats collection
 */
public class PerformanceChecker {

    private List<SearchStat> searchStats;

    public PerformanceChecker(List<SearchStat> searchStats) {
        this.searchStats = searchStats;
    }

    public Map<String, SearchHitCounter> getUniqueHitCounts() {
        Map<String, SearchHitCounter> hitCounts = new HashMap<>();

        for (SearchStat searchStat : this.searchStats) {
            String term = searchStat.getTerm();
            if (!hitCounts.containsKey(term)) {
                hitCounts.put(term, new SearchHitCounter());
            }
            hitCounts.get(term).add(searchStat.getUrl());
        }
        return hitCounts;
    }

    // TODO improve qid logic
    public Map<String, List<Judgement>> getTermJudgements() {
        Map<String, SearchHitCounter> hitCounts = this.getUniqueHitCounts();
        Map<String, List<Judgement>> judgementMap = new HashMap<>();

        int qid = 1;
        for (String term : hitCounts.keySet()) {
            List<Judgement> judgements = hitCounts.get(term).getJudgementList(qid);
            judgementMap.put(term, judgements);
            qid++;
        }

        return judgementMap;
    }

    public float[] cumulativeGain(float[] judgements) {
        float[] cumulativeGain = new float[judgements.length];

        float total = 0.0f;
        for (int i = 0; i < judgements.length; i++) {
            total += judgements[i];
            cumulativeGain[i] = total;
        }
        return cumulativeGain;
    }

    public static void main(String[] args) {
        float[] judgements = {3.0f, 2.0f, 1.0f, 0.0f};

//        Iterable<SearchStat> it = SearchStat.finder().find();
//        List<SearchStat> searchStats = new ArrayList<>();
//        it.forEach(searchStats::add);

        List<SearchStat> searchStats = null;
        try (ElasticSearchClient<SearchStat> searchClient = SearchClientUtils.getSearchClient()) {
             searchStats = SearchStat.search(searchClient).search();
        } catch (Exception e) {
            e.printStackTrace();
        }

        PerformanceChecker performanceChecker = new PerformanceChecker(searchStats);

        Map<String, List<Judgement>> judgementMap = performanceChecker.getTermJudgements();
        for (String term : judgementMap.keySet()) {
            System.out.println(String.format("%s:", term));
            for (Judgement judgement : judgementMap.get(term)) {
                System.out.println(judgement.getJudgement() + " : " + judgement.getFormattedComment());
            }
        }
    }
}
