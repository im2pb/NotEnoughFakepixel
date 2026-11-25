package com.nef.notenoughfakepixel.config.position;

import com.google.gson.annotations.Expose;

public record Position(@Expose int x, @Expose int y, @Expose boolean centerX, @Expose boolean centerY) {
    public Position(int x, int y) {
        this(x, y, false, false);
    }
}
