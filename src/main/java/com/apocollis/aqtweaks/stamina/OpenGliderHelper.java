package com.apocollis.aqtweaks.stamina;

import gr8pefish.openglider.api.helper.GliderHelper;
import gr8pefish.openglider.common.network.PacketClientGliding;
import gr8pefish.openglider.common.network.PacketHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.Loader;

/**
 * Compile-hard Open Glider access (formerly reflection in {@code Reflect}). No-ops when the mod is
 * absent; the guard keeps the Open Glider API classes from loading.
 */
public final class OpenGliderHelper {

    private static final boolean LOADED = Loader.isModLoaded("openglider");

    private OpenGliderHelper() {}

    /** Deployed glider in the air (not on ground, water or lava). */
    public static boolean isGliding(EntityPlayer player) {
        if (!LOADED || player == null) return false;
        return GliderHelper.getIsGliderDeployed(player)
                && !player.onGround && !player.isInWater() && !player.isInLava();
    }

    /**
     * Retract the glider. The API setters only change the server-side capability, but Open Glider
     * applies gliding movement on the client from its own copy of the flag. Without telling the
     * client the player kept gliding while the server (flag already false) stopped billing and let
     * the feathers regenerate. {@code PacketClientGliding(false)} is the packet Open Glider itself
     * uses to push the deploy state to a client.
     */
    public static void undeploy(EntityPlayer player) {
        if (!LOADED || player == null) return;
        GliderHelper.setIsGliderDeployed(player, false);
        GliderHelper.setIsPlayerGliding(player, false);
        if (player instanceof EntityPlayerMP) {
            PacketHandler.HANDLER.sendTo(new PacketClientGliding(false), (EntityPlayerMP) player);
        }
    }
}
