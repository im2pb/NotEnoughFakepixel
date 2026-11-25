package com.nef.notenoughfakepixel.features.skyblock.overlays.stats;

import lombok.Getter;
import lombok.Setter;

/**
 * Immutable-style data model that holds the player's current Skyblock stats.
 * Values are updated by StatBars when a type-2 action bar message arrives.
 */
@Getter
public class PlayerStats {

    // Health
    private int health;
    private int maxHealth;
    private int overflowHealth;

    // Mana
    private int mana;
    private int maxMana;
    private int overflowMana;

    // Other stats
    @Setter
    private int defence;
    @Setter
    private int speed = 100;
    private int expLevel;
    private float exp;

    // ── Setters ──────────────────────────────────────────────────────────────

    public void setHealth(int health, int maxHealth) {
        this.health = health;
        this.maxHealth = maxHealth;
        this.overflowHealth = Math.max(0, health - maxHealth);
    }

    public void setMana(int mana, int maxMana, int overflowMana) {
        this.mana = mana;
        this.maxMana = maxMana;
        this.overflowMana = overflowMana;
    }

    public void setExp(float exp, int expLevel) {
        this.exp = exp;
        this.expLevel = expLevel;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** @return true once we have received at least one action bar update */
    public boolean isValid() {
        return maxHealth > 0 && maxMana > 0;
    }
}
