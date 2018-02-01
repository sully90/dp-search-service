package com.github.onsdigital.search.nlp.word2vec;

import com.github.onsdigital.search.configuration.SearchEngineProperties;
import org.apache.commons.lang3.StringUtils;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author sullid (David Sullivan) on 01/02/2018
 * @project dp-search-service
 */
public class Word2VecString {

    private static final Word2Vec WORD_2_VEC = SearchEngineProperties.WORD2VEC.getWord2vec();

    private String word;
    private double[] vector = null;

    public Word2VecString(String word) {
        this.word = word;
    }

    public String getWord() {
        return this.word;
    }

    public double[] getVector() {
        if (this.vector == null) {
            INDArray indArray = null;
            if (StringUtils.containsWhitespace(this.word)) {
                for (String part : this.word.split("\\s+")) {
                    INDArray vec = Nd4j.create(WORD_2_VEC.getWordVector(part));
                    if (indArray == null) {
                        indArray = vec;
                    } else {
                        indArray = indArray.add(vec);
                    }
                }
                this.vector = indArray.data().asDouble();
            } else {
                this.vector = WORD_2_VEC.getWordVector(this.word);
            }
        }
        return this.vector;
    }

    protected Collection<String> nearest(int number) {
        return WORD_2_VEC.wordsNearest(Nd4j.create(this.getVector()), number);
    }

    public List<Word2VecString> nearestWords(int number) {
        List<Word2VecString> word2VecStrings = new ArrayList<>();

        this.nearest(number)
                .stream()
                .forEach(x -> word2VecStrings.add(new Word2VecString(x)));

        return word2VecStrings;
    }


}
