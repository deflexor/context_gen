
package com.cbroker.stemutils;

import java.io.IOException;
import java.net.URI;

import org.jsoup.nodes.Document;
import org.xml.sax.SAXException;

import com.cbroker.stemutils.data.SiteSection;
import com.cbroker.utils.DOMHelper;
import com.cbroker.utils.UserAgentHelper;

/**
 *
 * @author dfr
 */
public class SiteSectionHelper {
    
    /*
     * Фабрика SiteSection: загрузить страницу по URL, распарсить заголовок и ключевые слова
     * @param url - url страницы
     * @return объект SiteSection
     */
    public static SiteSection buildSiteSection(URI url) throws SAXException, IOException {
        SiteSection siteSection = new SiteSection(url);

        // fetch page and parse to DOM
        Document doc = UserAgentHelper.fetchURIasDocument(url.toString());
        
        //  title
        siteSection.setTitle( DOMHelper.findTitleInDocument(doc) );
        
        // keywords
        KeywordFinder finder = new KeywordFinder(doc);
        siteSection.setKeywordStats(finder.findKeywords());
        
        
        
        return siteSection;
    }

}
