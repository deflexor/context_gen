/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cbroker.stemutils.data;

import com.google.common.collect.Lists;
import static com.google.gdata.util.common.base.Preconditions.checkNotNull;
import java.util.List;
import java.util.Set;


/**
 *
 * @author dfr
 */
public class KeywordGroup implements Comparable<KeywordGroup> {
    
    private String title;
    private Set<String> keyWords;
    private List<String> keyPhrases;

    public KeywordGroup(String title) {
        this.title = title;
    }

    public KeywordGroup(String title, Set<String> keyWords, String phrase) {
        this.title = checkNotNull(title);
        this.keyWords = checkNotNull(keyWords);
        this.keyPhrases = Lists.newArrayList(phrase);
    }

    /**
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * @return the keyWords
     */
    public Set<String> getKeyWords() {
        return keyWords;
    }

    public int compareTo(KeywordGroup t) {
        return title.compareTo(t.getTitle());
    }

    public void addKeyPhrase(String phrase) {
        if(! keyPhrases.contains(phrase)) getKeyPhrases().add(phrase);
    }

    /**
     * @return the keyPhrases
     */
    public List<String> getKeyPhrases() {
        return keyPhrases;
    }

    /**
     * @param keyWords the keyWords to set
     */
    public void setKeyWords(Set<String> keyWords) {
        this.keyWords = keyWords;
    }
}
