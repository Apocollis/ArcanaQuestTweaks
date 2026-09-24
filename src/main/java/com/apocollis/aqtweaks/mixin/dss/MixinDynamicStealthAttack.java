package com.apocollis.aqtweaks.mixin.dss;

import com.apocollis.aqtweaks.reskillable.PerkOpportunistic;
import com.fantasticsource.dynamicstealth.DynamicStealth;
import com.fantasticsource.dynamicstealth.server.senses.sight.Sight;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = DynamicStealth.class, remap = false)
public abstract class MixinDynamicStealthAttack {

    @Redirect(
            method = "entityAttackedPre",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/fantasticsource/dynamicstealth/server/senses/sight/Sight;canSee(Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/entity/Entity;Z)Z"
            )
    )
    private static boolean aqtweaks$opportunistic(EntityLivingBase victim, Entity attacker, boolean sneaking) {
        if (PerkOpportunistic.qualifies(victim, attacker)) {
            return false;
        }
        return Sight.canSee(victim, attacker, sneaking);
    }
}
