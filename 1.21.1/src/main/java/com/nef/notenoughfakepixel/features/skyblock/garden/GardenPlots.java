package com.nef.notenoughfakepixel.features.skyblock.garden;

import com.nef.notenoughfakepixel.features.skyblock.garden.GardenWorldRenderer.Lines;
import net.minecraft.util.math.Box;

import java.util.Map;

final class GardenPlots {
    private static final double FACE_OFFSET = 0.01;
    private static final Box[] BOUNDS = {
            new Box(192, 67, 96, 288, 100, 192),
            new Box(96, 67, 192, 192, 100, 288),
            new Box(192, 67, 288, 288, 100, 384),
            new Box(288, 67, 192, 384, 100, 288),
            new Box(96, 67, 96, 192, 100, 192),
            new Box(288, 67, 96, 384, 100, 192),
            new Box(96, 67, 288, 192, 100, 384),
            new Box(288, 67, 288, 384, 100, 384),
            new Box(192, 67, 0, 288, 100, 96),
            new Box(0, 67, 192, 96, 100, 288),
            new Box(384, 67, 192, 480, 100, 288),
            new Box(192, 67, 384, 288, 100, 480),
            new Box(96, 67, 0, 192, 100, 96),
            new Box(288, 67, 0, 384, 100, 96),
            new Box(0, 67, 96, 96, 100, 192),
            new Box(384, 67, 96, 480, 100, 192),
            new Box(0, 67, 288, 96, 100, 384),
            new Box(384, 67, 288, 480, 100, 384),
            new Box(96, 67, 384, 192, 100, 480),
            new Box(288, 67, 384, 384, 100, 480),
            new Box(0, 67, 0, 96, 100, 96),
            new Box(384, 67, 0, 480, 100, 96),
            new Box(0, 67, 384, 96, 100, 480),
            new Box(384, 67, 384, 480, 100, 480)
    };

    private GardenPlots() {
    }

    static void drawInfested(Lines lines, Map<Integer, Integer> pests, double spacing) {
        for (var entry : pests.entrySet()) {
            int plot = entry.getKey();
            if (plot < 1 || plot > BOUNDS.length || entry.getValue() <= 0) continue;

            Box bounds = BOUNDS[plot - 1];
            drawFace(lines, bounds.minX, bounds.maxX, bounds.minY, bounds.maxY, bounds.minZ - FACE_OFFSET, true, spacing);
            drawFace(lines, bounds.minX, bounds.maxX, bounds.minY, bounds.maxY, bounds.maxZ + FACE_OFFSET, true, spacing);
            drawFace(lines, bounds.minZ, bounds.maxZ, bounds.minY, bounds.maxY, bounds.minX - FACE_OFFSET, false, spacing);
            drawFace(lines, bounds.minZ, bounds.maxZ, bounds.minY, bounds.maxY, bounds.maxX + FACE_OFFSET, false, spacing);
        }
    }

    private static void drawFace(Lines lines, double min, double max, double bottom, double top, double fixed,
                                 boolean alongX, double spacing) {
        faceLine(lines, min, bottom, max, bottom, fixed, alongX);
        faceLine(lines, min, top, max, top, fixed, alongX);
        faceLine(lines, min, bottom, min, top, fixed, alongX);
        faceLine(lines, max, bottom, max, top, fixed, alongX);

        for (double edge = min + spacing; edge < max; edge += spacing) {
            faceLine(lines, edge, bottom, edge, top, fixed, alongX);
        }
        for (double y = bottom + spacing; y < top; y += spacing) {
            faceLine(lines, min, y, max, y, fixed, alongX);
        }
    }

    private static void faceLine(Lines lines, double a, double y1, double b, double y2, double fixed, boolean alongX) {
        if (alongX) {
            lines.draw(a, y1, fixed, b, y2, fixed);
        } else {
            lines.draw(fixed, y1, a, fixed, y2, b);
        }
    }
}
