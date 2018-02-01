package com.github.onsdigital.search.nlp.word2vec.topics;

import com.github.onsdigital.search.configuration.SearchEngineProperties;
import com.github.onsdigital.search.nlp.word2vec.Word2VecClusterable;
import com.github.onsdigital.search.util.MapUtils;
import org.apache.commons.math3.ml.clustering.CentroidCluster;
import org.apache.commons.math3.ml.clustering.FuzzyKMeansClusterer;
import org.apache.commons.math3.ml.distance.DistanceMeasure;
import org.apache.commons.math3.ml.distance.EuclideanDistance;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomGenerator;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author sullid (David Sullivan) on 01/02/2018
 * @project dp-search-service
 *
 * Class to analyse search terms for common topics
 * k is the total possible number of topics to search for, however in practice there may be fewer (empty clusters)
 */
public class Topics {

    private static final Logger LOGGER = LoggerFactory.getLogger(Topics.class);

    private static final Word2Vec WORD_2_VEC = SearchEngineProperties.WORD2VEC.getWord2vec();

    private static final int SEED = 12345;
    private static final double EPSILON = 1e-3;

    private static final double DEFAULT_FUZZINESS = 1.1d;
    private static final int DEFAULT_MAX_ITERATIONS = 1000;
    private static final DistanceMeasure DEFAULT_DISTANCE_MEASURE = new EuclideanDistance();

    private final int k;
    private final double fuzziness;
    private final int maxIterations;
    private final DistanceMeasure distanceMeasure;
    private final List<String> words;
    private final int numberOfNeighbours;
    private List<Topic> topics;

    public Topics(int k, List<String> words) {
        this(k, DEFAULT_FUZZINESS, DEFAULT_MAX_ITERATIONS, DEFAULT_DISTANCE_MEASURE, words, 10);
    }

    public Topics(int k, double fuzziness, int maxIterations, DistanceMeasure distanceMeasure, List<String> words, int numberOfNeighbours) {
        this.k = k;
        this.fuzziness = fuzziness;
        this.maxIterations = maxIterations;
        this.distanceMeasure = distanceMeasure;
        this.words = words;
        this.numberOfNeighbours = numberOfNeighbours;
    }

    public List<Topic> getTopics() {
        if (null == this.topics || this.topics.isEmpty()) {
            this.topics = new LinkedList<>();

            List<CentroidCluster<Word2VecClusterable>> clusters = this.cluster();
            for (CentroidCluster<Word2VecClusterable> cluster : clusters) {
                if (!cluster.getPoints().isEmpty()) {
                    Topic topic = new Topic(cluster);
                    this.topics.add(topic);
                } else {
                    LOGGER.info("Found empty topic, skipping");
                }
            }
        }
        return this.topics;
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

    private List<CentroidCluster<Word2VecClusterable>> cluster() {
        List<Word2VecClusterable> clusterables = Word2VecClusterable.fromWords(this.words, this.numberOfNeighbours);

        RandomGenerator randomGenerator = new JDKRandomGenerator();
        randomGenerator.setSeed(SEED);

        FuzzyKMeansClusterer<Word2VecClusterable> kMeans = new FuzzyKMeansClusterer<>(this.k, this.fuzziness,
                this.maxIterations, this.distanceMeasure, EPSILON, randomGenerator);
        return kMeans.cluster(clusterables);
    }

    public static void main(String[] args) {
        List<String> searchTerms = new LinkedList<String>() {{
//            add("rpi");
            add("gender pay gap");
//            add("cpi");
//            add("gdp");
//            add("inflation");
            add("crime");
//            add("unemployment");
            add("population");
//            add("immigration");
            add("mental health");
//            add("london");
//            add("london population");
//            add("retail price index");
//            add("life expectancy");
//            add("obesity");
            add("religion");
//            add("migration");
//            add("poverty");
            add("social media");
            add("employment");
        }};

        Topics topics = new Topics(20, searchTerms);
        List<Topic> topicList = topics.getTopics();

        int i = 0;
        for (Topic topic : topicList) {
            i++;
            System.out.println(String.format("Topic %d : %s", i, topic.getTopWord()));
        }

        System.out.println(topics.similarTopics(topicList.get(0)));
    }

}
