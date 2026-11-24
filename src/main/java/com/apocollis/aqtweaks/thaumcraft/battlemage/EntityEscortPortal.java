package com.apocollis.aqtweaks.thaumcraft.battlemage;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import thaumcraft.common.entities.monster.cult.EntityCultistPortalLesser;
import thaumcraft.common.lib.SoundsTC;

/**
 * The cosmetic portal the Swarm Mage's escort drops out of (plan 18.7). It is a Thaumcraft Lesser Cultist Portal that does
 * nothing: {@link #onUpdate()} never calls the stock logic (which spawns cultists on a timer; Crimson Revelations mixes a
 * replacement spawner into that same method, so skipping {@code super.onUpdate()} bypasses both), it cannot be hit, collided
 * with, pushed, healed or saved, has no loot, and removes itself after {@link #lifeTicks} with a portal puff. It is always
 * "active" so the stock renderer draws the open portal.
 */
public class EntityEscortPortal extends EntityCultistPortalLesser {

    private int lifeTicks = EscortSpawner.PORTAL_LIFE_TICKS;

    public EntityEscortPortal(World world) {
        super(world);
        this.setEntityInvulnerable(true);
        this.setNoGravity(true);
        this.experienceValue = 0;
        this.setActive(true);
    }

    /** Overrides the remaining life (a portal re-opened after a reload mid-arrival). */
    public void setLifeTicks(int ticks) {
        this.lifeTicks = Math.max(1, ticks);
    }

    /** Pulses the portal (the stock renderer reads {@code pulse}, which the client sets from status 16). */
    public void pulse() {
        if (!this.world.isRemote) {
            this.world.setEntityState(this, (byte) 16);
        }
    }

    @Override
    public void onUpdate() {
        // No super call: the stock onUpdate counts up to spawnMinions(). We only keep the renderer's counters moving.
        this.ticksExisted++;
        this.activeCounter++;
        if (this.pulse > 0) {
            this.pulse--;
        }
        if (this.world.isRemote) {
            return;
        }
        if (--lifeTicks <= 0) {
            close();
        }
    }

    private void close() {
        if (this.world instanceof WorldServer) {
            ((WorldServer) this.world).spawnParticle(EnumParticleTypes.PORTAL, this.posX, this.posY + 1.5D, this.posZ, 60,
                    0.5D, 1.0D, 0.5D, 0.2D);
        }
        this.world.playSound(null, this.posX, this.posY, this.posZ, SoundsTC.hhon, SoundCategory.HOSTILE, 0.6F, 0.6F);
        this.setDead();
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        // Void and /kill still work so a stray portal can be removed.
        return source == DamageSource.OUT_OF_WORLD && super.attackEntityFrom(source, amount);
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean canBePushed() {
        return false;
    }

    @Override
    public void onCollideWithPlayer(EntityPlayer player) {
        // The stock portal hurts players that touch it; this one is only a picture.
    }

    @Override
    protected boolean canDespawn() {
        return false;
    }

    @Override
    public boolean writeToNBTOptional(NBTTagCompound compound) {
        return false;
    }
}
