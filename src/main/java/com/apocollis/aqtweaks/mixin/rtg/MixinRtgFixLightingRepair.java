package com.apocollis.aqtweaks.mixin.rtg;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.perf.StageTimer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import rtg.event.EventHandlerCommon;

/**
 * Replaces RTG's wool place-and-remove skylight repair inside {@code EventHandlerCommon.fixLighting(Chunk, int, int)}
 * with a single direct {@link World#checkLightFor(EnumSkyBlock, BlockPos)} call when {@code RTG Light Fix Wool} is
 * false (default is true: original wool behaviour). Avoids a second light flood, block state changes, and neighbor
 * block notifications. No block-change packet is sent in direct mode, so clients holding the chunk may keep stale
 * light until the planned client update is added.
 */
@Mixin(value = EventHandlerCommon.class, remap = false)
public abstract class MixinRtgFixLightingRepair {

    @Redirect(method = "fixLighting(Lnet/minecraft/world/chunk/Chunk;II)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/World;func_180501_a(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;I)Z"))
    private static boolean aqtweaks$directCheckLightOrWool(World world, BlockPos pos, IBlockState state, int flags) {
        StageTimer.recordNs("rtg.woolRelight", 0L, null);
        if (ArcanaQuestTweaksConfig.RtgModuleConfig.surface.rtgLightFixWool) {
            return world.setBlockState(pos, state, flags);
        }
        return world.checkLightFor(EnumSkyBlock.SKY, pos);
    }

    @Redirect(method = "fixLighting(Lnet/minecraft/world/chunk/Chunk;II)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/World;func_175698_g(Lnet/minecraft/util/math/BlockPos;)Z"))
    private static boolean aqtweaks$skipWoolSetAir(World world, BlockPos pos) {
        if (ArcanaQuestTweaksConfig.RtgModuleConfig.surface.rtgLightFixWool) {
            return world.setBlockToAir(pos);
        }
        return false;
    }
}
