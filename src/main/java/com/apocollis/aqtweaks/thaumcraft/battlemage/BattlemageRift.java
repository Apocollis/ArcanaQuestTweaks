package com.apocollis.aqtweaks.thaumcraft.battlemage;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig.BattlemageConfig;
import com.apocollis.aqtweaks.potion.Stun;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.api.casters.FocusEngine;
import thaumcraft.common.items.casters.foci.FocusEffectRift;
import thaumcraft.common.lib.network.PacketHandler;
import thaumcraft.common.lib.network.fx.PacketFXFocusPartImpact;
import thaumcraft.common.lib.utils.BlockUtils;

/**
 * The Battlemage's faked Rift (plan 3.14): the AI picks the wall and the exit with {@link RiftPicker}, opens the real
 * temporary Thaumcraft holes along the tunnel (feet and head level, through {@link FocusEffectRift#createHole}), and
 * teleports the mage through in the same tick, so it is never mid-tunnel and cannot be stopped inside it. A player that
 * follows can be left inside solid blocks when the holes close: the intended risk.
 *
 * <p>The Rift has its own cooldown ({@code riftCooldownMinTicks} to {@code riftCooldownMaxTicks}, rolled per use), independent
 * of the Blink cooldown in both directions. The stuck-in-a-wall unstick never uses or starts it.
 */
public final class BattlemageRift {

    /** The Rift focus's own particle key. */
    public static final String RIFT_FX_KEY = "thaumcraft.RIFT";
    /** Retry delay after a rift that found no wall or failed, in ticks. */
    private static final int FAILED_RETRY_TICKS = 40;

    private BattlemageRift() {}

    /** True when this mage may rift now: switches on, the focus carried, cooldown over, not stunned. */
    public static boolean ready(EntityCultistBattlemage mage, AiTuning ai) {
        return BattlemageConfig.riftEnabled && ai.riftEnabled && mage.indexOfFocus("rift") >= 0
                && mage.ticksExisted >= mage.getRiftReadyAt() && !Stun.isStunned(mage) && mage.isEntityAlive();
    }

    /** Escape through a wall to a safe pocket out of the target's sight. @return true when it teleported */
    public static boolean riftAway(EntityCultistBattlemage mage, EntityLivingBase target, AiTuning ai) {
        return rift(mage, target, ai, RiftPicker.Mode.DEFENSIVE);
    }

    /** Pursue through a wall to the target's side, landing inside the firing band. @return true when it teleported */
    public static boolean riftToward(EntityCultistBattlemage mage, EntityLivingBase target, AiTuning ai) {
        return rift(mage, target, ai, RiftPicker.Mode.OFFENSIVE);
    }

    private static boolean rift(EntityCultistBattlemage mage, EntityLivingBase target, AiTuning ai, RiftPicker.Mode mode) {
        if (mage.world.isRemote || !ready(mage, ai)) {
            return false;
        }
        World w = mage.world;
        RiftPicker.Request req = new RiftPicker.Request();
        req.mode = mode;
        req.fromX = (int) Math.floor(mage.posX);
        req.fromY = (int) Math.floor(mage.posY + 0.01D);
        req.fromZ = (int) Math.floor(mage.posZ);
        req.targetX = target.posX;
        req.targetZ = target.posZ;
        req.maxThickness = ai.riftMaxThickness;
        req.maxHoles = ai.riftMaxHolesPerCast;
        req.bandMax = Math.max(4.0D, ai.idealMax);
        RiftPicker.Plan plan = RiftPicker.pick(req, new WorldProbe(mage, target), mage.getRNG());
        if (plan == null) {
            mage.setRiftReadyAt(mage.ticksExisted + FAILED_RETRY_TICKS);
            return false;
        }
        if (BattlemageConfig.allowRealHoles && !openHoles(w, plan, ai)) {
            mage.setRiftReadyAt(mage.ticksExisted + FAILED_RETRY_TICKS);
            return false;
        }
        cue(w, mage.posX, mage.posY, mage.posZ);
        if (!BattlemageBlink.teleport(mage, plan.exitX + 0.5D, plan.exitY, plan.exitZ + 0.5D)) {
            mage.setRiftReadyAt(mage.ticksExisted + FAILED_RETRY_TICKS);
            return false;
        }
        cue(w, mage.posX, mage.posY, mage.posZ);
        int span = Math.max(0, ai.riftCooldownMaxTicks - ai.riftCooldownMinTicks);
        mage.setRiftReadyAt(mage.ticksExisted + ai.riftCooldownMinTicks + (span > 0 ? mage.getRNG().nextInt(span + 1) : 0));
        return true;
    }

    /** Opens every cell of the tunnel; true when all of them opened. */
    private static boolean openHoles(World w, RiftPicker.Plan plan, AiTuning ai) {
        EnumFacing facing = EnumFacing.getFacingFromVector((float) plan.dirX, 0.0F, (float) plan.dirZ);
        boolean all = true;
        for (int[] h : plan.holes) {
            if (!FocusEffectRift.createHole(w, new BlockPos(h[0], h[1], h[2]), facing, (byte) 0, ai.riftHoleDurationTicks)) {
                all = false;
            }
        }
        return all;
    }

    /** The Rift's particles by key, skipped silently when the focus is not registered. */
    private static void cue(World w, double x, double y, double z) {
        if (!(w instanceof WorldServer) || FocusEngine.getElement(RIFT_FX_KEY) == null) {
            return;
        }
        PacketHandler.INSTANCE.sendToAllAround(new PacketFXFocusPartImpact(x, y + 0.9D, z, new String[] {RIFT_FX_KEY}),
                new NetworkRegistry.TargetPoint(w.provider.getDimension(), x, y, z, 64.0D));
    }

    /** The picker's world view. */
    private static final class WorldProbe implements RiftPicker.Probe {
        private final EntityCultistBattlemage mage;
        private final EntityLivingBase target;
        private final World w;

        WorldProbe(EntityCultistBattlemage mage, EntityLivingBase target) {
            this.mage = mage;
            this.target = target;
            this.w = mage.world;
        }

        @Override
        public boolean open(int x, int y, int z) {
            BlockPos pos = new BlockPos(x, y, z);
            if (!w.isBlockLoaded(pos)) {
                return false;
            }
            IBlockState s = w.getBlockState(pos);
            if (s.getBlock() == BlocksTC.hole) {
                return true;
            }
            return s.getCollisionBoundingBox(w, pos) == null && !s.getMaterial().isLiquid();
        }

        @Override
        public boolean riftable(int x, int y, int z) {
            BlockPos pos = new BlockPos(x, y, z);
            if (!w.isBlockLoaded(pos)) {
                return false;
            }
            IBlockState s = w.getBlockState(pos);
            return !s.getBlock().isAir(s, w, pos) && w.getTileEntity(pos) == null && !BlockUtils.isPortableHoleBlackListed(s)
                    && s.getBlock() != Blocks.BEDROCK && s.getBlock() != BlocksTC.hole && s.getBlockHardness(w, pos) != -1.0F;
        }

        @Override
        public boolean standable(int x, int y, int z) {
            return BattlemageBlink.standingSpot(mage, x, y, z) != null;
        }

        @Override
        public boolean seenByTarget(int x, int y, int z) {
            Vec3d from = new Vec3d(x + 0.5D, y + 1.6D, z + 0.5D);
            Vec3d to = new Vec3d(target.posX, target.posY + target.getEyeHeight(), target.posZ);
            return w.rayTraceBlocks(from, to, false, true, false) == null;
        }
    }

}
