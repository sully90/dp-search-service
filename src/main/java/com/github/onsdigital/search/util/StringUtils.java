package com.github.onsdigital.search.util;

import java.util.*;

/**
 * @author sullid (David Sullivan) on 21/11/2017
 * @project SearchEngine
 */
public class StringUtils {

    public static List<String> generateNgramsUpto(String str, int maxGramSize) {

        List<String> sentence = Arrays.asList(str.split("(?!\\p{Sc})[\\W+]"));
//        List<String> sentence = Arrays.asList(str.split("\\s+"));

        List<String> ngrams = new LinkedList<>();
        int ngramSize = 0;
        StringBuilder sb = null;

        //sentence becomes ngrams
        for (ListIterator<String> it = sentence.listIterator(); it.hasNext();) {
            String word = it.next();

            //1- add the word itself
            sb = new StringBuilder(word);
            ngrams.add(word);
            ngramSize=1;
            it.previous();

            //2- insert prevs of the word and add those too
            while(it.hasPrevious() && ngramSize<maxGramSize){
                sb.insert(0,' ');
                sb.insert(0,it.previous());
                ngrams.add(sb.toString());
                ngramSize++;
            }

            //go back to initial position
            while(ngramSize>0){
                ngramSize--;
                it.next();
            }
        }
        return ngrams;
    }

    public static int countWords(String content) {
        if (content == null || content.isEmpty()) {
            return 0;
        }

        String[] words = content.split("\\s+");
        return words.length;
    }

    public static boolean containsCurrencySymbol(String str) {
        for (char c : str.toCharArray()) {
            if (Character.getType(c) == Character.CURRENCY_SYMBOL) {
                return true;
            }
        }
        return false;
    }

    public static <T> List<List<T>> combination(List<T> values, int size) {

        if (0 == size) {
            return Collections.singletonList(Collections.<T> emptyList());
        }

        if (values.isEmpty()) {
            return Collections.emptyList();
        }

        List<List<T>> combination = new LinkedList<List<T>>();

        T actual = values.iterator().next();

        List<T> subSet = new LinkedList<T>(values);
        subSet.remove(actual);

        List<List<T>> subSetCombination = combination(subSet, size - 1);

        for (List<T> set : subSetCombination) {
            List<T> newSet = new LinkedList<T>(set);
            newSet.add(0, actual);
            combination.add(newSet);
        }

        combination.addAll(combination(subSet, size));

        return combination;
    }

}
