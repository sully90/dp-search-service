package com.github.onsdigital.search.configuration;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

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

    public static String getProperty(String key) {
        return properties.getProperty(key);
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

    public static void main(String[] args) {
        System.out.println(getProperty("opennlp.ner.modes"));
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

}
