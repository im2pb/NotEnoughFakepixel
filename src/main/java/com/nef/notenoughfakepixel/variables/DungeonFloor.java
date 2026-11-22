/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.variables;

import java.util.Arrays;
import java.util.Locale;

public enum DungeonFloor {
    NONE(-1, -1),
    E0(20, -120),
    F0(20, -120),
    F1(30, -120),
    F2(40, -120),
    F3(50, -120),
    F4(60, -240),
    F5(70, -120),
    F6(85, -240),
    F7(100, -360),
    M1(30, 0),
    M2(40, 0),
    M3(50, 0),
    M4(60, 0),
    M5(70, 0),
    M6(85, -120),
    M7(100, -360);

    private final int secretPercentage;
    private final int t;

    DungeonFloor(int secretPercentage, int t) {
        this.secretPercentage = secretPercentage;
        this.t = t;
    }

    public int getSecretPercentage() {
        return secretPercentage;
    }

    public int getT() {
        return t;
    }

    public boolean isMasterMode() {
        return name().startsWith("M");
    }

    public boolean isF7orM7() {
        return this == F7 || this == M7;
    }

    public boolean isF6orM6() {
        return this == F6 || this == M6;
    }

    public boolean isF3orM3() {
        return this == F3 || this == M3;
    }

    public boolean isF2orM2() {
        return this == F2 || this == M2;
    }

    public static DungeonFloor getFloor(String fromValue) {
        if (fromValue == null) return DungeonFloor.NONE;
        String normalized = fromValue.trim().toUpperCase(Locale.ROOT);
        return Arrays.stream(DungeonFloor.values())
                .filter(floor -> floor.name().equals(normalized))
                .findFirst()
                .orElse(DungeonFloor.NONE);
    }

}
