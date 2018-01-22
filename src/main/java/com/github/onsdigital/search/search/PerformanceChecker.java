package com.github.onsdigital.search.search;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.onsdigital.elasticutils.client.generic.ElasticSearchClient;
import com.github.onsdigital.elasticutils.client.type.DefaultDocumentTypes;
import com.github.onsdigital.elasticutils.ml.query.SltrQueryBuilder;
import com.github.onsdigital.elasticutils.ml.ranklib.models.Judgement;
import com.github.onsdigital.elasticutils.ml.ranklib.models.Judgements;
import com.github.onsdigital.elasticutils.ml.requests.LogQuerySearchRequest;
import com.github.onsdigital.elasticutils.ml.util.JsonUtils;
import com.github.onsdigital.search.configuration.SearchEngineProperties;
import com.github.onsdigital.search.search.models.SearchHitCount;
import com.github.onsdigital.search.search.models.SearchHitCounter;
import com.github.onsdigital.search.search.models.SearchStat;
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
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private SortBy sortBy;
    private String model;

    private List<SearchStat> searchStats;

    public PerformanceChecker() throws IOException {
        this(SortBy.RELEVANCE);
    }

    public PerformanceChecker(SortBy sortBy) throws IOException {
        this(sortBy, null);
    }

    public PerformanceChecker(SortBy sortBy, String model) throws IOException {
        this.searchStats = PerformanceChecker.loadSearchStats(sortBy, model);
        if (this.searchStats.size() == 0) {
            throw new IOException(String.format("No search stats found for sortBy/model: %s/%s", sortBy.getSortBy(), model));
        }
        this.sortBy = sortBy;
        this.model = model;
    }

    public SortBy getSortBy() {
        return sortBy;
    }

    public String getModel() {
        return model;
    }

    public List<SearchStat> getSearchStats() {
        return searchStats;
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
            hitCounts.get(term).add(searchStat.getRedirUrl(), searchStat.getRank());
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

    public static List<SearchStat> loadSearchStats(SortBy sortBy) throws JsonProcessingException {
        return loadSearchStats(sortBy, null);
    }

    public static List<SearchStat> loadSearchStats(SortBy sortBy, String model) throws JsonProcessingException {
        // For now, load from mongoDB. This can be changed in the future depending on the direction we take.

        String queryString;
        if (sortBy.equals(SortBy.LTR)) {
            queryString = sortByQuery(sortBy, model == null ? SearchEngineProperties.LTR.getDefaultModel() : model);
        } else {
            queryString = sortByQuery(sortBy, null);
        }
        LOGGER.info(String.format("Querying mongoDB with query string: %s", queryString));
        Iterable<SearchStat> it = SearchStat.finder().find(queryString);

        List<SearchStat> searchStats = new LinkedList<>();
        it.forEach(searchStats::add);
        return searchStats;
    }

    private static String sortByQuery(SortBy sortBy, String model) throws JsonProcessingException {
        Map<String, Object> params = new HashMap<String, Object>() {{
            put("sortby", sortBy.getSortBy());
            if (model != null) {
                put("model", model);
            }
        }};

        return JsonUtils.toJson(params);
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
        // Placeholder log_name
        String logName = "logged_featureset";

        // Build the sltr query with keyword template injection
        SltrQueryBuilder sltrQueryBuilder = new SltrQueryBuilder(logName, featureSet);
        sltrQueryBuilder.setStore(store);
        sltrQueryBuilder.setParam("keywords", keywords);

        // Build the elasticsearch query, which performs a term filter on the _id field
        QueryBuilder qb = QueryBuilders.boolQuery()
                .must(QueryBuilders.termQuery("_id", id))
                .should(sltrQueryBuilder);

        // Build the LogQuerySearchRequest and return
        LogQuerySearchRequest logQuerySearchRequest = LogQuerySearchRequest.getRequestForQuery(qb, sltrQueryBuilder);
        return logQuerySearchRequest;
    }

}
