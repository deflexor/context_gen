package com.cbroker.stemutils;

import static com.cbroker.utils.Constants.FORMAT2;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import com.cbroker.stemutils.data.KeywordStats.KeywordsMap;
import com.cbroker.stemutils.data.KeywordStats.Score;
import com.cbroker.stemutils.data.SiteSection;
import com.cbroker.utils.MapUtils;
import com.cbroker.utils.MiniInfo;
import com.google.common.collect.Sets;
import com.google.common.primitives.Doubles;

public class RelevantPageFinderHybrid {

    private static String[] colors = new String[] { "#EEFFEE", "#FFEEFF",
            "#EEFFFF", "#FFEEEE", "#FFFFEE" };

    /*
     * Находим релевантную страницу: где чаще встречается фраза(название группы) целиком
     */
    public static String findRelevantSectionForGroup(String group,
            List<SiteSection> sections) throws Exception {
        HashMap<String, Pair> pscores = new HashMap<String, Pair>();
        HashMap<String, Double> p1scores = new HashMap<String, Double>();
        HashMap<String, Double> p2scores = new HashMap<String, Double>();
        Set<String> groupSet = RelevantPageFinder.makeGroupSet(group);
        // для каждого раздела найдем макс P
        for (SiteSection siteSection : sections) {
            //System.err.println("url:" + siteSection.getUrl().toString());
            //System.err.println("домен:" + siteSection.getKeywordStats().getStatsMap(1).get("домен").score);
            String url = siteSection.getUrl().toString();
            int wordsTotal = siteSection.getKeywordStats().getWordsTotal();
            // получим карту ключевых фраз
            int len = RelevantPageFinder.findKeywordsLength(group, siteSection);
            KeywordsMap kwMap = siteSection.getKeywordStats().getStatsMap(len);
            // для каждой фразы
            for (String k : kwMap.keySet()) {
                Score score = kwMap.get(k);
                // если она совпадает с названием группы
                double p1 = 0;
                double scorescore = 0;
                //MiniInfo.logln("фраза: '" + k);
                if (groupSet.equals(score.getKeywordSet())) {
                    // запомним число P для этого раздела
                    scorescore = score.getScore();
                    p1 = score.getScore() * groupSet.size() / wordsTotal * 100;
                    /*MiniInfo.logln("Pa = " + FORMAT2.format(score.score) + " * "
                     + groupSet.size() + " / "
                     + wordsTotal
                     + " * 100 = " + FORMAT2.format(p1));*/

                }
                Pair p2 = planBCoeff(groupSet, siteSection, scorescore, 
                        (wordsTotal < 200) ? 200 : wordsTotal);
                double p = p1 + p2.v;
                /*MiniInfo.logln("P = Pa + Pb = " + FORMAT2.format(p1) + " + "
                        + FORMAT2.format(p2) + " = " + FORMAT2.format(p));
                MiniInfo.logln("");*/
                Pair pscore = pscores.get(url);
                Double p1Max = p1scores.get(url);
                if ((pscore == null || p1Max < p1) && p >= 5) {
                    pscores.put(url, new Pair(p, p2.i));
                    p1scores.put(url, p1);
                    p2scores.put(url, p2.v); // TODO debug
                }
            }
        }
        if (pscores.isEmpty()) {
            //MiniInfo.logln("No result");
            return null;
        } else {
            // выберем раздел с наибольшим P и вернем его URL
            List<Entry<String, Pair>> sortedPscores = MapUtils
                    .sortByValueDesc(pscores);
            // --------- DEBUG
            /*
            MiniInfo.logln("План гибрид:");
            for (Entry<String, Pair> entry : sortedPscores) {
                MiniInfo.logln(entry.getKey() + ": <b>"
                        + FORMAT2.format(p1scores.get(entry.getKey()))
                        + " + "
                        //+ entry.getValue().i
                        + FORMAT2.format(p2scores.get(entry.getKey()))
                        + " = "
                        + FORMAT2.format(entry.getValue().v) + "</b>");
            }
            */
            // ---------
            return sortedPscores.get(0).getKey();
        }
    }

    private static Pair planBCoeff(Set<String> groupSet,
            SiteSection siteSection, double pscore, int wordsTotal) {
        // выберем односложные фразы
        final KeywordsMap kwMap = siteSection.getKeywordStats().getStatsMap(1);
        // выберем только фразы, которые встречаются в группе
        // и сортируем по score
        String[] keys1 = new String[kwMap.keySet().size()];
        short c = 0;
        for (String k : groupSet) {
            if (kwMap.containsKey(k)) {
                keys1[c++] = k;
            }
        }
        String[] keys = Arrays.copyOf(keys1, c);


        Arrays.sort(keys, new Comparator<String> () {
            @Override
            public int compare(String arg0, String arg1) {
                return Doubles.compare(kwMap.get(arg1).getScore(),
                        kwMap.get(arg0).getScore());
            }
        });
        
        
        double result = 0.0;
        // спец формула: R = K1 * x + (K2 - K1) * y + (K3 - K2) ...
        //MiniInfo.log("Pb = ");
        String planBinfo = "";
        for (short i = (short) (groupSet.size() - 1); i >= 0; i--) {
            String word = (i <= c - 1) ? keys[i]
                    : null;
            String prev_word = (i < c - 1) ? keys[i + 1] : null;
            double freq = word != null ? kwMap.get(word).getScore() : 0.0;
            double prev_freq = (prev_word != null) ? kwMap.get(prev_word).getScore()
                    : 0.0;
            double w = Math.pow(2, i + 1) - 1;
            //double sl1 = (i == groupSet.size() - 1) ?
            //        Math.abs(pscore + prev_freq - freq) : 
            //        w * Math.atan(Math.abs(pscore + prev_freq - freq) / wordsTotal * 100);
            double pscore_if = (i == groupSet.size() - 1) ? pscore : 0;
            double sl1 = Math.atan(Math.abs(pscore_if + prev_freq - freq ) / Math.sqrt(wordsTotal)*10);
            result += sl1 * w;
            // DEBUG
            /*
            planBinfo += ((i == groupSet.size() - 1) ? "" : " + ")
                    + "<span style=\"background-color: " + colors[i] + "\">"
                    + word
                    //+ ((i == groupSet.size() - 1) ? "(abs(" : "(atan2(abs(")
                    + "(atan(abs("
                    + "<span style=\"background-color: " +colors[4]+ "\">" + FORMAT2.format(pscore_if) + "</span>"
                    + " + " + FORMAT2.format(prev_freq) + " - " + FORMAT2.format(freq)
                    //+ ((i == groupSet.size() - 1) ? "))*" : "))*")
                    + ") / " + FORMAT2.format(Math.sqrt(wordsTotal)) + " *10" + ")*"
                    + FORMAT2.format(w) + "</span>";
            //if (i == 0) planBinfo += " / " + wordsTotal + "*100";
            //if (i == 0) planBinfo += "*100";
            */
            // DEBUG
        }
        //double r = result / wordsTotal * 100;
        double r = result;
        //MiniInfo.logln(" = " + FORMAT2.format(result) + " / " + wordsTotal
        //        + "*100" + " = " + FORMAT2.format(r));
        // сохраняем макс R для каждого раздела
        return new Pair(r, planBinfo);
    }
    
    public static class Pair implements Comparable {
        public double v;
        public String i;
        public Pair(double v, String i) {
            this.v = v;
            this.i = i;
        }
        @Override
        public int compareTo(Object o) {
            // TODO Auto-generated method stub
            return Doubles.compare(v, ((Pair)o).v);
        }
    }

}
