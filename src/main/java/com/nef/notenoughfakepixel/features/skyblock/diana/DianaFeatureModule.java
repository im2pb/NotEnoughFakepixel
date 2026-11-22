/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.diana;

import com.nef.notenoughfakepixel.core.feature.FeatureCategory;
import com.nef.notenoughfakepixel.core.feature.FeatureContext;
import com.nef.notenoughfakepixel.core.feature.FeatureDescriptor;
import com.nef.notenoughfakepixel.core.feature.FeatureModule;
import com.nef.notenoughfakepixel.features.skyblock.diana.core.DianaRuntimeContext;
import com.nef.notenoughfakepixel.features.skyblock.diana.guess.GuessBurrow;

/** Explicit lifecycle owner for Diana's legacy event listeners and shared runtime services. */
public final class DianaFeatureModule implements FeatureModule {
    private static final FeatureDescriptor DESCRIPTOR = new FeatureDescriptor(
            "diana",
            "Diana",
            "Burrow detection, guess, warp helper, Diana mobs and Inquisitor support.",
            FeatureCategory.SKYBLOCK
    );

    private DianaRuntimeContext runtimeContext;
    private Diana dianaListener;
    private GuessBurrow guessListener;
    private boolean dianaRegistered;
    private boolean guessRegistered;

    @Override
    public FeatureDescriptor getDescriptor() {
        return DESCRIPTOR;
    }

    @Override
    public synchronized void initialize(FeatureContext context) {
        if (runtimeContext != null) return;
        if (context == null) throw new IllegalArgumentException("Feature context cannot be null");

        DianaRuntimeContext newRuntime = new DianaRuntimeContext();
        Diana newDianaListener = new Diana(newRuntime);
        GuessBurrow newGuessListener = new GuessBurrow(newRuntime);
        boolean registeredDiana = false;
        boolean registeredGuess = false;

        try {
            context.registerEventListener(newDianaListener);
            registeredDiana = true;
            context.registerEventListener(newGuessListener);
            registeredGuess = true;

            runtimeContext = newRuntime;
            dianaListener = newDianaListener;
            guessListener = newGuessListener;
            dianaRegistered = true;
            guessRegistered = true;
        } catch (Throwable error) {
            if (registeredGuess) {
                try { context.unregisterEventListener(newGuessListener); }
                catch (Throwable cleanupError) { error.addSuppressed(cleanupError); }
            }
            if (registeredDiana) {
                try { context.unregisterEventListener(newDianaListener); }
                catch (Throwable cleanupError) { error.addSuppressed(cleanupError); }
            }
            try { newGuessListener.resetForLifecycle(); }
            catch (Throwable cleanupError) { error.addSuppressed(cleanupError); }
            try { newDianaListener.onModuleShutdown(); }
            catch (Throwable cleanupError) { error.addSuppressed(cleanupError); }
            try { newRuntime.close(); }
            catch (Throwable cleanupError) { error.addSuppressed(cleanupError); }
            throw new IllegalStateException("Could not initialize Diana module", error);
        }
    }

    @Override
    public synchronized void shutdown(FeatureContext context) {
        Throwable firstError = null;

        if (context != null && guessRegistered && guessListener != null) {
            try { context.unregisterEventListener(guessListener); }
            catch (Throwable error) { firstError = error; }
        }
        guessRegistered = false;

        if (guessListener != null) {
            try { guessListener.resetForLifecycle(); }
            catch (Throwable error) {
                if (firstError == null) firstError = error;
                else firstError.addSuppressed(error);
            }
        }

        if (context != null && dianaRegistered && dianaListener != null) {
            try { context.unregisterEventListener(dianaListener); }
            catch (Throwable error) {
                if (firstError == null) firstError = error;
                else firstError.addSuppressed(error);
            }
        }
        dianaRegistered = false;

        if (dianaListener != null) {
            try { dianaListener.onModuleShutdown(); }
            catch (Throwable error) {
                if (firstError == null) firstError = error;
                else firstError.addSuppressed(error);
            }
        }

        if (runtimeContext != null) {
            try { runtimeContext.close(); }
            catch (Throwable error) {
                if (firstError == null) firstError = error;
                else firstError.addSuppressed(error);
            }
        }

        runtimeContext = null;
        dianaListener = null;
        guessListener = null;

        if (firstError != null) throw new IllegalStateException("Could not fully stop Diana module", firstError);
    }
}
