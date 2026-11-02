package com.apocollis.aqtweaks.mixin.mdecore;

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.management.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fixes /mde tpx cross-dimension teleport crash by delegating dimension transfer
 * to standard Forge Entity.changeDimension instead of mdecore's broken manual packet pipeline.
 * Also cancels the spawn move when entering the Nether so the player is not displaced into bedrock.
 */
@Mixin(targets = "com.mattdahepic.mdecore.helpers.TeleportHelper", remap = false)
public abstract class MixinTeleportHelper {

    /**
     * @author ArcanaQuestTweaks
     * @reason Replace broken manual packet/respawn transfer with standard Forge changeDimension
     */
    @Overwrite
    public static void transferPlayerToDimension(EntityPlayerMP player, int dimension, PlayerList playerList) {
        if (player == null || player.world == null || player.world.isRemote) {
            return;
        }
        if (player.dimension != dimension) {
            player.changeDimension(dimension);
        }
    }

    @Inject(method = "sendPlayerToSpawnInCurrentWorld", at = @At("HEAD"), cancellable = true, remap = false)
    private static void aqtweaks$cancelNetherSpawnMove(ICommandSender sender, CallbackInfo ci) {
        if (sender instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) sender;
            if (player.dimension == -1) {
                // In the Nether, changeDimension places the player safely at a portal.
                // Do not move to Nether spawn (0, 0, 0) which drops player inside bedrock.
                ci.cancel();
            }
        }
    }
}
