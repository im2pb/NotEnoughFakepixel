/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.utils;

import java.util.Locale;
import java.util.regex.Pattern;

public final class RomanNumeralParser {

    private static final Pattern STRICT = Pattern.compile("^(?=[MDCLXVI])M*(C[MD]|D?C{0,3})(X[CL]|L?X{0,3})(I[XV]|V?I{0,3})$");
    private static final int MAX_MULTI_LETTER_VALUE = 50;
    private static final int MAX_RUN = 15;
    private static final char COLOR_PREFIX = '§';

    private static final String[] VERBS_AFTER_PRONOUN = {
            "am", "was", "have", "had", "will", "can", "would", "could", "should", "think", "know",
            "need", "want", "see", "got", "must", "like", "love", "feel", "hope"
    };

    private RomanNumeralParser() {}

    public static int parse(String input) {
        if (input == null) return -1;
        String value = input.trim().toUpperCase(Locale.ROOT);
        if (!STRICT.matcher(value).matches()) return -1;
        int total = 0;
        for (int i = 0; i < value.length(); i++) {
            int current = valueOf(value.charAt(i));
            if (i + 1 < value.length() && current < valueOf(value.charAt(i + 1))) total -= current;
            else total += current;
        }
        return total;
    }

    public static String replaceInString(String input) {
        if (input == null || input.isEmpty()) return input;
        if (!hasCandidate(input)) return input;

        int length = input.length();
        StringBuilder out = null; 
        int copied = 0;           
        char prevVisible = 0;     

        int i = 0;
        while (i < length) {
            char c = input.charAt(i);

            if (c == COLOR_PREFIX && i + 1 < length) {
                i += 2; 
                continue;
            }

            if (!isRomanLetter(c)) {
                prevVisible = c;
                i++;
                continue;
            }

            int start = i;
            while (i < length && i - start < MAX_RUN && isRomanLetter(input.charAt(i))) i++;
            int end = i;

            int value = convertible(input, start, end, prevVisible);
            if (value >= 0) {
                if (out == null) out = new StringBuilder(length + 8);
                out.append(input, copied, start).append(value);
                copied = end;
            }
            prevVisible = input.charAt(end - 1);
        }

        if (out == null) return input;
        return out.append(input, copied, length).toString();
    }

    private static int convertible(String s, int start, int end, char prevVisible) {
        int len = end - start;

        if (isWordPart(prevVisible)) return -1;

        int next = nextVisibleIndex(s, end);
        if (next < s.length() && isWordPartAfter(s.charAt(next))) return -1;

        int value = parse(s.substring(start, end));
        if (value < 0) return -1;

        if (len == 1) {
            char letter = s.charAt(start);
            if (letter != 'I' && letter != 'V' && letter != 'X') return -1;
            if (prevVisible != ' ') return -1; 
            if (letter == 'I' && followedByVerb(s, next)) return -1;
        } else if (value > MAX_MULTI_LETTER_VALUE) {
            return -1;
        }
        return value;
    }

    private static boolean followedByVerb(String s, int index) {
        int length = s.length();
        int i = index;
        if (i >= length || s.charAt(i) != ' ') return false;
        while (i < length && s.charAt(i) == ' ') i++;
        i = nextVisibleIndex(s, i);
        int wordStart = i;
        while (i < length && Character.isLetter(s.charAt(i)) && Character.isLowerCase(s.charAt(i))) i++;
        int wordLength = i - wordStart;
        if (wordLength < 2 || wordLength > 6) return false;
        for (String verb : VERBS_AFTER_PRONOUN) {
            if (verb.length() == wordLength && s.regionMatches(wordStart, verb, 0, wordLength)) return true;
        }
        return false;
    }

    private static int nextVisibleIndex(String s, int index) {
        int i = index;
        int length = s.length();
        while (i + 1 < length && s.charAt(i) == COLOR_PREFIX) i += 2;
        return i;
    }

    private static boolean hasCandidate(String s) {
        for (int i = 0, n = s.length(); i < n; i++) {
            if (isRomanLetter(s.charAt(i))) return true;
        }
        return false;
    }

    private static boolean isRomanLetter(char c) {
        switch (c) {
            case 'I': case 'V': case 'X': case 'L': case 'C': case 'D': case 'M':
                return true;
            default:
                return false;
        }
    }

    private static boolean isWordPartAfter(char c) {
        return c != '.' && isWordPart(c);
    }

    private static boolean isWordPart(char c) {
        return Character.isLetterOrDigit(c) || c == '-' || c == '\'' || c == '_' || c == '.';
    }

    private static int valueOf(char c) {
        switch (c) {
            case 'I': return 1;
            case 'V': return 5;
            case 'X': return 10;
            case 'L': return 50;
            case 'C': return 100;
            case 'D': return 500;
            case 'M': return 1000;
            default: return 0;
        }
    }
}
