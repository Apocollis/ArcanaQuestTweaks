package com.apocollis.aqtweaks.mixin.comfort;

import com.apocollis.aqtweaks.comfort.ComfortTooltips;
import com.google.common.collect.Ordering;
import java.util.List;
import net.minecraft.client.renderer.InventoryEffectRenderer;
import net.minecraft.potion.PotionEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Puts Rested, then Hearthside, at the top of the inventory effect column so their hover boxes have a
 * fixed corner. Vanilla sorts the effects after reading them, so the order is fixed on the sorted list.
 */
@Mixin(InventoryEffectRenderer.class)
public abstract class MixinInventoryEffectRenderer {

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Redirect(
            method = "drawActivePotionEffects",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/google/common/collect/Ordering;sortedCopy(Ljava/lang/Iterable;)Ljava/util/List;"))
    private List aqtweaks$pinRested(Ordering ordering, Iterable effects) {
        return ComfortTooltips.pinned(ordering.sortedCopy(effects));
    }
}
