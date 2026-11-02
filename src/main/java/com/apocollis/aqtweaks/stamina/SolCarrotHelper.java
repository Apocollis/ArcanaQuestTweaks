package com.apocollis.aqtweaks.stamina;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.Loader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class SolCarrotHelper {

    /** Returned by {@link #milestonesAchieved} when SolCarrot's data could not be read (not the same as 0). */
    public static final int UNREADABLE = -1;

    private static final Logger LOGGER = LogManager.getLogger("AQTweaks");
    private static final boolean LOADED = Loader.isModLoaded("solcarrot");
    private static boolean unreadableLogged;

    private SolCarrotHelper() {}

    public static boolean isLoaded() {
        return LOADED;
    }

    /**
     * Retrieves the count of food milestones achieved by the player in Spice of Life: Carrot Edition.
     * Returns 0 if SolCarrot is not loaded or the player is null, and {@link #UNREADABLE} if SolCarrot is loaded but
     * its data cannot be read right now (an exception, or no food list). The first failure is logged once.
     */
    public static int milestonesAchieved(EntityPlayer player) {
        if (!LOADED || player == null) return 0;
        try {
            int count = Impl.milestonesAchieved(player);
            if (count == UNREADABLE) logUnreadable("no food list or progress info", null);
            return count;
        } catch (Throwable t) {
            logUnreadable("exception", t);
            return UNREADABLE;
        }
    }

    private static synchronized void logUnreadable(String why, Throwable t) {
        if (unreadableLogged) return;
        unreadableLogged = true;
        LOGGER.warn("SolCarrot milestones unreadable ({}); the stored count is used and no SolCarrot stamina bonus"
                + " is dropped by this read. Further failures are not logged.", why, t);
    }

    private static final class Impl {
        static int milestonesAchieved(EntityPlayer player) {
            com.cazsius.solcarrot.tracking.FoodList list = com.cazsius.solcarrot.tracking.FoodList.get(player);
            if (list == null) return UNREADABLE;
            com.cazsius.solcarrot.tracking.ProgressInfo info = list.getProgressInfo();
            return info != null ? info.milestonesAchieved() : UNREADABLE;
        }
    }
}
