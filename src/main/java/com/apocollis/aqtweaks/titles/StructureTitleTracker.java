package com.apocollis.aqtweaks.titles;

import com.apocollis.aqtweaks.ArcanaQuestTweaks;
import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig.StructureTitlesConfig;
import com.apocollis.aqtweaks.comfort.home.HomeRegistry;
import com.apocollis.aqtweaks.comfort.home.HomeStructure;
import com.apocollis.aqtweaks.network.PacketStructureTitle;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.util.Arrays;

/**
 * Server side of the structure titles. Every {@code Structure Titles Interval Ticks} (staggered per player) it
 * finds the structure the player stands in, first a registered player home, then each name through vanilla's
 * {@code isInsideStructure} (Roguelike Dungeons answers its own name through the same call), and sends one
 * packet when that changes. Only the cheap {@code isInsideStructure} test is used, never a nearest-structure
 * search, so nothing is generated or located.
 */
public final class StructureTitleTracker {

    private final StructureTitles.Tracker tracker = new StructureTitles.Tracker();

    /** The structure a player stands in: the packet key and the title text sent with it. */
    static final class Hit {
        final String key;
        final String title;

        Hit(String key, String title) {
            this.key = key;
            this.title = title;
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side != Side.SERVER || !StructureTitlesConfig.enabled) {
            return;
        }
        if (!(event.player instanceof EntityPlayerMP)) {
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) event.player;
        int interval = Math.max(5, StructureTitlesConfig.intervalTicks);
        if ((player.ticksExisted + player.getEntityId()) % interval != 0) {
            return;
        }
        World world = player.world;
        Hit hit = detect(world, player.getPosition());
        StructureTitles.Transition t = tracker.update(player.getUniqueID(), hit == null ? null : hit.key,
                world.getTotalWorldTime(), Math.max(0, StructureTitlesConfig.repeatCooldownTicks));
        if (t.kind == StructureTitles.Kind.ENTER && hit != null) {
            ArcanaQuestTweaks.NETWORK.sendTo(new PacketStructureTitle(hit.key, hit.title, "", true), player);
        } else if (t.kind == StructureTitles.Kind.EXIT) {
            ArcanaQuestTweaks.NETWORK.sendTo(new PacketStructureTitle(t.key, "", "", false), player);
        }
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        tracker.forget(event.player.getUniqueID());
    }

    static Hit detect(World world, BlockPos pos) {
        HomeStructure home = HomeRegistry.structureAt(world, pos);
        if (home != null) {
            return new Hit(StructureTitles.HOME_KEY, home.displayName);
        }
        if (!(world.getChunkProvider() instanceof ChunkProviderServer)) {
            return null;
        }
        ChunkProviderServer provider = (ChunkProviderServer) world.getChunkProvider();
        for (String name : StructureTitles.names(Arrays.asList(StructureTitlesConfig.extraNames))) {
            if (provider.isInsideStructure(world, name, pos)) {
                return new Hit(StructureTitles.keyOf(name), name);
            }
        }
        return null;
    }
}
