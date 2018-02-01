package com.github.onsdigital.search.nlp.word2vec;

import org.apache.commons.math3.exception.NotPositiveException;
import org.apache.commons.math3.exception.NullArgumentException;
import org.apache.commons.math3.ml.clustering.Cluster;
import org.apache.commons.math3.ml.clustering.DBSCANClusterer;
import org.apache.commons.math3.ml.distance.DistanceMeasure;

import java.util.Collection;
import java.util.List;

/**
 * @author sullid (David Sullivan) on 01/02/2018
 * @project dp-search-service
 */
public class Word2VecDBScan extends DBSCANClusterer<Word2VecClusterable> {
    public Word2VecDBScan(double eps, int minPts) throws NotPositiveException {
        super(eps, minPts);
    }

    public Word2VecDBScan(double eps, int minPts, DistanceMeasure measure) throws NotPositiveException {
        super(eps, minPts, measure);
    }

    @Override
    public List<Cluster<Word2VecClusterable>> cluster(Collection<Word2VecClusterable> points) throws NullArgumentException {
        return super.cluster(points);
    }
}
