package com.apocollis.aqtweaks.thaumcraft.battlemage;

import net.minecraft.item.Item;
import thaumcraft.api.casters.FocusPackage;

/** A resolved loadout focus: the data row, the deserialized package template and what the gauntlet shows. */
public final class BattlemageFocus {
    public final FocusDef def;
    /** Null for AI-performed foci (Blink, Rift). Never cast directly: copy it first. */
    public final FocusPackage template;
    public final Item item;
    public final int color;

    public BattlemageFocus(FocusDef def, FocusPackage template, Item item, int color) {
        this.def = def;
        this.template = template;
        this.item = item;
        this.color = color;
    }
}
