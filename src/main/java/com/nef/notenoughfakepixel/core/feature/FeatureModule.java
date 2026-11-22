/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.core.feature;

/**
 * A cohesive feature domain with an explicit lifecycle. New feature groups should use modules rather than
 * relying on package scanning or static initialization.
 */
public interface FeatureModule {
    FeatureDescriptor getDescriptor();
    void initialize(FeatureContext context) throws Exception;
    void shutdown(FeatureContext context) throws Exception;
}
