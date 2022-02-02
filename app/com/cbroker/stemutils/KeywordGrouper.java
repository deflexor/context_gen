/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cbroker.stemutils;

import com.cbroker.stemutils.data.KeywordGroup;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicates;
import com.google.common.base.Splitter;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import static com.google.common.base.Predicates.not;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Группировщик ключевых фраз
 * @author dfr
 */
public class KeywordGrouper {

    private static byte PHRASE_MAX_WORDS = 7;
    public static final Splitter WHITESPACE_SPLITTER = Splitter.on(' ').omitEmptyStrings().trimResults();

    /* @param keyphrases список улючевых фраз
     * @return Сортированный массив ключевых групп, в каждой группе также харнится
     * список включенных ключевых фраз
     */
    public static List<KeywordGroup> groupKeywords(Set<String> keyphrasesParam) {
        Map<String, KeywordGroup> groups = Maps.newHashMap();
        Set<String> keyphrases = Sets.newHashSet(keyphrasesParam);
        // по каждой длине фразы
        for (byte groupSize = 1; groupSize <= PHRASE_MAX_WORDS; groupSize++) {
            // выделим группы с кол-вом слов: groupSize
            groups = extractGroups(groupSize, groups, keyphrases);
            // заполним все группы ключевыми фразами подходящими под текущую группу
            Set<String> missedWords = fillWithKeywords(groups, groupSize, keyphrases);
            // для односложных групп не отслеживаем незадействованные фразы, а сразу
            // переходим к парсингу двусложных
            if (groupSize == 1) continue;
            
            if (missedWords.isEmpty()) {
                // если все фразы задействованы в группах, то на этом успокаиваемся
                // и заканчиваем парсинг
                break;
            } else {
                // если остались фразы без группы, то для них проводим следующую итерацию
                // цикла с увеличенной длинной фразы (groupSize)
                keyphrases = missedWords;
            }
        }
        // уберем keyWords для облегчения структуры (необязательный шаг)
        Map<String, KeywordGroup> liteGroups = Maps.transformValues(groups, new Function<KeywordGroup, KeywordGroup>() {
            public KeywordGroup apply(KeywordGroup g) {
                g.setKeyWords(null);
                return g;
            }
        });
        return Ordering.natural().sortedCopy(liteGroups.values());
    }

    private static Map<String, KeywordGroup> extractGroups(byte groupSize, Map<String, KeywordGroup> groups, Set<String> keyphrases) {
        for (String phrase : keyphrases) {
            List<String> allWords = Lists.newArrayList(WHITESPACE_SPLITTER.split(phrase));
            Collection<String> regularWords = Collections2.filter(allWords,
                    not(Predicates.containsPattern("^(\\+|-|!)")));
            if (regularWords.size() != groupSize) {
                continue;
            }
            Collection<String> stemmedWords = Collections2.transform(regularWords, new StemWordFunction());
            String key = Joiner.on(' ').join(Ordering.natural().sortedCopy(stemmedWords));
            String title = Joiner.on(' ').join(regularWords);
            if (!groups.containsKey(key)) {
                KeywordGroup group = new KeywordGroup(title, new HashSet<String>(stemmedWords), Joiner.on(' ').join(allWords));
                groups.put(key, group);
            }
        }
        return groups;
    }

    private static Set<String> fillWithKeywords(Map<String, KeywordGroup> groups, byte groupSize, Set<String> keyphrases) {
        Set<String> missedWords = Sets.newHashSet();
        byte minWords = (byte) ((groupSize == 1) ? 1 : 2);
        for (String phrase : keyphrases) {

            List<String> allWords = Lists.newArrayList(WHITESPACE_SPLITTER.split(phrase));
            Collection<String> words = Collections2.filter(allWords, not(Predicates.containsPattern("^(\\+|-|!)")));
            if (words.size() < minWords || words.size() > PHRASE_MAX_WORDS) {
                continue;
            }
            Set<String> stemmedWords = Sets.newHashSet(Collections2.transform(words, new StemWordFunction()));
            int found = 0;
            for (String k : groups.keySet()) {
                Set<String> groupKeyWords = groups.get(k).getKeyWords();
                if (groupKeyWords.size() == 1 && words.size() > 2) {
                    continue;
                }
                if (stemmedWords.containsAll(groupKeyWords)) {
                    groups.get(k).addKeyPhrase(Joiner.on(' ').join(allWords));
                    found++;
                }
            }
            if (found == 0) {
                missedWords.add(phrase);
            }
        }
        return missedWords;
    }

    private static class StemWordFunction implements Function<String, String> {

        public String apply(String word) {
            return Morphology.stemWord(word);
        }
    }
}
