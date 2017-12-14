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

    public Map<String, SearchHitCounter> getUniqueHitCounts() {
        Map<String, SearchHitCounter> hitCounts = new HashMap<>();

        for (SearchStat searchStat : this.searchStats) {
            String term = searchStat.getTerm();
            if (!hitCounts.containsKey(term)) {
                hitCounts.put(term, new SearchHitCounter());
            }
            hitCounts.get(term).add(searchStat.getUrl(), searchStat.getRank());
        }
        return hitCounts;
    }

    // TODO improve qid logic
    public Map<String, List<Judgement>> getTermJudgements() {
        Map<String, SearchHitCounter> hitCounts = this.getUniqueHitCounts();

        Map<String, List<Judgement>> judgementMap = new HashMap<>();

        int qid = 1;
        for (String term : hitCounts.keySet()) {
            List<Judgement> judgements = hitCounts.get(term).getJudgementList(term, qid);
            judgementMap.put(term, judgements);
            qid++;
        }

        return judgementMap;
    }

    /**
     * Computes the NDCG metric for each query term. The closer the NDCG is to unity (1), the better
     * the performance of the search engine (as a score of 1 means we hit the ideal value)
     * @return
     */
    public Map<String, Float[]> computeNdcg() {
        Map<String, List<Judgement>> judgementMap = this.getTermJudgements();

        Map<String, Float[]> ndcgMap = new HashMap<>();

        for (String term : judgementMap.keySet()) {
            List<Judgement> judgementList = judgementMap.get(term);
            int qid = judgementList.get(0).getQueryId();

            Judgements judgements = new Judgements(qid, judgementList);
            float[] ndcg = judgements.normalisedDiscountedCumulativeGain();

            Float[] ndcgConverted = new Float[ndcg.length];
            for (int i = 0; i < ndcg.length; i++) {
                ndcgConverted[i] = ndcg[i];
            }
            ndcgMap.put(term, ndcgConverted);
        }

        return ndcgMap;
    }

    public static List<SearchStat> loadSearchStats() {
        // For now, load from mongoDB. This can be changed in the future depending on the direction we take.
        Iterable<SearchStat> it = SearchStat.finder().find();
        List<SearchStat> searchStats = new ArrayList<>();
        it.forEach(searchStats::add);

        return searchStats;
    }

    public static void main(String[] args) {

        PerformanceChecker performanceChecker = new PerformanceChecker();

        Map<String, SearchHitCounter> uniqueHitCounts = performanceChecker.getUniqueHitCounts();

        Map<String, Float[]> ndcgMap = performanceChecker.computeNdcg();
        for (String term : ndcgMap.keySet()) {
            System.out.println(String.format("Term: %s", term));
            System.out.println(Arrays.toString(ndcgMap.get(term)));
            System.out.println(uniqueHitCounts.get(term));
        }
    }
}
