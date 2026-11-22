/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.envcheck.registers;

public interface GatedListener {

    boolean isListenerActive();

    default void onListenerDeactivated() {
    }
}
