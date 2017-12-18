package com.github.onsdigital.search.search;

import com.github.onsdigital.elasticutils.ml.ranklib.models.Judgement;
import com.github.onsdigital.elasticutils.ml.ranklib.models.Judgements;
import com.github.onsdigital.search.search.models.SearchHitCounter;
import com.github.onsdigital.search.search.models.SearchStat;

import java.util.*;


/**
 * @author sullid (David Sullivan) on 13/12/2017
 * @project dp-search-service
 *
 * Scores the performance of the Search Engine using the searchstats collection
 */
public class PerformanceChecker {

    private List<SearchStat> searchStats;

    public PerformanceChecker() {
        this.searchStats = PerformanceChecker.loadSearchStats();
    }

    public PerformanceChecker(List<SearchStat> searchStats) {
        this.searchStats = searchStats;
    }

    // TODO improve qid logic
    public Map<String, SearchHitCounter> getUniqueHitCounts() {
        Map<String, SearchHitCounter> hitCounts = new LinkedHashMap<>();

        int qid = 1;
        for (SearchStat searchStat : this.searchStats) {
            String term = searchStat.getTerm();
            if (!hitCounts.containsKey(term)) {
                hitCounts.put(term, new SearchHitCounter(qid));
                qid++;
            }
            hitCounts.get(term).add(searchStat.getUrl(), searchStat.getRank());
        }
        return hitCounts;
    }

    public Map<String, Judgements> getTermJudgements() {
        Map<String, SearchHitCounter> hitCounts = this.getUniqueHitCounts();

        Map<String, Judgements> judgementMap = new LinkedHashMap<>();

        int qid = 1;
        for (String term : hitCounts.keySet()) {
            List<Judgement> judgements = hitCounts.get(term).getJudgementList(term);
            judgementMap.put(term, new Judgements(qid, judgements));
            qid++;
        }

        return judgementMap;
    }

    public static List<SearchStat> loadSearchStats() {
        // For now, load from mongoDB. This can be changed in the future depending on the direction we take.
        Iterable<SearchStat> it = SearchStat.finder().find();
        List<SearchStat> searchStats = new LinkedList<>();
        it.forEach(searchStats::add);

        return searchStats;
    }

    public static void main(String[] args) {

        PerformanceChecker performanceChecker = new PerformanceChecker();

        Map<String, SearchHitCounter> uniqueHits = performanceChecker.getUniqueHitCounts();
        for (String term : uniqueHits.keySet()) {
            Judgements judgements = uniqueHits.get(term).getJudgements(term);
            System.out.println("Term: " + term);
            System.out.println(uniqueHits.get(term));
            System.out.println(Arrays.toString(judgements.normalisedDiscountedCumulativeGain()));
        }
    }
}
