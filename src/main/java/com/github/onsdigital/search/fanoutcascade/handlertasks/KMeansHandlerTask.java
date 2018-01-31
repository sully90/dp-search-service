package com.github.onsdigital.search.fanoutcascade.handlertasks;

import com.github.onsdigital.fanoutcascade.handlertasks.HandlerTask;

import java.util.List;

/**
 * @author sullid (David Sullivan) on 31/01/2018
 * @project dp-search-service
 */
public class KMeansHandlerTask extends HandlerTask {

    private final List<String> words;

    public KMeansHandlerTask(List<String> words) {
        super(KMeansHandlerTask.class);
        this.words = words;
    }

    public List<String> getWords() {
        return words;
    }
}
