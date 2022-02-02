/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cbroker.utils;

/**
 * Временный класс для дебаггинга, для работы не нужен
 * 
 * @author dfr
 */
public class MiniInfo {
 
    static StringBuffer info = new StringBuffer();
    
    public static void logln(String s) {
        log(s + "\n");
    }

    public static void log(String s) {
        info.append(s);
    }
    
    public static String reset() {
        String str = info.toString();
        info = new StringBuffer();
        return str;
    }

    public static String getString() {
        return info.toString();
    }

}
