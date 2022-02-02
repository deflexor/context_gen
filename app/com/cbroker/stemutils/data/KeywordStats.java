/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cbroker.stemutils.data;

import com.cbroker.utils.MapUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Структура данных - статисктика ключевых слов, создается в KeyowordFinder
 * 
 * 
 * @author dfr
 */
public class KeywordStats {

    public static class Score implements Comparable<Score> {
        
        private double score;
        private Set<String> keywordSet;

        public Score(String phrase) {
            keywordSet = new HashSet<String>(Arrays.asList(phrase.split("\\s+")));
        }

        private void addScore(double new_score) {
            score += new_score;
        }

        public int compareTo(Score t) {
            return Double.compare(getScore(), t.getScore());
        }

        public double getScore() {
            return score;
        }

        public Set<String> getKeywordSet() {
            return keywordSet;
        }

    }

    // just for alias
    public static class KeywordsMap extends HashMap<String, Score> { public KeywordsMap() {} }

    private int wordsTotal = 0;
    private final ArrayList<KeywordsMap> kwMaps = new ArrayList<KeywordsMap>();

    public void addWordsTotal(int length) {
        wordsTotal += length;
    }

    public int getWordsTotal() {
        return wordsTotal;
    }

    public boolean hasKeywordGroupWords(int i) {
        try {
            return kwMaps.get(i) != null;
        } catch (Exception e) {
            return false;
        }
    }

    public KeywordStats() {
        kwMaps.add(null);
        kwMaps.add(new KeywordsMap());
        kwMaps.add(new KeywordsMap());
    }

    public boolean containsKeyword(int nwords, String k) {
        return kwMaps.get(nwords).containsKey(k);
    }

    public KeywordsMap getStatsMap(int nwords) {
        return kwMaps.get(nwords);
    }

    public void keyWordAddScore(int nwords, String k, double new_score) {
        KeywordsMap map = kwMaps.get(nwords);
        Score score = map.get(k);
        if(score == null) score = new Score(k);
        score.addScore(new_score);
        //if("домен".equals(k)) System.err.println(nwords + " ? " +  k +" " + score.score);
        map.put(k, score);
    
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        for (int i = 1; i < kwMaps.size(); i++) {
            result.append(">>> Фразы сложностью: " + i + "\n");
            KeywordsMap map = kwMaps.get(i);
            List<Entry<String, Score>> sorted = MapUtils.sortByValueDesc(map);
            for (Entry<String, Score> entry : sorted) {
                result.append("  >" + entry.getKey() + " " + entry.getValue().getScore() + "\n");
            }
        }
        return result.toString();
    }

}
