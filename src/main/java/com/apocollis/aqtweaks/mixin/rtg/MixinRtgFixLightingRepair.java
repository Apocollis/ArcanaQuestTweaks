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
 * with a single direct {@link World#checkLightFor(EnumSkyBlock, BlockPos)} call when {@code RTG Light Hack Fix} is
 * true (the default). Avoids a second light flood, block state changes, and neighbor block notifications. The direct
 * call sends no block-change packet, so a client that already holds the chunk may keep stale light until that chunk
 * is resent. False leaves RTG's wool place-and-remove in place.
 */
@Mixin(value = EventHandlerCommon.class, remap = false)
public abstract class MixinRtgFixLightingRepair {

    @Redirect(method = "fixLighting(Lnet/minecraft/world/chunk/Chunk;II)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/World;func_180501_a(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;I)Z"))
    private static boolean aqtweaks$directCheckLightOrWool(World world, BlockPos pos, IBlockState state, int flags) {
        StageTimer.recordNs("rtg.woolRelight", 0L, null);
        if (ArcanaQuestTweaksConfig.RtgModuleConfig.surface.rtgLightHackFix) {
            return world.checkLightFor(EnumSkyBlock.SKY, pos);
        }
        return world.setBlockState(pos, state, flags);
    }

    @Redirect(method = "fixLighting(Lnet/minecraft/world/chunk/Chunk;II)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/World;func_175698_g(Lnet/minecraft/util/math/BlockPos;)Z"))
    private static boolean aqtweaks$skipWoolSetAir(World world, BlockPos pos) {
        if (ArcanaQuestTweaksConfig.RtgModuleConfig.surface.rtgLightHackFix) {
            return false;
        }
        return world.setBlockToAir(pos);
    }
}
