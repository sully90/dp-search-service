package com.github.onsdigital.search.nlp.word2vec.topics;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
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
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.ml.clustering.CentroidCluster;
import org.apache.commons.math3.ml.clustering.FuzzyKMeansClusterer;
import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer;
import org.apache.commons.math3.ml.distance.DistanceMeasure;
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
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.ops.transforms.Transforms;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(Topics.class);

    private static final Word2Vec WORD_2_VEC;

    static {
        try {
            WORD_2_VEC = SearchEngineProperties.WORD2VEC.readBinaryModel(SearchEngineProperties.WORD2VEC.Models.ONS_GZIP);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

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

//    private FuzzyKMeansClusterer<Word2VecClusterable> kMeans;
    private KMeansPlusPlusClusterer<Word2VecClusterable> kMeans;

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

    public Topic forWord(String word) {
        double similarity = Double.MIN_VALUE;
        Topic nearestTopic = null;
        for (Topic topic : this.getTopics()) {
            double sim = WORD_2_VEC.similarity(word, topic.getTopWord());
            if (sim > similarity) {
                similarity = sim;
                nearestTopic = topic;
            }
        }
        return nearestTopic;
    }

    public List<Topic> getTopics() {
        if (null == this.topics || this.topics.isEmpty()) {
            List<Word2VecClusterable> clusterables = new ArrayList<>();
            for (String word : this.words) {
                clusterables.add(new Word2VecClusterable(word));
            }

            LOGGER.info(String.format("Clustering %d words", clusterables.size()));

            this.topics = new LinkedList<>();

            List<CentroidCluster<Word2VecClusterable>> clusters = this.kMeans.cluster(clusterables);
//            List<CentroidCluster<Word2VecClusterable>> clusters = this.kMeans.getClusters();
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

//    public RealMatrix getMembershipMatrix() {
//        return this.kMeans.getMembershipMatrix();
//    }
//
//    public double[] getMembershipVectorForTopic(Topic topic) {
//        return this.getMembershipMatrix().getColumn(topic.getTopicNumber());
//    }

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

    private KMeansPlusPlusClusterer<Word2VecClusterable> cluster() {
//        List<Word2VecClusterable> clusterables = new ArrayList<>();
//        for (String word : this.words) {
//            clusterables.add(new Word2VecClusterable(word));
//        }
//
//        LOGGER.info(String.format("Clustering %d words", clusterables.size()));

        RandomGenerator randomGenerator = new JDKRandomGenerator();
        randomGenerator.setSeed(SEED);

//        FuzzyKMeansClusterer<Word2VecClusterable> kMeans = new FuzzyKMeansClusterer<>(this.k, this.fuzziness,
//                this.maxIterations, this.distanceMeasure, EPSILON, randomGenerator);
//        kMeans.cluster(clusterables);

        KMeansPlusPlusClusterer<Word2VecClusterable> kMeans = new KMeansPlusPlusClusterer<>(this.k, this.maxIterations, this.distanceMeasure, randomGenerator);
        return kMeans;
    }

    public static void main(String[] args) {

//        List<String> searchTerms = new LinkedList<String>() {{
//            add("rpi");
//            add("gender pay gap");
//            add("cpi");
//            add("gdp");
//            add("inflation");
//            add("crime");
//            add("unemployment");
//            add("population");
//            add("immigration");
//            add("mental health");
//            add("london");
//            add("london population");
//            add("retail price index");
//            add("life expectancy");
//            add("obesity");
//            add("religion");
//            add("migration");
//            add("poverty");
//            add("social media");
//            add("employment");
//        }};

        System.out.println(WORD_2_VEC.wordsNearest("rpi", 10).toString());

        try (ElasticSearchClient<Page> searchClient = SearchClientUtils.getSearchClient(ClientType.TCP)) {
            SearchRequest searchRequest = searchClient.prepareSearch("ons*")
                    .setQuery(QueryBuilders.matchAllQuery())
                    .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                    .setScroll(new TimeValue(60000))
                    .setSize(1000)
                    .request();

            SearchResponse searchResponse = searchClient.search(searchRequest);
            System.out.println(String.format("Got %d hits", searchResponse.getHits().totalHits));
            final String scrollId = searchResponse.getScrollId();

            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            Set<String> keywords = new HashSet<>();

            Collection<String> vocab = WORD_2_VEC.vocab().words();
            System.out.println(String.format("%d words in vocab", vocab.size()));

            do {
                SearchHits searchHits = searchResponse.getHits();

                for (SearchHit searchHit : searchHits.getHits()) {
                    Map<String, Object> fields = mapper.readValue(searchHit.getSourceAsString(), new TypeReference<Map<String, Object>>(){});
                    if (fields.containsKey("description")) {
                        Object description = fields.get("description");
                        if (description instanceof Map) {
                            Map<String, Object> descriptionMap = (Map<String, Object>) description;
                            if (descriptionMap.containsKey("keywords")) {
                                List<String> pageKeywords = (List<String>) descriptionMap.get("keywords");
                                for (String keyword : pageKeywords) {
                                    keyword = keyword.toLowerCase();
                                    if (null != keyword && !keyword.isEmpty() && vocab.contains(keyword)) {
                                        keywords.add(keyword);
                                    }
                                }
                            }
                        }
                    }
                }

                SearchScrollRequest searchScrollRequest = new SearchScrollRequest()
                        .scrollId(scrollId)
                        .scroll(new TimeValue(60000));
                searchResponse = searchClient.searchScroll(searchScrollRequest);

            } while (searchResponse.getHits().getHits().length != 0);

            String[] keywordsArray = keywords.toArray(new String[keywords.size()]);
            for (String keyword : keywordsArray) {
                for (String similar : WORD_2_VEC.wordsNearest(keyword, 5)) {
                    if (!similar.isEmpty()) {
                        keywords.add(similar);
                    }
                }
            }

            long start = System.currentTimeMillis();
            Topics topics = new Topics(30, keywords);
            for (Topic topic : topics.getTopics()) {
                System.out.println(String.format("Topic %d: %s : %s", topic.getTopicNumber(), topic.getTopWord(), topic.getTopTopics(10)));
                topic.getWords().stream().forEach(System.out::println);
                System.out.println();
            }
            long stop = System.currentTimeMillis();
            long duration = stop - start;

            System.out.format("Milli = %s, ( S_Start : %s, S_End : %s ) \n", duration, start, stop );
            System.out.println("Human-Readable format : "+ millisToShortDHMS( duration ) );

            String testWord = "rpi";
            Topic topicForWord = topics.forWord(testWord);
            if (topicForWord != null) {
                System.out.println(String.format("Topic for word '%s': %s", testWord, topicForWord.getTopTopics(10)));
            } else {
                System.out.println(String.format("No topic for word '%s'", testWord));
            }

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

    @Override
    public ObjectWriter writer() {
        return null;
    }

    @Override
    public ObjectId getObjectId() {
        return null;
    }
}
