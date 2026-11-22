/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.utils;

import java.util.Random;

public class RandomUtils {

    private static Random RANDOM;

    public static Random getInstance() {
        if (RANDOM == null) RANDOM = new Random();
        return RANDOM;
    }

}
