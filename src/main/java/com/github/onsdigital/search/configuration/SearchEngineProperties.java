package com.github.onsdigital.search.configuration;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

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

}
