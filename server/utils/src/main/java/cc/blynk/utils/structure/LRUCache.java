package cc.blynk.utils.structure;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 17.11.17.
 */
public class LRUCache<K, V> extends LinkedHashMap<K, V> {

    private final int maxSize;

    //size hardcoded for now
    public static final Map<String, CacheEntry> LOGIN_TOKENS_CACHE =
            Collections.synchronizedMap(new LRUCache<>(1000));

    public LRUCache(int maxSize) {
        this.maxSize = maxSize;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > maxSize;
    }

    public final static class CacheEntry {
        public final String value;
        public CacheEntry(String value) {
            this.value = value;
        }
    }
}
