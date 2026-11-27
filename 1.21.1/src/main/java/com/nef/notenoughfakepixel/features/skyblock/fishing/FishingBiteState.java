package com.nef.notenoughfakepixel.features.skyblock.fishing;

final class FishingBiteState {
    private static final long UNSET = Long.MIN_VALUE;
    private static final long SLUG_DELAY = 20_000;
    private static final long SPLASH_WINDOW = 500;
    private static final long BITE_DISPLAY_TIME = 600;
    private static final long BITE_SOUND_DELAY = 50;

    private long lavaSince = UNSET;
    private long splashUntil = UNSET;
    private long biteUntil = UNSET;
    private long soundAt = UNSET;
    private boolean caughtFish;
    private boolean muted;
    private boolean nearWake;

    boolean update(boolean inLava, boolean slugMode, boolean nowCaughtFish, long now) {
        boolean changedFluid = inLava != inLava();
        if (changedFluid) lavaSince = inLava ? now : UNSET;
        boolean suppress = slugMode && inLava && now - lavaSince < SLUG_DELAY;
        if (changedFluid || (suppress && !muted)) clearSignals();
        muted = suppress;

        // Record the edge even while muted so it cannot replay after the wait.
        boolean newBite = nowCaughtFish && !caughtFish && confirm(now);
        caughtFish = nowCaughtFish;
        return changedFluid || muted || newBite;
    }

    void observeApproach(boolean near, long now) {
        if (near && !nearWake && !muted && !biting(now)) splashUntil = now + SPLASH_WINDOW;
        nearWake = near;
    }

    boolean waitingForSplash(long now) {
        return !muted && now <= splashUntil;
    }

    boolean confirmSplash(long now) {
        return waitingForSplash(now) && confirm(now);
    }

    boolean consumeSound(long now) {
        if (soundAt == UNSET || now < soundAt) return false;
        soundAt = UNSET;
        return !muted && biting(now);
    }

    boolean inLava() {
        return lavaSince != UNSET;
    }

    boolean muted() {
        return muted;
    }

    boolean biting(long now) {
        return now < biteUntil;
    }

    void clear() {
        lavaSince = UNSET;
        caughtFish = false;
        muted = false;
        clearSignals();
    }

    private boolean confirm(long now) {
        if (muted || biting(now)) return false;
        splashUntil = UNSET;
        nearWake = false;
        biteUntil = now + BITE_DISPLAY_TIME;
        soundAt = now + BITE_SOUND_DELAY;
        return true;
    }

    private void clearSignals() {
        splashUntil = UNSET;
        biteUntil = UNSET;
        soundAt = UNSET;
        nearWake = false;
    }
}
