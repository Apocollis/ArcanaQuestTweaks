package com.apocollis.aqtweaks.mixin.comfort;

import com.apocollis.aqtweaks.comfort.PotionHomestead;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.client.gui.inventory.InventoryEffectRenderer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.PotionEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Puts Rested at the top of the inventory effect column so its hover box has a fixed corner. */
@Mixin(InventoryEffectRenderer.class)
public abstract class MixinInventoryEffectRenderer {

    @Redirect(
            method = "drawActivePotionEffects",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/EntityPlayer;getActivePotionEffects()Ljava/util/Collection;"))
    private Collection<PotionEffect> aqtweaks$restedFirst(EntityPlayer player) {
        List<PotionEffect> ordered = new ArrayList<>();
        PotionEffect rested = null;
        for (PotionEffect effect : player.getActivePotionEffects()) {
            if (effect.getPotion() == PotionHomestead.INSTANCE) {
                rested = effect;
            } else {
                ordered.add(effect);
            }
        }
        if (rested != null) {
            ordered.add(0, rested);
        }
        return ordered;
    }
}
