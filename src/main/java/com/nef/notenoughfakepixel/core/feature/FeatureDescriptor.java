/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.core.feature;

/** Immutable identity and documentation for a feature module. */
public final class FeatureDescriptor {
    private final String id;
    private final String displayName;
    private final String description;
    private final FeatureCategory category;

    public FeatureDescriptor(String id, String displayName, String description, FeatureCategory category) {
        if (id == null || id.trim().isEmpty()) throw new IllegalArgumentException("Feature id cannot be blank");
        if (displayName == null || displayName.trim().isEmpty()) throw new IllegalArgumentException("Feature name cannot be blank");
        if (category == null) throw new IllegalArgumentException("Feature category cannot be null");
        this.id = id.trim();
        this.displayName = displayName.trim();
        this.description = description == null ? "" : description.trim();
        this.category = category;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public FeatureCategory getCategory() { return category; }
}
