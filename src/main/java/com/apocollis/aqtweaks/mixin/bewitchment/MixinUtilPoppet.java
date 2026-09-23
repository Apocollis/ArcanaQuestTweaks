package com.apocollis.aqtweaks.mixin.bewitchment;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.reskillable.MagicSchoolPresence;
import com.bewitchment.Util;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Util.class, remap = false)
public abstract class MixinUtilPoppet {

    @Unique
    private static final ThreadLocal<Integer> AQTWEAKS$HITS = ThreadLocal.withInitial(() -> 0);

    @Inject(method = "attemptDamagePoppet", at = @At("HEAD"))
    private static void aqtweaks$resetHits(EntityLivingBase living, net.minecraft.item.Item item,
            CallbackInfoReturnable<Boolean> cir) {
        AQTWEAKS$HITS.set(0);
    }

    @Redirect(method = "attemptDamagePoppet",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/item/ItemStack;func_77972_a(ILnet/minecraft/entity/EntityLivingBase;)V",
                    remap = false))
    private static void aqtweaks$stitch(ItemStack stack, int amount, EntityLivingBase entity) {
        int hit = AQTWEAKS$HITS.get();
        AQTWEAKS$HITS.set(hit + 1);
        boolean stitch = entity instanceof EntityPlayer player
                && MagicSchoolPresence.unlocked(player, "aqtweaks:stitch",
                ArcanaQuestTweaksConfig.ReskillableModuleConfig.perks.stitch.enable);
        if (hit >= 1 && stitch) {
            return;
        }
        stack.damageItem(amount, entity);
    }
}
