/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.cosmetics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CosmeticsManager {

    public static List<CosmeticsHandler> cosmetics = new ArrayList<>();

    public static void registerCosmetics(CosmeticsHandler... handlers) {
        cosmetics.addAll(Arrays.asList(handlers));
    }
}
