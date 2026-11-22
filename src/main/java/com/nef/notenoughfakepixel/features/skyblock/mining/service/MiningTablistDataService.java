/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.mining.service;

import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import com.nef.notenoughfakepixel.utils.NumberUtils;
import com.nef.notenoughfakepixel.utils.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Owns mining values sampled from the SkyBlock tab list.
 *
 * <p>The Forge-facing TablistParser remains responsible for reading and ordering
 * player-info rows. This service handles mining-specific server-line parsing and
 * atomically publishes a stable commission snapshot for the legacy list bridge.</p>
 */
public final class MiningTablistDataService {
    private static final MiningTablistDataService INSTANCE = new MiningTablistDataService();

    private volatile List<String> commissions = Collections.emptyList();

    public static MiningTablistDataService getInstance() {
        return INSTANCE;
    }

    /**
     * Applies mining powder values from an already stripped server-info line.
     * The existing SkyblockData fields remain the compatibility storage used by
     * the rest of the mod.
     */
    public void acceptServerLine(String line) {
        if (line == null || line.isEmpty()) return;

        if (StringUtils.startsWithFast(line, "Mithril Powder: ")) {
            final String number = StringUtils.sliceAfter(line, "Mithril Powder: ");
            SkyblockData.setMithrilPowder(NumberUtils.parseIntSafe(StringUtils.removeChars(number, ",")));
        } else if (StringUtils.startsWithFast(line, "Gemstone Powder: ")) {
            final String number = StringUtils.sliceAfter(line, "Gemstone Powder: ");
            SkyblockData.setGemstonePowder(NumberUtils.parseIntSafe(StringUtils.removeChars(number, ",")));
        }
    }

    /**
     * Publishes the commission rows collected during one complete tab-list scan.
     * The current immutable list is reused when content is unchanged.
     *
     * @return true only when the published commission contents changed
     */
    public boolean publishCommissions(List<String> sampledCommissions) {
        if (sampledCommissions == null || sampledCommissions.isEmpty()) {
            if (commissions.isEmpty()) return false;
            commissions = Collections.emptyList();
            return true;
        }

        if (commissions.equals(sampledCommissions)) return false;

        commissions = Collections.unmodifiableList(new ArrayList<String>(sampledCommissions));
        return true;
    }

    /** Returns the latest immutable commission snapshot. */
    public List<String> getCommissions() {
        return commissions;
    }
}
