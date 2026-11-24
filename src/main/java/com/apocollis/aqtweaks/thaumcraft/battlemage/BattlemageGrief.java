package com.apocollis.aqtweaks.thaumcraft.battlemage;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig.BattlemageConfig;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import thaumcraft.api.casters.FocusEffect;
import thaumcraft.api.casters.FocusPackage;

/** Fire and Frost block placement by a mob caster (plan 2.2): off unless {@code Allow Mob Focus Block Placement} is on. */
public final class BattlemageGrief {

    private BattlemageGrief() {}

    /** True when this effect's cast may place blocks: a player's always, a mob's only when the config allows it. */
    public static boolean mayPlaceBlocks(FocusEffect effect) {
        if (BattlemageConfig.allowMobFocusBlockPlacement) {
            return true;
        }
        FocusPackage pkg = effect.getPackage();
        EntityLivingBase caster = pkg != null ? pkg.getCaster() : null;
        return caster == null || caster instanceof EntityPlayer;
    }
}
