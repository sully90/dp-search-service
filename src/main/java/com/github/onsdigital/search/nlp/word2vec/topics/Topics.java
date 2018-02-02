package com.github.onsdigital.search.nlp.word2vec.topics;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.onsdigital.elasticutils.client.generic.ElasticSearchClient;
import com.github.onsdigital.mongo.WritableObject;
import com.github.onsdigital.mongo.util.ObjectWriter;
import com.github.onsdigital.search.configuration.SearchEngineProperties;
import com.github.onsdigital.search.nlp.word2vec.CosineDistance;
import com.github.onsdigital.search.nlp.word2vec.Word2VecClusterable;
import com.github.onsdigital.search.util.ClientType;
import com.github.onsdigital.search.util.MapUtils;
import com.github.onsdigital.search.util.SearchClientUtils;
import com.github.onsdigitial.elastic.importer.models.page.base.Page;
import com.github.onsdigitial.elastic.importer.models.page.base.PageType;
import com.github.onsdigitial.elastic.importer.models.page.census.HomePageCensus;
import com.github.onsdigitial.elastic.importer.models.page.compendium.CompendiumChapter;
import com.github.onsdigitial.elastic.importer.models.page.compendium.CompendiumData;
import com.github.onsdigitial.elastic.importer.models.page.compendium.CompendiumLandingPage;
import com.github.onsdigitial.elastic.importer.models.page.home.HomePage;
import com.github.onsdigitial.elastic.importer.models.page.statistics.data.timeseries.TimeSeries;
import com.github.onsdigitial.elastic.importer.models.page.statistics.dataset.TimeSeriesDataset;
import com.github.onsdigitial.elastic.importer.models.page.statistics.document.article.Article;
import com.github.onsdigitial.elastic.importer.models.page.statistics.document.bulletin.Bulletin;
import com.github.onsdigitial.elastic.importer.models.page.taxonomy.ProductPage;
import com.github.onsdigitial.elastic.importer.models.page.taxonomy.TaxonomyLandingPage;
import opennlp.tools.tokenize.WhitespaceTokenizer;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.ml.clustering.CentroidCluster;
import org.apache.commons.math3.ml.clustering.FuzzyKMeansClusterer;
import org.apache.commons.math3.ml.distance.DistanceMeasure;
import org.apache.commons.math3.ml.distance.EuclideanDistance;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomGenerator;
import org.bson.types.ObjectId;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author sullid (David Sullivan) on 01/02/2018
 * @project dp-search-service
 *
 * Class to analyse search terms for common topics
 * k is the total possible number of topics to search for, however in practice there may be fewer (empty clusters)
 */
public class Topics implements WritableObject {

    private static final ObjectMapper MAPPER;

    static {
        MAPPER = new ObjectMapper();
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(Topics.class);

    private static final Word2Vec WORD_2_VEC = SearchEngineProperties.WORD2VEC.getWord2vec();

    private static final int SEED = 12345;
    private static final double EPSILON = 1e-6;

    private static final double DEFAULT_FUZZINESS = 1.01d;
    private static final int DEFAULT_MAX_ITERATIONS = 1000;
    private static final DistanceMeasure DEFAULT_DISTANCE_MEASURE = new CosineDistance();

    private ObjectId _id;
    private int k;
    private double fuzziness;
    private int maxIterations;
    private DistanceMeasure distanceMeasure;
    private Set<String> words;
    private int numberOfNeighbours;
    private List<Topic> topics;

    private FuzzyKMeansClusterer<Word2VecClusterable> kMeans;

    public Topics(int k, Set<String> words) {
        this(k, DEFAULT_FUZZINESS, DEFAULT_MAX_ITERATIONS, DEFAULT_DISTANCE_MEASURE, words, 10);
    }

    public Topics(int k, double fuzziness, int maxIterations, DistanceMeasure distanceMeasure, Set<String> words, int numberOfNeighbours) {
        this.k = k;
        this.fuzziness = fuzziness;
        this.maxIterations = maxIterations;
        this.distanceMeasure = distanceMeasure;
        this.words = words;
        this.numberOfNeighbours = numberOfNeighbours;

        this.kMeans = this.cluster();
    }

    private Topics() {
        // For Jackson
    }

    public Topic forVector(double[] vector) {
        double distance = Double.MAX_VALUE;
        Topic nearestTopic = null;
        for (Topic topic : this.getTopics()) {
            double d = this.distanceMeasure.compute(topic.getTopicVector(), vector);
            if (d < distance) {
                distance = d;
                nearestTopic = topic;
            }
        }
        return nearestTopic;
    }

    public Topic forWord(String word) {
        double[] vector = WORD_2_VEC.getWordVector(word);
        return this.forVector(vector);
    }

    public List<Topic> getTopics() {
        if (null == this.topics || this.topics.isEmpty()) {
            this.topics = new LinkedList<>();

            List<CentroidCluster<Word2VecClusterable>> clusters = this.kMeans.getClusters();
            for (int i = 0; i < clusters.size(); i++) {
                CentroidCluster<Word2VecClusterable> cluster = clusters.get(i);
                if (!cluster.getPoints().isEmpty()) {
                    Topic topic = new Topic(i, cluster);
                    this.topics.add(topic);
                } else {
                    LOGGER.info("Found empty topic, skipping");
                }
            }
        }
        return this.topics;
    }

    public RealMatrix getMembershipMatrix() {
        return this.kMeans.getMembershipMatrix();
    }

    public double[] getMembershipVectorForTopic(Topic topic) {
        return this.getMembershipMatrix().getColumn(topic.getTopicNumber());
    }

    public SortedSet<Map.Entry<Topic, Double>> similarTopics(Topic otherTopic) {
        String otherTopicTopWord = otherTopic.getTopWord();

        Map<Topic, Double> topicMap = new HashMap<>();

        for (Topic topic : this.getTopics()) {
            double similarity = WORD_2_VEC.similarity(topic.getTopWord(), otherTopicTopWord);
            if (similarity > 0.5d) {
                topicMap.put(topic, similarity);
            }
        }
        return MapUtils.entriesSortedByValues(topicMap);
    }

    private FuzzyKMeansClusterer<Word2VecClusterable> cluster() {
//        List<Word2VecClusterable> clusterables = Word2VecClusterable.fromWords(this.words, this.numberOfNeighbours);
        List<Word2VecClusterable> clusterables = new ArrayList<>();
        for (String word : this.words) {
            clusterables.add(new Word2VecClusterable(word));
        }

        LOGGER.info(String.format("Clustering %d words", clusterables.size()));

        RandomGenerator randomGenerator = new JDKRandomGenerator();
        randomGenerator.setSeed(SEED);

        FuzzyKMeansClusterer<Word2VecClusterable> kMeans = new FuzzyKMeansClusterer<>(this.k, this.fuzziness,
                this.maxIterations, this.distanceMeasure, EPSILON, randomGenerator);
        kMeans.cluster(clusterables);
        return kMeans;
    }

    public static void main(String[] args) {

        List<String> searchTerms = new LinkedList<String>() {{
            add("rpi");
            add("gender pay gap");
            add("cpi");
            add("gdp");
            add("inflation");
            add("crime");
            add("unemployment");
            add("population");
            add("immigration");
            add("mental health");
            add("london");
            add("london population");
            add("retail price index");
            add("life expectancy");
            add("obesity");
            add("religion");
            add("migration");
            add("poverty");
            add("social media");
            add("employment");
        }};

        try (ElasticSearchClient<Page> searchClient = SearchClientUtils.getSearchClient(ClientType.TCP)) {
            SearchRequest searchRequest = searchClient.prepareSearch("ons*")
                    .setQuery(QueryBuilders.matchAllQuery())
                    .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                    .setScroll(new TimeValue(60000))
                    .setSize(100)
                    .request();

            SearchResponse searchResponse = searchClient.search(searchRequest);
            final String scrollId = searchResponse.getScrollId();

            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            Set<String> keywords = new HashSet<>();

            for (String searchTerm : searchTerms) {
                keywords.add(searchTerm);
//                keywords.addAll(WORD_2_VEC.similarWordsInVocabTo(searchTerm, 10));
            }

            do {
                SearchHits searchHits = searchResponse.getHits();

                for (SearchHit searchHit : searchHits.getHits()) {
                    Page page = fromHit(searchHit);
                    if (page != null && page.getDescription() != null) {
                        if (page.getDescription().getKeywords() != null) {
                            for (String keyword : page.getDescription().getKeywords()) {
                                keywords.add(keyword.toLowerCase().trim());
                            }
                        }

                        if (page.getDescription().getTitle() != null && page.getDescription().getSummary() != null) {
                            StringBuilder sb = new StringBuilder(page.getDescription().getTitle())
                                    .append(page.getDescription().getSummary());

                            String text = sb.toString();
                            String[] tokens = WhitespaceTokenizer.INSTANCE.tokenize(text);
                            for (String token : tokens) {
                                keywords.add(token);
                            }
                        }

//                        if (page.getEntities() != null) {
//                            for (String key : page.getEntities().keySet()) {
//                                for (String entity : page.getEntities().get(key)) {
//                                    if (!WORD_2_VEC.getStopWords().contains(entity)) {
//                                        keywords.add(entity);
//                                    }
//                                }
//                            }
//                        }
                    }
                }

                SearchScrollRequest searchScrollRequest = new SearchScrollRequest()
                        .scrollId(scrollId)
                        .scroll(new TimeValue(60000));
                searchResponse = searchClient.searchScroll(searchScrollRequest);

            } while (searchResponse.getHits().getHits().length != 0);

            System.out.println(keywords.size());

            long start = System.currentTimeMillis();
            Topics topics = new Topics(20, keywords);
            for (Topic topic : topics.getTopics()) {
                System.out.println(String.format("Topic %d: %s : %s", topic.getTopicNumber(), topic.getTopWord(), topic.getTopTopics(10)));
                System.out.println(topic.getWords());
            }
            long stop = System.currentTimeMillis();
            long duration = stop - start;

            System.out.format("Milli = %s, ( S_Start : %s, S_End : %s ) \n", duration, start, stop );
            System.out.println("Human-Readable format : "+ millisToShortDHMS( duration ) );

            Topic topicForWord = topics.forWord("crime");
            System.out.println(String.format("Topic for word 'crime': %s", topicForWord.getTopTopics(10)));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String millisToShortDHMS(long duration) {
        String res = "";    // java.util.concurrent.TimeUnit;
        long days       = TimeUnit.MILLISECONDS.toDays(duration);
        long hours      = TimeUnit.MILLISECONDS.toHours(duration) -
                TimeUnit.DAYS.toHours(TimeUnit.MILLISECONDS.toDays(duration));
        long minutes    = TimeUnit.MILLISECONDS.toMinutes(duration) -
                TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(duration));
        long seconds    = TimeUnit.MILLISECONDS.toSeconds(duration) -
                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration));
        long millis     = TimeUnit.MILLISECONDS.toMillis(duration) -
                TimeUnit.SECONDS.toMillis(TimeUnit.MILLISECONDS.toSeconds(duration));

        if (days == 0)      res = String.format("%02d:%02d:%02d.%04d", hours, minutes, seconds, millis);
        else                res = String.format("%dd %02d:%02d:%02d.%04d", days, hours, minutes, seconds, millis);
        return res;
    }

    private static Page asClass(String fileString, Class<? extends Page> returnClass) throws IOException {
        return MAPPER.readValue(fileString, returnClass);
    }

    public static Page fromHit(SearchHit searchHit) throws IOException {
        String type = searchHit.getType();
        String fileString = searchHit.getSourceAsString();
        Object obj;
        try {
            obj = MAPPER.readValue(fileString, Object.class);
        } catch (JsonParseException | JsonMappingException e) {
            LOGGER.warn("Error constructing object", e);
            return null;
        }

        if (obj instanceof Map) {
            Map<String, Object> objectMap;
            try {
                objectMap = MAPPER.readValue(fileString, new TypeReference<Map<String, Object>>() {
                });
            } catch (JsonMappingException e) {
                LOGGER.warn("Error constructing object", e);
                return null;
            }

            if (objectMap.containsKey("type") && objectMap.get("type") instanceof String) {
                type = type.toLowerCase().replaceAll("\\s", "_");

                PageType identifiedPage = PageType.forType(type);
                if (identifiedPage == null) {
                    identifiedPage = PageType.forType(String.format("%s_page", type));
                    if (identifiedPage == null) {
                        LOGGER.info(String.format("Failed to identify page %s, exiting.", type));
                    } else {
                        switch (identifiedPage) {
                            case home_page:
                                return asClass(fileString, HomePage.class);
                            case home_page_census:
                                return asClass(fileString, HomePageCensus.class);
                            case taxonomy_landing_page:
                                return asClass(fileString, TaxonomyLandingPage.class);
                            case product_page:
                                return asClass(fileString, ProductPage.class);
                            case bulletin:
                                return asClass(fileString, Bulletin.class);
                            case article:
                                return asClass(fileString, Article.class);
                            case timeseries:
                                return asClass(fileString, TimeSeries.class);
                            case compendium_landing_page:
                                return asClass(fileString, CompendiumLandingPage.class);
                            case compendium_chapter:
                                return asClass(fileString, CompendiumChapter.class);
                            case compendium_data:
                                return asClass(fileString, CompendiumData.class);
                            case timeseries_dataset:
                                return asClass(fileString, TimeSeriesDataset.class);
                            case reference_tables:
                            case chart:
                            case table:
                            case image:
                            case visualisation:
                            case equation:
                                return null;
//                        case reference_tables:
//                            return asClass(fileString, ReferenceTables.class);
//                        case chart:
//                            return asClass(fileString, Chart.class);
//                        case table:
//                            return asClass(fileString, Table.class);
//                        case image:
//                            return asClass(fileString, Image.class);
//                        case visualisation:
//                            return asClass(fileString, Visualisation.class);
//                        case equation:
//                            return asClass(fileString, Equation.class);
                            default:
                                LOGGER.warn("Unknown type: " + type);
                        }
                    }
                }
            }
        }
        return null;
    }

    @Override
    public ObjectWriter writer() {
        return null;
    }

    @Override
    public ObjectId getObjectId() {
        return null;
    }
}
