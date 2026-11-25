package com.nef.notenoughfakepixel.variables;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public enum Mayor {
    NONE("None"),
    AATROX("Aatrox"),
    COLE("Cole"),
    DIANA("Diana"),
    DIAZ("Diaz"),
    FINNEGAN("Finnegan"),
    MARINA("Marina"),
    DERPY("Derpy"),
    FOXY("Foxy"),
    SCORPIUS("Scorpius"),
    JERRY("Jerry"),
    PAUL("Paul");

    private final String displayName;

    public static Mayor fromName(String value) {
        if (value == null) return NONE;

        String normalized = value.trim();
        for (Mayor mayor : values()) {
            if (mayor.displayName.equalsIgnoreCase(normalized)) {
                return mayor;
            }
        }
        return NONE;
    }
}
