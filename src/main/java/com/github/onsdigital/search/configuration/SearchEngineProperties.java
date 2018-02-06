package com.github.onsdigital.search.configuration;

import org.apache.commons.compress.compressors.gzip.GzipUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.deeplearning4j.models.embeddings.inmemory.InMemoryLookupTable;
import org.deeplearning4j.models.embeddings.learning.impl.elements.SkipGram;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.word2vec.VocabWord;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.deeplearning4j.models.word2vec.wordstore.VocabCache;
import org.deeplearning4j.models.word2vec.wordstore.inmemory.AbstractCache;
import org.deeplearning4j.text.sentenceiterator.BasicLineIterator;
import org.deeplearning4j.text.sentenceiterator.SentenceIterator;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.ops.transforms.Transforms;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.nio.ch.IOUtil;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

/**
 * @author sullid (David Sullivan) on 22/11/2017
 * @project dp-search-service
 */
public class SearchEngineProperties {

    private static Properties properties;

    static {
        properties = new Properties();
        InputStream input = null;

        try {

            input = new FileInputStream("conf/application.conf");

            // load a properties file
            properties.load(input);

        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public String getGroupProperty(String group, String key) {
        return getProperty(group + '.' + key);
    }

    public static boolean keyExists(String key) {
        return properties.containsKey(key);
    }

    public static String getProperty(String key) {
        return properties.getProperty(key);
    }

    public static boolean getBoolProperty(String key) {
        return getProperty(key).equals("true");
    }

    public static String[] getPropertyArray(String key) {
        return getPropertyArray(key, ",");
    }

    public static String[] getPropertyArray(String key, String delimiter) {
        return properties.getProperty(key).split(delimiter);
    }

    public static String filenameInClasspath(String filename) {
        return Thread.currentThread().getContextClassLoader().getResource(filename).getFile();
    }

    public static class LTR {
        private static final String DEFAULT_MODEL_KEY = "elastic.ltr.default_model";

        public static String getDefaultModel() {
            return getProperty(DEFAULT_MODEL_KEY);
        }
    }

    public static class OPENNLP {
        private static final String NER_ENABLED_MODELS_KEY = "opennlp.ner.models";
        private static final String NER_CONFIGURATION_KEY = "opennlp.ner.model.file";

        public static String getNerEnabledModelsKey() {
            return NER_ENABLED_MODELS_KEY;
        }

        public static String getNerConfigurationKey() {
            return NER_CONFIGURATION_KEY;
        }
    }

    public static class FANOUTCASCADE {
        private static final String PERFORMANCE_CHECKER_TIMEUNIT_KEY = "fanoutcascade.performancechecker.sleep.timeunit";
        private static final String PERFORMANCE_CHECKER_VALUE_KEY = "fanoutcascade.performancechecker.sleep.value";

        private static final String SUBMIT_TIMEUNIT_KEY = "fanoutcascade.submit.timeunit";
        private static final String SUBMIT_VALUE_KEY = "fanoutcascade.submit.value";

        public static TimeUnit getPerformanceCheckerSleepTimeUnit() {
            return TimeUnit.valueOf(getProperty(PERFORMANCE_CHECKER_TIMEUNIT_KEY));
        }

        public static long getPerformanceCheckerSleepValue() {
            return Long.valueOf(getProperty(PERFORMANCE_CHECKER_VALUE_KEY));
        }

        public static TimeUnit getSubmitTimeUnit() {
            return TimeUnit.valueOf(getProperty(SUBMIT_TIMEUNIT_KEY));
        }

        public static long getSubmitValue() {
            return Long.valueOf(getProperty(SUBMIT_VALUE_KEY));
        }
    }

    public static class WORD2VEC {

        private static final Logger LOGGER = LoggerFactory.getLogger(WORD2VEC.class);

        private static Word2Vec word2vec;

        public static List<String> getStopWords() {
            ClassLoader classLoader = WORD2VEC.class.getClassLoader();
            File stopFile = new File(classLoader.getResource(String.format("vectorModels/%s", "stop.txt")).getFile());
            List<String> stopWords = new ArrayList<>();

            try (BufferedReader br = new BufferedReader(new FileReader(stopFile))) {
                String currentLine;

                while ((currentLine = br.readLine()) != null) {
                    stopWords.add(currentLine);
                }
            } catch (IOException e) {
                LOGGER.warn("Unable to locate stop words file, returning empty list");
            }
            return stopWords;
        }

        private static File getVectorsFile(Models model) {
            ClassLoader classLoader = WORD2VEC.class.getClassLoader();
            File file = new File(classLoader.getResource(String.format("vectorModels/%s", model.getFileName())).getFile());
            return file;
        }

        public static Word2Vec getWord2vec() {
            return getWord2vec(Models.GOOGLE_SLIM);
        }

        public static Word2Vec getWord2vec(Models model) {
            if (word2vec == null) {
                File gModel = getVectorsFile(model);
                word2vec = WordVectorSerializer.readWord2VecModel(gModel);
            }
            return word2vec;
        }

        public static Word2Vec readBinaryModel(Models model) throws IOException {
            File gModel = getVectorsFile(model);
            return readBinaryModel(gModel, false, false);
        }

        private static Word2Vec readBinaryModel(File modelFile, boolean linebreaks, boolean normalize)
                throws NumberFormatException, IOException {
            InMemoryLookupTable<VocabWord> lookupTable;
            VocabCache<VocabWord> cache;
            INDArray syn0;
            int words, size;

            int originalFreq = Nd4j.getMemoryManager().getOccasionalGcFrequency();
            boolean originalPeriodic = Nd4j.getMemoryManager().isPeriodicGcActive();

            if (originalPeriodic)
                Nd4j.getMemoryManager().togglePeriodicGc(false);

            Nd4j.getMemoryManager().setOccasionalGcFrequency(50000);

            try (BufferedInputStream bis = new BufferedInputStream(GzipUtils.isCompressedFilename(modelFile.getName())
                    ? new GZIPInputStream(new FileInputStream(modelFile)) : new FileInputStream(modelFile));
                 DataInputStream dis = new DataInputStream(bis)) {
                words = Integer.parseInt(WordVectorSerializer.readString(dis));
                size = Integer.parseInt(WordVectorSerializer.readString(dis));
                syn0 = Nd4j.create(words, size);
                cache = new AbstractCache<>();

                WordVectorSerializer.printOutProjectedMemoryUse(words, size, 1);

                lookupTable = (InMemoryLookupTable<VocabWord>) new InMemoryLookupTable.Builder<VocabWord>().cache(cache)
                        .useHierarchicSoftmax(false).vectorLength(size).seed(1).negative(5).build();

                String word;
                float[] vector = new float[size];
                for (int i = 0; i < words; i++) {

                    word = WordVectorSerializer.readString(dis);
                    LOGGER.trace("Loading " + word + " with word " + i);

                    for (int j = 0; j < size; j++) {
                        vector[j] = WordVectorSerializer.readFloat(dis);
                    }

                    syn0.putRow(i, normalize ? Transforms.unitVec(Nd4j.create(vector)) : Nd4j.create(vector));


                    VocabWord vw = new VocabWord(1.0, word);
                    vw.setIndex(cache.numWords());

                    cache.addToken(vw);
                    cache.addWordToIndex(vw.getIndex(), vw.getLabel());

                    cache.putVocabWord(word);

                    if (linebreaks) {
                        dis.readByte(); // line break
                    }

                    Nd4j.getMemoryManager().invokeGcOccasionally();
                }
            } finally {
                if (originalPeriodic)
                    Nd4j.getMemoryManager().togglePeriodicGc(true);

                Nd4j.getMemoryManager().setOccasionalGcFrequency(originalFreq);
            }

            lookupTable.setSyn0(syn0);

            Word2Vec ret = new Word2Vec.Builder()
                    .useHierarchicSoftmax(true)
                    .resetModel(true)
                    .layerSize(syn0.columns())
                    .allowParallelTokenization(true)
                    .learningRate(0.025)
                    .seed(12345)
                    .minLearningRate(1e-4)
                    .elementsLearningAlgorithm(new SkipGram<>())
                    .iterations(5)
                    .workers(4)
                    .stopWords(getStopWords())
                    .windowSize(10)
                    .workers(4)
                    .minWordFrequency(10)
                    .sampling(1e-3)
                    .vocabCache(cache)
                    .lookupTable(lookupTable)
                    .build();

            return ret;
        }

        public enum Models {
            GOOGLE("GoogleNews-vectors-negative300.bin.gz"),
            GOOGLE_SLIM("GoogleNews-vectors-negative300-SLIM.bin.gz"),
            ONS_GZIP("ons_w2v_model.bin.gz");

            private String fileName;

            Models(String fileName) {
                this.fileName = fileName;
            }

            public String getFileName() {
                return fileName;
            }
        }
    }

}
