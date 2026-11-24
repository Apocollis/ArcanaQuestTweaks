package com.apocollis.aqtweaks.comfort.home;

import net.minecraft.block.BlockBed;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.player.PlayerSetSpawnEvent;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ExplosionEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Forge event wiring for player homes: establish on sleeping or setting spawn at a bed, remove when the owner
 * sets spawn elsewhere, dirty marking on block changes, and the fill tick. Server only.
 */
public final class HomeEvents {

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        World world = event.getWorld();
        if (!world.isRemote && world.provider.getDimension() == 0) {
            HomeRegistry.load(world);
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        World world = event.getWorld();
        if (!world.isRemote && world.provider.getDimension() == 0) {
            HomeRegistry.clear();
        }
    }

    @SubscribeEvent
    public void onSetSpawn(PlayerSetSpawnEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        if (!valid(player)) {
            return;
        }
        BlockPos spawn = event.getNewSpawn();
        World world = player.world;
        if (spawn != null && world.isBlockLoaded(spawn) && world.getBlockState(spawn).getBlock() instanceof BlockBed) {
            HomeRegistry.establish(world, player, spawn);
        } else {
            // Spawn cleared or moved to something that is not a bed: no longer their home.
            HomeRegistry.removeOwner(player.getUniqueID());
        }
    }

    @SubscribeEvent
    public void onWake(PlayerWakeUpEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        if (!valid(player)) {
            return;
        }
        World world = player.world;
        BlockPos bed = player.getBedLocation(world.provider.getDimension());
        if (bed != null && world.isBlockLoaded(bed) && world.getBlockState(bed).getBlock() instanceof BlockBed) {
            HomeRegistry.establish(world, player, bed);
            HomeRegistry.refreshName(player);
        }
    }

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (valid(event.player)) {
            HomeRegistry.refreshName(event.player);
        }
    }

    @SubscribeEvent
    public void onPlace(BlockEvent.PlaceEvent event) {
        HomeRegistry.markDirty(event.getWorld(), event.getPos());
    }

    @SubscribeEvent
    public void onBreak(BlockEvent.BreakEvent event) {
        HomeRegistry.markDirty(event.getWorld(), event.getPos());
    }

    @SubscribeEvent
    public void onExplosion(ExplosionEvent.Detonate event) {
        World world = event.getWorld();
        for (BlockPos pos : event.getAffectedBlocks()) {
            HomeRegistry.markDirty(world, pos);
        }
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.world.isRemote) {
            HomeRegistry.tick(event.world);
        }
    }

    private static boolean valid(EntityPlayer player) {
        return player != null && !(player instanceof FakePlayer) && player.world != null && !player.world.isRemote;
    }
}
