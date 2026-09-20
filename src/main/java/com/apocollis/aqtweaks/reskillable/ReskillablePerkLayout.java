package com.apocollis.aqtweaks.reskillable;

import net.minecraftforge.common.config.Config;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ReskillablePerkLayout {
    private static final Logger LOGGER = LogManager.getLogger("AQTweaks-Reskillable");

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
    public int cost = 4;

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

    /**
     * Drops CAD {@code trait|} rows that are not Reskillable / Tweaks unlockables
     * (e.g. instance {@code trait|elenaidodge2:dodge}).
     */
    public void sanitizeRequirements() {
        if (requirements == null || requirements.length == 0) {
            return;
        }
        java.util.List<String> kept = new java.util.ArrayList<>(requirements.length);
        for (String raw : requirements) {
            if (raw == null) {
                continue;
            }
            String req = raw.trim();
            if (req.isEmpty()) {
                continue;
            }
            if (isSupportedRequirement(req)) {
                kept.add(req);
            } else {
                LOGGER.warn("[AQTweaks] Dropping unresolvable perk requirement '{}'.", req);
            }
        }
        requirements = kept.toArray(new String[0]);
    }

    static boolean isSupportedRequirement(String req) {
        if (req.startsWith("trait|")) {
            String id = req.substring("trait|".length());
            return id.startsWith("reskillable:") || id.startsWith("aqtweaks:");
        }
        int bar = req.indexOf('|');
        if (bar <= 0 || bar == req.length() - 1) {
            return false;
        }
        String skill = req.substring(0, bar);
        String level = req.substring(bar + 1);
        return skill.indexOf(':') > 0 && level.chars().allMatch(Character::isDigit);
    }
}
