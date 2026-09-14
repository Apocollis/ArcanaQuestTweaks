package com.apocollis.aqtweaks.spawning;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig.SpawningModuleConfig;
import net.minecraft.world.World;

/**
 * Cage {@code MobSpawnerBaseLogic}: short delay after a failed spawn attempt only.
 */
public final class CageSpawnerHooks {

    private static final ThreadLocal<Boolean> STARTED_AT_ZERO = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private CageSpawnerHooks() {}

    public static void onUpdateHead(int spawnDelay) {
        STARTED_AT_ZERO.set(spawnDelay == 0);
    }

    /**
     * @return ticks to assign to {@code spawnDelay}, or {@code -1} if this tick is not a failed attempt
     */
    public static int failRecheckDelayOrSkip(int spawnDelayNow, World world) {
        try {
            if (!SpawnGroupSizes.moduleEnabled() || world == null || world.isRemote) {
                return -1;
            }
            if (!Boolean.TRUE.equals(STARTED_AT_ZERO.get()) || spawnDelayNow != 0) {
                return -1;
            }
            int delay = SpawningModuleConfig.general.cageFailRecheckDelay;
            return Math.max(1, Math.min(200, delay));
        } finally {
            STARTED_AT_ZERO.remove();
        }
    }
}
