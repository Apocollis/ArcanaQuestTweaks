package com.apocollis.aqtweaks.reskillable;

import net.minecraftforge.common.config.Config;

public class ReskillablePerkLayout {
    @Config.Name("Enable")
    @Config.Comment("If false, the trait is registered but disabled (hidden / unpurchasable). Restart after change.")
    public boolean enable = true;

    @Config.Name("X-Pos")
    @Config.RangeInt(min = 0, max = 4)
    public int x = 0;

    @Config.Name("Y-Pos")
    @Config.RangeInt(min = 0, max = 3)
    public int y = 0;

    @Config.Name("Skill Point Cost")
    @Config.RangeInt(min = 0, max = 32)
    public int cost = 6;

    @Config.Name("Parent Skill")
    @Config.Comment("Registry id, e.g. reskillable:attack")
    public String parentSkill = "reskillable:attack";

    @Config.Name("Requirements")
    @Config.Comment("CAD requirement strings, e.g. reskillable:mining|24")
    public String[] requirements = new String[0];

    public ReskillablePerkLayout() {}

    public ReskillablePerkLayout(int x, int y, int cost, String parentSkill, String... requirements) {
        this.x = x;
        this.y = y;
        this.cost = cost;
        this.parentSkill = parentSkill;
        this.requirements = requirements;
    }
}
