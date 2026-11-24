package com.apocollis.aqtweaks.thaumcraft.battlemage;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.AxisAlignedBB;
import thaumcraft.common.entities.monster.EntitySpellBat;
import thaumcraft.common.entities.projectile.EntityFocusCloud;

import java.util.List;

/**
 * Counts the Spell Bats and Focus Clouds a Battlemage owns (plan 3.12). Thaumcraft keeps no per-owner registry, but
 * {@code EntitySpellBat.getOwner()} and {@code EntityFocusCloud.getOwner()} are public, so a read-only AABB query
 * around the mage is enough. Refreshed every {@code upkeepIntervalTicks}, and at once after a summon is cast.
 */
public final class OwnedSummonTracker {

    private int bats;
    private int clouds;
    private long nextScan;

    public int bats() {
        return bats;
    }

    public int clouds() {
        return clouds;
    }

    /** Forces the next {@link #tick} to rescan (called right after a summon or cloud cast). */
    public void invalidate() {
        nextScan = 0L;
    }

    public void tick(EntityLivingBase mage, long now, int intervalTicks, double radius) {
        if (now < nextScan) {
            return;
        }
        nextScan = now + Math.max(1, intervalTicks);
        AxisAlignedBB box = mage.getEntityBoundingBox().grow(radius);
        int b = 0;
        List<EntitySpellBat> batList = mage.world.getEntitiesWithinAABB(EntitySpellBat.class, box);
        for (EntitySpellBat bat : batList) {
            if (!bat.isDead && bat.getOwner() == mage) {
                b++;
            }
        }
        int c = 0;
        List<EntityFocusCloud> cloudList = mage.world.getEntitiesWithinAABB(EntityFocusCloud.class, box);
        for (EntityFocusCloud cloud : cloudList) {
            if (!cloud.isDead && cloud.getOwner() == mage) {
                c++;
            }
        }
        bats = b;
        clouds = c;
    }
}
