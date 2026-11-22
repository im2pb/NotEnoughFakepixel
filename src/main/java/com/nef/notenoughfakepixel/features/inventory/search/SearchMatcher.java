/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.inventory.search;

import com.nef.notenoughfakepixel.utils.ItemUtils;
import com.nef.notenoughfakepixel.variables.Rarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Search parser and bounded ItemStack metadata cache for the inventory search overlay.
 *
 * Query syntax is deliberately small and forgiving:
 *   words are ANDed, quoted text is one phrase, and a leading '-' excludes a term.
 *   name:, lore:, id:, type:, rarity: and count: provide field-specific filters.
 *
 * All expensive ItemStack/NBT reads are performed at most once per stack per client tick.
 */
public final class SearchMatcher {
    private static final int MAX_CACHED_STACKS = 1024;
    private static final int CACHE_TARGET_AFTER_EVICTION = 768;
    private static final int MAX_LORE_LINES = 96;
    private static final int MAX_LINE_LENGTH = 256;
    private static final int MAX_SEARCHABLE_CHARS = 8192;

    private static final Map<ItemStack, CacheEntry> CACHE = new IdentityHashMap<>();
    private static String cachedRawQuery;
    private static QueryTerm[] cachedTerms = new QueryTerm[0];
    private static long clientTick;

    private SearchMatcher() {}

    /** Returns true only when the stack satisfies every positive term and none of the excluded terms. */
    public static boolean matches(ItemStack stack, String query) {
        if (stack == null) return false;

        QueryTerm[] terms = getTerms(query);
        // An empty/quote-only query is a no-op. This also makes malformed input fail open.
        if (terms.length == 0) return true;

        CacheEntry entry;
        try {
            entry = getEntry(stack);
        } catch (RuntimeException ignored) {
            return fallbackDisplayNameMatch(stack, terms);
        }

        // If every metadata source failed, keep the item visible and clickable instead of hiding it blindly.
        if (entry.searchText.isEmpty()) return true;

        try {
            for (QueryTerm term : terms) {
                boolean matched = term.matches(entry);
                if (term.negative ? matched : !matched) return false;
            }
            return true;
        } catch (RuntimeException ignored) {
            // A malformed/custom item must remain visible and clickable instead of breaking a GUI.
            return true;
        }
    }

    /** Called once at the end of each client tick; rendering can query the same stack repeatedly. */
    public static void beginClientTick() {
        if (clientTick == Long.MAX_VALUE) {
            clientTick = 0L;
            CACHE.clear();
        } else {
            clientTick++;
        }
    }

    /** Re-parses the active expression without throwing away reusable item metadata. */
    public static void invalidateQuery() {
        cachedRawQuery = null;
        cachedTerms = new QueryTerm[0];
    }

    /** Clears all retained stacks when a container closes or changes. */
    public static void clearCache() {
        CACHE.clear();
        invalidateQuery();
    }

    private static QueryTerm[] getTerms(String rawQuery) {
        String safeRawQuery = safeLimit(rawQuery, MAX_SEARCHABLE_CHARS);
        if (safeRawQuery.equals(cachedRawQuery)) return cachedTerms;
        cachedRawQuery = safeRawQuery;
        String query = normalize(safeRawQuery);
        if (query.isEmpty()) {
            cachedTerms = new QueryTerm[0];
            return cachedTerms;
        }

        List<String> tokens = tokenize(query);
        List<QueryTerm> parsed = new ArrayList<>(tokens.size());
        for (String token : tokens) {
            QueryTerm term = parseTerm(token);
            if (term != null) parsed.add(term);
        }
        cachedTerms = parsed.toArray(new QueryTerm[0]);
        return cachedTerms;
    }

    /** Handles quoted phrases, whitespace and escaped quote/backslash characters. */
    private static List<String> tokenize(String query) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        boolean escaped = false;

        for (int i = 0; i < query.length(); i++) {
            char c = query.charAt(i);
            if (escaped) {
                if (c == '"' || c == '\\') current.append(c);
                else current.append('\\').append(c);
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '"') {
                quoted = !quoted;
            } else if (Character.isWhitespace(c) && !quoted) {
                addToken(tokens, current);
            } else {
                current.append(c);
            }
        }
        if (escaped) current.append('\\');
        addToken(tokens, current);
        return tokens;
    }

    private static void addToken(List<String> tokens, StringBuilder current) {
        if (current.length() > 0) tokens.add(current.toString());
        current.setLength(0);
    }

    private static QueryTerm parseTerm(String rawToken) {
        if (rawToken == null || rawToken.isEmpty() || "-".equals(rawToken)) return null;
        boolean negative = rawToken.charAt(0) == '-' && rawToken.length() > 1;
        String token = negative ? rawToken.substring(1) : rawToken;
        if (token.isEmpty()) return null;

        int kind = QueryTerm.TEXT;
        String value = token;
        int colon = token.indexOf(':');
        if (colon > 0) {
            String prefix = token.substring(0, colon).toLowerCase(Locale.ROOT);
            String candidate = token.substring(colon + 1);
            if ("name".equals(prefix)) {
                kind = QueryTerm.NAME;
                value = candidate;
            } else if ("lore".equals(prefix)) {
                kind = QueryTerm.LORE;
                value = candidate;
            } else if ("id".equals(prefix)) {
                kind = QueryTerm.ID;
                value = candidate;
            } else if ("meta".equals(prefix)) {
                kind = QueryTerm.META;
                value = candidate;
            } else if ("type".equals(prefix)) {
                kind = QueryTerm.TYPE;
                value = candidate;
            } else if ("rarity".equals(prefix)) {
                kind = QueryTerm.RARITY;
                value = candidate;
            } else if ("count".equals(prefix)) {
                CountFilter countFilter = CountFilter.parse(candidate);
                // An unfinished search query typed by the user must not produce a misleading match result.
                return countFilter == null ? null : new QueryTerm(QueryTerm.COUNT, "", negative, countFilter);
            }
        }

        String normalized = normalize(value);
        if (normalized.isEmpty()) return null;
        return new QueryTerm(kind, normalized, negative, null);
    }

    private static CacheEntry getEntry(ItemStack stack) {
        CacheEntry entry = CACHE.get(stack);
        if (entry != null && entry.validatedTick == clientTick) return entry;

        StackSignature signature = readSignature(stack);
        if (entry != null && entry.matches(signature)) {
            entry.validatedTick = clientTick;
            return entry;
        }

        CacheEntry updated = buildEntry(stack, signature);
        if (CACHE.size() >= MAX_CACHED_STACKS) evictCacheEntries();
        CACHE.put(stack, updated);
        return updated;
    }

    /** Prunes stale entries first and then trims only a quarter of the cache, never flushing it wholesale. */
    private static void evictCacheEntries() {
        Iterator<Map.Entry<ItemStack, CacheEntry>> iterator = CACHE.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<ItemStack, CacheEntry> cached = iterator.next();
            if (clientTick - cached.getValue().validatedTick > 2L) iterator.remove();
        }
        if (CACHE.size() < MAX_CACHED_STACKS) return;

        iterator = CACHE.entrySet().iterator();
        while (CACHE.size() > CACHE_TARGET_AFTER_EVICTION && iterator.hasNext()) {
            iterator.next();
            iterator.remove();
        }
    }

    private static StackSignature readSignature(ItemStack stack) {
        int itemId = -1;
        int metadata = 0;
        int size = 0;
        NBTTagCompound tag = null;
        int tagHash = 0;
        String displayName = safeDisplayName(stack);

        try {
            Item item = stack.getItem();
            itemId = item == null ? -1 : Item.getIdFromItem(item);
            metadata = stack.getMetadata();
            size = stack.stackSize;
            tag = stack.getTagCompound();
            tagHash = tag == null ? 0 : relevantTagFingerprint(tag);
        } catch (RuntimeException ignored) {
            // Preserve a partial signature; display-name searching can still work.
            tagHash = Integer.MIN_VALUE;
        }
        return new StackSignature(itemId, metadata, size, displayName, tag, tagHash);
    }

    private static CacheEntry buildEntry(ItemStack stack, StackSignature signature) {
        StringBuilder all = new StringBuilder(128);
        StringBuilder loreText = new StringBuilder(96);
        StringBuilder idText = new StringBuilder(64);

        appendLimited(all, signature.displayName);
        appendLimited(idText, signature.itemId < 0 ? "" : String.valueOf(signature.itemId));

        String internalName = "";
        try {
            internalName = safeLimit(ItemUtils.getInternalName(stack), MAX_LINE_LENGTH);
            appendLimited(idText, internalName);
        } catch (RuntimeException ignored) {
            // ExtraAttributes are optional and may be malformed on custom/server items.
        }
        appendLimited(all, idText.toString());

        String rarity = "none";
        try {
            LoreSnapshot lore = readLoreSnapshot(stack);
            for (String line : lore.searchLines) {
                if (loreText.length() >= MAX_SEARCHABLE_CHARS) break;
                appendLimited(loreText, safeLimit(line, MAX_LINE_LENGTH));
            }
            if (!lore.lastLine.isEmpty()) {
                String cleanRarity = com.nef.notenoughfakepixel.utils.StringUtils.stripFormattingFastRarity(lore.lastLine);
                Rarity parsed = Rarity.fromString(cleanRarity);
                rarity = parsed == null ? "none" : parsed.name().toLowerCase(Locale.ROOT);
            }
        } catch (RuntimeException ignored) {
            // A malformed NBT lore list must never escape into the render loop.
        }
        appendLimited(all, loreText.toString());

        return new CacheEntry(signature, normalize(all.toString()), normalize(signature.displayName),
                normalize(loreText.toString()), normalize(idText.toString()),
                normalize(signature.displayName + " " + idText), rarity,
                signature.stackSize, clientTick);
    }



    /**
     * Hash only the NBT fields that affect this search index. NBTTagCompound.hashCode() traverses
     * the complete server-provided item tree on every cache validation; custom items can carry
     * very large nested data that is irrelevant to name/lore/ID search. This bounded fingerprint
     * keeps mutation detection while limiting the work to the same capped lore slice we index.
     */
    private static int relevantTagFingerprint(NBTTagCompound tag) {
        int hash = 1;
        try {
            if (tag.hasKey("display", 10)) {
                NBTTagCompound display = tag.getCompoundTag("display");
                if (display != null) {
                    if (display.hasKey("Name", 8)) hash = 31 * hash + safeLimit(display.getString("Name"), MAX_LINE_LENGTH).hashCode();
                    NBTTagList lore = display.getTagList("Lore", 8);
                    if (lore != null) {
                        int count = lore.tagCount();
                        hash = 31 * hash + count;
                        int headCount = count > MAX_LORE_LINES ? MAX_LORE_LINES - 1 : count;
                        for (int i = 0; i < headCount; i++) {
                            hash = 31 * hash + safeLimit(lore.getStringTagAt(i), MAX_LINE_LENGTH).hashCode();
                        }
                        if (count > MAX_LORE_LINES) {
                            hash = 31 * hash + safeLimit(lore.getStringTagAt(count - 1), MAX_LINE_LENGTH).hashCode();
                        }
                    }
                }
            }
            if (tag.hasKey("ExtraAttributes", 10)) {
                NBTTagCompound extra = tag.getCompoundTag("ExtraAttributes");
                if (extra != null && extra.hasKey("id", 8)) {
                    hash = 31 * hash + safeLimit(extra.getString("id"), MAX_LINE_LENGTH).hashCode();
                }
            }
        } catch (RuntimeException ignored) {
            // The caller's partial signature still contains item ID, metadata, count and display name.
            return Integer.MIN_VALUE;
        }
        return hash;
    }

    /** Reads at most MAX_LORE_LINES without materializing an unbounded copy of the NBT list. */
    private static LoreSnapshot readLoreSnapshot(ItemStack stack) {
        if (stack == null || !stack.hasTagCompound()) return LoreSnapshot.EMPTY;
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null || !tag.hasKey("display")) return LoreSnapshot.EMPTY;
        NBTTagCompound display = tag.getCompoundTag("display");
        if (display == null || !display.hasKey("Lore")) return LoreSnapshot.EMPTY;
        NBTTagList lore = display.getTagList("Lore", 8);
        if (lore == null) return LoreSnapshot.EMPTY;

        int count = lore.tagCount();
        if (count <= 0) return LoreSnapshot.EMPTY;
        List<String> lines = new ArrayList<>(Math.min(count, MAX_LORE_LINES));
        int headCount = count > MAX_LORE_LINES ? MAX_LORE_LINES - 1 : count;
        for (int i = 0; i < headCount; i++) {
            lines.add(safeLimit(lore.getStringTagAt(i), MAX_LINE_LENGTH));
        }
        String lastLine = safeLimit(lore.getStringTagAt(count - 1), MAX_LINE_LENGTH);
        if (count > MAX_LORE_LINES) lines.add(lastLine);
        return new LoreSnapshot(lines, lastLine);
    }

    private static boolean fallbackDisplayNameMatch(ItemStack stack, QueryTerm[] terms) {
        String name = normalize(safeDisplayName(stack));
        // In an error path, retaining an item is safer than making an item disappear or blocking its click.
        if (name.isEmpty()) return true;
        for (QueryTerm term : terms) {
            if (term.kind != QueryTerm.TEXT && term.kind != QueryTerm.NAME && term.kind != QueryTerm.TYPE) continue;
            boolean match = name.contains(term.value);
            if (term.negative ? match : !match) return false;
        }
        return true;
    }

    /** ItemStack#getDisplayName returns the translated vanilla item name even without a custom display tag. */
    private static String safeDisplayName(ItemStack stack) {
        try {
            if (stack == null) return "";
            return safeLimit(stack.getDisplayName(), MAX_LINE_LENGTH);
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    private static void appendLimited(StringBuilder target, String value) {
        if (value == null || value.isEmpty() || target.length() >= MAX_SEARCHABLE_CHARS) return;
        int remaining = MAX_SEARCHABLE_CHARS - target.length();
        if (remaining <= 1) return;
        target.append(' ').append(value, 0, Math.min(value.length(), remaining - 1));
    }

    private static String safeLimit(String value, int maxLength) {
        if (value == null || value.isEmpty()) return "";
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private static String normalize(String value) {
        if (value == null || value.isEmpty()) return "";
        String limited = safeLimit(value, MAX_SEARCHABLE_CHARS);
        try {
            return StringUtils.stripControlCodes(limited).toLowerCase(Locale.ROOT).trim();
        } catch (RuntimeException ignored) {
            return limited.toLowerCase(Locale.ROOT).trim();
        }
    }

    private static final class LoreSnapshot {
        private static final LoreSnapshot EMPTY = new LoreSnapshot(Collections.<String>emptyList(), "");
        private final List<String> searchLines;
        private final String lastLine;

        private LoreSnapshot(List<String> searchLines, String lastLine) {
            this.searchLines = searchLines;
            this.lastLine = lastLine == null ? "" : lastLine;
        }
    }

    private static final class QueryTerm {
        private static final int TEXT = 0;
        private static final int NAME = 1;
        private static final int LORE = 2;
        private static final int ID = 3;
        private static final int META = 4;
        private static final int TYPE = 5;
        private static final int RARITY = 6;
        private static final int COUNT = 7;

        private final int kind;
        private final String value;
        private final boolean negative;
        private final CountFilter countFilter;

        private QueryTerm(int kind, String value, boolean negative, CountFilter countFilter) {
            this.kind = kind;
            this.value = value;
            this.negative = negative;
            this.countFilter = countFilter;
        }

        private boolean matches(CacheEntry entry) {
            switch (kind) {
                case NAME: return entry.nameText.contains(value);
                case LORE: return entry.loreText.contains(value);
                case ID: return entry.idText.contains(value);
                case META: return String.valueOf(entry.metadata).equals(value);
                case TYPE: return entry.typeText.contains(value);
                case RARITY: return entry.rarity.equals(value);
                case COUNT: return countFilter != null && countFilter.matches(entry.stackSize);
                default: return entry.searchText.contains(value);
            }
        }
    }

    private static final class CountFilter {
        private static final int EQ = 0;
        private static final int GT = 1;
        private static final int GTE = 2;
        private static final int LT = 3;
        private static final int LTE = 4;
        private final int operator;
        private final int value;

        private CountFilter(int operator, int value) {
            this.operator = operator;
            this.value = value;
        }

        private static CountFilter parse(String raw) {
            if (raw == null || raw.trim().isEmpty()) return null;
            String input = raw.trim();
            int operator = EQ;
            if (input.startsWith(">=")) { operator = GTE; input = input.substring(2); }
            else if (input.startsWith("<=")) { operator = LTE; input = input.substring(2); }
            else if (input.startsWith(">")) { operator = GT; input = input.substring(1); }
            else if (input.startsWith("<")) { operator = LT; input = input.substring(1); }
            else if (input.startsWith("=")) { operator = EQ; input = input.substring(1); }
            try {
                int value = Integer.parseInt(input.trim());
                return value < 0 ? null : new CountFilter(operator, value);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        private boolean matches(int count) {
            switch (operator) {
                case GT: return count > value;
                case GTE: return count >= value;
                case LT: return count < value;
                case LTE: return count <= value;
                default: return count == value;
            }
        }
    }

    private static final class StackSignature {
        private final int itemId;
        private final int metadata;
        private final int stackSize;
        private final String displayName;
        private final NBTTagCompound tag;
        private final int tagHash;

        private StackSignature(int itemId, int metadata, int stackSize, String displayName,
                               NBTTagCompound tag, int tagHash) {
            this.itemId = itemId;
            this.metadata = metadata;
            this.stackSize = stackSize;
            this.displayName = displayName == null ? "" : displayName;
            this.tag = tag;
            this.tagHash = tagHash;
        }
    }

    private static final class CacheEntry {
        private final int itemId;
        private final int metadata;
        private final int stackSize;
        private final String displayName;
        private final NBTTagCompound tag;
        private final int tagHash;
        private final String searchText;
        private final String nameText;
        private final String loreText;
        private final String idText;
        private final String typeText;
        private final String rarity;
        private long validatedTick;

        private CacheEntry(StackSignature signature, String searchText, String nameText, String loreText,
                           String idText, String typeText, String rarity, int stackSize, long validatedTick) {
            this.itemId = signature.itemId;
            this.metadata = signature.metadata;
            this.stackSize = stackSize;
            this.displayName = signature.displayName;
            this.tag = signature.tag;
            this.tagHash = signature.tagHash;
            this.searchText = searchText;
            this.nameText = nameText;
            this.loreText = loreText;
            this.idText = idText;
            this.typeText = typeText;
            this.rarity = rarity;
            this.validatedTick = validatedTick;
        }

        private boolean matches(StackSignature signature) {
            return itemId == signature.itemId && metadata == signature.metadata && stackSize == signature.stackSize
                    && displayName.equals(signature.displayName) && tag == signature.tag && tagHash == signature.tagHash;
        }
    }
}
