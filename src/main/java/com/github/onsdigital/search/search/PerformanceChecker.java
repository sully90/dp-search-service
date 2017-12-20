package com.github.onsdigital.search.search;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.onsdigital.elasticutils.client.generic.ElasticSearchClient;
import com.github.onsdigital.elasticutils.client.type.DefaultDocumentTypes;
import com.github.onsdigital.elasticutils.ml.client.http.LearnToRankClient;
import com.github.onsdigital.elasticutils.ml.client.response.sltr.SltrHit;
import com.github.onsdigital.elasticutils.ml.client.response.sltr.SltrResponse;
import com.github.onsdigital.elasticutils.ml.client.response.sltr.models.Fields;
import com.github.onsdigital.elasticutils.ml.query.SltrQueryBuilder;
import com.github.onsdigital.elasticutils.ml.ranklib.models.Judgement;
import com.github.onsdigital.elasticutils.ml.ranklib.models.Judgements;
import com.github.onsdigital.elasticutils.ml.requests.LogQuerySearchRequest;
import com.github.onsdigital.elasticutils.ml.util.LearnToRankHelper;
import com.github.onsdigital.search.search.models.SearchHitCount;
import com.github.onsdigital.search.search.models.SearchHitCounter;
import com.github.onsdigital.search.search.models.SearchStat;
import com.github.onsdigital.search.util.ClientType;
import com.github.onsdigital.search.util.SearchClientUtils;
import com.github.onsdigitial.elastic.importer.models.page.adhoc.AdHoc;
import com.github.onsdigitial.elastic.importer.models.page.base.Page;
import com.github.onsdigitial.elastic.importer.models.page.base.PageType;
import com.github.onsdigitial.elastic.importer.models.page.census.HomePageCensus;
import com.github.onsdigitial.elastic.importer.models.page.compendium.CompendiumChapter;
import com.github.onsdigitial.elastic.importer.models.page.compendium.CompendiumData;
import com.github.onsdigitial.elastic.importer.models.page.compendium.CompendiumLandingPage;
import com.github.onsdigitial.elastic.importer.models.page.home.HomePage;
import com.github.onsdigitial.elastic.importer.models.page.release.Release;
import com.github.onsdigitial.elastic.importer.models.page.staticpage.StaticArticle;
import com.github.onsdigitial.elastic.importer.models.page.staticpage.StaticLandingPage;
import com.github.onsdigitial.elastic.importer.models.page.staticpage.StaticPage;
import com.github.onsdigitial.elastic.importer.models.page.staticpage.foi.FOI;
import com.github.onsdigitial.elastic.importer.models.page.staticpage.qmi.QMI;
import com.github.onsdigitial.elastic.importer.models.page.statistics.data.DataSlice;
import com.github.onsdigitial.elastic.importer.models.page.statistics.data.timeseries.TimeSeries;
import com.github.onsdigitial.elastic.importer.models.page.statistics.dataset.Dataset;
import com.github.onsdigitial.elastic.importer.models.page.statistics.dataset.DatasetLandingPage;
import com.github.onsdigitial.elastic.importer.models.page.statistics.dataset.ReferenceTables;
import com.github.onsdigitial.elastic.importer.models.page.statistics.dataset.TimeSeriesDataset;
import com.github.onsdigitial.elastic.importer.models.page.statistics.document.article.Article;
import com.github.onsdigitial.elastic.importer.models.page.statistics.document.article.ArticleDownload;
import com.github.onsdigitial.elastic.importer.models.page.statistics.document.bulletin.Bulletin;
import com.github.onsdigitial.elastic.importer.models.page.statistics.document.figure.chart.Chart;
import com.github.onsdigitial.elastic.importer.models.page.statistics.document.figure.equation.Equation;
import com.github.onsdigitial.elastic.importer.models.page.statistics.document.figure.image.Image;
import com.github.onsdigitial.elastic.importer.models.page.statistics.document.figure.table.Table;
import com.github.onsdigitial.elastic.importer.models.page.taxonomy.ProductPage;
import com.github.onsdigitial.elastic.importer.models.page.taxonomy.TaxonomyLandingPage;
import com.github.onsdigitial.elastic.importer.models.page.visualisation.Visualisation;
import org.apache.commons.io.IOUtils;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;


/**
 * @author sullid (David Sullivan) on 13/12/2017
 * @project dp-search-service
 *
 * Scores the performance of the Search Engine using the searchstats collection
 */
public class PerformanceChecker {

    private static final Logger LOGGER = LoggerFactory.getLogger(PerformanceChecker.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

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

    // TODO improve qid logic
    public Map<String, SearchHitCounter> getUniqueHitCountsByQuery(ElasticSearchClient<SearchStat> searchClient) {
        Map<String, SearchHitCounter> hitCounts = new LinkedHashMap<>();

        int qid = 1;
        for (SearchStat searchStat : this.searchStats) {
            String term = searchStat.getTerm();
            String url = searchStat.getUrl();
            QueryBuilder qb = QueryBuilders.boolQuery()
                    .must(QueryBuilders.matchPhraseQuery("term", term))
                    .must(QueryBuilders.matchPhraseQuery("url", url));
            try {
                List<SearchStat> searchStatHits = SearchStat.search(searchClient).search(qb, DefaultDocumentTypes.DOCUMENT);
                int count = searchStatHits.size();

                SearchHitCount searchHitCount = new SearchHitCount(searchStat.getRank(), count);
                SearchHitCounter counter = new SearchHitCounter(qid);
                counter.put(url, searchHitCount);

                hitCounts.put(term, counter);
            } catch (IOException e) {
                e.printStackTrace();
            }
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

    private static Page asClass(String fileString, Class<? extends Page> returnClass) throws IOException {
        return MAPPER.readValue(fileString, returnClass);
    }

    public static Page fromSearchHit(SearchHit searchHit) throws IOException {
        String json = searchHit.getSourceAsString();
        Object obj;
        try {
            obj = MAPPER.readValue(json, Object.class);
        } catch (JsonParseException | JsonMappingException e) {
            LOGGER.warn("Error constructing object", e);
            return null;
        }

        if (obj instanceof Map) {
            Map<String, Object> objectMap;
            try {
                objectMap = MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {});
            } catch (JsonMappingException e) {
                LOGGER.warn("Error constructing objectMap", e);
                return null;
            }

            if (objectMap.containsKey("type") && objectMap.get("type") instanceof String) {
                String type = (String) objectMap.get("type");
                PageType pageType = PageType.forType(type);
                if (pageType != null) {
                    switch (pageType) {
                        case home_page:
                            return asClass(json, HomePage.class);
                        case home_page_census:
                            return asClass(json, HomePageCensus.class);
                        case taxonomy_landing_page:
                            return asClass(json, TaxonomyLandingPage.class);
                        case product_page:
                            return asClass(json, ProductPage.class);
                        case bulletin:
                            return asClass(json, Bulletin.class);
                        case article:
                            return asClass(json, Article.class);
                        case article_download:
                            return asClass(json, ArticleDownload.class);
                        case timeseries:
                            return asClass(json, TimeSeries.class);
                        case data_slice:
                            return asClass(json, DataSlice.class);
                        case compendium_landing_page:
                            return asClass(json, CompendiumLandingPage.class);
                        case compendium_chapter:
                            return asClass(json, CompendiumChapter.class);
                        case compendium_data:
                            return asClass(json, CompendiumData.class);
                        case static_landing_page:
                            return asClass(json, StaticLandingPage.class);
                        case static_article:
                            return asClass(json, StaticArticle.class);
                        case static_page:
                            return asClass(json, StaticPage.class);
                        case static_qmi:
                            return asClass(json, QMI.class);
                        case static_foi:
                            return asClass(json, FOI.class);
                        case static_adhoc:
                            return asClass(json, AdHoc.class);
                        case dataset:
                            return asClass(json, Dataset.class);
                        case dataset_landing_page:
                            return asClass(json, DatasetLandingPage.class);
                        case timeseries_dataset:
                            return asClass(json, TimeSeriesDataset.class);
                        case release:
                            return asClass(json, Release.class);
                        case reference_tables:
                            return asClass(json, ReferenceTables.class);
                        case chart:
                            return asClass(json, Chart.class);
                        case table:
                            return asClass(json, Table.class);
                        case image:
                            return asClass(json, Image.class);
                        case visualisation:
                            return asClass(json, Visualisation.class);
                        case equation:
                            return asClass(json, Equation.class);
                        default:
                            LOGGER.warn("Unknown type: " + type);
                    }
                }
            }
        }
        return null;
    }

    private static LogQuerySearchRequest getLogQuerySearchRequest(String store, String featureSet,
                                                                  String id, String keywords) {
        String logName = "logged_featureset";

        SltrQueryBuilder sltrQueryBuilder = new SltrQueryBuilder(logName, featureSet);
        sltrQueryBuilder.setStore(store);
        sltrQueryBuilder.setParam("keywords", keywords);

        QueryBuilder qb = QueryBuilders.boolQuery()
                .must(QueryBuilders.termQuery("_id", id))
                .should(sltrQueryBuilder);

        LogQuerySearchRequest logQuerySearchRequest = LogQuerySearchRequest.getRequestForQuery(qb, sltrQueryBuilder);
        return logQuerySearchRequest;
    }

    public static void main(String[] args) {

        PerformanceChecker performanceChecker = new PerformanceChecker();
        String store = "ons_featurestore";
        String featureSet = "ons_features";

        try (ElasticSearchClient<Page> searchClient = SearchClientUtils.getSearchClient(ClientType.TCP)) {
            try (LearnToRankClient learnToRankClient = LearnToRankHelper.getLTRClient("localhost")) {

                Map<String, SearchHitCounter> uniqueHits = performanceChecker.getUniqueHitCounts();
                for (String term : uniqueHits.keySet()) {
                    Judgements judgements = uniqueHits.get(term).getJudgements(term);
                    float[] ndcg = judgements.normalisedDiscountedCumulativeGain();

                    List<Judgement> judgementList = judgements.getJudgementList();
                    Collections.sort(judgementList);

                    System.out.println("Term: " + term);
                    for (int i = 0; i < ndcg.length; i++) {
                        System.out.println(judgementList.get(i).getRank() + " : " + ndcg[i]);
                        Object obj = judgementList.get(i).getAttr("url");
                        if (obj instanceof String) {
                            String url = String.valueOf(obj);

                            LogQuerySearchRequest logQuerySearchRequest = getLogQuerySearchRequest(store,
                                    featureSet, url, term);
                            SltrResponse sltrResponse = learnToRankClient.search("ons_*", logQuerySearchRequest);
                            List<SltrHit> sltrHits = sltrResponse.getHits().getHits();
                            if (sltrHits.size() > 0) {
                                Fields fields = sltrHits.get(0).getFields();
                                System.out.println(fields.getValues().toString());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

//        Iterator<WritableJudgements> it = WritableJudgements.finder().find().iterator();
//        while (it.hasNext()) {
//            WritableJudgements writableJudgements = it.next();
//
//            String term = writableJudgements.getTerm();
//            Judgements judgements = writableJudgements.getJudgements();
//            float[] ndcg = judgements.normalisedDiscountedCumulativeGain();
//
//            List<Judgement> judgementList = judgements.getJudgementList();
//            Collections.sort(judgementList);
//
//            System.out.println("Term: " + term);
//            for (int i = 0; i < ndcg.length; i++) {
//                System.out.println(judgementList.get(i).getRank() + " : " + ndcg[i]);
//            }
//        }
    }
}
