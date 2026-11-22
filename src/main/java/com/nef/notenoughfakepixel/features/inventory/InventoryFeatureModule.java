/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.inventory;

import com.nef.notenoughfakepixel.core.feature.AnnotatedPackageFeatureModule;
import com.nef.notenoughfakepixel.core.feature.FeatureCategory;
import com.nef.notenoughfakepixel.core.feature.FeatureDescriptor;
import com.nef.notenoughfakepixel.core.feature.FeatureContext;
import com.nef.notenoughfakepixel.core.feature.FeatureModule;
import com.nef.notenoughfakepixel.features.inventory.search.SearchBar;
import com.nef.notenoughfakepixel.features.skyblock.inventory.equipment.EquipmentOverlay;

/** Owns inventory-facing listeners plus annotated extensions in the inventory packages. */
public final class InventoryFeatureModule implements FeatureModule {
    private static final FeatureDescriptor DESCRIPTOR = new FeatureDescriptor(
            "inventory",
            "Inventory Features",
            "Search, equipment preview and inventory interaction features.",
            FeatureCategory.INVENTORY
    );

    private final SearchBar searchBar = new SearchBar();
    private final EquipmentOverlay equipmentOverlay = new EquipmentOverlay();
    private final AnnotatedPackageFeatureModule packageExtensions;
    private boolean searchBarRegistered;
    private boolean equipmentOverlayRegistered;
    private boolean extensionsInitialized;

    public InventoryFeatureModule(String... packageRoots) {
        packageExtensions = new AnnotatedPackageFeatureModule(
                new FeatureDescriptor("inventory-extensions", "Inventory Extensions",
                        "Annotated inventory listeners and singleton extensions.", FeatureCategory.INVENTORY),
                packageRoots);
    }

    @Override
    public FeatureDescriptor getDescriptor() {
        return DESCRIPTOR;
    }

    @Override
    public synchronized void initialize(FeatureContext context) {
        try {
            if (!searchBarRegistered) {
                context.registerEventListener(searchBar);
                searchBarRegistered = true;
            }
            if (!equipmentOverlayRegistered) {
                context.registerEventListener(equipmentOverlay);
                equipmentOverlayRegistered = true;
            }
            if (!extensionsInitialized) {
                packageExtensions.initialize(context);
                extensionsInitialized = true;
            }
        } catch (Throwable error) {
            shutdown(context);
            throw new IllegalStateException("Could not initialize inventory features", error);
        }
    }

    @Override
    public synchronized void shutdown(FeatureContext context) {
        Throwable firstError = null;
        if (extensionsInitialized) {
            try {
                packageExtensions.shutdown(context);
            } catch (Throwable error) {
                firstError = error;
            }
            extensionsInitialized = false;
        }
        if (equipmentOverlayRegistered) {
            try {
                context.unregisterEventListener(equipmentOverlay);
            } catch (Throwable error) {
                if (firstError == null) firstError = error;
                else firstError.addSuppressed(error);
            }
            equipmentOverlayRegistered = false;
        }
        if (searchBarRegistered) {
            try {
                context.unregisterEventListener(searchBar);
            } catch (Throwable error) {
                if (firstError == null) firstError = error;
                else firstError.addSuppressed(error);
            }
            searchBarRegistered = false;
        }
        if (firstError != null) throw new IllegalStateException("Could not fully stop inventory features", firstError);
    }
}
