package com.apocollis.aqtweaks.starter;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** One class entry of {@code aqtweaks_starter_classes.json}. */
public final class StarterClassDef {
    public String id = "";
    public String name = "";
    public String flavor = "";
    public String icon = "";
    @SerializedName("csg_kit")
    public String csgKit = "";
    /** Reskillable skill id to target level (raise only). */
    public Map<String, Integer> skills = new LinkedHashMap<>();
    /** Registry id of a class-exclusive perk, for example {@code aqtweaks:riposte}; empty for none. */
    public String perk = "";
    /** Console commands run after the reward; {@code %player%} is the player's name. */
    public List<String> commands = new ArrayList<>();

    public StarterClassDef() {}

    public StarterClassDef(String id, String name, String flavor, String icon, String csgKit, String perk) {
        this.id = id;
        this.name = name;
        this.flavor = flavor;
        this.icon = icon;
        this.csgKit = csgKit;
        this.perk = perk;
    }

    public StarterClassDef skill(String skillId, int level) {
        skills.put(skillId, level);
        return this;
    }

    public StarterClassDef command(String command) {
        commands.add(command);
        return this;
    }

    /** The perk path after the namespace, or an empty string. */
    public String perkKey() {
        if (perk == null || perk.isEmpty()) return "";
        int colon = perk.indexOf(':');
        return colon < 0 ? perk : perk.substring(colon + 1);
    }
}
