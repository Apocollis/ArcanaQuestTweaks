package com.apocollis.aqtweaks.thaumcraft.battlemage;

import com.apocollis.aqtweaks.potion.Stun;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.init.MobEffects;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The Battlemage's brain (plan 3.2): one task owns movement, looking and casting. Stage 2 priority order:
 * Stun gate, summon upkeep (rule 6, always keep Spell Bats active), cast (rule 9, with the cast budget and burst hold-back),
 * melee burst (rule 10), then movement (rule 11): ENGAGE, REPOSITION and strafe inside the preferred band, and a short
 * retreat after a melee burst. Defense, blink, rift, shield and phases are later stages.
 */
public final class AIBattlemageCombat extends EntityAIBase {

    private final EntityCultistBattlemage mage;
    private AiTuning ai;
    private int strafeSign = 1;
    private int strafeFlipAt;
    private int lastPathTick = -1000;
    private int targetLostTicks;
    private boolean opening = true;
    /** Walking in for a melee burst during a cast hold (plan: the mage must melee more). */
    private boolean closingIn;
    private boolean holdRolled;
    /** The current melee burst began with a strike blink: end it with a blink back out. */
    private boolean strikeActive;
    /** Tick the first blow of a strike may start (after the blink-in window), or -1. */
    private long strikeStartAt = -1L;
    /** Tick the blink back out may fire (after the last-blow window), or -1. */
    private long strikeExitAt = -1L;
    /** Next tick the timed reposition blink may be considered. */
    private long nextRepositionAt;
    private int retreatUntil;

    public AIBattlemageCombat(EntityCultistBattlemage mage) {
        this.mage = mage;
        this.setMutexBits(3);
    }

    @Override
    public boolean shouldExecute() {
        EntityLivingBase t = mage.getAttackTarget();
        return t != null && t.isEntityAlive() && !mage.getFoci().isEmpty();
    }

    @Override
    public boolean shouldContinueExecuting() {
        EntityLivingBase t = mage.getAttackTarget();
        return t != null && t.isEntityAlive() && targetLostTicks < tuning().giveUpTicks;
    }

    @Override
    public void startExecuting() {
        targetLostTicks = 0;
        strafeFlipAt = 0;
        opening = true;
        retreatUntil = 0;
        closingIn = false;
        holdRolled = false;
        resetStrike();
        mage.getDefense().reset();
        mage.getTracker().invalidate();
        mage.getMelee().abort();
    }

    @Override
    public void resetTask() {
        mage.getNavigator().clearPath();
        mage.getMelee().abort();
    }

    /**
     * Plan 3.11 item 3: every few ticks, with two or more players around, re-score them (last attacker, low health, a player
     * casting, line of sight, distance) and keep the best, with a stickiness bonus so it does not flip every re-pick.
     */
    private void repickTarget(EntityLivingBase current, AiTuning t) {
        if (mage.ticksExisted % t.targetRepickTicks != 0) {
            return;
        }
        AxisAlignedBB box = mage.getEntityBoundingBox().grow(t.followRange, t.followRange * 0.5D, t.followRange);
        List<EntityPlayer> players = mage.world.getEntitiesWithinAABB(EntityPlayer.class, box);
        List<TargetScorer.Candidate> cands = new ArrayList<>(players.size());
        for (EntityPlayer p : players) {
            if (!p.isEntityAlive() || p.isCreative() || p.isSpectator()) {
                continue;
            }
            TargetScorer.Candidate c = new TargetScorer.Candidate(p.getEntityId());
            c.distance = mage.getDistance(p);
            c.healthFraction = p.getHealth() / Math.max(1.0F, p.getMaxHealth());
            c.hitMeLast = mage.getRevengeTarget() == p;
            c.busy = p.isHandActive() && p.getActiveItemStack().getItem() instanceof thaumcraft.common.items.casters.ItemCaster;
            c.hasLos = mage.getEntitySenses().canSee(p);
            c.current = p == current;
            cands.add(c);
        }
        if (cands.size() < 2) {
            return;
        }
        int id = TargetScorer.pick(cands);
        if (id >= 0 && id != current.getEntityId()) {
            net.minecraft.entity.Entity chosen = mage.world.getEntityByID(id);
            if (chosen instanceof EntityLivingBase) {
                mage.setAttackTarget((EntityLivingBase) chosen);
                mage.getTracker().invalidate();
            }
        }
    }

    /** This mage's resolved tuning (global, then its tier's overrides, then its variant's). */
    private AiTuning tuning() {
        if (ai == null) {
            ai = mage.getAi();
        }
        return ai;
    }

    @Override
    public void updateTask() {
        EntityLivingBase target = mage.getAttackTarget();
        if (target == null) {
            return;
        }
        AiTuning t = tuning();
        double dist = mage.getDistance(target);
        boolean los = mage.getEntitySenses().canSee(target);
        targetLostTicks = los ? 0 : targetLostTicks + 1;
        mage.getLookHelper().setLookPositionWithEntity(target, 30.0F, 30.0F);
        if (Stun.isStunned(mage)) {
            mage.getNavigator().clearPath();
            mage.getMelee().abort();
            if (strikeActive || strikeStartAt >= 0L || strikeExitAt >= 0L) {
                mage.getDefense().startPostStunEscape();
            }
            resetStrike();
            return;
        }
        BattlemageDefense defense = mage.getDefense();
        if (defense.postStunEscape(target, dist, t)) {
            return;
        }
        if (defense.act(target, dist, los)) {
            resetStrike();
            return;
        }
        if (defense.retreating()) {
            defense.retreatMove(target, t);
            return;
        }
        repickTarget(target, t);
        int now = mage.ticksExisted;
        if (strikeStep(target, t, now)) {
            return;
        }
        if (repositionBlink(target, dist, t, now)) {
            return;
        }
        if (mage.consumeBlinkFlag() && mage.getPhase() >= BattlemagePhase.DESPERATE) {
            opening = true;
        }
        boolean restricted = mage.surgeCastRestricted();
        updateCloseIn(target, dist, t);
        boolean decide = !restricted && now % t.decisionIntervalTicks == 0 && now >= mage.getNextCastTick();
        boolean acted = false;
        if (decide) {
            List<FocusSelector.Entry> entries = entries(target);
            acted = tryUpkeep(entries, target, t);
            if (!acted && mage.variantPreCast(target, dist, los)) {
                acted = true;
            }
            if (!acted && !mage.getMelee().active()) {
                acted = mage.handlesCasting() ? mage.variantCast(target, dist, los) : tryCast(entries, target, dist, los, t);
            }
        }
        if (!restricted && mage.variantStep(target, dist, los)) {
            return;
        }
        if (meleeStep(target, dist, (decide || restricted) && !acted, t)) {
            return;
        }
        move(target, dist, los, t);
    }

    private List<FocusSelector.Entry> entries(EntityLivingBase target) {
        List<BattlemageFocus> foci = mage.getFoci();
        List<FocusSelector.Entry> entries = new ArrayList<>(foci.size());
        boolean guarded = OpponentReading.shieldRaised(target, mage);
        for (BattlemageFocus f : foci) {
            FocusDef d = f.def;
            FocusSelector.Entry e = new FocusSelector.Entry(d.id, d.role, d.minRange, d.maxRange, d.requireLos,
                    d.baseWeight * mage.getRoleWeight(d.role) * OpponentReading.roleScale(d.role, guarded), d.opener, d.followUp);
            e.complexity = d.complexity;
            e.summonCount = d.summonCount;
            e.refireOnExpire = d.refireOnExpire;
            entries.add(e);
        }
        return entries;
    }

    /** Rule 6: keep Spell Bats active. Exempt from the cast budget, range and line of sight; only its own cooldown applies. */
    private boolean tryUpkeep(List<FocusSelector.Entry> entries, EntityLivingBase target, AiTuning t) {
        int castSize = FocusSelector.summonCastSize(entries);
        if (castSize <= 0) {
            return false;
        }
        int desired = SummonUpkeep.desired(t.desiredActiveSpellBats + mage.phaseExtraBats(), castSize);
        int pick = FocusSelector.pickUpkeep(entries, mage.getReadyAt(), mage.ticksExisted, mage.getTracker().bats(), desired);
        if (pick < 0) {
            return false;
        }
        mage.castFocus(pick, target, t);
        return true;
    }

    /** Rule 9: the best ready focus for the band, within the cast budget. */
    private boolean tryCast(List<FocusSelector.Entry> entries, EntityLivingBase target, double dist, boolean los, AiTuning t) {
        CastBudget budget = mage.getBudget();
        FocusSelector.Context ctx = new FocusSelector.Context();
        ctx.distance = dist;
        ctx.hasLos = los;
        ctx.tick = mage.ticksExisted;
        ctx.aoeMinRange = t.aoeMinRange;
        ctx.lastFocusId = mage.getLastFocusId();
        ctx.random = mage.getRNG();
        if (t.castBudgetEnabled) {
            ctx.energy = budget.energy();
            ctx.maxEnergy = budget.max();
            ctx.holding = budget.holding();
        } else {
            // Energy pool off (Bombardier): the context's defaults are unlimited energy and no hold.
            ctx.energy = Double.POSITIVE_INFINITY;
            ctx.maxEnergy = Double.POSITIVE_INFINITY;
            ctx.holding = false;
        }
        ctx.costPerComplexity = t.costPerComplexity;
        ctx.ownedClouds = mage.getTracker().clouds();
        ctx.opening = opening;
        addTargetStates(ctx.targetStates, target);
        int pick = FocusSelector.pick(entries, mage.getReadyAt(), ctx);
        if (pick < 0) {
            return false;
        }
        mage.castFocus(pick, target, t);
        opening = false;
        return true;
    }

    private static void addTargetStates(Set<String> states, EntityLivingBase target) {
        if (Stun.isStunned(target)) {
            states.add("stunned");
        }
        if (target.isPotionActive(MobEffects.SLOWNESS)) {
            states.add("slowed");
        }
        if (target.isWet()) {
            states.add("wet");
        }
        if (target.isBurning()) {
            states.add("burning");
        }
        if (target.isPotionActive(MobEffects.WEAKNESS)) {
            states.add("debuffed");
        }
    }

    /**
     * Rule 10: a burst of 2 to 3 weapon attacks when a player is in melee range and nothing better was cast, then a short
     * retreat. The weapon is in the main hand (its attributes give the damage); casts swing the off hand.
     *
     * @return true while a burst owns movement this tick
     */
    private boolean meleeStep(EntityLivingBase target, double dist, boolean mayStart, AiTuning t) {
        MeleeBurst burst = mage.getMelee();
        int now = mage.ticksExisted;
        if (!burst.active()) {
            if (!mayStart || dist > t.meleeTriggerRange || !burst.canStart(now)) {
                return false;
            }
            burst.start(now, MeleeBurst.plan(t.meleeBurstMin, t.meleeBurstMax, mage.getRNG()));
        }
        if (burst.finished(now, t.meleeBurstMaxTicks)) {
            endBurst(now, t, target);
            return false;
        }
        if (dist > t.meleeReach) {
            if (now - lastPathTick >= t.repositionIntervalTicks || mage.getNavigator().noPath()) {
                mage.getNavigator().tryMoveToEntityLiving(target, t.moveSpeed * 1.2D);
                lastPathTick = now;
            }
        } else {
            mage.getNavigator().clearPath();
            if (burst.attackDue(now)) {
                mage.swingArm(EnumHand.MAIN_HAND);
                mage.attackEntityAsMob(target);
                burst.onAttack(now, t.meleeAttackIntervalTicks);
                if (burst.finished(now, t.meleeBurstMaxTicks)) {
                    endBurst(now, t, target);
                }
            }
        }
        return true;
    }

    private void endBurst(int now, AiTuning t, EntityLivingBase target) {
        mage.getMelee().end(now, t.meleeBurstCooldownTicks);
        closingIn = false;
        if (strikeActive) {
            // The strike combo: stay a moment after the last blow, then blink out (see strikeStep).
            strikeExitAt = now + t.meleeStrikeOutWindowTicks;
            mage.getNavigator().clearPath();
            lastPathTick = -1000;
            return;
        }
        boolean strike = false;
        retreatUntil = now + t.meleeRetreatTicks;
        mage.getNavigator().clearPath();
        lastPathTick = -1000;
        mage.getDefense().afterMelee(target, t, strike);
    }

    /**
     * The timed reposition blink (plan 3.10): every {@code repositionBlinkIntervalTicks}, with {@code repositionBlinkChance},
     * a mage with Blink ready that is not in a burst, a combo or a volley blinks to a new spot in its firing band, which
     * puts distance between it and a player who crowds it, closes on one who kites, or flanks one who holds still.
     */
    private boolean repositionBlink(EntityLivingBase target, double dist, AiTuning t, int now) {
        if (t.repositionBlinkChance <= 0.0D || now < nextRepositionAt) {
            return false;
        }
        if (mage.handlesCasting() || mage.getMelee().active() || strikeActive || closingIn || mage.indexOfFocus("blink") < 0
                || !mage.blinkReady() || Stun.isStunned(mage)) {
            return false;
        }
        nextRepositionAt = now + t.repositionBlinkIntervalTicks;
        if (mage.getRNG().nextDouble() >= t.repositionBlinkChance) {
            return false;
        }
        double min = Math.min(t.idealMin, t.idealMax);
        if (BattlemageBlink.blinkToRange(mage, target, t, min, Math.max(min, t.idealMax))) {
            mage.getNavigator().clearPath();
            lastPathTick = -1000;
            return true;
        }
        nextRepositionAt = now + 40;
        return false;
    }

    private void resetStrike() {
        strikeActive = false;
        strikeStartAt = -1L;
        strikeExitAt = -1L;
    }

    /**
     * The strike combo's two waits: after the blink in the mage stands still for {@code strikeInWindowTicks} (the player's
     * reaction window), then the burst runs; after its last blow it stays {@code strikeOutWindowTicks} (a window to punish
     * it), then blinks out. @return true while a wait owns the tick
     */
    private boolean strikeStep(EntityLivingBase target, AiTuning t, int now) {
        if (strikeStartAt >= 0L) {
            mage.getNavigator().clearPath();
            if (now < strikeStartAt) {
                return true;
            }
            strikeStartAt = -1L;
            if (mage.getMelee().canStart(now)) {
                mage.getMelee().start(now, MeleeBurst.plan(t.meleeBurstMin, t.meleeBurstMax, mage.getRNG()));
            } else {
                strikeActive = false;
            }
            return false;
        }
        if (strikeExitAt >= 0L) {
            mage.getNavigator().clearPath();
            if (now < strikeExitAt) {
                return true;
            }
            strikeExitAt = -1L;
            strikeActive = false;
            if (mage.getDefense().afterMelee(target, t, true)) {
                return true;
            }
            retreatUntil = now + t.meleeRetreatTicks;
        }
        if (strikeActive && strikeStartAt < 0L && strikeExitAt < 0L && !mage.getMelee().active()) {
            strikeActive = false;
        }
        return false;
    }

    /**
     * The offensive blink (plan 3.10): port in next to the target, hit, and jump back out when the burst ends. Needs Blink
     * ready and the target within {@code meleeStrikeBlinkMaxRange}; {@code meleeStrikeBlinkChance} of the close-ins use it.
     */
    private boolean strikeBlink(EntityLivingBase target, double dist, AiTuning t) {
        if (dist > t.meleeStrikeBlinkMaxRange || dist <= t.meleeTriggerRange || mage.indexOfFocus("blink") < 0
                || !mage.blinkReady() || mage.getRNG().nextDouble() >= t.meleeStrikeBlinkChance) {
            return false;
        }
        if (!BattlemageBlink.blinkToRange(mage, target, t, 1.2D, Math.max(1.5D, t.meleeReach - 0.3D))) {
            return false;
        }
        strikeActive = true;
        strikeStartAt = mage.ticksExisted + t.meleeStrikeInWindowTicks;
        strikeExitAt = -1L;
        mage.getNavigator().clearPath();
        return true;
    }

    /**
     * While the cast budget holds the mage back it used to stand in its band doing nothing useful. Now, with
     * {@code meleeCloseInChance}, it walks in for a melee burst (then the usual blink or retreat takes it back out).
     * Rolled once per hold, and only when a burst is available and the target is near enough.
     */
    private void updateCloseIn(EntityLivingBase target, double dist, AiTuning t) {
        boolean holding = t.castBudgetEnabled && mage.getBudget().holding();
        if (!holding) {
            closingIn = false;
            holdRolled = false;
            return;
        }
        if (holdRolled) {
            return;
        }
        holdRolled = true;
        closingIn = t.meleeCloseInChance > 0.0D && mage.getRNG().nextDouble() < t.meleeCloseInChance
                && dist <= t.meleeCloseInMaxRange && mage.getMelee().canStart(mage.ticksExisted);
        if (closingIn && strikeBlink(target, dist, t)) {
            closingIn = false;
        }
    }

    private void move(EntityLivingBase target, double dist, boolean los, AiTuning t) {
        int tick = mage.ticksExisted;
        if (mage.getDefense().hazardMove(t)) {
            return;
        }
        double widen = mage.getShield().widenBand(mage) ? t.shieldBrokenBandBonus : 0.0D;
        BandSteering.Move move = tick < Math.max(retreatUntil, mage.getDefense().kiteUntil())
                ? BandSteering.Move.RETREAT
                : (closingIn && los && dist > t.meleeTriggerRange ? BandSteering.Move.APPROACH
                : BandSteering.decide(dist, t.idealMin + widen, t.idealMax + widen, los));
        boolean repath = tick - lastPathTick >= t.repositionIntervalTicks;
        switch (move) {
            case APPROACH:
                if (repath || mage.getNavigator().noPath()) {
                    mage.getNavigator().tryMoveToEntityLiving(target, t.moveSpeed);
                    lastPathTick = tick;
                }
                break;
            case RETREAT:
                if (repath || mage.getNavigator().noPath()) {
                    Vec3d away = RandomPositionGenerator.findRandomTargetBlockAwayFrom(mage, 8, 4, target.getPositionVector());
                    if (away != null) {
                        mage.getNavigator().tryMoveToXYZ(away.x, away.y, away.z, t.moveSpeed);
                    }
                    lastPathTick = tick;
                }
                break;
            case STRAFE:
                strafe(target, t, tick);
                break;
            case HOLD:
            default:
                mage.getNavigator().clearPath();
                break;
        }
    }

    private void strafe(EntityLivingBase target, AiTuning t, int tick) {
        if (tick >= strafeFlipAt) {
            strafeSign = mage.getRNG().nextBoolean() ? 1 : -1;
            strafeFlipAt = tick + t.strafeFlipMinTicks + mage.getRNG().nextInt(Math.max(1, t.strafeFlipMaxTicks - t.strafeFlipMinTicks + 1));
        }
        if (!mage.getNavigator().noPath() && tick - lastPathTick < t.repositionIntervalTicks) {
            return;
        }
        Vec3d toTarget = target.getPositionVector().subtract(mage.getPositionVector());
        Vec3d perp = new Vec3d(-toTarget.z, 0.0D, toTarget.x).normalize().scale(strafeSign * t.strafeStep);
        double x = mage.posX + perp.x;
        double z = mage.posZ + perp.z;
        BlockPos ground = new BlockPos(x, mage.posY - 1.0D, z);
        if (mage.world.isAirBlock(ground) && mage.world.isAirBlock(ground.down())) {
            strafeSign = -strafeSign;
            return;
        }
        mage.getNavigator().tryMoveToXYZ(x, mage.posY, z, t.moveSpeed);
        lastPathTick = tick;
    }
}
