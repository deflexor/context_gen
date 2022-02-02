/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cbroker.stemutils;

import static com.cbroker.utils.Constants.FORMAT2;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;

import org.jsoup.nodes.Document;

import play.jobs.Job;
import play.libs.F.Either;
import play.libs.F.Promise;
import Jama.Matrix;
import Jama.SingularValueDecomposition;

import com.cbroker.stemutils.data.KeywordStats.KeywordsMap;
import com.cbroker.stemutils.data.KeywordStats.Score;
import com.cbroker.stemutils.data.SiteSection;
import com.cbroker.utils.DOMHelper;
import com.cbroker.utils.MapUtils;
import com.cbroker.utils.MiniInfo;
import com.cbroker.utils.UserAgentHelper;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.primitives.Doubles;

/**
 * Находит релевантную страницу по url и списку лючевых фраз: List<String> groups = Arrays.asList("группа 1",
 * "еще группа"); HashMap<String, String> relevantPages = RelevantPageFinder.findPagesForGroups("http://my.site/",
 * groups);
 */
public class RelevantPageFinder {

    // мксимальное кол-во параллельных загрузок страниц одного сайта при анализе
    // сайта
    private static final int MAX_PARALLEL_REQUESTS = 3;

    /*
     * Для списка групп и URL сайта, найти на данном сайте наиболее релевантную страницу для каждой группы.
     * 
     * @param url Адрес сайта
     * 
     * @param keygroups Список ключевых фраз
     * 
     * @return Ассоциативный список "ключевая фраза" -> "url"
     */
    public static HashMap<String, String> findPagesForGroups(String url,
            List<String> keygroups) throws Exception {
        long start1 = System.currentTimeMillis();
        HashMap<String, String> groupsPages = new HashMap<String, String>();
        // загрузить первую страницу сайта
        Document firstPageDoc = UserAgentHelper.fetchURIasDocument(url);
        // найти все ссылки на сайт
        Set<URI> links = DOMHelper.findLinksInDocument(firstPageDoc, url);
        //links.clear();
        //links.add(new URL("http://www.reg.ru/service/mobile"));
        //links.add(new URL("http://www.reg.ru/support/index"));
        // превращаем ссылки в разделы сайта (SiteSection или String с ошибкой,
        // если парсинг не удался)
        // тут нужно загрузить каждую ссылку и распарсить html страницы
        // для ускорения задачи по каждому URL запускаем параллельно
        List<SiteSection> sections = fetchSectionsParallel(links); // Sets.newHashSet(Iterables.limit(links,5))
        long start2 = System.currentTimeMillis();

        // Полученный список разделов сайта используем для поиска подходящего
        // раздела.
        // Для каждой группы пройдемся по разделам и найдем наиболее релевантный
        // и запомним его url.
        for (String group : keygroups) {
            MiniInfo.reset();
            String relevantSection = RelevantPageFinderHybrid.findRelevantSectionForGroup(group,
                    sections);
            if (relevantSection != null) {
                groupsPages.put(group, relevantSection);
                groupsPages.put("_info" + group, MiniInfo.getString()); // debug
            }
        }

        long end = System.currentTimeMillis();
        groupsPages.put("_time_fetch", FORMAT2.format((start2-start1)/1000));
        groupsPages.put("_time_relev", FORMAT2.format((end-start2)/1000));
        return groupsPages;
    }

    /*
     * Загрузить ссылки links, распарсить и вернуть в виде списка SiteSection
     */
    static List<SiteSection> fetchSectionsParallel(Set<URI> links)
            throws InterruptedException, ExecutionException {
        ArrayList<Promise<Either<SiteSection, String>>> jobs = new ArrayList<Promise<Either<SiteSection, String>>>();
        final Semaphore semaphore = new Semaphore(MAX_PARALLEL_REQUESTS, true);
        for (final URI link : links) {
            jobs.add(new Job<Either<SiteSection, String>>() {

                @Override
                public Either<SiteSection, String> doJobWithResult()
                        throws Exception {
                    try {
                        semaphore.acquire();
                        System.err.println("loading link:" +link.toString());
                        SiteSection section = SiteSectionHelper
                                .buildSiteSection(link);
                        // System.err.println("loaded:" + link.toString());
                        return Either._1(section);
                    } catch (Throwable e) {
                        return Either._2(link.toString() + ": " + e.toString());
                    } finally {
                        semaphore.release();
                    }
                }
            }.now());
        }
        List<Either<SiteSection, String>> sectionsOrErrors = Promise.waitAll(
                jobs).get();
        List<SiteSection> sections = new ArrayList<SiteSection>();
        for (Either<SiteSection, String> soe : sectionsOrErrors) {
            if (soe._1.isDefined()) {
                sections.add(soe._1.get());
            } else {
                System.err.println("Ошибка при загрузке url:" + soe._2.get());
            }
        }
        return sections;
    }

    /*
     * Находим релевантную страницу: где чаще встречается фраза(название группы) целиком
     */
    private static String findRelevantSectionForGroup(String group,
            List<SiteSection> sections) throws Exception {
        HashMap<String, Double> pscores = new HashMap<String, Double>();
        Set<String> groupSet = makeGroupSet(group);
        // для каждого раздела найдем макс P
        for (SiteSection siteSection : sections) {
            String url = siteSection.getUrl().toString();
            // получим карту ключевых фраз
            int len = findKeywordsLength(group, siteSection);
            KeywordsMap kwMap = siteSection.getKeywordStats().getStatsMap(len);
            // для каждой фразы
            for (String k : kwMap.keySet()) {
                Score score = kwMap.get(k);
                // если она совпадает с названием группы
                if (groupSet.equals(score.getKeywordSet())) {
                    // запомним число P для этого раздела
                    double p = score.getScore() * groupSet.size()
                            / siteSection.getKeywordStats().getWordsTotal()
                            * 100;
                    MiniInfo.logln("url:" + siteSection.getUrl().toString());
                    MiniInfo.logln("раздел:'" + siteSection.getTitle());
                    MiniInfo.logln("фраза: '" + k);
                    MiniInfo.logln("P = " + FORMAT2.format(score.getScore()) + " * "
                            + groupSet.size() + " / "
                            + siteSection.getKeywordStats().getWordsTotal()
                            + " * 100 = " + FORMAT2.format(p));
                    MiniInfo.logln("");
                    Double pscore = pscores.get(url);
                    if (pscore == null || pscore < p) {
                        pscores.put(url, p);
                    }
                }
            }
        }
        if (pscores.isEmpty()) {
            return findRelevantSectionForGroupPlanC(sections, groupSet);
        } else {
            // выберем раздел с наибольшим P и вернем его URL
            List<Entry<String, Double>> sortedPscores = MapUtils
                    .sortByValueDesc(pscores);
            // --------- DEBUG
            MiniInfo.logln("План А:");
            for (Entry<String, Double> entry : sortedPscores) {
                MiniInfo.logln(entry.getKey() + ": <b>"
                        + FORMAT2.format(entry.getValue()) + "</b>");
            }
            // ---------
            return sortedPscores.get(0).getKey();
        }
    }

    /*
     * Находим релевантную страницу по плану Ц: латентно-семантический анализ
     */
    private static String findRelevantSectionForGroupPlanC(
            List<SiteSection> sections, Set<String> groupSet) throws Exception {
        MiniInfo.logln("План C:");
        final KeywordsMap kwMap = new KeywordsMap();
        // объеденим карты ключевиков дял односложных фраз со всех секций в одну
        for (SiteSection siteSection : sections) {
            kwMap.putAll(siteSection.getKeywordStats().getStatsMap(1));
        }
        int cols = sections.size();
        int rows = kwMap.size();
        double[][] mdata = new double[rows][cols];
        // строим матрицу
        List<Integer> groupCoords = Lists.newArrayList();
        int rowIdx = 0;
        MiniInfo.log("<b>[");
        for (String keyWord : kwMap.keySet()) {
            for (int colIdx = 0; colIdx < cols; colIdx++) {
                SiteSection section = sections.get(colIdx);
                Score score = section.getKeywordStats().getStatsMap(1)
                        .get(keyWord);
                mdata[rowIdx][colIdx] = score != null ? score.getScore() : 0.0;
            }
            // параллельно найдем номера столбцов для слов из текущей группы
            if (groupSet.contains(keyWord)) {
                groupCoords.add(rowIdx);
                MiniInfo.log(keyWord + " ");
            }
            rowIdx++;
        }
        MiniInfo.logln("]</b>");
        if (groupCoords.isEmpty()) return null;
        Matrix matrix = new Matrix(mdata, rows, cols); // ok

        // раскладываем матрицу
        SingularValueDecomposition svd = matrix.svd();
        Matrix u = svd.getU(); // столбцы (слова)
        Matrix v = svd.getV(); // строки (страницы)
        int N = 2; // кол-во столбцов u и строк v для пространства координат
        String bestUrl = null;
        double bestDist = Double.MAX_VALUE;
        for (int pageCoord = 0; pageCoord < cols; pageCoord++) {
            double totalDist = 0.0;
            MiniInfo.log(sections.get(pageCoord).getUrl() + " - (");
            for (Integer wordCoord : groupCoords) {
                double sum = 0.0;
                for (int i = 0; i < N; i++) {
                    // System.err.println("cols:" + cols + " rows: " + rows +
                    // " i:" + i + " wordi:" + wordCoord + " pagei:" +
                    // pageCoord);
                    sum += Math.pow(u.get(wordCoord, i) - v.get(i, pageCoord),
                            2);
                }
                MiniInfo.log("" + FORMAT2.format(Math.sqrt(sum)));
                MiniInfo.log(" + ");
                totalDist += Math.sqrt(sum);
            }
            MiniInfo.logln(" 0) = " + FORMAT2.format(totalDist));
            if (totalDist < bestDist) {
                bestDist = totalDist;
                bestUrl = sections.get(pageCoord).getUrl().toString();
            }
        }
        return bestUrl;
    }

    /*
     * Находим релевантную страницу по плану Б: считаем частоту односложных фраз
     */
    private static String findRelevantSectionForGroupPlanB(
            List<SiteSection> sections, Set<String> groupSet) {
        HashMap<String, Double> rscores = new HashMap<String, Double>();
        // для каждого раздела
        // DEBUG
        String[] colors = new String[] { "#EEFFEE", "#FFEEFF", "#EEFFFF",
                "#FFEEEE", "#FFFFEE" };
        MiniInfo.logln("План Б:");
        // DEBUG
        for (SiteSection siteSection : sections) {
            String url = siteSection.getUrl().toString();
            List<String> keys = new ArrayList<String>();
            // выберем односложные фразы
            final KeywordsMap kwMap = siteSection.getKeywordStats()
                    .getStatsMap(1);
            // выберем только фразы, которые встречаются в группе
            for (String k : kwMap.keySet()) {
                if (groupSet.contains(k)) {
                    keys.add(k);
                }
            }
            Ordering<String> wordsbyScoreOrdering = new Ordering<String>() {

                @Override
                public int compare(String arg0, String arg1) {
                    return Doubles.compare(kwMap.get(arg1).getScore(),
                            kwMap.get(arg0).getScore());
                }
            };
            List<String> sortedKeys = wordsbyScoreOrdering.sortedCopy(keys);
            double result = 0.0;
            // спец формула: R = K1 * x + (K2 - K1) * y + (K3 - K2) ...
            MiniInfo.logln("раздел:" + siteSection.getUrl().toString());
            MiniInfo.log("R = ");
            int wordsTotal = siteSection.getKeywordStats().getWordsTotal();
            if (wordsTotal < 200) wordsTotal = 200;
            for (int i = groupSet.size() - 1; i >= 0; i--) {
                String word = (i <= sortedKeys.size() - 1) ? sortedKeys.get(i)
                        : null;
                String prev_word = (i < sortedKeys.size() - 1) ? sortedKeys
                        .get(i + 1) : null;
                double freq = word != null ? kwMap.get(word).getScore() : 0.0;
                double prev_freq = (prev_word != null) ? kwMap.get(prev_word).getScore()
                        : 0.0;
                double w = Math.pow(2, i + 1) - 1;
                double sl1 = (i == groupSet.size() - 1) ? Math.abs(prev_freq
                        - freq) : 3 * Math.atan2(Math.abs(prev_freq - freq), 2);
                result += sl1 * w;
                // DEBUG
                MiniInfo.log(((i == groupSet.size() - 1) ? "" : " + ")
                        + "<span style=\"background-color: "
                        + colors[i]
                        + "\">"
                        + word
                        + ((i == groupSet.size() - 1) ? "(abs("
                                : "(3*atan2(abs(") + FORMAT2.format(prev_freq)
                        + " - " + FORMAT2.format(freq)
                        + ((i == groupSet.size() - 1) ? ")*" : "))*")
                        + FORMAT2.format(w) + "</span>");
                if (i == 0) MiniInfo.log(" / " + wordsTotal + "*100");
                // DEBUG
            }
            double r = result / wordsTotal * 100;
            MiniInfo.logln(" = " + FORMAT2.format(result) + " / " + wordsTotal
                    + "*100" + " = " + FORMAT2.format(r));
            // сохраняем макс R для каждого раздела
            Double rscore = rscores.get(url);
            if (rscore == null || rscore < r) {
                rscores.put(url, r);
            }
        }
        if (rscores.isEmpty()) {
            return null;
        } else {
            List<Entry<String, Double>> sortedScores = MapUtils
                    .sortByValueDesc(rscores);
            // --------- DEBUG
            for (Entry<String, Double> entry : sortedScores) {
                MiniInfo.logln(entry.getKey() + ": <b>"
                        + FORMAT2.format(entry.getValue()) + "</b>");
            }
            // ---------
            // выберем и вернем макс R
            return sortedScores.get(0).getKey();
        }
    }

    /*
     * Найти оптимальную длину ключевой фразы для поиска по структуре KeywordStats
     */
    static int findKeywordsLength(String group, SiteSection section)
            throws Exception {
        String[] words = group.split("\\s+");
        int len = words.length;
        while (!section.getKeywordStats().hasKeywordGroupWords(len) && len > 0) {
            len--;
        }
        if (len == 0) {
            throw new Exception(
                    "Не найдена подходящая длина фразы для группы: " + group);
        }
        return len;
    }

    /*
     * Превратить ключевую фразу в Set из стеммированных слов
     */
    static Set<String> makeGroupSet(String group) {
        List<String> groupList = Arrays.asList(group.trim().split("\\s+"));
        Set<String> groupSet = new HashSet<String>();
        for (String groupWord : groupList) {
            groupSet.add(Morphology.stemWord(groupWord.toLowerCase()));
        }
        return groupSet;
    }
}
