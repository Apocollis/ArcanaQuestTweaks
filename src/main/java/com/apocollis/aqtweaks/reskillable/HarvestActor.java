package com.apocollis.aqtweaks.reskillable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.world.BlockEvent;

/**
 * Who a harvest belongs to. {@code BlockEvent.HarvestDropsEvent.getHarvester()} is null for blocks
 * broken with {@code World.destroyBlock(pos, true)}, which is how Tree Chopper fells a tree, so
 * every harvest perk (Lumberjack, Reforester, ...) used to be skipped for felled trees. While a tree
 * is being felled, {@code MixinTreeHandler} records the felling player here and the perk code falls
 * back to it. Thread-local: world code runs on the server thread, and the actor is cleared when
 * the felling call returns (and at the end of every server tick as a safety net).
 */
public final class HarvestActor {

    private static final ThreadLocal<EntityPlayer> FELLER = new ThreadLocal<>();
    /** Saplings granted by Reforester during the current fell. */
    private static final ThreadLocal<int[]> SAPLINGS = ThreadLocal.withInitial(() -> new int[1]);

    private HarvestActor() {}

    public static void beginFell(EntityPlayer player) {
        FELLER.set(player);
        SAPLINGS.get()[0] = 0;
    }

    public static void endFell() {
        FELLER.remove();
        SAPLINGS.get()[0] = 0;
    }

    public static boolean felling() {
        return FELLER.get() != null;
    }

    /** The harvesting player: the event's harvester, else the player felling a tree right now. */
    public static EntityPlayer of(BlockEvent.HarvestDropsEvent event) {
        EntityPlayer harvester = event.getHarvester();
        if (harvester != null) return harvester;
        EntityPlayer feller = FELLER.get();
        if (feller != null && event.getWorld() == feller.world) return feller;
        return null;
    }

    /**
     * Reserve one Reforester sapling for the current fell. Always true outside a fell. Inside one it
     * is true until {@code cap} saplings have been granted (cap 0 or less = unlimited).
     */
    public static boolean takeSapling(int cap) {
        if (FELLER.get() == null || cap <= 0) return true;
        int[] count = SAPLINGS.get();
        if (count[0] >= cap) return false;
        count[0]++;
        return true;
    }
}
