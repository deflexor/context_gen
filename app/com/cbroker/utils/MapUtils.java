/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cbroker.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class MapUtils {

    public static <K, V extends Comparable<V>> List<Entry<K, V>> sortByValue(Map<K, V> map) {
        List<Entry<K, V>> entries = new ArrayList<Entry<K, V>>(map.entrySet());
        Collections.sort(entries, new ByValue<K, V>());
        return entries;
    }

    public static <K, V extends Comparable<V>> List<Entry<K, V>> sortByValueDesc(Map<K, V> map) {
        List<Entry<K, V>> entries = new ArrayList<Entry<K, V>>(map.entrySet());
        Collections.sort(entries, new ByValueDesc<K, V>());
        return entries;
    }

    private static class ByValue<K, V extends Comparable<V>> implements Comparator<Entry<K, V>> {
        public int compare(Entry<K, V> o1, Entry<K, V> o2) {
            return o1.getValue().compareTo(o2.getValue());
        }
    }
    private static class ByValueDesc<K, V extends Comparable<V>> implements Comparator<Entry<K, V>> {
        public int compare(Entry<K, V> o1, Entry<K, V> o2) {
            return o2.getValue().compareTo(o1.getValue());
        }
    }
}
