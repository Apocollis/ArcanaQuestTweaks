package com.apocollis.aqtweaks.starter;

import java.util.ArrayList;
import java.util.List;

/** Root of {@code config/arcanaquesttweaks/aqtweaks_starter_classes.json}. */
public final class StarterSettings {
    /** Text shown above the options (the story paragraph). */
    public String intro = "";
    /** The question shown directly above the options. */
    public String prompt = "";
    /** Text above the options when a Tabula Rasa draught reopens the menu. */
    public String resetIntro = "";
    /** The question above the options for a reawakening. */
    public String resetPrompt = "";
    /** Test switch reserved for showing skills and perk on the choice screen; the packet does not carry them yet. */
    public boolean showDetails = false;
    /** How long the reveal panel stays when the player does not click Continue. */
    public int revealHoldSeconds = 12;
    public List<StarterClassDef> classes = new ArrayList<>();
}
