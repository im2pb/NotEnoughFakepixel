/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class CalculatorEngine {
    private CalculatorEngine() {}

    public static Double evaluate(String input) {
        if (input == null || input.trim().isEmpty()) return null;
        try {
            Parser parser = new Parser(input);
            double value = parser.parseExpression();
            parser.skipWhitespace();
            if (!parser.atEnd() || !Double.isFinite(value)) return null;
            return value;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    public static boolean looksLikeExpression(String input) {
        if (input == null || input.trim().isEmpty() || evaluate(input) == null) return false;
        return input.matches(".*[+\\-*/%^xX()].*")
                || input.matches("(?i).*\\d(?:\\.\\d+)?[skmbt]\\s*$")
                || input.matches("(?i).*(?:sin|cos|tan|asin|acos|atan|sind|cosd|tand|log|log10|ln|log2|exp|sqrt|cbrt|abs|ceil|floor|round|max|min|pow)\\s*\\(.*");
    }

    public static String format(double value) {
        if (!Double.isFinite(value)) return "Error";
        double rounded = Math.rint(value);
        if (Math.abs(value - rounded) < 1e-10 && Math.abs(rounded) <= Long.MAX_VALUE) {
            return String.format(Locale.ROOT, "%,d", (long) rounded);
        }
        BigDecimal decimal = BigDecimal.valueOf(value).setScale(10, RoundingMode.HALF_UP).stripTrailingZeros();
        return decimal.toPlainString();
    }

    private static final class Parser {
        private final String text;
        private int index;

        Parser(String text) {
            this.text = text;
        }

        double parseExpression() {
            double value = parseTerm();
            while (true) {
                skipWhitespace();
                if (consume('+')) value += parseTerm();
                else if (consume('-')) value -= parseTerm();
                else return value;
            }
        }

        double parseTerm() {
            double value = parsePower();
            while (true) {
                skipWhitespace();
                if (consume('*') || consume('x') || consume('X')) value *= parsePower();
                else if (consume('/')) value /= parsePower();
                else if (consume('%')) value %= parsePower();
                else return value;
            }
        }

        double parsePower() {
            double left = parseUnary();
            skipWhitespace();
            if (consume('^')) return Math.pow(left, parsePower());
            return left;
        }

        double parseUnary() {
            skipWhitespace();
            if (consume('+')) return parseUnary();
            if (consume('-')) return -parseUnary();
            return parsePrimary();
        }

        double parsePrimary() {
            skipWhitespace();
            if (consume('(')) {
                double value = parseExpression();
                expect(')');
                return value;
            }
            if (index < text.length() && (Character.isDigit(text.charAt(index)) || text.charAt(index) == '.')) {
                return parseNumber();
            }
            if (index < text.length() && Character.isLetter(text.charAt(index))) {
                String name = parseIdentifier();
                skipWhitespace();
                if (consume('(')) {
                    List<Double> args = new ArrayList<>();
                    skipWhitespace();
                    if (!consume(')')) {
                        do {
                            args.add(parseExpression());
                            skipWhitespace();
                        } while (consume(','));
                        expect(')');
                    }
                    return applyFunction(name, args);
                }
                if ("pi".equals(name)) return Math.PI;
                if ("e".equals(name)) return Math.E;
                throw new IllegalArgumentException("Unknown identifier: " + name);
            }
            throw new IllegalArgumentException("Expected value");
        }

        double parseNumber() {
            int start = index;
            boolean dot = false;
            while (index < text.length()) {
                char c = text.charAt(index);
                if (Character.isDigit(c)) index++;
                else if (c == '.' && !dot) { dot = true; index++; }
                else break;
            }
            if (start == index) throw new IllegalArgumentException();
            double value = Double.parseDouble(text.substring(start, index));
            if (index < text.length()) {
                char suffix = Character.toLowerCase(text.charAt(index));
                switch (suffix) {
                    case 's': value *= 64.0; index++; break;
                    case 'k': value *= 1_000.0; index++; break;
                    case 'm': value *= 1_000_000.0; index++; break;
                    case 'b': value *= 1_000_000_000.0; index++; break;
                    case 't': value *= 1_000_000_000_000.0; index++; break;
                    default: break;
                }
            }
            return value;
        }

        String parseIdentifier() {
            int start = index;
            while (index < text.length() && Character.isLetter(text.charAt(index))) index++;
            return text.substring(start, index).toLowerCase(Locale.ROOT);
        }

        double applyFunction(String name, List<Double> args) {
            if (args.isEmpty() && !("pi".equals(name) || "e".equals(name))) throw new IllegalArgumentException();
            if (args.size() == 1) {
                double x = args.get(0);
                switch (name) {
                    case "sin": return Math.sin(x);
                    case "cos": return Math.cos(x);
                    case "tan": return Math.tan(x);
                    case "asin": return Math.asin(x);
                    case "acos": return Math.acos(x);
                    case "atan": return Math.atan(x);
                    case "sind": return Math.sin(Math.toRadians(x));
                    case "cosd": return Math.cos(Math.toRadians(x));
                    case "tand": return Math.tan(Math.toRadians(x));
                    case "log": return Math.log(x);
                    case "log10": return Math.log10(x);
                    case "ln": return Math.log(x);
                    case "log2": return Math.log(x) / Math.log(2.0);
                    case "exp": return Math.exp(x);
                    case "sqrt": return Math.sqrt(x);
                    case "cbrt": return Math.cbrt(x);
                    case "abs": return Math.abs(x);
                    case "ceil": return Math.ceil(x);
                    case "floor": return Math.floor(x);
                    case "round": return Math.round(x);
                    default: break;
                }
            }
            if ("pow".equals(name) && args.size() == 2) return Math.pow(args.get(0), args.get(1));
            if ("max".equals(name) && !args.isEmpty()) {
                double result = -Double.MAX_VALUE;
                for (double value : args) result = Math.max(result, value);
                return result;
            }
            if ("min".equals(name) && !args.isEmpty()) {
                double result = Double.MAX_VALUE;
                for (double value : args) result = Math.min(result, value);
                return result;
            }
            throw new IllegalArgumentException("Unknown function: " + name);
        }

        boolean consume(char c) {
            if (index < text.length() && text.charAt(index) == c) {
                index++;
                return true;
            }
            return false;
        }

        void expect(char c) {
            if (!consume(c)) throw new IllegalArgumentException("Expected " + c);
        }

        void skipWhitespace() {
            while (index < text.length() && Character.isWhitespace(text.charAt(index))) index++;
        }

        boolean atEnd() {
            return index >= text.length();
        }
    }
}
