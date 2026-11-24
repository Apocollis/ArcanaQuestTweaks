package com.apocollis.aqtweaks.thaumcraft.battlemage;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig.BattlemageConfig;
import com.apocollis.aqtweaks.potion.Stun;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.EnderTeleportEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import thaumcraft.api.casters.FocusEngine;
import thaumcraft.common.entities.monster.cult.EntityCultist;
import thaumcraft.common.entities.projectile.EntityFocusCloud;
import thaumcraft.common.entities.projectile.EntityFocusMine;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.lib.network.PacketHandler;
import thaumcraft.common.lib.network.fx.PacketFXFocusPartImpact;

import java.util.List;
import java.util.Random;

/**
 * The Battlemage's teleport (plan 3.10), done directly by the AI while reusing Thaumic Wonders' Blink cue: the same two
 * Thaumcraft sounds, the Blink particle burst (sent by key, so it is skipped silently when Thaumic Wonders is absent, with
 * a vanilla portal puff as the fallback), the Ender teleport sound, and Forge's {@link EnderTeleportEvent} so other mods can
 * cancel it. The destination comes from {@link BlinkPicker}; the world checks live here.
 *
 * <p>One cooldown bucket for every blink mode ({@link AiTuning#blinkCooldownTicks}), kept on the entity. This part builds
 * the {@code TOWARD_ALLY} mode used by the Swarm Mage. <b>Extension point for the Bombardier's reposition blink:</b> add a
 * method that builds a {@link BlinkPicker.Request} for the new mode (with its {@link BlinkPicker.Penalty} list) and calls
 * {@link #blinkTo(EntityCultistBattlemage, BlinkPicker.Request, AiTuning)}.
 */
public final class BattlemageBlink {

    /** Thaumic Wonders' Blink particle key (a string only: no compile dependency). */
    public static final String BLINK_FX_KEY = "focus.thaumicwonders.blink";
    /** At most this many valid spots are tried before giving up for a short while. */
    private static final int MAX_ATTEMPTS = 3;
    /** Retry delay after a failed blink, in ticks. */
    private static final int FAILED_RETRY_TICKS = 20;

    private BattlemageBlink() {}

    /** Blinks the mage to a spot 1 to 2 blocks (tuning) from {@code ally}. @return true when it teleported */
    public static boolean blinkToAlly(EntityCultistBattlemage mage, EntityLivingBase ally, AiTuning ai) {
        BlinkPicker.Request req = new BlinkPicker.Request();
        req.mode = BlinkPicker.Mode.TOWARD_ALLY;
        req.centerX = ally.posX;
        req.centerY = ally.posY;
        req.centerZ = ally.posZ;
        req.minRadius = ai.blinkAllyMinDistance;
        req.maxRadius = ai.blinkAllyMaxDistance;
        return blinkTo(mage, req, ai);
    }

    /**
     * Reposition blink (plan 19.8): lands on the volley band around {@code target} (default 8 to 14 blocks, never closer than
     * the mage's own area minimum range plus a block), with line of sight to the target preferred, never inside one of its
     * own clouds (hard reject) and out of enemy clouds and mines when it can (soft cost: if every spot is inside some enemy
     * cloud the least bad one is taken). @return true when it teleported
     */
    public static boolean blinkToBand(EntityCultistBattlemage mage, EntityLivingBase target, AiTuning ai) {
        BlinkPicker.Request req = new BlinkPicker.Request();
        req.mode = BlinkPicker.Mode.REPOSITION_BAND;
        req.centerX = target.posX;
        req.centerY = target.posY;
        req.centerZ = target.posZ;
        double min = Math.max(ai.blinkBandMinDistance, ai.aoeMinRange + 1.0D);
        req.minRadius = min;
        req.maxRadius = Math.max(min, ai.blinkBandMaxDistance);
        req.preferRadius = (req.minRadius + req.maxRadius) * 0.5D;
        req.penalties.add(cloudAvoidance(mage, target, ai));
        req.penalties.add(new LosPenalty(mage.world, target, LOS_COST, true));
        return blinkTo(mage, req, ai);
    }

    /**
     * Blink to a ring around the target (plan 3.10 point 3, used by every mage): the firing band after a melee burst or when
     * no firing line exists. Sight to the target is preferred, own clouds are never landed in, enemy clouds are avoided.
     * @return true when it teleported
     */
    public static boolean blinkToRange(EntityCultistBattlemage mage, EntityLivingBase target, AiTuning ai, double min, double max) {
        if (!BattlemageConfig.blinkEnabled) {
            return false;
        }
        BlinkPicker.Request req = new BlinkPicker.Request();
        req.mode = BlinkPicker.Mode.REPOSITION_BAND;
        req.centerX = target.posX;
        req.centerY = target.posY;
        req.centerZ = target.posZ;
        req.minRadius = min;
        req.maxRadius = Math.max(min, max);
        req.preferRadius = (req.minRadius + req.maxRadius) * 0.5D;
        req.penalties.add(cloudAvoidance(mage, target, ai));
        req.penalties.add(new LosPenalty(mage.world, target, LOS_COST, true));
        return blinkTo(mage, req, ai);
    }

    /**
     * The strict defensive blink (plan 3.10): lands {@code awayBandMin} to {@code awayBandMax} from the player, hidden from
     * it when it can be, out of clouds. The caller has already checked the four conditions. @return true when it teleported
     */
    public static boolean blinkAway(EntityCultistBattlemage mage, EntityLivingBase target, AiTuning ai) {
        if (!BattlemageConfig.blinkEnabled) {
            return false;
        }
        BlinkPicker.Request req = new BlinkPicker.Request();
        req.mode = BlinkPicker.Mode.AWAY;
        req.centerX = target.posX;
        req.centerY = target.posY;
        req.centerZ = target.posZ;
        req.minRadius = ai.awayBandMinDistance;
        req.maxRadius = Math.max(ai.awayBandMinDistance, ai.awayBandMaxDistance);
        req.preferRadius = (req.minRadius + req.maxRadius) * 0.5D;
        req.penalties.add(cloudAvoidance(mage, target, ai));
        req.penalties.add(new LosPenalty(mage.world, target, AWAY_EXPOSED_COST, false));
        return blinkTo(mage, req, ai);
    }

    /** Score cost of a landing spot with no line of sight to the target. */
    private static final double LOS_COST = 4.0D;
    /** Score cost of an escape spot the player can still see. */
    private static final double AWAY_EXPOSED_COST = 8.0D;

    /**
     * Builds the cloud zones around the target (the ring lies within the band of it): clouds this mage cast are own zones,
     * every other cloud whose owner is not a cultist, and every mine, is an enemy zone.
     */
    private static CloudAvoidance cloudAvoidance(EntityCultistBattlemage mage, EntityLivingBase target, AiTuning ai) {
        CloudAvoidance avoid = new CloudAvoidance(ai.cloudAvoidMargin, ai.enemyCloudCost);
        double reach = Math.max(ai.blinkBandMaxDistance, ai.aoeMinRange) + 8.0D;
        AxisAlignedBB box = target.getEntityBoundingBox().grow(reach);
        List<EntityFocusCloud> clouds = mage.world.getEntitiesWithinAABB(EntityFocusCloud.class, box);
        for (EntityFocusCloud cloud : clouds) {
            if (cloud.isDead) {
                continue;
            }
            EntityLivingBase owner = cloud.getOwner();
            CloudAvoidance.Zone zone = new CloudAvoidance.Zone(cloud.posX, cloud.posY, cloud.posZ, cloud.getRadius());
            if (owner == mage) {
                avoid.addOwn(zone);
            } else if (!(owner instanceof EntityCultist)) {
                avoid.addEnemy(zone);
            }
        }
        List<EntityFocusMine> mines = mage.world.getEntitiesWithinAABB(EntityFocusMine.class, box);
        for (EntityFocusMine mine : mines) {
            if (!mine.isDead) {
                avoid.addEnemy(new CloudAvoidance.Zone(mine.posX, mine.posY, mine.posZ, MINE_RADIUS));
            }
        }
        return avoid;
    }

    /** A mine has no radius to read; its effect is local, so treat it as a small zone. */
    private static final double MINE_RADIUS = 2.0D;

    /**
     * A sight-line cost. With {@code wantSight} a spot the target cannot see costs a little (the band fires better with line
     * of sight); without it a spot the target CAN see costs (the escape wants cover).
     */
    private static final class LosPenalty implements BlinkPicker.Penalty {
        private final World world;
        private final EntityLivingBase target;
        private final double cost;
        private final boolean wantSight;

        LosPenalty(World world, EntityLivingBase target, double cost, boolean wantSight) {
            this.world = world;
            this.target = target;
            this.cost = cost;
            this.wantSight = wantSight;
        }

        @Override
        public double penalty(double x, double y, double z) {
            Vec3d from = new Vec3d(x, y + 1.6D, z);
            Vec3d to = new Vec3d(target.posX, target.posY + target.getEyeHeight(), target.posZ);
            boolean seen = world.rayTraceBlocks(from, to, false, true, false) == null;
            return seen == wantSight ? 0.0D : cost;
        }
    }

    // ---- stuck in a wall (plan 3.14) ----

    /** Spots are tried in this order: nearest first, up before down (see {@link UnstickSearch}). */
    private static List<int[]> unstickOrder;

    /**
     * Emergency: the mage is inside a block. Teleports to the nearest safe standing spot within {@code radius}, ignoring the
     * blink cooldown, the strict rule, health, Stun and the leash; it neither uses nor starts the blink cooldown. If another
     * mod cancels the teleport event the move is made directly: an unstick is not an action to be vetoed.
     * @return true when the mage moved
     */
    public static boolean unstick(EntityCultistBattlemage mage, double radius) {
        World w = mage.world;
        if (w.isRemote || !mage.isEntityAlive()) {
            return false;
        }
        int r = (int) Math.min(16.0D, Math.max(2.0D, radius));
        if (unstickOrder == null || unstickOrder.isEmpty()) {
            unstickOrder = UnstickSearch.offsets(16, 8, 8);
        }
        int bx = (int) Math.floor(mage.posX);
        int by = (int) Math.floor(mage.posY);
        int bz = (int) Math.floor(mage.posZ);
        for (int[] o : unstickOrder) {
            if (o[0] * o[0] + o[2] * o[2] > r * r) {
                continue;
            }
            double[] spot = standingSpot(mage, bx + o[0], by + o[1], bz + o[2]);
            if (spot == null) {
                continue;
            }
            if (teleport(mage, spot[0], spot[1], spot[2])) {
                return true;
            }
            mage.setPositionAndUpdate(spot[0], spot[1], spot[2]);
            mage.getNavigator().clearPath();
            return true;
        }
        return false;
    }

    /** An exact feet cell the mage may stand in: solid floor right below, the box fits, no liquid or hazard. */
    static double[] standingSpot(EntityCultistBattlemage mage, int x, int y, int z) {
        World w = mage.world;
        BlockPos feet = new BlockPos(x, y, z);
        if (!w.isBlockLoaded(feet)) {
            return null;
        }
        BlockPos below = feet.down();
        IBlockState ground = w.getBlockState(below);
        if (!ground.isSideSolid(w, below, EnumFacing.UP)) {
            return null;
        }
        double px = x + 0.5D;
        double pz = z + 0.5D;
        AxisAlignedBB box = mage.getEntityBoundingBox().offset(px - mage.posX, y - mage.posY, pz - mage.posZ);
        if (!w.getCollisionBoxes(mage, box).isEmpty() || w.containsAnyLiquid(box)) {
            return null;
        }
        if (hazard(w.getBlockState(feet)) || hazard(w.getBlockState(feet.up())) || hazard(ground)) {
            return null;
        }
        return new double[] {px, y, pz};
    }

    /**
     * The shared blink: fills in the mage's position, travel cap and sample count, ranks candidates, walks the list through
     * the world checks (at most three valid spots) and teleports. Sets the shared cooldown on success, a short retry on failure.
     */
    public static boolean blinkTo(EntityCultistBattlemage mage, BlinkPicker.Request req, AiTuning ai) {
        if (mage.world.isRemote || !BattlemageConfig.blinkEnabled || !mage.blinkReady() || Stun.isStunned(mage) || !mage.isEntityAlive()) {
            return false;
        }
        req.fromX = mage.posX;
        req.fromY = mage.posY;
        req.fromZ = mage.posZ;
        req.maxTravel = ai.blinkMaxTravel;
        req.samples = ai.blinkSamples;
        List<BlinkPicker.Candidate> ranked = BlinkPicker.rank(req, mage.getRNG(), MAX_ATTEMPTS * 3);
        int attempts = 0;
        for (BlinkPicker.Candidate c : ranked) {
            if (attempts >= MAX_ATTEMPTS) {
                break;
            }
            double[] spot = groundSpot(mage, c.x, c.y, c.z);
            if (spot == null) {
                continue;
            }
            attempts++;
            if (teleport(mage, spot[0], spot[1], spot[2])) {
                mage.setBlinkReadyAt(mage.ticksExisted + mage.effectiveBlinkCooldown(ai));
                mage.noteBlink();
                return true;
            }
        }
        mage.setBlinkReadyAt(mage.ticksExisted + FAILED_RETRY_TICKS);
        return false;
    }

    /**
     * Walks a candidate down to solid ground (at most 4 blocks, one block up allowed) and checks the mage's box fits there
     * with no collision, no liquid and no fire, lava, magma or cactus. @return the feet position, or null when the spot is no good
     */
    static double[] groundSpot(EntityCultistBattlemage mage, double x, double y, double z) {
        World w = mage.world;
        int bx = (int) Math.floor(x);
        int bz = (int) Math.floor(z);
        int top = (int) Math.floor(y) + 1;
        for (int dy = 0; dy <= 5; dy++) {
            BlockPos feet = new BlockPos(bx, top - dy, bz);
            if (!w.isBlockLoaded(feet)) {
                return null;
            }
            BlockPos below = feet.down();
            IBlockState ground = w.getBlockState(below);
            if (!ground.isSideSolid(w, below, EnumFacing.UP)) {
                continue;
            }
            double px = bx + 0.5D;
            double pz = bz + 0.5D;
            AxisAlignedBB box = mage.getEntityBoundingBox().offset(px - mage.posX, feet.getY() - mage.posY, pz - mage.posZ);
            if (!w.getCollisionBoxes(mage, box).isEmpty() || w.containsAnyLiquid(box)) {
                return null;
            }
            if (hazard(w.getBlockState(feet)) || hazard(w.getBlockState(feet.up())) || hazard(ground)) {
                return null;
            }
            return new double[] {px, feet.getY(), pz};
        }
        return null;
    }

    static boolean hazard(IBlockState state) {
        Block b = state.getBlock();
        Material m = state.getMaterial();
        return b == Blocks.FIRE || b == Blocks.LAVA || b == Blocks.FLOWING_LAVA || b == Blocks.MAGMA || b == Blocks.CACTUS
                || m == Material.FIRE || m == Material.LAVA;
    }

    /** The cue and the move, as in plan 3.10 point 2. @return false when a mod cancelled it or the move did not happen */
    static boolean teleport(EntityCultistBattlemage mage, double x, double y, double z) {
        World w = mage.world;
        Random r = mage.getRNG();
        double ox = mage.posX;
        double oy = mage.posY;
        double oz = mage.posZ;
        w.playSound(null, ox, oy, oz, SoundsTC.hhoff, SoundCategory.HOSTILE, 0.8F, 0.45F + (r.nextFloat() - 0.5F) * 0.1F);
        mage.swingArm(EnumHand.OFF_HAND);
        EnderTeleportEvent event = new EnderTeleportEvent(mage, x, y, z, 0.0F);
        if (MinecraftForge.EVENT_BUS.post(event)) {
            return false;
        }
        mage.dismountRidingEntity();
        if (!mage.attemptTeleport(event.getTargetX(), event.getTargetY(), event.getTargetZ())) {
            return false;
        }
        mage.getNavigator().clearPath();
        burst(w, ox, oy, oz);
        burst(w, mage.posX, mage.posY, mage.posZ);
        w.playSound(null, mage.posX, mage.posY, mage.posZ, SoundsTC.hhon, SoundCategory.HOSTILE, 0.8F, 0.85F);
        w.playSound(null, ox, oy, oz, SoundEvents.ENTITY_ENDERMEN_TELEPORT, SoundCategory.HOSTILE, 1.0F, 1.0F);
        w.playSound(null, mage.posX, mage.posY, mage.posZ, SoundEvents.ENTITY_ENDERMEN_TELEPORT, SoundCategory.HOSTILE, 1.0F, 1.0F);
        return true;
    }

    /** Blink's own particle burst by key; a vanilla portal puff when the element is not registered. */
    private static void burst(World w, double x, double y, double z) {
        if (!(w instanceof WorldServer)) {
            return;
        }
        if (FocusEngine.getElement(BLINK_FX_KEY) != null) {
            PacketHandler.INSTANCE.sendToAllAround(new PacketFXFocusPartImpact(x, y + 0.9D, z, new String[] {BLINK_FX_KEY}),
                    new NetworkRegistry.TargetPoint(w.provider.getDimension(), x, y, z, 64.0D));
        } else {
            ((WorldServer) w).spawnParticle(EnumParticleTypes.PORTAL, x, y + 1.0D, z, 30, 0.3D, 0.6D, 0.3D, 0.1D);
        }
    }
}
