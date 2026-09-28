package com.apocollis.aqtweaks.mixin.elenaidodge;

import com.elenai.elenaidodge2.util.ClientStorage;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Stock {@code getWeight} appends to a static {@code ArrayList} and clears it while iterating.
 * A nested tooltip then throws {@code ConcurrentModificationException}. This reads a local split instead.
 * Construct's Armory is not in this pack, so that branch is omitted.
 */
@Mixin(targets = "com.elenai.elenaidodge2.event.TooltipEventListener", remap = false)
public class MixinTooltipEventListener {

    @Inject(method = "getWeight", at = @At("HEAD"), cancellable = true, remap = false)
    private static void aqtweaks$localWeight(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        String values = ClientStorage.weightValues;
        if (values == null) {
            cir.setReturnValue(1);
            return;
        }
        Item item = stack.getItem();
        for (String entry : values.split(",")) {
            int eq = entry.indexOf('=');
            if (eq <= 0 || eq >= entry.length() - 1) {
                continue;
            }
            if (item == Item.getByNameOrId(entry.substring(0, eq))) {
                cir.setReturnValue((int) Math.floor(Double.parseDouble(entry.substring(eq + 1))));
                return;
            }
        }
        ItemArmor armor = (ItemArmor) item;
        cir.setReturnValue((int) Math.floor((armor.damageReduceAmount / 2) * 1.8D));
    }
}
