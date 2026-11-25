package com.nef.notenoughfakepixel;

import com.google.gson.annotations.Expose;
import com.nef.notenoughfakepixel.config.features.*;
import io.github.notenoughupdates.moulconfig.Config;
import io.github.notenoughupdates.moulconfig.annotations.Category;
import net.fabricmc.loader.api.FabricLoader;

public final class Configuration extends Config {
    @Expose
    @Category(name = "About", desc = "Mod information and links")
    public About about = new About();

    @Expose
    @Category(name = "Quality of Life", desc = "Quality of life settings")
    public QualityOfLife qol = new QualityOfLife();

    @Expose
    @Category(name = "Dungeons", desc = "Dungeons settings")
    public Dungeons dungeons = new Dungeons();

    @Expose
    @Category(name = "Diana", desc = "Diana settings")
    public DianaF diana = new DianaF();

    @Expose
    @Category(name = "Slayer", desc = "Slayer settings")
    public Slayer slayer = new Slayer();

    @Expose
    @Category(name = "Experimentation Table", desc = "Experimentation Table settings")
    public Experimentation experimentation = new Experimentation();

    @Expose
    @Category(name = "Chocolate Factory", desc = "Chocolate Factory settings")
    public ChocolateFactory chocolateFactory = new ChocolateFactory();

    @Expose
    @Category(name = "Crimson", desc = "Crimson settings")
    public Crimson crimson = new Crimson();

    @Expose
    @Category(name = "Mining", desc = "Mining settings")
    public Mining mining = new Mining();

    @Expose
    @Category(name = "Garden", desc = "Garden settings")
    public Garden garden = new Garden();

    @Expose
    @Category(name = "Fishing", desc = "Fishing settings")
    public Fishing fishing = new Fishing();

    @Expose
    @Category(name = "Slot Locking", desc = "Slot locking settings")
    public SlotLocking sl = new SlotLocking();

    @Expose
    @Category(name = "Misc", desc = "Miscellaneous settings")
    public Misc misc = new Misc();

    @Expose
    @Category(name = "Waypoints", desc = "Waypoint settings")
    public Waypoints waypoints = new Waypoints();

    @Expose
    @Category(name = "Overlays", desc = "Overlay settings")
    public Overlays overlays = new Overlays();

    @Expose
    @Category(name = "Accessories", desc = "Accessory settings")
    public Accessories accessories = new Accessories();

    @Expose
    @Category(name = "Pets", desc = "Pet settings")
    public Pets pets = new Pets();

    @Expose
    @Category(name = "Debug", desc = "Debug settings")
    public Debug debug = new Debug();

    @Override
    public String getTitle() {
        return FabricLoader.getInstance().getModContainer("notenoughfakepixel")
                .map(mod -> "NotEnoughFakepixel " + mod.getMetadata().getVersion().getFriendlyString())
                .orElse("NotEnoughFakepixel");
    }
}
