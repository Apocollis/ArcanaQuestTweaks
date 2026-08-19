package com.apocollis.aqtweaks.mixin.dss;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.stamina.DssSkillCosts;
import com.apocollis.aqtweaks.util.Reflect;
import com.elenai.elenaidodge2.api.FeathersHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "dynamicswordskills.skills.SkillActive", remap = false)
public abstract class MixinSkillActive {

    @Inject(method = "trigger", at = @At("HEAD"), cancellable = true)
    private void aqtweaks$gateDssStamina(World world, EntityPlayer player, boolean wasTriggered,
                                        CallbackInfoReturnable<Boolean> cir) {
        if (!aqtweaks$shouldHandle(world, player)) return;
        int cost = DssSkillCosts.costFor(aqtweaks$registryName());
        if (cost <= 0) return;
        if (!Reflect.hasEnoughStamina(player, cost)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "trigger", at = @At("RETURN"))
    private void aqtweaks$spendDssStamina(World world, EntityPlayer player, boolean wasTriggered,
                                         CallbackInfoReturnable<Boolean> cir) {
        if (!aqtweaks$shouldHandle(world, player)) return;
        if (cir.getReturnValue() == null || !cir.getReturnValue()) return;
        int cost = DssSkillCosts.costFor(aqtweaks$registryName());
        if (cost <= 0) return;
        if (player instanceof EntityPlayerMP) {
            FeathersHelper.decreaseFeathers((EntityPlayerMP) player, cost);
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
        Reflect.addExhaustion(player, exhaustion);
    }

    @Unique
    private boolean aqtweaks$shouldHandle(World world, EntityPlayer player) {
        if (!ArcanaQuestTweaksConfig.StaminaModuleConfig.dynamicSwordSkills.enableSkillCost) return false;
        if (world == null || player == null) return false;
        if (Reflect.isRemote(player) || Reflect.isCreative(player) || Reflect.isSpectator(player)) return false;
        return true;
    }

    @Unique
    private String aqtweaks$registryName() {
        try {
            Object name = this.getClass().getMethod("getRegistryName").invoke(this);
            if (name instanceof ResourceLocation) return name.toString();
            if (name != null) return name.toString();
        } catch (Exception ignored) {}
        return null;
    }
}
