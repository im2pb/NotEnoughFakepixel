/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.utils;

import lombok.Data;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

public class MapUtils {

    @SafeVarargs
    public static <K, V> Map<K, V> mapOf(Pair<K, V>... entries) {
        return Arrays.stream(entries)
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(entry -> entry.key, entry -> entry.value),
                        Collections::unmodifiableMap
                ));
    }

    public static <K, V> K getKeyFromValue(Map<K, V> map, V value) {
        return map.entrySet().stream()
                .filter(entry -> entry.getValue().equals(value))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    @Data
    public static class Pair<K, V> {
        final K key;
        final V value;

        public static <K, V> Pair<K, V> of(K key, V value) {
            return new Pair<>(key, value);
        }
    }

}
