package com.apocollis.aqtweaks.comfort;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.comfort.home.HomeArea;
import com.apocollis.aqtweaks.comfort.home.HomeRecord;
import com.apocollis.aqtweaks.comfort.home.HomeRegistry;
import com.apocollis.aqtweaks.reskillable.PerkAccess;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.util.FakePlayer;

/**
 * The Homesteader's Hearthkeeper perk (starter-class plan 13.6), server side. Two parts, both added to the Comfort
 * score and never bypassing the entry rules:
 * <ul>
 *   <li><b>Open Hearth</b> follows the Homesteader: other players within range get a flat bonus, and the
 *       Homesteader gets a diminishing bonus for the company.</li>
 *   <li><b>Home box</b> is anchored on the Homesteader's bed (the cached home area from {@link HomeRegistry}):
 *       anyone resting inside gets a share of the cozy score, and a longer-lasting Rested (Carry the Warmth). It
 *       works while the owner is offline because the home record carries the {@code hearthkeeper} flag.</li>
 * </ul>
 */
public final class HearthkeeperHooks {

    public static final String PERK_ID = "aqtweaks:hearthkeeper";

    private HearthkeeperHooks() {}

    private static ArcanaQuestTweaksConfig.ReskillableStarterHomePerks cfg() {
        return ArcanaQuestTweaksConfig.ReskillableModuleConfig.starterHome;
    }

    public static boolean keeper(EntityPlayer player) {
        return PerkAccess.on(player, PERK_ID, ArcanaQuestTweaksConfig.ReskillableModuleConfig.perks.hearthkeeper.enable);
    }

    /**
     * Keeps the home record's {@code hearthkeeper} flag in step with the owner's perk. Called from the comfort state
     * pass (every 40 ticks), which covers login, sleeping in the bed and the perk being granted.
     */
    public static void syncFlag(EntityPlayer player) {
        if (player == null || player instanceof FakePlayer) {
            return;
        }
        HomeRecord record = HomeRegistry.recordOf(player.getUniqueID());
        if (record == null) {
            return;
        }
        boolean has = keeper(player);
        if (record.hearthkeeper != has) {
            HomeRegistry.setHearthkeeper(player.getUniqueID(), has);
        }
    }

    /** The extra comfort score for {@code player} from Open Hearth and the home box. Zero when none applies. */
    public static float scoreBonus(EntityPlayer player, float cozyScore) {
        if (player == null || player instanceof FakePlayer || player.world == null || player.world.isRemote) {
            return 0.0f;
        }
        ArcanaQuestTweaksConfig.ReskillableStarterHomePerks c = cfg();
        double bonus = 0.0D;

        double range = Math.max(0.0D, c.openHearthRange);
        if (range > 0.0D) {
            boolean self = keeper(player);
            double rangeSq = range * range;
            int others = 0;
            boolean nearKeeper = false;
            for (EntityPlayer other : player.world.playerEntities) {
                if (other == player || other.isSpectator() || other.isDead || other.getDistanceSq(player) > rangeSq) {
                    continue;
                }
                others++;
                if (!nearKeeper && keeper(other)) {
                    nearKeeper = true;
                }
            }
            if (self) {
                bonus += HearthkeeperMath.companyBonus(others, c.keeperCurve, c.keeperTail, c.keeperCap);
            } else if (nearKeeper) {
                bonus += c.visitorBonus;
            }
        }

        if (c.homeBoxScoreBonus > 0.0D) {
            HomeArea home = HomeRegistry.areaAtPlayer(player);
            if (home != null && home.anyHearthkeeper()) {
                bonus += HearthkeeperMath.boxBonus(cozyScore, c.homeBoxScoreBonus);
            }
        }
        return (float) bonus;
    }

    /**
     * Carry the Warmth: the Rested duration refreshed while resting inside a Hearthkeeper's home is longer. The
     * duration is held at this value while the player rests and counts down once they stop, so the extra time shows
     * up as a longer lingering Rested. (Chosen over scaling the countdown: it needs no per-tick work.)
     */
    public static int restedDuration(EntityPlayer player, int baseTicks) {
        if (player == null || player instanceof FakePlayer || player.world == null || player.world.isRemote) {
            return baseTicks;
        }
        double extra = cfg().carryTheWarmthExtra;
        if (extra <= 0.0D) {
            return baseTicks;
        }
        HomeArea home = HomeRegistry.areaAtPlayer(player);
        if (home == null || !home.anyHearthkeeper()) {
            return baseTicks;
        }
        return HearthkeeperMath.lingerDuration(baseTicks, extra);
    }
}
