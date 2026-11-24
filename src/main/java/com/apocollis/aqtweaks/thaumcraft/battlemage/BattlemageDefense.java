package com.apocollis.aqtweaks.thaumcraft.battlemage;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig.BattlemageConfig;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IProjectile;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.item.ItemBow;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import thaumcraft.common.entities.monster.cult.EntityCultist;
import thaumcraft.common.entities.projectile.EntityFocusCloud;
import thaumcraft.common.entities.projectile.EntityFocusMine;

import java.util.List;

/**
 * The Battlemage's survival layer (plan 3.5, 22), one per mage, run before the normal casts every AI tick.
 * Order: the strict defensive escape, peel (Knock Back), heal, then movement overrides (withdraw to heal, step back after a
 * peel, step out of fire or an enemy cloud). Sidestepping and the "no firing line" blink never take the tick.
 *
 * <p>Stun is a hard gate: the AI never calls this while the mage is stunned. All decisions are in {@link DefenseRules}.
 */
public final class BattlemageDefense {

    static final String KNOCK_BACK_ID = "knock_back";
    static final String HEAL_SELF_ID = "heal_self";
    /** Knock Back moves the player away; the mage then steps back for this long. */
    private static final int KITE_TICKS = 30;
    private static final double HAZARD_SCAN = 8.0D;
    private static final double MINE_RADIUS = 2.0D;

    private final EntityCultistBattlemage mage;
    private final ReachabilityProbe reach = new ReachabilityProbe();

    private int pressureTicks;
    private int noLosTicks;
    private long lastPeelAt = -1000L;
    private long lastKnockBackAt = Long.MIN_VALUE;
    private long lastSidestepAt = -1000L;
    private boolean retreating;
    private long retreatStartedAt;
    private long retreatCooldownUntil;
    private long kiteUntil;
    private long hazardUntil;
    private int lastMoveTick = -1000;
    private int lastHazardCheck = -1000;

    public BattlemageDefense(EntityCultistBattlemage mage) {
        this.mage = mage;
    }

    public boolean retreating() {
        return retreating;
    }

    /** The AI keeps its short step-back going until this tick (after a peel). */
    public long kiteUntil() {
        return kiteUntil;
    }

    /** True while a hazard step (fire, enemy cloud) owns the movement. */
    public boolean hazardActive() {
        return mage.ticksExisted < hazardUntil;
    }

    public void reset() {
        pressureTicks = 0;
        postStunStep = -1;
        noLosTicks = 0;
        retreating = false;
        kiteUntil = 0L;
        hazardUntil = 0L;
        reach.reset();
    }

    /**
     * Casts and teleports that consume the tick. @return true when the mage acted and the AI should stop for this tick
     */
    public boolean act(EntityLivingBase target, double dist, boolean los) {
        AiTuning t = mage.getAi();
        int now = mage.ticksExisted;
        mage.getHealBudget().tick(now);
        pressureTicks = dist <= t.awayCloseRange ? pressureTicks + 1 : 0;
        noLosTicks = los ? 0 : noLosTicks + 1;
        double hp = mage.getHealth() / Math.max(1.0F, mage.getMaxHealth());
        int kb = mage.indexOfFocus(KNOCK_BACK_ID);
        int heal = mage.indexOfFocus(HEAL_SELF_ID);
        boolean kbReady = kb >= 0 && mage.getReadyAt()[kb] <= now;
        boolean healReady = heal >= 0 && mage.getReadyAt()[heal] <= now;
        boolean charge = mage.getHealBudget().canHeal();

        sidestep(target, dist, now, t);

        if (escape(target, dist, hp, kbReady, t, now)) {
            return true;
        }
        if (noFiringLine(target, t, now)) {
            return true;
        }
        if (offensiveRift(target, dist, los, t)) {
            return true;
        }
        if (!mage.surgeCastRestricted() && peel(target, dist, kb, kbReady, t, now)) {
            return true;
        }
        updateRetreat(hp, charge && heal >= 0, t, now);
        boolean mayHeal = retreating
                ? healReady && charge
                : DefenseRules.healInPlace(hp, t.healHpFraction, charge, healReady, dist, t.meleeTriggerRange + 1.0D);
        if (mayHeal && now >= mage.getNextCastTick()) {
            mage.castFocus(heal, mage, t);
            mage.getHealBudget().spend(now);
            return true;
        }
        return false;
    }

    // ---- the strict escape (plan 3.10) ----

    private boolean escape(EntityLivingBase target, double dist, double hp, boolean kbReady, AiTuning t, int now) {
        if (hp >= t.awayHpFraction || dist > t.awayCloseRange) {
            return false;
        }
        boolean cornered = RandomPositionGenerator.findRandomTargetBlockAwayFrom(mage, 8, 4, target.getPositionVector()) == null;
        long sinceKb = lastKnockBackAt == Long.MIN_VALUE ? -1L : now - lastKnockBackAt;
        if (!DefenseRules.awayEscape(hp, t.awayHpFraction, dist, t.awayCloseRange, pressureTicks, t.awayPressureTicks, cornered,
                kbReady, sinceKb, t.awayKnockBackWindowTicks)) {
            return false;
        }
        boolean blink = mage.indexOfFocus("blink") >= 0 && mage.blinkReady();
        boolean rift = BattlemageRift.ready(mage, t);
        // Cornered against a wall: the rift is the better way out; otherwise the open-air blink, with the rift behind it.
        if (cornered && rift && BattlemageRift.riftAway(mage, target, t)) {
            return afterEscape();
        }
        if (blink && BattlemageBlink.blinkAway(mage, target, t)) {
            return afterEscape();
        }
        if (!cornered && rift && BattlemageRift.riftAway(mage, target, t)) {
            return afterEscape();
        }
        return false;
    }

    private boolean afterEscape() {
        pressureTicks = 0;
        mage.getNavigator().clearPath();
        mage.getMelee().abort();
        return true;
    }

    // ---- the firing line and the offensive rift (plan 3.11 item 8, 3.14) ----

    /** No line of sight for a while: blink to a spot that has one. */
    private boolean noFiringLine(EntityLivingBase target, AiTuning t, int now) {
        if (noLosTicks < t.noLosBlinkTicks || retreating || mage.indexOfFocus("blink") < 0 || !mage.blinkReady()) {
            return false;
        }
        noLosTicks = 0;
        return BattlemageBlink.blinkToRange(mage, target, t, t.idealMin, t.idealMax);
    }

    /** The target is behind a wall with no sane walking route: rift to its side and land in the firing band. */
    private boolean offensiveRift(EntityLivingBase target, double dist, boolean los, AiTuning t) {
        if (los || noLosTicks < 20 || dist < 5.0D || retreating || !BattlemageRift.ready(mage, t)) {
            return false;
        }
        if (!reach.blocked(mage, target, t)) {
            return false;
        }
        boolean done = BattlemageRift.riftToward(mage, target, t);
        if (done) {
            noLosTicks = 0;
            mage.getNavigator().clearPath();
            mage.getMelee().abort();
        }
        return done;
    }

    // ---- after a stun broke a strike combo (user, 2026-11-20) ----

    private int postStunStep = -1;

    /** A Stun cut the strike combo short: once it ends, Knock Back and then an escape blink. */
    public void startPostStunEscape() {
        postStunStep = 0;
    }

    /**
     * Step 0 casts Knock Back if it is ready and the player is close; step 1 blinks away (the cooldown of the blink in is
     * waived; a rift is the fallback). Runs only after the Stun is over. @return true while it used the tick
     */
    public boolean postStunEscape(EntityLivingBase target, double dist, AiTuning t) {
        if (postStunStep < 0) {
            return false;
        }
        int now = mage.ticksExisted;
        if (postStunStep == 0) {
            postStunStep = 1;
            int kb = mage.indexOfFocus(KNOCK_BACK_ID);
            if (kb >= 0 && mage.getReadyAt()[kb] <= now && dist <= Math.min(t.peelRange, BattlemageConfig.touchReach) + 1.0D) {
                long keep = mage.getNextCastTick();
                mage.castFocus(kb, target, t);
                mage.setNextCastTick(keep);
                lastPeelAt = now;
                lastKnockBackAt = now;
                mage.getNavigator().clearPath();
                return true;
            }
        }
        postStunStep = -1;
        if (mage.indexOfFocus("blink") >= 0) {
            mage.setBlinkReadyAt(now);
            if (BattlemageBlink.blinkAway(mage, target, t)) {
                return afterEscape();
            }
        }
        if (BattlemageRift.ready(mage, t) && BattlemageRift.riftAway(mage, target, t)) {
            return afterEscape();
        }
        return false;
    }

    // ---- peel (plan 3.5) ----

    private boolean peel(EntityLivingBase target, double dist, int kb, boolean kbReady, AiTuning t, int now) {
        if (kb < 0 || !mage.usesGenericPeel()) {
            return false;
        }
        int cooldown = mage.getShield().widenBand(mage) ? t.peelCooldownTicks / 2 : t.peelCooldownTicks;
        boolean cooled = now - lastPeelAt >= cooldown;
        if (!DefenseRules.peel(dist, Math.min(t.peelRange, BattlemageConfig.touchReach), kbReady, cooled, mage.getMelee().active())) {
            return false;
        }
        long keep = mage.getNextCastTick();
        mage.castFocus(kb, target, t);
        mage.setNextCastTick(keep);
        lastPeelAt = now;
        lastKnockBackAt = now;
        kiteUntil = now + KITE_TICKS;
        mage.getNavigator().clearPath();
        return true;
    }

    // ---- withdraw to heal (plan 3.5) ----

    private void updateRetreat(double hp, boolean healAvailable, AiTuning t, int now) {
        if (!mage.canRetreat()) {
            retreating = false;
            return;
        }
        if (retreating) {
            if (DefenseRules.endRetreat(hp, t.retreatResumeFraction, healAvailable, now - retreatStartedAt, t.retreatMaxTicks)) {
                retreating = false;
                retreatCooldownUntil = now + t.retreatCooldownTicks;
                mage.getNavigator().clearPath();
            }
        } else if (DefenseRules.startRetreat(hp, t.retreatHpFraction, healAvailable, now >= retreatCooldownUntil)) {
            retreating = true;
            retreatStartedAt = now;
            mage.getMelee().abort();
            mage.getNavigator().clearPath();
        }
    }

    /** While retreating the mage withdraws (breaking line of sight when it can) and casts nothing else. */
    public void retreatMove(EntityLivingBase target, AiTuning t) {
        int now = mage.ticksExisted;
        if (mage.getNavigator().noPath() || now - lastMoveTick >= t.repositionIntervalTicks) {
            Vec3d anchor = mage.retreatAnchor();
            Vec3d away = anchor != null ? anchor : RandomPositionGenerator.findRandomTargetBlockAwayFrom(mage, 12, 4, target.getPositionVector());
            if (away != null) {
                mage.getNavigator().tryMoveToXYZ(away.x, away.y, away.z, t.moveSpeed * 1.3D);
            }
            lastMoveTick = now;
        }
    }

    // ---- sidestep (plan 3.5) ----

    private void sidestep(EntityLivingBase target, double dist, int now, AiTuning t) {
        if (now - lastSidestepAt < t.sidestepCooldownTicks || now % 2 != 0 || !mage.onGround) {
            return;
        }
        int side = 0;
        double dirX = 0.0D;
        double dirZ = 0.0D;
        Vec3d me = new Vec3d(mage.posX, mage.posY + mage.height * 0.5D, mage.posZ);
        AxisAlignedBB box = mage.getEntityBoundingBox().grow(t.sidestepRange);
        List<Entity> near = mage.world.getEntitiesWithinAABB(Entity.class, box);
        for (Entity e : near) {
            if (!(e instanceof IProjectile) || e == mage || e.isDead) {
                continue;
            }
            side = DodgePlanner.sideFor(e.posX, e.posY, e.posZ, e.motionX, e.motionY, e.motionZ, me.x, me.y, me.z, 1.4D, 8);
            if (side != 0) {
                dirX = e.motionX;
                dirZ = e.motionZ;
                break;
            }
        }
        if (side == 0 && bowAimedAtMe(target, dist, t)) {
            dirX = mage.posX - target.posX;
            dirZ = mage.posZ - target.posZ;
            side = mage.getRNG().nextBoolean() ? 1 : -1;
        }
        if (side == 0) {
            return;
        }
        double[] p = DodgePlanner.perpendicular(dirX, dirZ, side);
        mage.motionX += p[0] * t.sidestepImpulse;
        mage.motionZ += p[1] * t.sidestepImpulse;
        mage.motionY += 0.12D;
        mage.velocityChanged = true;
        lastSidestepAt = now;
    }

    /** A drawn bow whose owner is looking at the mage (plan 3.11 item 4). */
    private boolean bowAimedAtMe(EntityLivingBase target, double dist, AiTuning t) {
        if (dist > t.sidestepRange * 2.0D || !target.isHandActive() || !(target.getActiveItemStack().getItem() instanceof ItemBow)) {
            return false;
        }
        if (target.getItemInUseMaxCount() < 10) {
            return false;
        }
        Vec3d look = target.getLookVec();
        Vec3d to = new Vec3d(mage.posX - target.posX, 0.0D, mage.posZ - target.posZ).normalize();
        return look.x * to.x + look.z * to.z > 0.95D;
    }

    // ---- enemy hazards (plan 3.11 item 7) ----

    /**
     * Steps out of fire or an enemy cloud or mine. Sets a short movement override so the normal band movement does not
     * walk straight back in. @return true when a hazard step owns this tick's movement
     */
    public boolean hazardMove(AiTuning t) {
        int now = mage.ticksExisted;
        if (now < hazardUntil) {
            return true;
        }
        if (now - lastHazardCheck < 10) {
            return false;
        }
        lastHazardCheck = now;
        if (mage.isBurning() && !mage.isImmuneToFire()) {
            BlockPos water = nearestWater();
            if (water != null) {
                mage.getNavigator().tryMoveToXYZ(water.getX() + 0.5D, water.getY(), water.getZ() + 0.5D, t.moveSpeed * 1.4D);
                hazardUntil = now + 20;
                return true;
            }
        }
        double[] out = enemyCloudExit(t);
        if (out != null) {
            mage.getNavigator().tryMoveToXYZ(out[0], mage.posY, out[1], t.moveSpeed * 1.4D);
            hazardUntil = now + 15;
            return true;
        }
        return false;
    }

    private BlockPos nearestWater() {
        BlockPos base = new BlockPos(mage);
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.getAllInBoxMutable(base.add(-6, -2, -6), base.add(6, 1, 6))) {
            if (mage.world.getBlockState(p).getMaterial() == Material.WATER) {
                double d = p.distanceSq(base);
                if (d < bestD) {
                    bestD = d;
                    best = p.toImmutable();
                }
            }
        }
        return best;
    }

    /** If the mage stands in a player's cloud or a mine's reach: a point just outside it, else null. */
    private double[] enemyCloudExit(AiTuning t) {
        AxisAlignedBB box = mage.getEntityBoundingBox().grow(HAZARD_SCAN);
        List<EntityFocusCloud> clouds = mage.world.getEntitiesWithinAABB(EntityFocusCloud.class, box);
        for (EntityFocusCloud c : clouds) {
            EntityLivingBase owner = c.getOwner();
            if (c.isDead || owner == mage || owner instanceof EntityCultist) {
                continue;
            }
            CloudAvoidance.Zone z = new CloudAvoidance.Zone(c.posX, c.posY, c.posZ, c.getRadius());
            if (z.contains(mage.posX, mage.posY, mage.posZ, 0.25D)) {
                return exitPoint(z);
            }
        }
        List<EntityFocusMine> mines = mage.world.getEntitiesWithinAABB(EntityFocusMine.class, box);
        for (EntityFocusMine m : mines) {
            CloudAvoidance.Zone z = new CloudAvoidance.Zone(m.posX, m.posY, m.posZ, MINE_RADIUS);
            if (!m.isDead && z.contains(mage.posX, mage.posY, mage.posZ, 0.25D)) {
                return exitPoint(z);
            }
        }
        return null;
    }

    private double[] exitPoint(CloudAvoidance.Zone z) {
        double dx = mage.posX - z.x;
        double dz = mage.posZ - z.z;
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 0.01D) {
            dx = 1.0D;
            dz = 0.0D;
            len = 1.0D;
        }
        double reach = z.radius + 2.0D;
        return new double[] {z.x + dx / len * reach, z.z + dz / len * reach};
    }

    // ---- after a melee burst (plan 3.10 point 3) ----

    /** A burst that began with a strike blink always ends with a blink back out, once the entry blink's cooldown is waived. */
    public boolean afterMelee(EntityLivingBase target, AiTuning t, boolean strike) {
        if (strike && mage.indexOfFocus("blink") >= 0) {
            mage.setBlinkReadyAt(mage.ticksExisted);
            double min = Math.min(t.idealMin, t.idealMax);
            if (BattlemageBlink.blinkToRange(mage, target, t, min, Math.max(min, t.idealMax))) {
                return true;
            }
        }
        return afterMelee(target, t);
    }

    /** A melee burst just ended: with Blink ready, return to the firing band some of the time. @return true when it blinked */
    public boolean afterMelee(EntityLivingBase target, AiTuning t) {
        if (mage.indexOfFocus("blink") < 0 || !mage.blinkReady() || mage.getRNG().nextDouble() >= t.blinkAfterMeleeChance) {
            return false;
        }
        double min = Math.min(t.idealMin, t.idealMax);
        return BattlemageBlink.blinkToRange(mage, target, t, min, Math.max(min, t.idealMax));
    }
}
