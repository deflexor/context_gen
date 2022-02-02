/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cbroker.stemutils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.cbroker.stemutils.data.KeywordStats;
import com.cbroker.utils.Permute;
import com.google.common.base.Function;
import com.google.common.base.Splitter;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;

/**
 * Ищет список ключевых слов в DOM документе
 * 
 * @author dfr
 */
public class KeywordFinder {

    private final Document doc;
    private final Map<String, String> stemToNormal = new HashMap<String, String>();
    private final String RE_INTERJOINS = "^(.|\\d+|еще|него|сказать|а|ж|нее|со|без|же|"
            + "ней|совсем|более|жизнь|нельзя|так|больше|за|нет|такой|будет|зачем|ни|там|"
            + "будто|здесь|нибудь|тебя|бы|и|никогда|тем|был|из|ним|теперь|была|из-за|них|"
            + "то|были|или|ничего|тогда|было|им|но|того|быть|иногда|ну|тоже|в|их|о|только|"
            + "вам|к|об|том|вас|кажется|один|тот|вдруг|как|он|три|ведь|какая|она|тут|во|"
            + "какой|они|ты|вот|когда|опять|у|впрочем|конечно|от|уж|все|которого|перед|уже|"
            + "всегда|которые|по|хорошо|всего|кто|под|хоть|всех|куда|после|чего|всю|ли|потом|"
            + "человек|вы|лучше|потому|чем|г|между|почти|через|где|меня|при|что|говорил|мне|про|"
            + "чтоб|да|много|раз|чтобы|даже|может|разве|чуть|два|можно|с|эти|для|мой|сам|этого|"
            + "до|моя|свое|этой|другой|мы|свою|этом|его|на|себе|этот|ее|над|себя|эту|ей|надо|"
            + "сегодня|я|ему|наконец|сейчас|если|нас|сказал|есть|не|сказала)$";
    private final String LOOK_IN_TAGS_STR = "title|h1|h2|h3|h4|h5|h6|p|div|span|td|li|a|b|strong|i|small|font|form|input|option|label|button";
    //private final Pattern RE_LOOK_IN_TAGS1 = Pattern.compile("<(" + LOOK_IN_TAGS_STR + ")", Pattern.CASE_INSENSITIVE);
    //private final Pattern RE_LOOK_IN_TAGS = Pattern.compile("^(" + LOOK_IN_TAGS_STR + "|function)$", Pattern.CASE_INSENSITIVE);
    //private final Pattern RE_TEXT_OK_TAGS = Pattern.compile("^(br|b|)$", Pattern.CASE_INSENSITIVE);
    private final int MAX_KEYWORD_LENGTH = 2;
    private final KeywordStats kwk = new KeywordStats();
    //private final Function<Element, String> EXTRACT_TAGNAME_FUNC = new ExtractTagNameFunction();
    //private final Predicate<Element> HAS_TEXT_PREDICATE = new ElementHasTextPredicate();

    public KeywordFinder(Document doc) {
        this.doc = doc;
    }

    /*
     * Находит ключевые слова и генеир структуру KeywWordsStats
     * @return структура со статистикой ключевых фраз
     */
    public KeywordStats findKeywords() {
        Iterable<String> lookInTags = Splitter.on("|").split(LOOK_IN_TAGS_STR);
        Elements result = doc.getAllElements();
        String baseUri = doc.baseUri();
        for (Element node : result) {
            if (!Iterables.contains(lookInTags, node.tagName())) {
                continue;
            }
            // отберем элементы только с нижнего уровня иерархии
            //if(RE_LOOK_IN_TAGS1.matcher(node.html()).matches()) continue; // TODO: not working, why?
            //Iterable<String> childTags = Iterables.transform(node.children(), EXTRACT_TAGNAME_FUNC);
            //if (Iterables.any(node.children(), HAS_TEXT_PREDICATE)) {
            //    continue;
            //}

            String nodeText = node.ownText();
            List<Passage> passages = findPassages(nodeText);
            // also some strange child nodes ?
            /* for (Element child : node.getAllElements()) {
            if(child.hasText() && !RE_LOOK_IN_TAGS.matcher(child.tagName()).matches()) {
            passages = Lists.newArrayList( Iterables.concat(passages, findPassages(child.ownText())) );
            }
            }
             */

            // подсчет коэффициента в зависимости от полезности текста
            double factor = 1;
            String tagName = node.tagName();
            // наиболее полезный текст в заголовке
            if ("title".equals(tagName)) {
                factor = 5;
            }
            if (tagName.matches("^h\\d+$")) {
                factor = 2;
            }
            // текст в ссылке ведущей на себя - еще более полезный ?
            if ("a".equals(tagName) && baseUri != null && baseUri.equals(node.attr("abs:href"))) {
                factor = 5.02;
            }

            //System.err.println("f:" + factor);
            //System.err.println(">> " + Joiner.on(">>").join(passages));

            for (Passage passage : passages) {
                calcKeywords(passage, factor);
            }
        }

        // here we have nwords and kwk filled with goodness
        // TODO: beautify
        return kwk;
    }

    /*
     * Заполняет структуру KeywWordsStats, данными с переданного корпуса
     * @param corpus текст корпуса
     * @param factor число, которое добавляется в стаистику по этому корпусу
     */
    private void calcKeywords(Passage passage, double factor) {
        for (int l = 1; l <= MAX_KEYWORD_LENGTH; l++) {
            List<List<String>> keywords = findKeywords(passage.text, l);
            for (List<String> kw : keywords) {
                for (Iterator permuter = new Permute(kw.toArray()); permuter.hasNext();) {
                    Object[] next = (Object[]) permuter.next();
                    final String[] wordsVariant = new String[next.length];
                    System.arraycopy(next, 0, wordsVariant, 0, next.length);
                    //final String[] kw_variant = Arrays.copyOf(next, next.length, String[].class);

                    final String k = StringUtils.join(wordsVariant, ' ');

                    double multiply = 
                            (next.length == 1) ? 1 :
                                (factor == 5.02) ? 1
                                : passage.findMultiplyFor(wordsVariant);
                    
                    if (kwk.containsKeyword(next.length, k)) {
                        updateStemToNormal(k, wordsVariant);
                        //if("домен".equals(k)) System.out.println(k + ": ("  + passage.origText + ") = " + FORMAT2.format(kwk.getStatsMap(next.length).get(k).score) + " + " + factor*multiply);
                        kwk.keyWordAddScore(next.length, k, factor * multiply);
                        break;
                    } else if (!permuter.hasNext()) {
                        updateStemToNormal(k, wordsVariant);
                        //if("домен".equals(k)) System.err.println(k + ": ("  + passage.text + ") = 0 + " + factor*multiply);
                        kwk.keyWordAddScore(next.length, k, factor * multiply);
                    }

                }
            }
        }
        int passageSize = Iterables.size(KeywordGrouper.WHITESPACE_SPLITTER.split(passage.text));
        //if (passageSize > 0) {
        //    System.err.println("total: " + kwk.getWordsTotal() + " -> " + passageSize + " (" + passage + ")");
        //}
        kwk.addWordsTotal(passageSize);
    }

    /*
     * Делим текст на список предложений (корпусов)
     * @param text
     * @return список корпусов
     */
    private List<Passage> findPassages(String text) {
        //
        String[] passage_array = text
                .replace(String.valueOf((char) 0xa0), " ")
                .replaceAll("[-()!?\"«»]", "")
                .split("[,;:]+|\\.\\s");
        List<Passage> passages = Lists.newArrayList();
        for (int i = 0; i < passage_array.length; i++) {
            passages.add(new Passage(passage_array[i], filterString(passage_array[i])));
        }
        return passages;
    }

    /*
     * Фильтровать строку
     * Отфильтровываем все, кром рус и англ букв, убираем одиноко стоящие цифры и лишние пробелы
     * @param str строка
     * @return фильтрованная строка
     */
    private String filterString(String str) {

        //String[] words = str.trim().split("\\s+");
        String[] words = Iterables.toArray(KeywordGrouper.WHITESPACE_SPLITTER.split(str), String.class);
        for (int i = 0; i < words.length; i++) {
            if (words[i].length() <= 1) {
                words[i] = "";
                continue;
            }
            words[i] = words[i].replaceAll("[^0-9a-zA-Zа-яА-ЯёЁ]+", "");
            words[i] = words[i].replaceAll("^\\d+", "");
            words[i] = words[i].replaceAll(RE_INTERJOINS, "");
        }
        String result = StringUtils.join(words, ' ');
        return result.replaceAll("\\s+", " ").trim().toLowerCase();
    }

    /*
     * @param text - текст без знаков, предлогов и частиц
     * @param len - длина ключевой фразы в словах
     * 
     * @return список ключевых фраз (где ключевая фраза - список стеммированных слов)
     * Примеры:
     * findKeywords("привет семье", 2) -> [ ["привет","семья"] ]
     * findKeywords("бетонный цементный пол", 2) -> [ ["бетон","пол"], ["цемент","пол"], ["бетон","цемент"] ]
     * findKeywords("бетонный цементный пол", 3) -> [ ["бетон","цемент", "пол"] ]
     * 
     */
    private List<List<String>> findKeywords(String text, int len) {
        Iterable<String> words = KeywordGrouper.WHITESPACE_SPLITTER.split(text);
        Set<String> stemmed_words = Sets.newLinkedHashSet();
        for (String word : words) {
            final String stemmed = Morphology.stemWord(word);
            stemmed_words.add(stemmed);
            stemToNormal.put(stemmed, word);
        }
        //
        final List<List<String>> keywords = new ArrayList<List<String>>();
        final Iterator<String> it = stemmed_words.iterator();
        while (it.hasNext()) {
            final String word = it.next();
            it.remove();
            final ArrayList<String> kw = new ArrayList<String>();
            kw.add(word);
            short limit = (short) (len - 1 < 0 ? 0 : len - 1);
            for (final Iterator<String> it1 = Iterables.limit(stemmed_words, limit).iterator(); it1.hasNext();) {
                final String word1 = it1.next();
                kw.add(word1);
            }
            if (kw.size() == len) {
                keywords.add(kw);
            }
        }
        return keywords;
    }

    /*
     * Обновить глобальный словарь stemToNormal, добавляем новые соответствия
     * @param stemmed_phrase ключевая фраза из стеммированных слов для ассоциирования с нормальной фразой
     * @param stemmed_words список стеммированных слов
     */
    private void updateStemToNormal(String stemmed_phrase, String[] stemmed_words) {
        final String[] normal_words = new String[stemmed_words.length];
        for (int i = 0; i < stemmed_words.length; i++) {
            final String normal = stemToNormal.get(stemmed_words[i]);
            if (normal != null) {
                normal_words[i] = normal;
            } else {
                normal_words[i] = stemmed_words[i];
            }
        }
        String normal_phrase = StringUtils.join(normal_words, ' ');
        stemToNormal.put(stemmed_phrase, normal_phrase);
    }

    public class Passage {

        public String text;
        public String origText;
        public HashMultiset<String> wordIndexes = HashMultiset.create();
        private final Ordering<Comparable> naturalOrd = Ordering.natural();
        private final Ordering<Comparable> naturalOrdRev = Ordering.natural().reverse();

        public Passage(String origText, String text) {
            this.text = text;
            this.origText = origText;
            calcWordIndexes(origText);
        }

        private void calcWordIndexes(String text) {
            Iterable<String> words = KeywordGrouper.WHITESPACE_SPLITTER.split(text);
            for (short i = 0; i < Iterables.size(words); i++) {
                String word = Iterables.get(words, i).toLowerCase();
                wordIndexes.setCount(Morphology.stemWord(word), i);
            }
        }

        private double findMultiplyFor(String[] words) {

            double k = 0.4;
            Iterable<Integer> indexes = Iterables.transform(Arrays.asList(words), new WordToIndexFunction());
            if(naturalOrd.isStrictlyOrdered(indexes)) {
                k = 1;
            }
            else if(naturalOrdRev.isStrictlyOrdered(indexes)) {
                k = 0.8;
            }
            else if(naturalOrd.isOrdered(indexes)) {
                k = 0.6;
            }
            //System.err.println("mult: " + StringUtils.join(words, ' ') + " (" + origText + ") -> " + k);
            return k;
        }

        private class WordToIndexFunction implements Function<String, Integer> {

            public Integer apply(String word) {
                return wordIndexes.count(word);
            }
        }
        
        
    }
}
