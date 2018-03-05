package com.github.onsdigital.search.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * @author sullid (David Sullivan) on 05/03/2018
 * @project dp-search-service
 */
public class JsonUtils<T> {

    private static final ObjectMapper DEFAULT_MAPPER = new ObjectMapper();

    private Class<T> asType;
    private ObjectMapper mapper;

    public JsonUtils(Class<T> asType) {
        this(asType, DEFAULT_MAPPER);
    }

    public JsonUtils(Class<T> asType, ObjectMapper mapper) {
        this.asType = asType;
        this.mapper = mapper;
    }

    public T fromJsonString(String json) throws IOException {
        return this.mapper.readValue(json, this.asType);
    }

    public static String toJson(Object object) throws JsonProcessingException {
        return DEFAULT_MAPPER.writeValueAsString(object);
    }

}
