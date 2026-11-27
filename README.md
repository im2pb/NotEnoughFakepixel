# NotEnoughFakepixel

![NEF](1.8.9/src/main/resources/assets/notenoughfakepixel/logo.png)

This is a remaster of NotEnoughFakepixel mod from GinaFro.

<div align="center">
    <a href="https://discord.gg/xjFy4GQmbp" target="_blank">
        <img alt="Discord" src="https://img.shields.io/discord/1318248790452342784?style=for-the-badge&logo=github&logoColor=white&color=008b8b">
    </a>
     <a href="https://github.com/davidbelesp/NotEnoughFakepixel/graphs/contributors" target="_blank">
        <img alt="GitHub contributors" src="https://img.shields.io/github/contributors-anon/davidbelesp/NotEnoughFakepixel?style=for-the-badge&color=008b8b">
    </a>
    <a href="https://deepwiki.com/davidbelesp/NotEnoughFakepixel"><img src="https://deepwiki.com/badge.svg" alt="Ask DeepWiki"></a>
    <a href="https://github.com/davidbelesp/NotEnoughFakepixel/releases" target="_blank">
       <img alt="GitHub Downloads (all assets, all releases)" src="https://img.shields.io/github/downloads/davidbelesp/NotEnoughFakepixel/total?style=for-the-badge&color=008b8b">
    </a>
</div>

<div align="left">

## Minecraft versions

| Project | Mod loader | Build requirements | Status |
| --- | --- | --- | --- |
| [`1.8.9/`](1.8.9/) | Forge | JDK 17 for Gradle and a Java 8 toolchain | Existing mod with the features listed below |
| [`1.21.1/`](1.21.1/) | Fabric | JDK 21 | Configuration GUI available. Gameplay features are still being ported. |

Open either version folder as a Gradle project. From that folder, run `.\gradlew.bat build` on Windows or `bash ./gradlew build` on macOS or Linux. The JAR is written to that project's `build/libs/` directory.

In IntelliJ IDEA, open the repository folder to use the shared run configurations. If the Gradle tool window does not list both builds, import `1.8.9/build.gradle.kts` and `1.21.1/build.gradle.kts` as Gradle projects. Use each project's Gradle wrapper and set the Gradle JVM to JDK 21. Select **Forge 1.8.9 Client** or **Fabric 1.21.1 Client** in the Run dropdown. The first launch downloads the game's assets and runtime libraries.

If you open a version folder as its own project, run `runForgeClient` for Forge or `runClient` for Fabric from its Gradle tool window. Pass `-PdevAuth=true` for Microsoft sign-in.

For servers that require a signed-in account, select **Forge 1.8.9 Client (Microsoft Sign-In)** or **Fabric 1.21.1 Client (Microsoft Sign-In)**. Both configurations enable DevAuth. Open the Microsoft sign-in link from IntelliJ's Run console when prompted. Use the standard client configurations for local testing.

If DevAuth reports `No account specified` on first setup, uncomment `defaultAccount = "main"` in its `config.toml` and run the configuration again. On Windows that file is in `%USERPROFILE%\.devauth`.

In the 1.21.1 Fabric client, install **Fabric API** and open the configuration GUI with **P**, `/nef`, or `/notenoughfakepixel`. To open with a search, use a command such as `/nef Dungeons`. Settings are saved to `config/notenoughfakepixel.json` when the screen closes. The menu includes the legacy categories and editable settings. Use `/nef Debug` to find the debug loggers.

### Registering Fabric features

Add features under `com.nef.notenoughfakepixel` and use the annotations in `env.registers`.

- `@RegisterEvents` goes on a class that implements `ClientModInitializer`. Attach Fabric event callbacks in `onInitializeClient()`.
- `@RegisterCommand` goes on a class that implements `ClientCommandRegistrationCallback`. Add your commands in `register()`.
- `@RegisterKeybind` goes on an initialized static `KeyBinding` field.
- `@RegisterInstance` goes on an initialized static field holding a feature that implements either callback interface. Use it for singletons or constructors with arguments.

Classes registered directly need a no-argument constructor. [ClientConfig](1.21.1/src/main/java/com/nef/notenoughfakepixel/config/ClientConfig.java) shows how to register events, commands, and a keybind on one feature.

## License
This project is licensed under the GNU Lesser General Public License v3.0 or later (LGPL-3.0-or-later).
See the [LICENSE](LICENSE) file for details.

## Features

<!-- port-progress:start -->

![1.21 port progress (18.5%)](https://progress-bar.xyz/18/?scale=100&width=350&progress_color=338b33) `39/211 features`

<!-- port-progress:end -->

### Quality of life

| Subcategory | Feature | 1.8.9 | 1.21 |
| --- | --- | --- | --- |
| General | Alerts | X |  |
| General | Aliases | X |  |
| General | Disable block breaking particles | X | X |
| General | Full block lever | X |  |
| General | Block placing items | X |  |
| General | Show unclaimed Jacob rewards | X | X |
| General | Spider's den relic waypoints | X |  |
| General | Etherwarp overlay | X |  |
| General | Etherwarp sounds | X |  |
| General | Hide flaming fists | X |  |
| General | Hide player armor | X |  |
| General | Hide dead mobs | X |  |
| General | Hide falling blocks | X |  |
| General | Item animations | X |  |
| General | Smol people | X |  |
| General | Reforge helper | X |  |
| General | Disable Hyperion Explosions | X |  |
| General | Disable Thunderlord enchantment bolts | X |  |
| General | No Hurt camera animation | X |  |
| General | Golden enchants | X |  |
| General | Fullbright | X |  |
| General | Disable Potion Effects in inventory | X |  |
| General | Middle click on Menus | X |  |
| General | Disable sounds from Jerry-chine gun and AOTE | X |  |
| General | Midas Staff minimal animation and sounds | X |  |
| General | Wardrobe key shortcut | X |  |
| General | Equipment key shortcut | X |  |
| General | Hide players close to NPCs | X |  |
| General | Show Ender Nodes | X |  |
| General | Visual item cooldowns | X |  |
| General | Disable Enderman teleporting | X |  |
| General | Full block buttons | X |  |
| General | Highlight active wardrobe slot | X | X |
| General | Smooth AOTE teleport | X |  |
| General | Pingless cactus | X |  |
| General | Disable rain | X |  |
| General | Item rarity backgrounds | X |  |
| General | Etherwarp zoom | X |  |
| General | Etherwarp text helper | X |  |
| General | Wardrobe slot shortcuts | X |  |
| General | Island, Hub, and Dungeon Hub warp shortcuts | X |  |
| General | Drinking animation fixes | X |  |
| General | Salvage protection by item rarity | X |  |
| General | Admin item tooltip tags | X |  |
| General | Stacking enchantment counters | X |  |
| General | Dark Auction timer | X | X |
| General | Dark Auction reminder | X |  |
| Chat | Disable Friend > joined messages | X |  |
| Chat | Disable Info & Watchdog messages | X |  |
| Chat | Disable selling ranks messages | X |  |
| Chat | Disable zombie rare drop messages | X |  |
| Chat | Disable server announcements | X |  |
| Chat | Disable game invite spam | X |  |

### Farming

| Feature | 1.8.9 | 1.21 |
| --- | --- | --- |
| 1.12 Crop height | X | X |

### Garden

| Feature | 1.8.9 | 1.21 |
| --- | --- | --- |
| Pests overlay | X | X |
| Visitors overlay | X | X |
| Crop milestone overlay | X | X |
| Pest plot grid | X | X |
| Pest finder with boxes and tracers | X | X |
| Vacuum pests bag count | X | X |

### Fishing

| Feature | 1.8.9 | 1.21 |
| --- | --- | --- |
| Fishing countdown | X |  |
| Fishing notifier for Legendary Sea Creatures | X |  |
| Notifier for Trophy Fishing | X |  |
| Incoming bite alert | X |  |
| Slug mode | X |  |

### Dungeons

| Subcategory | Feature | 1.8.9 | 1.21 |
| --- | --- | --- | --- |
| General | Custom Leap GUI | X |  |
| General | Custom Terminal GUI | X |  |
| General | Blood ready title | X |  |
| General | Boulder Solver | X |  |
| General | Silverfish Solver | X |  |
| General | Wither doors highlight | X |  |
| General | Teleport maze Solver | X |  |
| General | Terminal waypoints | X | X |
| General | Custom S+ message | X |  |
| General | Spirit bow tracer | X |  |
| General | Dungeon Map | X | X |
| General | Three Weirdos Solver | X | X |
| General | Auto Ready | - | -|
| General | Auto close secret chests | X |  |
| General | "Unable to locate sign" hidden | X |  |
| General | S+ reminder | X |  |
| General | Score overlay | X |  |
| General | Secret overlay | X |  |
| General | Wither and Blood key tracers | X |  |
| General | Mute irrelevant dungeon messages | X |  |
| General | Dungeon salvage protection | X |  |
| General | Correct Livid finder | X |  |
| General | Highlight opened Croesus chests | X | X |
| General | Spirit Leap announcements | X |  |
| General | Dungeon map rotation | X |  |
| General | S notifier | X |  |
| General | Simple score overlay | X |  |
| General | Paul-aware score calculation | X |  |
| General | Item secret and Wither Essence highlights | X |  |
| Starred mobs | Starred mob display | X | X |
| Starred mobs | Fel position display | X |  |
| Starred mobs | Bat position display | X |  |
| Starred mobs | Starred mob ESP | X | X |
| Floor 7 | Terminal solvers | X |  |
| Floor 7 | Terminal tracker | X |  |
| Floor 7 | Box withers | X |  |
| Floor 7 | Hide incorrect terminal slots | X |  |
| Floor 7 | Prevent terminal misclicks | X |  |
| Floor 7 | Hide terminal tooltips | X |  |
| Floor 7 | First Device Solver | X |  |
| Floor 7 | Third Device Solver | X |  |
| Master Mode 7 | M7 dragon highlight | X |  |
| Master Mode 7 | M7 relic waypoints | X |  |
| Master Mode 7 | Dragon statue distance boxes | X |  |
| Puzzle Solvers | Creeper Solver | X |  |
| Puzzle Solvers | Water Solver | X |  |

### Mining

| Subcategory | Feature | 1.8.9 | 1.21 |
| --- | --- | --- | --- |
| General | Drill Animation Reset FIX | X |  |
| General | Mining Ability ready notifier | X |  |
| General | Dwarven waypoints | X | X |
| Mining Overlay | Drill Fuel Overlay | X |  |
| Mining Overlay | Mining Ability cooldown Overlay | X |  |
| Mining Overlay | Commissions Overlay | X |  |
| Mining Overlay | Mithril Powder Overlay | X |  |
| Mining Overlay | Puzzler NPC Solver | X |  |
| Mining Overlay | Gemstone Powder Overlay | X |  |
| Crystal Hollows | Crystal Hollows Map | X |  |
| Crystal Hollows | Crystal Hollows Waypoints | X |  |
| Crystal Hollows | FullBlock glass panes | X |  |
| Crystal Hollows | Treasure Found notifier | X |  |
| Crystal Hollows | Precursor City Overlay | X |  |
| Crystal Hollows | Automaton Overlay | X |  |
| Crystal Hollows | Metal Detector Guesser | X |  |
| Crystal Hollows | Worm Notifier | X |  |
| Crystal Hollows | Mines of Divan Overlay | X |  |
| Crystal Hollows | Treasure chest particle helper | X |  |
| Crystal Hollows | Worm spawn timer | X | X |
| Crystal Hollows | Heat notifier | X |  |
| Crystal Hollows | Chest tracker | X |  |
| Dwarven Mines | Reveal invisible Ghosts | X |  |
| Dwarven Mines | Suppress Don Espresso messages | X |  |

### Experimentation Table

| Feature | 1.8.9 | 1.21 |
| --- | --- | --- |
| Chronomatron solver | X |  |
| Ultrasequencer solver | X |  |
| Prevent missclicks | X |  |
| Superpairs solver | X |  |
| Hide Experimentation tooltips | X |  |

### Chocolate Factory

| Feature | 1.8.9 | 1.21 |
| --- | --- | --- |
| Highlight the best Chocolate Factory upgrade | X |  |
| Hoppity's hunt eggs waypoints | X |  |
| New Egg notifier | X |  |
| New Egg timer | X | X |

### Crimson Isle

| Feature | 1.8.9 | 1.21 |
| --- | --- | --- |
| Bosses notifier | X |  |
| Ashfang helper & overlay | X |  |
| Crimson Isle Waypoints | X | X |
| Ashfang and Gravity Orb waypoints | X |  |
| Ashfang minion hitboxes | X |  |
| Ashfang chat and sound filters | X |  |
| Ashfang hit sound | X |  |

### Diana

| Feature | 1.8.9 | 1.21 |
| --- | --- | --- |
| Burrow guess | X |  |
| Warp keybind | X |  |
| Burrow waypoints | X |  |
| Gaia construct helper | X |  |
| Siamese Lynx helper | X |  |
| Minos Inquisitor party chat & waypoint | X |  |
| Inquisitor Outline | X |  |
| Burrow guess tracer | X |  |
| Burrow waypoint labels and tracers | X |  |
| Warp helper | X |  |
| Diana waypoint sounds | X |  |
| Mute Diana explosion sounds | X |  |
| Hide Ancestral Spade cooldown messages | X |  |

### Slayers

| Feature | 1.8.9 | 1.21 |
| --- | --- | --- |
| Voidgloom Seraph beacon waypoint BETA | X |  |
| Miniboss spawn alert | X |  |
| Slayer health display | X |  |
| Slayer boss time | X |  |
| Blaze pillar title | X |  |
| Blaze attunements | X |  |
| Faster Maddox calling | X |  |
| Slayer progress overlay | X |  |
| Slayer miniboss hitboxes | X |  |
| Slayer boss hitboxes and outlines | X |  |
| Voidgloom beacon tracer | X |  |
| Voidgloom beacon notifier | X |  |
| Hellion Shield chat filter | X |  |

### Waypoints

| Feature | 1.8.9 | 1.21 |
| --- | --- | --- |
| Fairy soul waypoints | X | X |
| Global waypoint toggle | X | X |
| Fairy soul progress reset | X | X |
| Jerry Island gift waypoints | X | X |
| St. Jerry waypoint | X | X |

### Slot Locking

| Feature | 1.8.9 | 1.21 |
| --- | --- | --- |
| Slot locking & binding | X | X |
| Bound-slot drop protection | X | X |
| Slot locking storage exception | X |  |
| Item swap drop delay | X |  |
| Slot lock sounds | X | X |

### Overlays

| Feature | 1.8.9 | 1.21 |
| --- | --- | --- |
| Equipment overlay | X |  |
| Storage overlay | X |  |
| Custom health, mana, XP, speed, and defense bars | X |  |
| Hide the vanilla action bar and HUD icons | X |  |

### Accessories

| Feature | 1.8.9 | 1.21 |
| --- | --- | --- |
| Missing Accessories GUI | X |  |

### Pets

| Feature | 1.8.9 | 1.21 |
| --- | --- | --- |
| Show current pet in pet menu | X | X |
| Pet tracker | X |  |
| Pets menu shortcut | X |  |

### Misc

| Feature | 1.8.9 | 1.21 |
| --- | --- | --- |
| Always sprint | X | X |
| Copy chat message | X | X |
| Scrollable tooltips | X | X |
| Terminal simulator (`/termsim`) | X |  |
| Mod sounds toggle | X |  |

### Debug

| Subcategory | Feature | 1.8.9 | 1.21 |
| --- | --- | --- | --- |
| General | Chat and console debug logs | X | X |
| General | Force Pojav | X |  |
| General | Copy hovered item NBT | X |  |
| General | SkyBlock change tracing | — |  |
| General | Sound debug logging | — |  |
| General | Trace outside SkyBlock | — |  |
| Loggers | Log location | X | X |
| Loggers | Log gamemode and SkyBlock status | X | X |
| Loggers | Log scoreboard | X | X |
| Loggers | Show held item SkyBlock ID | X |  |
| Loggers | Log loaded SkyBlock data | X |  |
| Loggers | Copy Fairy Souls API data | X |  |
| Loggers | Trigger the worm spawn timer | X |  |
| Loggers | Log tab list | X | X |
| Loggers | Copy loaded SkyBlock data | X | X |
| Loggers | Log client environment | — |  |


</div>
