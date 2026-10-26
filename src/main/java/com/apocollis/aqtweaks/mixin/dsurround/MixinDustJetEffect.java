package com.apocollis.aqtweaks.mixin.dsurround;

import com.teammetallurgy.atum.world.WorldProviderAtum;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.orecruncher.dsurround.client.fx.DustJetEffect;
import org.orecruncher.lib.chunk.IBlockAccessEx;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Random;

@Mixin(value = DustJetEffect.class, remap = false)
public abstract class MixinDustJetEffect {

    @Inject(method = "canTrigger", at = @At("HEAD"), cancellable = true, remap = false)
    private void aqtweaks$suppressAtumDustJet(IBlockAccessEx world, IBlockState state, BlockPos pos, Random rand, CallbackInfoReturnable<Boolean> cir) {
        if (world != null) {
            World mcWorld = world.getWorld();
            if (mcWorld != null && mcWorld.provider != null) {
                if (mcWorld.provider instanceof WorldProviderAtum || mcWorld.provider.getDimension() == 17) {
                    cir.setReturnValue(false);
                }
            }
        }
    }
}
