/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cbroker.utils;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.google.common.base.Objects;
import com.google.common.base.Strings;

/**
 *
 * @author dfr
 */
public class DOMHelper {
   
    /*
     * Получить заголовок страницы, переданной в виде DOM-дерева
     * @param doc DOM-документ
     * @return заголовок
     */
    public static String findTitleInDocument(Document doc) {
        Elements titleElement = Objects.firstNonNull(doc.select("title"), doc.select("h1"));
        String title = Strings.nullToEmpty(titleElement.first().text()).trim();

        System.err.println("Found title:" + title);

        return title.trim();
    }

    /*
     * Найти список ссылок (URL) в дом дереве
     * @param doc документ DOM
     * @param baseURLstr строка URL сайта, требуется для отсеивания внешних ссылок
     * @return список урлов
     */
    public static Set<URI> findLinksInDocument(Document doc, String baseURLstr) throws URISyntaxException {
        URI baseURL = new URI(baseURLstr);

        // parse first page dom node
        Elements links = doc.select("a");
        List<URI> urls = new ArrayList<URI>();
        String baseURLhost = baseURL.getHost(); //
        String baseURLhostNoWWW = baseURL.getHost().replaceFirst("^www\\.", "");
        for (Element link : links) {
            try {
                URI url = normalizedUrl(baseURL, link.attr("href"), baseURLhost);
                if (hostsEqual(baseURLhostNoWWW, url.getHost())) {
                    urls.add(url);
                }
            }
            catch (Exception ex) {
                // skip bad url
            }
        }
        return uniqUrls(urls);
    }
    
    private static Set<URI> uniqUrls(List<URI> urls) {
        HashSet<String> urlset = new HashSet<String>();
        HashSet<URI> newurlset = new HashSet<URI>();
        for (URI url : urls) {
            if(urlset.add(url.toString().replaceFirst("/$", ""))) {
                newurlset.add(url);
            }
        }
        return newurlset;
    }

    private static URI normalizedUrl(URI baseURL, String href, String baseURLhost) throws URISyntaxException {
        URI url = baseURL.resolve(href);
        String urlHost = url.getHost();
        if (baseURLhost.startsWith("www.") && !urlHost.startsWith("www.")) {
            urlHost = "www." + urlHost;
        }
        else if (!baseURLhost.startsWith("www.") && urlHost.startsWith("www.")) {
            urlHost = urlHost.replaceFirst("^www\\.", "");
        }
        return new URI("http", urlHost, url.getPath(), url.getQuery(), null);
    }

    private static boolean hostsEqual(String baseURLhost, String host) {
        int i = host.indexOf(baseURLhost);
        if (i == 0) {
            return true;
        }
        else if (i > 0) {
            /*
             * тут отрабатываем случаи совпадения субдоменов до n+1 уровня
             * например
             * www.reg.ru == reg.ru
             * banking.reg.ru == reg.ru
             * www.banking.reg.ru == reg.ru
             * long.banking.reg.ru != reg.ru
             */
            String subdomain = host.substring(0, i - 1).replaceFirst("^www\\.", "");
            if (subdomain.contains(".")) {
                return false;
            }
            else {
                return true;
            }
        }
        else {
            return false;
        }

    }
}
