package com.apocollis.aqtweaks.mixin.travelerstitles;

import com.apocollis.aqtweaks.titles.TravelersTitlesStructureHook;
import com.yungnickyoung.minecraft.travelerstitles.render.TitleRenderManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * While a structure title is on screen, Traveler's Titles does not start a biome title: a structure title
 * overrides it. Its private update method is stopped at the head; the title already showing is cleared when the
 * structure title starts ({@link TravelersTitlesStructureHook}). The Waystones title is covered by player homes
 * only, in {@link MixinWaystonesCompat}. {@code require = 0}: if another Traveler's Titles build renames the
 * method, nothing is injected and the hook still clears the biome title each tick.
 */
@Mixin(value = TitleRenderManager.class, remap = false)
public abstract class MixinTitleRenderManagerStructure {

    @Inject(method = "updateBiomeTitle", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void aqtweaks$structureOverridesBiome(World world, BlockPos pos, EntityPlayer player, boolean flag,
            CallbackInfo ci) {
        if (TravelersTitlesStructureHook.isActive()) {
            ci.cancel();
        }
    }
}
