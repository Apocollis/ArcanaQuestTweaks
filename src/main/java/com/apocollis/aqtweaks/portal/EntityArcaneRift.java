package com.apocollis.aqtweaks.portal;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig.PortalModuleConfig;

import net.minecraft.entity.EntityTracker;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.ForgeChunkManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class EntityArcaneRift extends Entity {

    private static final DataParameter<Integer> REMAINING =
            EntityDataManager.createKey(EntityArcaneRift.class, DataSerializers.VARINT);
    private static final DataParameter<Boolean> WILD =
            EntityDataManager.createKey(EntityArcaneRift.class, DataSerializers.BOOLEAN);

    private UUID linkedId;
    private int linkedDim;
    private ForgeChunkManager.Ticket chunkTicket;
    private boolean collapsing;
    public int lastClientFxTick = -1;

    public EntityArcaneRift(World world) {
        super(world);
        setSize(1.6F, 2.4F);
        noClip = true;
        isImmuneToFire = true;
        ignoreFrustumCheck = true;
    }

    @Override
    protected void entityInit() {
        dataManager.register(REMAINING, PortalModuleConfig.general.lifespanTicks);
        dataManager.register(WILD, false);
    }

    public void setWild(boolean wild) {
        dataManager.set(WILD, wild);
    }

    public boolean isWild() {
        return dataManager.get(WILD);
    }

    public void setLinked(UUID id, int dim) {
        this.linkedId = id;
        this.linkedDim = dim;
    }

    public void setChunkTicket(ForgeChunkManager.Ticket ticket) {
        this.chunkTicket = ticket;
    }

    public ChunkPos getChunkPos() {
        return new ChunkPos(MathHelper.floor(posX) >> 4, MathHelper.floor(posZ) >> 4);
    }

    public int getRemainingTicks() {
        return dataManager.get(REMAINING);
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        RiftLighting.tick(this);
        int remaining = getRemainingTicks() - 1;
        if (!world.isRemote) {
            dataManager.set(REMAINING, remaining);
            if (remaining <= 0) {
                world.playSound(null, getPosition(), SoundEvents.BLOCK_PORTAL_TRIGGER, SoundCategory.BLOCKS, 0.4F, 0.5F);
                setDead();
                return;
            }
            if (remaining % 40 == 0) {
                world.playSound(null, getPosition(), SoundEvents.BLOCK_PORTAL_AMBIENT, SoundCategory.BLOCKS, 0.35F, 0.9F);
            }
            tryTeleportOverlapping();
        }
    }

    private void tryTeleportOverlapping() {
        EntityArcaneRift other = findLinked();
        if (other == null || other.isDead) {
            return;
        }
        List<Entity> list = world.getEntitiesWithinAABBExcludingEntity(this, getEntityBoundingBox());
        for (Entity entity : list) {
            tryTeleport(entity, other);
        }
    }

    private void tryTeleport(Entity entity, EntityArcaneRift dest) {
        if (entity.timeUntilPortal > 0 || entity.isDead) {
            return;
        }
        if (entity instanceof EntityArcaneRift) {
            return;
        }
        if (entity instanceof EntityItem) {
            moveToExit(entity, dest);
            return;
        }
        if (entity instanceof net.minecraft.entity.passive.EntityTameable tame && tame.isTamed() && tame.isSitting()) {
            return;
        }
        if (entity instanceof EntityPlayer player) {
            if (player.isRiding()) {
                Entity mount = player.getRidingEntity();
                player.dismountRidingEntity();
                List<Entity> companions = PortalModule.collectCompanions(player);
                Set<Entity> leashed = leashedAmong(companions);
                Entity newMount = moveToExit(mount, dest);
                Set<Entity> newLeashed = transferCompanions(companions, leashed, dest);
                Entity transferred = moveToExit(player, dest);
                if (transferred instanceof EntityPlayer destPlayer && newMount != null && !newMount.isDead) {
                    destPlayer.startRiding(newMount, true);
                    rebindLeashes(destPlayer, newLeashed);
                }
                return;
            }
            List<Entity> companions = PortalModule.collectCompanions(player);
            Set<Entity> leashed = leashedAmong(companions);
            Set<Entity> newLeashed = transferCompanions(companions, leashed, dest);
            Entity transferred = moveToExit(player, dest);
            if (transferred instanceof EntityPlayer destPlayer) {
                rebindLeashes(destPlayer, newLeashed);
            }
            return;
        }
        if (entity instanceof EntityLiving living && living.isBeingRidden()) {
            for (Entity passenger : new ArrayList<>(living.getPassengers())) {
                if (passenger instanceof EntityPlayer player) {
                    player.dismountRidingEntity();
                    List<Entity> companions = PortalModule.collectCompanions(player);
                    Set<Entity> leashed = leashedAmong(companions);
                    Entity newMount = moveToExit(living, dest);
                    Set<Entity> newLeashed = transferCompanions(companions, leashed, dest);
                    Entity transferred = moveToExit(player, dest);
                    if (transferred instanceof EntityPlayer destPlayer && newMount != null && !newMount.isDead) {
                        destPlayer.startRiding(newMount, true);
                        rebindLeashes(destPlayer, newLeashed);
                    }
                    return;
                }
            }
        }
        moveToExit(entity, dest);
    }

    private Set<Entity> transferCompanions(List<Entity> companions, Set<Entity> leashed, EntityArcaneRift dest) {
        Set<Entity> newLeashed = new HashSet<>();
        for (Entity companion : companions) {
            boolean wasLeashed = leashed.contains(companion);
            Entity transferred = moveToExit(companion, dest);
            if (wasLeashed && transferred != null && !transferred.isDead) {
                newLeashed.add(transferred);
            }
        }
        return newLeashed;
    }

    private static Set<Entity> leashedAmong(List<Entity> companions) {
        HashSet<Entity> set = new HashSet<>();
        for (Entity companion : companions) {
            if (companion instanceof EntityLiving living && living.getLeashed()) {
                set.add(companion);
            }
        }
        return set;
    }

    private void rebindLeashes(EntityPlayer player, Set<Entity> leashed) {
        for (Entity companion : leashed) {
            if (companion instanceof EntityLiving living) {
                living.setLeashHolder(player, true);
            }
        }
    }

    private Entity moveToExit(Entity entity, EntityArcaneRift dest) {
        if (entity == null || entity.isDead) {
            return entity;
        }
        Vec3d look = dest.getLookVec();
        double offset = PortalModuleConfig.general.exitOffset;
        double x = dest.posX + look.x * offset;
        double y = dest.posY;
        double z = dest.posZ + look.z * offset;
        float yaw = dest.rotationYaw;
        int destDim = dest.world.provider.getDimension();
        if (entity.world.provider.getDimension() == destDim) {
            if (entity instanceof EntityPlayerMP mp) {
                mp.connection.setPlayerLocation(x, y, z, yaw, entity.rotationPitch);
            } else {
                entity.setLocationAndAngles(x, y, z, yaw, entity.rotationPitch);
            }
            entity.timeUntilPortal = PortalModuleConfig.general.cooldownTicks;
            entity.fallDistance = 0.0F;
            entity.motionX = 0.0;
            entity.motionY = 0.0;
            entity.motionZ = 0.0;
            if (entity instanceof EntityPlayerMP mp) {
                resyncRiftForPlayer(mp, dest);
            }
            return entity;
        }
        Entity transferred = entity.changeDimension(destDim, new RiftTeleporter(x, y, z, yaw));
        Entity arrived = transferred != null ? transferred : entity;
        if (arrived instanceof EntityPlayerMP mp) {
            resyncRiftForPlayer(mp, dest);
        }
        return arrived;
    }

    private static void resyncRiftForPlayer(EntityPlayerMP player, EntityArcaneRift rift) {
        if (rift == null || rift.isDead || !(rift.world instanceof WorldServer ws)) {
            return;
        }
        MinecraftServer server = ws.getMinecraftServer();
        if (server == null) {
            return;
        }
        server.addScheduledTask(() -> {
            if (rift.isDead || player.isDead || player.world != rift.world) {
                return;
            }
            EntityTracker tracker = ws.getEntityTracker();
            tracker.untrack(rift);
            tracker.track(rift);
        });
    }

    private EntityArcaneRift findLinked() {
        if (linkedId == null || world.isRemote) {
            return null;
        }
        MinecraftServer server = world.getMinecraftServer();
        if (server == null) {
            return null;
        }
        WorldServer preferred = server.getWorld(linkedDim);
        if (preferred != null) {
            Entity found = preferred.getEntityFromUuid(linkedId);
            if (found instanceof EntityArcaneRift rift) {
                return rift;
            }
        }
        for (WorldServer other : server.worlds) {
            Entity found = other.getEntityFromUuid(linkedId);
            if (found instanceof EntityArcaneRift rift) {
                return rift;
            }
        }
        return null;
    }

    @Override
    public void setDead() {
        RiftLighting.remove(this);
        if (collapsing) {
            super.setDead();
            return;
        }
        collapsing = true;
        if (!world.isRemote) {
            PortalModule.releaseTicket(chunkTicket);
            chunkTicket = null;
            EntityArcaneRift other = findLinked();
            if (other != null && !other.isDead) {
                other.setDead();
            }
        }
        super.setDead();
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
    public boolean writeToNBTOptional(NBTTagCompound compound) {
        return false;
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound compound) {}

    @Override
    protected void writeEntityToNBT(NBTTagCompound compound) {}
}
