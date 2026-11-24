package com.apocollis.aqtweaks.starter;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.item.ItemStack;

/** What the "what you gained" panel shows after the server applied a class (plan 12.10). */
public final class StarterResult {

    /** One raised skill: its registry id, a plain display name and the level it now has. */
    public static final class SkillGain {
        public final String skillId;
        public final String skillName;
        public final int level;

        public SkillGain(String skillId, String skillName, int level) {
            this.skillId = skillId;
            this.skillName = skillName;
            this.level = level;
        }
    }

    public final String classId;
    public final String className;
    /** Perk path, for example {@code riposte}; lang keys are {@code aqtweaks.perk.<perkKey>.name} and {@code .desc}. */
    public final String perkKey;
    public final List<SkillGain> skills;
    /** Capped at 12, armor first: the kit minus the shared default items. */
    public final List<ItemStack> gear;

    public StarterResult(String classId, String className, String perkKey, List<SkillGain> skills, List<ItemStack> gear) {
        this.classId = classId;
        this.className = className;
        this.perkKey = perkKey;
        this.skills = skills == null ? new ArrayList<SkillGain>() : skills;
        this.gear = gear == null ? new ArrayList<ItemStack>() : gear;
    }
}
