/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cbroker.stemutils;

import java.io.IOException;
import java.util.List;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;

/**
 *
 * @author dfr
 */
public class Morphology {
    
    private static final LuceneMorphology luceneMorph = initMorphology();
    
    public static String stemWord(String word) {
        if(!word.matches("^[а-яё]+$")) return word;
        List<String> wordBaseForms = luceneMorph.getNormalForms(word);
        if(wordBaseForms.isEmpty()) {
            return word;
        }
        else {
            return wordBaseForms.get(0);
        }
    }
    
    private static LuceneMorphology initMorphology() {
        LuceneMorphology morph = null;
        try {
            morph = new RussianLuceneMorphology();
        } catch (IOException ex) {
            System.exit(1); // TODO, report error and exit
        }
        return morph;
    }

    
}
