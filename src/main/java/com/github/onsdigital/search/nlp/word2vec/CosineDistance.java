package com.github.onsdigital.search.nlp.word2vec;

import org.apache.commons.math3.ml.distance.DistanceMeasure;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.ops.transforms.Transforms;

/**
 * @author sullid (David Sullivan) on 02/02/2018
 * @project dp-search-service
 */
public class CosineDistance implements DistanceMeasure {
    @Override
    public double compute(double[] doubles, double[] doubles1) {
        return Transforms.cosineDistance(Nd4j.create(doubles), Nd4j.create(doubles1));
    }
}
