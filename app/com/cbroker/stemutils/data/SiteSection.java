/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cbroker.stemutils.data;

import java.net.URI;
import java.net.URL;

/**
 *
 * @author dfr
 */
public class SiteSection {
    
    private String title;
    private URI url;
    private KeywordStats keywordStats;

    public SiteSection(URI url) {
        this.url = url;
    }

    /**
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * @param title the title to set
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * @return the url
     */
    public URI getUrl() {
        return url;
    }

    /**
     * @param url the url to set
     */
    public void setUrl(URI url) {
        this.url = url;
    }

    public void setKeywordStats(KeywordStats keywordStats) {
        this.keywordStats = keywordStats;
    }

    /**
     * @return the keywordStats
     */
    public KeywordStats getKeywordStats() {
        return keywordStats;
    }

}
