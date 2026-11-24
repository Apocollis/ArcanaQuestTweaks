package com.apocollis.aqtweaks.thaumcraft.battlemage;

import com.apocollis.aqtweaks.ArcanaQuestTweaks;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Never despawn (plan 21). Vanilla fires {@code AllowDespawn} for every non-persistent mob about every 32 ticks and a
 * DENY ends the check, so denying it is enough for a mage or an escort.
 *
 * <ul>
 * <li>The four battlemage entities are denied always (they are also persistent and {@code canDespawn()} is false).
 * <li>An escort (tag {@link EscortRelease#TAG}) is denied while its owner mage is alive or merely unloaded; once the owner's
 *     UUID is in {@link EscortReleasedData} (it died) the tag is removed and vanilla despawning applies again.
 * </ul>
 *
 * Escorts are deliberately not made persistent: vanilla's persistence flag is private and cannot be cleared again.
 * No Thaumcraft types are used here.
 */
@Mod.EventBusSubscriber(modid = ArcanaQuestTweaks.MODID)
public final class BattlemageDespawnGuard {

    private BattlemageDespawnGuard() {}

    @SubscribeEvent
    public static void onAllowDespawn(LivingSpawnEvent.AllowDespawn event) {
        EntityLivingBase living = event.getEntityLiving();
        if (living == null || living.world == null || living.world.isRemote) {
            return;
        }
        ResourceLocation key = EntityList.getKey(living);
        if (key != null && EscortRelease.isBattlemagePath(key.getNamespace(), key.getPath())) {
            event.setResult(Event.Result.DENY);
            return;
        }
        NBTTagCompound data = living.getEntityData();
        if (!data.hasKey(EscortRelease.TAG, 8)) {
            return;
        }
        EscortReleasedData released = EscortReleasedData.get(living.world);
        EscortRelease.Verdict verdict = EscortRelease.verdict(data.getString(EscortRelease.TAG),
                released != null ? released.released() : null);
        if (verdict == EscortRelease.Verdict.DENY) {
            event.setResult(Event.Result.DENY);
        } else if (verdict == EscortRelease.Verdict.RELEASE) {
            data.removeTag(EscortRelease.TAG);
        }
    }
}
