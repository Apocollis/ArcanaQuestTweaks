package com.apocollis.aqtweaks.mixin.thaumcraft;

import com.apocollis.aqtweaks.thaumcraft.ThaumcraftPerkHooks;
import baubles.api.cap.IBaublesItemHandler;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thaumcraft.common.items.casters.CasterManager;

@Mixin(value = CasterManager.class, remap = false)
public abstract class MixinCasterManager {

    /**
     * Stock encodes bauble pouches as {@code slot - 4} so only slots 0–3 stay negative.
     * BaublesEX extra slots encode ≥ 0 and are read as {@code mainInventory}. Use 100 so
     * bauble ids stay negative; do not touch {@code getTotalVisDiscount}'s armor {@code 4}.
     */
    @ModifyConstant(
            method = {"changeFocus", "fetchFocusFromPouch", "addFocusToPouch"},
            constant = @Constant(intValue = 4)
    )
    private static int aqtweaks$baublePouchOffset(int original) {
        return 100;
    }

    /**
     * BaublesEX {@code setChanged} is a no-op. A later transform reads missing field {@code player}
     * and throws after the focus was already removed from the pouch, so the gauntlet never receives it.
     */
    @Redirect(
            method = {"fetchFocusFromPouch", "addFocusToPouch"},
            at = @At(
                    value = "INVOKE",
                    target = "Lbaubles/api/cap/IBaublesItemHandler;setChanged(IZ)V"
            )
    )
    private static void aqtweaks$skipBaubleSetChanged(IBaublesItemHandler handler, int slot, boolean changed) {
    }

    @Inject(method = "getTotalVisDiscount", at = @At("RETURN"), cancellable = true)
    private static void aqtweaks$visThrift(EntityPlayer player, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(ThaumcraftPerkHooks.visDiscount(player, cir.getReturnValueF()));
    }
}
