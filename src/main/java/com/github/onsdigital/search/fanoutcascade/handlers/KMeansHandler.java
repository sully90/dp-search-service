package com.github.onsdigital.search.fanoutcascade.handlers;

import com.github.onsdigital.fanoutcascade.handlers.Handler;
import com.github.onsdigital.fanoutcascade.handlertasks.HandlerTask;
import com.github.onsdigital.search.configuration.SearchEngineProperties;
import com.github.onsdigital.search.fanoutcascade.handlertasks.KMeansHandlerTask;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.deeplearning4j.clustering.cluster.Cluster;
import org.deeplearning4j.clustering.cluster.ClusterSet;
import org.deeplearning4j.clustering.cluster.Point;
import org.deeplearning4j.clustering.kmeans.KMeansClustering;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author sullid (David Sullivan) on 31/01/2018
 * @project dp-search-service
 */
public class KMeansHandler implements Handler {
    @Override
    public Object handleTask(HandlerTask handlerTask) throws Exception {
        KMeansHandlerTask task = (KMeansHandlerTask) handlerTask;

        return null;
    }

    private static ClusterSet word2Vec(List<String> words) {
        Word2Vec word2Vec = SearchEngineProperties.WORD2VEC.getWord2vec();

        // Remove stop words
//        List<String> wordsToProcess = new ArrayList<>();
//        Collection<String> stopWords = word2Vec.getStopWords();
//        for (String word : words) {
//            if (!stopWords.contains(word)) {
//                wordsToProcess.add(word);
//            }
//        }

        long startTime = System.currentTimeMillis();

        int initialClusterCount = 10;
//        int maxIterationCount = 1000;
        double minDistributionVariationRate = 0.025;
        boolean allowEmptyClusters = true;

        String distanceFunction = "euclidean";
        KMeansClustering kMeansClustering = KMeansClustering.setup(initialClusterCount, minDistributionVariationRate, distanceFunction, allowEmptyClusters);

        List<INDArray> vectors = new ArrayList<>();
        // Populate vectors list
        for (String word : words) {
            INDArray vector;
            if (word.contains("\\s")) {
                // Construct new vector which is a linear sum of each part vector
                vector = Nd4j.zeros(300);
                for (String part : word.split("\\s")) {
                    INDArray partVec = Nd4j.create(word2Vec.getWordVector(part));
                    vector.add(partVec);
                }
            } else {
                double[] vec = word2Vec.getWordVector(word);
                vector = Nd4j.create(vec);
            }
            vectors.add(vector);
        }

        List<Point> points = Point.toPoints(vectors);
        ClusterSet clusterSet = kMeansClustering.applyTo(points);

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        System.out.format("KMeans: Milli = %s, ( S_Start : %s, S_End : %s ) \n", duration, startTime, endTime );
        System.out.println("Human-Readable format : "+ millisToShortDHMS( duration ) );

        return clusterSet;
    }

    private static void pca(List<INDArray> vectors) {
        int nDims = vectors.get(0).length();
        int shape[] = {vectors.size(), nDims};
        INDArray dataset = Nd4j.create(vectors, shape);

        boolean normalize = true;
//        PCA pca = PCA.pca(dataset, nDims, normalize);
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

    public static void main(String[] args) {
        // From GA on 15/12/17
        Logger.getRootLogger().setLevel(Level.ERROR);
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

        Word2Vec word2Vec = SearchEngineProperties.WORD2VEC.getWord2vec();

        List<String> words = new ArrayList<>();
        for (String term : searchTerms) {
            words.add(term);
            words.addAll(word2Vec.wordsNearest(term, 10));
        }

        ClusterSet clusterSet = word2Vec(words);

        for (Cluster cluster : clusterSet.getClusters()) {
            System.out.println(cluster.getId());
            System.out.println(cluster.getPoints().size());
        }
    }
}
