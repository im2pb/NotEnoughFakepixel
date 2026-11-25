package com.nef.notenoughfakepixel.variables;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
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
    private final int timeOffsetSeconds;

    public static DungeonFloor fromCode(String code) {
        for (DungeonFloor floor : values()) {
            if (floor.name().equals(code)) return floor;
        }
        return NONE;
    }
}

