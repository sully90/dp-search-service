package com.github.onsdigital.search.util;

import java.util.Comparator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * @author sullid (David Sullivan) on 18/12/2017
 * @project dp-search-service
 */
public class MapUtils {

    public static <K, V extends Comparable<? super V>> SortedSet<Map.Entry<K, V>> entriesSortedByValues(Map<K, V> map) {
        SortedSet<Map.Entry<K, V>> sortedEntries = new TreeSet<>(
                Comparator.comparing(Map.Entry::getValue));
        sortedEntries.addAll(map.entrySet());
        return sortedEntries;
    }

}
