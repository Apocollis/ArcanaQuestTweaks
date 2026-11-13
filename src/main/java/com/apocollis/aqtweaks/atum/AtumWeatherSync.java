package com.apocollis.aqtweaks.atum;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.teammetallurgy.atum.network.NetworkHandler;
import com.teammetallurgy.atum.network.packet.PacketWeather;
import com.teammetallurgy.atum.world.WorldProviderAtum;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Keeps the client's Atum storm countdown in step with the server by sending Atum's own
 * {@code PacketWeather}. Atum sends that packet only when the storm state flips, never on join or dimension
 * change, so a client that joins mid-countdown would keep default fields and redraw a random countdown from its
 * own RNG. Sent only to players in the Atum dimension: on login there, on entering it, on respawning there,
 * and every {@code Warning Resync Seconds} while the server is not storming. Nothing is sent while vanilla
 * clean-weather time pins the server countdown.
 */
public class AtumWeatherSync {

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        send(event.player);
    }

    @SubscribeEvent
    public void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        send(event.player);
    }

    @SubscribeEvent
    public void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        send(event.player);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        int seconds = ArcanaQuestTweaksConfig.AtumModuleConfig.warningResyncSeconds;
        if (seconds <= 0) return;
        for (WorldServer world : DimensionManager.getWorlds()) {
            if (!(world.provider instanceof WorldProviderAtum)) continue;
            long interval = seconds * 20L;
            if (world.getTotalWorldTime() % interval != 0L) continue;
            WorldProviderAtum provider = (WorldProviderAtum) world.provider;
            if (provider.isStorming || cleanWeatherPinned(world)) continue;
            for (EntityPlayer player : world.playerEntities) {
                send(player);
            }
        }
    }

    private static boolean cleanWeatherPinned(World world) {
        return world.getWorldInfo().getCleanWeatherTime() > 0;
    }

    private static void send(EntityPlayer player) {
        if (!(player instanceof EntityPlayerMP)) return;
        World world = player.world;
        if (world == null || world.isRemote || !(world.provider instanceof WorldProviderAtum)) return;
        if (cleanWeatherPinned(world)) return;
        WorldProviderAtum provider = (WorldProviderAtum) world.provider;
        NetworkHandler.WRAPPER.sendTo(new PacketWeather(provider.isStorming, provider.stormTime), (EntityPlayerMP) player);
    }
}
