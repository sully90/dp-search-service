package com.github.onsdigital.search.nlp.opennlp;

import com.github.onsdigital.search.configuration.SearchEngineProperties;
import com.github.onsdigital.search.util.StringUtils;
import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.tokenize.WhitespaceTokenizer;
import opennlp.tools.util.Span;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author sullid (David Sullivan) on 22/11/2017
 * @project dp-search-service
 *
 * Service to handle NLP tasks i.e Named Entity Recognition
 */
public class OpenNlpService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenNlpService.class);

    // TokenNameFinder is not thread safe, so use a threadLocal hack
    private ThreadLocal<TokenNameFinderModel> threadLocal = new ThreadLocal<>();
    private Map<String, TokenNameFinderModel> nameFinderModels = new ConcurrentHashMap<>();

    protected double probabilityLowerLimit = 0.75d;

    private static OpenNlpService INSTANCE = new OpenNlpService();

    public static OpenNlpService getInstance() {
        return INSTANCE;
    }

    private OpenNlpService() {
        String[] enabledModels = SearchEngineProperties.getPropertyArray(
                SearchEngineProperties.OPENNLP.getNerEnabledModelsKey()
        );

        StringBuilder sb;
        for (String model : enabledModels) {
            sb = new StringBuilder(SearchEngineProperties.OPENNLP.getNerConfigurationKey())
                    .append(".")
                    .append(model);

            String configKey = sb.toString();
            String modelname = SearchEngineProperties.getProperty(configKey);
            if (null != modelname) {
                TokenNameFinderModel tokenNameFinderModel = getTokenNameFinderModel(modelname);
                if (null != tokenNameFinderModel) {
                    this.nameFinderModels.put(model, tokenNameFinderModel);
                }
            } else {
                LOGGER.error("Unable to locate opennlp ner model: {}", model);
            }
        }
    }

    public Set<String> availableModels() {
        return this.nameFinderModels.keySet();
    }

    public Set<String> find(String content, String field) {
        try {
            if (!nameFinderModels.containsKey(field)) {
                throw new RuntimeException(String.format("Could not find field [%s], possible values %s", field, nameFinderModels.keySet()));
            }

            TokenNameFinderModel model = nameFinderModels.get(field);
            if (threadLocal.get() == null || !threadLocal.get().equals(model)) {
                threadLocal.set(model);
            }
            Set<String> nameSet = new HashSet<>();

            TokenizerME tokenizer = new TokenizerME(loadTokenizerModel());

            int maxNgramSize = WhitespaceTokenizer.INSTANCE.tokenize(content).length;
            List<String> nGrams = StringUtils.generateNgramsUpto(content, maxNgramSize);

            for (String nGram : nGrams) {
                String[] tokens = tokenizer.tokenize(field + " " + nGram);

                // Perform the named entity extraction
                Span[] spans = new NameFinderME(model).find(tokens);
                String[] names = Span.spansToStrings(spans, tokens);

                // Add to the named entity set if we pass the confidence test
                for (int i = 0; i < names.length; i++) {
                    String name = names[i];
                    Span span = spans[i];
//                    System.out.println(field + " : " + name + " : " + span.getProb());
                    if (span.getProb() >= this.probabilityLowerLimit && content.contains(name)) nameSet.add(name);
                }
            }

            return nameSet;
        } finally {
            threadLocal.remove();
        }
    }

    public Map<String, Set<String>> getNamedEntities(String content) {
        Set<String> modelKeySet = this.nameFinderModels.keySet();
        String[] models = modelKeySet.stream().toArray(String[]::new);
        return this.getNamedEntities(content, models);
    }

    public Map<String, Set<String>> getNamedEntities(String content, String[] models) {
        Map<String, Set<String>> namedEntities = new HashMap<>();

        for (String model : models) {
            if (nameFinderModels.containsKey(model)) {
                namedEntities.put(model, this.find(content, model));
            } else {
                throw new RuntimeException(String.format("Could not find field [%s], possible values %s", model, nameFinderModels.keySet()));
            }
        }

        return namedEntities;
    }

    public Map<String, TokenNameFinderModel> getNameFinderModels() {
        return nameFinderModels;
    }

    private static TokenNameFinderModel getTokenNameFinderModel(String modelname) {
        String filename = SearchEngineProperties.filenameInClasspath(modelname);

        try(InputStream is = new FileInputStream(filename)) {
            LOGGER.info("Loading TokenNameFinderModel: {}", modelname);
            return new TokenNameFinderModel(is);
        } catch (FileNotFoundException e) {
            LOGGER.error("Error loading TokenNameFinderModel {}: File not found.", modelname, e);
        } catch (IOException e) {
            LOGGER.error("Error loading TokenNameFinderModel {}.", modelname, e);
        }
        return null;
    }

    private static TokenizerModel loadTokenizerModel() {
        String filename = SearchEngineProperties.filenameInClasspath("en-token.bin");

        try(InputStream is = new FileInputStream(filename)) {
            return new TokenizerModel(is);
        } catch (FileNotFoundException e) {
            LOGGER.error("Error loading TokenizerModel {}: File not found.", filename, e);
        } catch (IOException e) {
            LOGGER.error("Error loading TokenizerModel {}.", filename, e);
        }
        return null;
    }

}
