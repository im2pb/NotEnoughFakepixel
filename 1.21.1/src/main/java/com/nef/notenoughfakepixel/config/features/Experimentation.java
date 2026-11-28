package com.nef.notenoughfakepixel.config.features;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class Experimentation {

    @Expose
    @ConfigOption(name = "Enable Solvers", desc = "Enable experimentation Solvers")
    @ConfigEditorBoolean
    public boolean experimentationSolvers = true;

    @Expose
    @ConfigOption(name = "Chronomatron Solver", desc = "Highlight the next color in Chronomatron, including Metaphysical.")
    @ConfigEditorBoolean
    public boolean experimentationChronomatronSolver = true;

    @Expose
    @ConfigOption(name = "Ultrasequencer Solver", desc = "Show the hidden stack counts in their original slots.")
    @ConfigEditorBoolean
    public boolean experimentationUltraSequencerSolver = true;

    @Expose
    @ConfigOption(name = "Superpairs Solver", desc = "Remember revealed cards and highlight possible pairs, matches and powerups.")
    @ConfigEditorBoolean
    public boolean experimentationSuperpairsSolver = true;

    @Expose
    @ConfigOption(name = "Hide Tooltips", desc = "Hide tooltips during experiments")
    @ConfigEditorBoolean
    public boolean experimentationHideTooltips = true;

    @Expose
    @ConfigOption(name = "Prevent Missclicks", desc = "Prevent missclicks during experiments")
    @ConfigEditorBoolean
    public boolean experimentationPreventMissclicks = true;

    @Expose
    @ConfigOption(
            name = "Ultrasequencer Upcoming",
            desc = "Set the border color for the Ultrasequencer slot after the next guess."
    )
    @ConfigEditorDropdown(
            values = {
                    "None", "White", "Orange", "Light Purple", "Light Blue", "Yellow", "Light Green", "Pink",
                    "Gray", "Light Gray", "Cyan", "Dark Purple", "Dark Blue", "Brown", "Dark Green", "Red", "Black"
            }
    )
    public int seqUpcoming = 5;

    @Expose
    @ConfigOption(
            name = "Superpairs Matched",
            desc = "Highlight color for confirmed matches."
    )
    @ConfigEditorDropdown(
            values = {
                    "None", "White", "Orange", "Light Purple", "Light Blue", "Yellow", "Light Green", "Pink",
                    "Gray", "Light Gray", "Cyan", "Dark Purple", "Dark Blue", "Brown", "Dark Green", "Red", "Black"
            }
    )
    public int supMatched = 6;

    @Expose
    @ConfigOption(
            name = "Superpairs Possible",
            desc = "Highlight color for cards that could match."
    )
    @ConfigEditorDropdown(
            values = {
                    "None", "White", "Orange", "Light Purple", "Light Blue", "Yellow", "Light Green", "Pink",
                    "Gray", "Light Gray", "Cyan", "Dark Purple", "Dark Blue", "Brown", "Dark Green", "Red", "Black"
            }
    )
    public int supPossible = 2;

    @Expose
    @ConfigOption(
            name = "Superpairs Unmatched",
            desc = "Highlight color for remembered cards with no known partner."
    )
    @ConfigEditorDropdown(
            values = {
                    "None", "White", "Orange", "Light Purple", "Light Blue", "Yellow", "Light Green", "Pink",
                    "Gray", "Light Gray", "Cyan", "Dark Purple", "Dark Blue", "Brown", "Dark Green", "Red", "Black"
            }
    )
    public int supUnmatched = 5;

    @Expose
    @ConfigOption(
            name = "Superpairs Powerups",
            desc = "Highlight color for revealed powerups."
    )
    @ConfigEditorDropdown(
            values = {
                    "None", "White", "Orange", "Light Purple", "Light Blue", "Yellow", "Light Green", "Pink",
                    "Gray", "Light Gray", "Cyan", "Dark Purple", "Dark Blue", "Brown", "Dark Green", "Red", "Black"
            }
    )
    public int supPower = 11;
}
