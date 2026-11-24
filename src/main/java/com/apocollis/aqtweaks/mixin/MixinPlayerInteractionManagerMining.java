package com.apocollis.aqtweaks.mixin;

import com.apocollis.aqtweaks.stamina.MiningBilling;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.management.PlayerInteractionManager;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hold-based mining stamina (plan 20261120-1700): the server keeps a player's dig in {@code isDestroyingBlock} and
 * {@code destroyPos} from the moment the client starts hitting a block until it stops or finishes, and runs
 * {@code updateBlockRemoving} once per tick. Nothing else (no event, no packet) reports a hold in progress, so this
 * one cheap hook feeds {@link MiningBilling#onDigTick} while a dig is active.
 */
@Mixin(PlayerInteractionManager.class)
public abstract class MixinPlayerInteractionManagerMining {

    @Shadow
    private boolean isDestroyingBlock;

    @Shadow
    private BlockPos destroyPos;

    @Shadow
    public EntityPlayerMP player;

    @Inject(method = "updateBlockRemoving", at = @At("HEAD"))
    private void aqtweaks$digTick(CallbackInfo ci) {
        if (this.isDestroyingBlock && this.player != null) {
            MiningBilling.onDigTick(this.player, this.destroyPos);
        }
    }
}
