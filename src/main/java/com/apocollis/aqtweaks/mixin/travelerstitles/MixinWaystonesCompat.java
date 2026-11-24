package com.apocollis.aqtweaks.mixin.travelerstitles;

import com.apocollis.aqtweaks.titles.TravelersTitlesStructureHook;
import com.yungnickyoung.minecraft.travelerstitles.compat.WaystonesCompat;
import com.yungnickyoung.minecraft.travelerstitles.render.TitleRenderer;
import net.blay09.mods.waystones.util.WaystoneEntry;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A player home's title covers the Waystones title instead of only delaying it. Traveler's Titles shows a
 * Waystones title whenever the closest named waystone is not in its recent entries, and an entry is added only
 * after a title displays, so cancelling the update just moved the title to when the home title faded. While a
 * home title shows, the closest waystone is marked as seen instead: no title, no sound, now or afterwards.
 * Leaving range and coming back later behaves as normal. {@code require = 0}: a different build is a no-op.
 */
@Mixin(value = WaystonesCompat.class, remap = false)
public abstract class MixinWaystonesCompat {

    @Shadow
    private static WaystoneEntry closestWaystone;

    @Shadow
    @Final
    private static TitleRenderer<WaystoneEntry> waystoneTitleRenderer;

    @Inject(method = "updateWaystoneTitle", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void aqtweaks$homeCoversWaystone(EntityPlayer player, CallbackInfoReturnable<Boolean> cir) {
        if (!TravelersTitlesStructureHook.isHomeActive()) {
            return;
        }
        WaystoneEntry waystone = closestWaystone;
        if (waystone != null && waystoneTitleRenderer != null) {
            waystoneTitleRenderer.addRecentEntry(waystone);
        }
        cir.setReturnValue(false);
    }
}
