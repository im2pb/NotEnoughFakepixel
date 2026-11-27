package com.nef.notenoughfakepixel.config.position;

public record OverlayLayout(int width, int height, float scale, boolean centerElement) {
    public OverlayLayout {
        scale = Float.isFinite(scale) ? Math.clamp(scale, 0.5F, 5.0F) : 1.0F;
    }

    public Bounds bounds(Position position, int viewportWidth, int viewportHeight) {
        int scaledWidth = (int) Math.ceil(width * scale);
        int scaledHeight = (int) Math.ceil(height * scale);
        int x = axisStart(position.x(), position.centerX(), viewportWidth, scaledWidth);
        int y = axisStart(position.y(), position.centerY(), viewportHeight, scaledHeight);
        return new Bounds(x, y, scaledWidth, scaledHeight);
    }

    Position move(Position position, Bounds bounds, int x, int y, int viewportWidth, int viewportHeight) {
        return new Position(
                offsetAt(x, position.x(), position.centerX(), viewportWidth, bounds.width()),
                offsetAt(y, position.y(), position.centerY(), viewportHeight, bounds.height()),
                position.centerX(), position.centerY());
    }

    private int axisStart(int offset, boolean centered, int viewport, int size) {
        int end = viewport - size;
        long start = centered ? viewport / 2L + offset - centerOffset(size)
                : offset < 0 ? (long) end + offset : offset;
        return (int) Math.clamp(start, 0, Math.max(0, end));
    }

    private int offsetAt(int position, int previousOffset, boolean centered, int viewport, int size) {
        int end = Math.max(0, viewport - size);
        int start = Math.clamp(position, 0, end);
        if (centered) return start - viewport / 2 + centerOffset(size);
        if (previousOffset < 0 && start < end) return start - end;
        return start;
    }

    private int centerOffset(int size) {
        return centerElement ? size / 2 : 0;
    }

    public record Bounds(int x, int y, int width, int height) {
    }
}
