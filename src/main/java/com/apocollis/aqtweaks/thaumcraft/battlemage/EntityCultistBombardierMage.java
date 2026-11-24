package com.apocollis.aqtweaks.thaumcraft.battlemage;

import com.apocollis.aqtweaks.potion.Stun;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import thaumcraft.common.entities.projectile.EntityFocusCloud;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig.BattlemageConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The Bombardier Mage ({@code aqtweaks:cultist_bombardier_mage}, plan 19): a Tier 2 variant that carries the fixed four plus
 * all five area foci (the roller is skipped), with the energy pool off and {@code aoeMinRange} 6.
 *
 * <p><b>Volley</b> ({@link VolleyPlanner}): it replaces the normal cast choice ({@link #handlesCasting}). One area cast every
 * {@code volleyGapTicks}, each of the five once in a random order, then a {@code volleyPauseTicks} pause. A focus is skipped
 * while its own cooldown runs or while a cloud from its previous cast is still alive ({@link CloudLedger}: one live cast per
 * focus, so up to five casts' worth of clouds can be out at once). Targets closer than {@code aoeMinRange} get no cast; the
 * volley clock keeps running.
 *
 * <p><b>Close response</b> ({@link CloseSequence}, driven from {@link #variantStep}): a target inside {@code aoeMinRange}
 * gets 1 to 2 melee attacks, Knock Back, then a reposition blink to the volley band ({@link BattlemageBlink#blinkToBand}),
 * which stays out of its own clouds and avoids enemy ones. A target 3 to 6 blocks away (out of both reaches) goes straight to
 * the blink. With Blink on cooldown it steps back on foot.
 */
public class EntityCultistBombardierMage extends EntityCultistBattlemage {

    public static final String VARIANT_ID = "bombardier_mage";
    private static final Logger LOG = LogManager.getLogger("AQTweaks-Battlemage");
    private static final String KNOCK_BACK_ID = "knock_back";
    /** Radius of the owned-cloud scan around the mage: clouds are placed at the target, at most about 30 blocks out. */
    private static final double CLOUD_SCAN_RADIUS = 40.0D;
    /** A cloud that spawns this many ticks after its cast is still credited to that cast. */
    private static final int ATTRIBUTION_WINDOW_TICKS = 10;
    private static final int CLOUD_SCAN_INTERVAL = 4;

    private final CloudLedger ledger = new CloudLedger();
    private final CloseSequence close = new CloseSequence();
    private VolleyPlanner volley;
    private int lastRetreatPathTick = -1000;

    public EntityCultistBombardierMage(World world) {
        super(world);
    }

    @Override
    public int getTier() {
        return 2;
    }

    @Override
    public String getVariantId() {
        return VARIANT_ID;
    }

    /** The Bombardier keeps its own close sequence (plan 19.8) instead of the generic Knock Back peel. */
    @Override
    public boolean usesGenericPeel() {
        return false;
    }

    /** It holds its ground at range and does not withdraw to heal. */
    @Override
    public boolean canRetreat() {
        return false;
    }

    public CloudLedger getCloudLedger() {
        return ledger;
    }

    private VolleyPlanner volley() {
        if (volley == null) {
            AiTuning t = getAi();
            volley = new VolleyPlanner(t.volleyGapTicks, t.volleyPauseTicks, t.volleyMaxTicks, this.getRNG());
        }
        return volley;
    }

    // ---- cloud tracking (plan 19.6) ----

    @Override
    protected void onVariantTick() {
        if (this.ticksExisted % CLOUD_SCAN_INTERVAL == 0 && (this.getAttackTarget() != null || ledger.liveCasts() > 0)) {
            scanClouds(this.ticksExisted);
        }
    }

    /** Reads the clouds this mage owns and lets the ledger attribute any new ones to the cast in progress. */
    private void scanClouds(long now) {
        AxisAlignedBB box = this.getEntityBoundingBox().grow(CLOUD_SCAN_RADIUS);
        List<EntityFocusCloud> clouds = this.world.getEntitiesWithinAABB(EntityFocusCloud.class, box);
        List<UUID> alive = new ArrayList<>(clouds.size());
        for (EntityFocusCloud cloud : clouds) {
            if (!cloud.isDead && cloud.getOwner() == this) {
                alive.add(cloud.getUniqueID());
            }
        }
        ledger.observe(alive, now);
    }

    // ---- volley (plan 19.7) ----

    @Override
    public boolean handlesCasting() {
        return true;
    }

    /** The ids of the carried area foci (role {@code aoe} or {@code area_denial}), in loadout order. */
    private List<String> areaFocusIds() {
        List<String> ids = new ArrayList<>();
        for (BattlemageFocus f : getFoci()) {
            if (f.template != null && ("aoe".equals(f.def.role) || "area_denial".equals(f.def.role))) {
                ids.add(f.def.id);
            }
        }
        return ids;
    }

    /** One decision tick: fire the next volley focus if it is time and the target is in the area band. */
    @Override
    public boolean variantCast(EntityLivingBase target, double dist, boolean los) {
        final AiTuning t = getAi();
        final long now = this.ticksExisted;
        List<String> ids = areaFocusIds();
        if (ids.isEmpty()) {
            return false;
        }
        final double distance = dist;
        final boolean sight = los;
        String pick = volley().next(now, ids, new VolleyPlanner.Readiness() {
            @Override
            public boolean ready(String focusId) {
                return areaReady(focusId, distance, sight, t, now);
            }
        });
        if (pick == null) {
            return false;
        }
        int idx = indexOfFocus(pick);
        if (idx < 0) {
            return false;
        }
        ledger.expect(null, 0L);
        scanClouds(now); // seed: every cloud out right now is not this cast's
        ledger.expect(pick, now + ATTRIBUTION_WINDOW_TICKS);
        castFocus(idx, target, t);
        scanClouds(now);
        if (BattlemageConfig.debug) {
            LOG.info("Bombardier volley cast {} (clouds live for {} foci)", pick, ledger.liveCasts());
        }
        return true;
    }

    /** A focus can fire when it is off cooldown, has no live cloud, the target is in the area band and sight is not needed or held. */
    private boolean areaReady(String focusId, double dist, boolean los, AiTuning t, long now) {
        int idx = indexOfFocus(focusId);
        if (idx < 0 || getReadyAt()[idx] > now || ledger.hasLive(focusId)) {
            return false;
        }
        FocusDef def = getFoci().get(idx).def;
        if (dist < t.aoeMinRange || dist > def.maxRange) {
            return false;
        }
        return !def.requireLos || los;
    }

    // ---- close response (plan 19.8) ----

    @Override
    public boolean variantStep(EntityLivingBase target, double dist, boolean los) {
        if (Stun.isStunned(this)) {
            close.reset();
            return false;
        }
        AiTuning t = getAi();
        long now = this.ticksExisted;
        int kb = indexOfFocus(KNOCK_BACK_ID);
        CloseSequence.Input in = new CloseSequence.Input();
        in.now = now;
        in.dist = dist;
        in.closeRange = t.aoeMinRange;
        in.meleeRange = t.meleeTriggerRange;
        in.touchReach = BattlemageConfig.touchReach;
        in.knockBackReady = kb >= 0 && getReadyAt()[kb] <= now;
        in.blinkReady = blinkReady();
        in.burstActive = getMelee().active();
        in.meleeMin = t.closeMeleeMin;
        in.meleeMax = t.closeMeleeMax;
        in.maxTicks = t.closeSequenceMaxTicks;
        CloseSequence.Action action = close.step(in, this.getRNG());
        if (!close.active() && getMelee().active()) {
            getMelee().abort(); // the target left the close ring in the middle of a burst
        }
        switch (action) {
            case MELEE:
                getMelee().start(now, close.burstSize());
                return driveBurst(target, dist, t);
            case KNOCK_BACK:
                castKnockBack(kb, target, t);
                return true;
            case BLINK:
                if (BattlemageBlink.blinkToBand(this, target, t)) {
                    close.blinked();
                }
                return true;
            case RETREAT:
                retreat(target, t);
                return true;
            case NONE:
            default:
                if (close.state() == CloseSequence.State.MELEE && getMelee().active()) {
                    return driveBurst(target, dist, t);
                }
                return false;
        }
    }

    /** Casts Knock Back without disturbing the volley's gap (the cast gap would otherwise delay the next area cast). */
    private void castKnockBack(int index, EntityLivingBase target, AiTuning t) {
        if (index < 0) {
            return;
        }
        long keep = getNextCastTick();
        castFocus(index, target, t);
        setNextCastTick(keep);
    }

    /** The close melee burst: step in if needed, swing when due, end when it has made its attacks. @return true while it owns the tick */
    private boolean driveBurst(EntityLivingBase target, double dist, AiTuning t) {
        MeleeBurst burst = getMelee();
        int now = this.ticksExisted;
        if (burst.finished(now, t.meleeBurstMaxTicks)) {
            endBurst(burst, now);
            return true;
        }
        if (dist > t.meleeReach) {
            if (now - lastRetreatPathTick >= t.repositionIntervalTicks || this.getNavigator().noPath()) {
                this.getNavigator().tryMoveToEntityLiving(target, t.moveSpeed * 1.2D);
                lastRetreatPathTick = now;
            }
            return true;
        }
        this.getNavigator().clearPath();
        if (burst.attackDue(now)) {
            this.swingArm(EnumHand.MAIN_HAND);
            this.attackEntityAsMob(target);
            burst.onAttack(now, t.meleeAttackIntervalTicks);
            if (burst.finished(now, t.meleeBurstMaxTicks)) {
                endBurst(burst, now);
            }
        }
        return true;
    }

    /** Ends the burst with no cooldown: the close sequence decides what comes next. */
    private void endBurst(MeleeBurst burst, int now) {
        burst.end(now, 0);
        this.getNavigator().clearPath();
        lastRetreatPathTick = -1000;
    }

    /** Steps back on foot (Blink is on cooldown); repaths about twice a second. */
    private void retreat(EntityLivingBase target, AiTuning t) {
        int now = this.ticksExisted;
        if (this.getNavigator().noPath() || now - lastRetreatPathTick >= t.repositionIntervalTicks) {
            Vec3d away = RandomPositionGenerator.findRandomTargetBlockAwayFrom(this, 8, 4, target.getPositionVector());
            if (away != null) {
                this.getNavigator().tryMoveToXYZ(away.x, away.y, away.z, t.moveSpeed * 1.2D);
            }
            lastRetreatPathTick = now;
        }
    }
}
