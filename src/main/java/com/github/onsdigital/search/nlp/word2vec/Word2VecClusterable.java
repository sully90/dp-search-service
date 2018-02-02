package com.github.onsdigital.search.nlp.word2vec;

import org.apache.commons.math3.ml.clustering.Clusterable;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @author sullid (David Sullivan) on 01/02/2018
 * @project dp-search-service
 */
public class Word2VecClusterable extends Word2VecString implements Clusterable {

    public Word2VecClusterable(String word) {
        super(word);
    }

    @Override
    public double[] getPoint() {
        return this.getVector();
    }

    public List<Word2VecClusterable> nearestClusterableWords(int number) {
        List<Word2VecClusterable> clusterables = new ArrayList<>();

        super.nearest(number)
                .stream()
                .forEach(x -> clusterables.add(new Word2VecClusterable(x)));

        return clusterables;
    }

    public static List<Word2VecClusterable> fromWords(List<String> words, int number) {
        List<Word2VecClusterable> clusterables = new LinkedList<>();
        words.stream()
                .forEach(x -> {
                    Word2VecClusterable clusterable = new Word2VecClusterable(x);
                    clusterables.add(clusterable);
                    clusterables.addAll(clusterable.nearestClusterableWords(number));
                });

        return clusterables;
    }
}
