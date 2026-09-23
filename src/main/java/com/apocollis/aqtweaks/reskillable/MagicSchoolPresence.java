package com.apocollis.aqtweaks.reskillable;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.util.Reflect;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;

public final class MagicSchoolPresence {

    private MagicSchoolPresence() {}

    private static final ThreadLocal<EntityPlayer> ASTRAL_CRAFTER = new ThreadLocal<>();

    public static void pushAstralCrafter(EntityPlayer player) {
        ASTRAL_CRAFTER.set(player);
    }

    public static void popAstralCrafter() {
        ASTRAL_CRAFTER.remove();
    }

    public static EntityPlayer astralCrafter() {
        return ASTRAL_CRAFTER.get();
    }

    public static boolean unlocked(EntityPlayer player, String perkId, boolean enabled) {
        if (player == null || player instanceof FakePlayer || !enabled) {
            return false;
        }
        return Reflect.hasUnlockable(player, perkId);
    }

    public static boolean nearby(World world, BlockPos pos, double range, String perkId, boolean enabled) {
        if (world == null || world.isRemote || pos == null || !enabled) {
            return false;
        }
        double r2 = range * range;
        for (EntityPlayer player : world.playerEntities) {
            if (unlocked(player, perkId, true) && player.getDistanceSq(pos) <= r2) {
                return true;
            }
        }
        return false;
    }

    public static double thrift() {
        return ArcanaQuestTweaksConfig.ReskillableModuleConfig.magic.schoolThrift;
    }

    public static int scaleSpend(int amount, boolean apply) {
        if (!apply || amount <= 0) {
            return amount;
        }
        return Math.max(1, (int) Math.floor(amount * thrift()));
    }

    public static double scaleSpend(double amount, boolean apply) {
        if (!apply || amount <= 0) {
            return amount;
        }
        return amount * thrift();
    }
}
