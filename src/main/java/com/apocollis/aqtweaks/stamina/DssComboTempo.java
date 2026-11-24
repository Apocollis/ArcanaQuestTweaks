package com.apocollis.aqtweaks.stamina;

import dynamicswordskills.entity.DSSPlayerInfo;
import dynamicswordskills.skills.Combo;
import dynamicswordskills.skills.ICombo;
import dynamicswordskills.skills.SkillBase;
import net.minecraft.entity.player.EntityPlayer;

/** Weapon Tempo's read of the player's live DSS combo. Touched only when Dynamic Sword Skills is loaded. */
final class DssComboTempo {

    private DssComboTempo() {}

    /**
     * True when the melee hit about to land is free. {@code interval}: every Nth hit of the combo is free. Hits 1
     * and 2 never are; any other hit rolls {@code chancePerLevel x Basic Technique level}. Server side only.
     */
    static boolean nextHitFree(EntityPlayer player, int interval, double chancePerLevel) {
        DSSPlayerInfo info = DSSPlayerInfo.get(player);
        if (info == null) return false;
        ICombo skill = info.getComboSkill();
        if (skill == null) return false;
        Combo combo = skill.getCombo();
        if (combo == null || combo.isFinished()) return false;
        int next = combo.getNumHits() + 1;
        if (next <= 2) return false;
        if (interval > 0 && next % interval == 0) return true;
        if (chancePerLevel <= 0.0D) return false;
        SkillBase basic = info.getPlayerSkill(SkillBase.swordBasic);
        int level = basic == null ? 0 : basic.getLevel();
        double chance = Math.min(1.0D, level * chancePerLevel);
        return chance > 0.0D && player.getRNG().nextDouble() < chance;
    }
}
