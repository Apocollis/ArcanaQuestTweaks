package com.apocollis.aqtweaks.mixin.dss;

import com.apocollis.aqtweaks.stamina.StaminaFeathers;
import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.stamina.DssSkillCosts;
import dynamicswordskills.skills.SkillActive;
import dynamicswordskills.skills.SkillBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = SkillActive.class, remap = false)
public abstract class MixinSkillActive {

    @Unique
    private int aqtweaks$triggerCost;

    @Inject(method = "trigger", at = @At("HEAD"), cancellable = true)
    private void aqtweaks$gateDssStamina(World world, EntityPlayer player, boolean wasTriggered,
                                        CallbackInfoReturnable<Boolean> cir) {
        aqtweaks$triggerCost = 0;
        if (!aqtweaks$shouldHandle(world, player)) return;
        aqtweaks$triggerCost = DssSkillCosts.costFor(aqtweaks$registryName());
        if (aqtweaks$triggerCost <= 0) return;
        if (!StaminaFeathers.hasEnoughStamina(player, aqtweaks$triggerCost)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "trigger", at = @At("RETURN"))
    private void aqtweaks$spendDssStamina(World world, EntityPlayer player, boolean wasTriggered,
                                         CallbackInfoReturnable<Boolean> cir) {
        if (!aqtweaks$shouldHandle(world, player)) return;
        if (cir.getReturnValue() == null || !cir.getReturnValue()) return;
        int cost = aqtweaks$triggerCost;
        if (cost <= 0) return;
        if (player instanceof EntityPlayerMP) {
            StaminaFeathers.decreaseFeathers((EntityPlayerMP) player, cost);
        }
    }

    @Redirect(
            method = "trigger",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/EntityPlayer;addExhaustion(F)V",
                    remap = true
            )
    )
    private void aqtweaks$replaceDssHunger(EntityPlayer player, float exhaustion) {
        if (ArcanaQuestTweaksConfig.StaminaModuleConfig.dynamicSwordSkills.enableSkillCost
                && ArcanaQuestTweaksConfig.StaminaModuleConfig.dynamicSwordSkills.replaceHungerExhaustion) {
            return;
        }
        player.addExhaustion(exhaustion);
    }

    @Unique
    private boolean aqtweaks$shouldHandle(World world, EntityPlayer player) {
        if (!ArcanaQuestTweaksConfig.StaminaModuleConfig.dynamicSwordSkills.enableSkillCost) return false;
        if (world == null || player == null) return false;
        if (player.world.isRemote || player.capabilities.isCreativeMode || player.isSpectator()) return false;
        return true;
    }

    /** Final, non-null on {@code SkillBase}; the key {@code DssSkillCosts} is keyed on. */
    @Unique
    private String aqtweaks$registryName() {
        return ((SkillBase) (Object) this).getUnlocalizedName();
    }
}
