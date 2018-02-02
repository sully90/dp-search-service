package com.github.onsdigital.search.nlp.word2vec.topics;

import com.github.onsdigital.search.configuration.SearchEngineProperties;
import com.github.onsdigital.search.nlp.word2vec.Word2VecClusterable;
import org.apache.commons.math3.ml.clustering.CentroidCluster;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.nd4j.linalg.factory.Nd4j;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * @author sullid (David Sullivan) on 01/02/2018
 * @project dp-search-service
 */
public class Topic {

    private static final Word2Vec WORD_2_VEC = SearchEngineProperties.WORD2VEC.getWord2vec();

    private int k;
    private CentroidCluster<Word2VecClusterable> centroidCluster;

    public Topic(int k, CentroidCluster<Word2VecClusterable> centroidCluster) {
        this.k = k;
        this.centroidCluster = centroidCluster;
    }

    private Topic() {
        // For Jenkins
    }

    public int getTopicNumber() {
        return this.k;
    }

    public double[] getTopicVector() {
        return this.centroidCluster.getCenter().getPoint();
    }

    public String getTopWord() {
        Collection<String> words = WORD_2_VEC.wordsNearest(Nd4j.create(this.getTopicVector()), 1);
        return words.iterator().next();
    }

    public Collection<String> getTopTopics(int N) {
        // Returns the top N topics for this centroid
        return WORD_2_VEC.wordsNearest(Nd4j.create(this.getTopicVector()), N);
    }

    public int size() {
        return this.centroidCluster.getPoints().size();
    }

    /**
     *
     * @return List of words belonging to this topic
     */
    public List<String> getWords() {
        List<String> words = new LinkedList<>();
        for (Word2VecClusterable clusterable : this.centroidCluster.getPoints()) {
            words.add(clusterable.getWord());
        }
        return words;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.centroidCluster);
    }

    @Override
    public boolean equals(Object obj) {
        return this.centroidCluster.equals(obj);
    }

    @Override
    public String toString() {
        return String.format("Topic: %s", this.getTopWord());
    }
}
