package com.apocollis.aqtweaks.thaumcraft.battlemage;

/**
 * The numbers of the {@code ai} block (plan 4.1). Every value is data; none is hard-coded in the AI. The library holds
 * one global instance; every tier and every variant gets its own resolved copy ({@link #copy()}) with that entry's
 * {@code ai} overrides applied on top (schema 3), read through {@code EntityCultistBattlemage#getAi()}.
 */
public final class AiTuning implements Cloneable {
    public int decisionIntervalTicks = 2;
    public double idealMin = 8.0D;
    public double idealMax = 14.0D;
    /** Minimum ticks between two casts (plan 17.3: Tier 1 24, Tier 2 16). */
    public int globalCastGapTicks = 24;
    public int strafeFlipMinTicks = 20;
    public int strafeFlipMaxTicks = 50;
    public boolean leadShots = true;
    public int repositionIntervalTicks = 10;
    public int giveUpTicks = 200;
    public double aoeMinRange = 14.0D;
    public double followRange = 40.0D;
    public double moveSpeed = 1.0D;
    public int startupDelayTicks = 40;
    public double strafeStep = 3.0D;
    /** Blocks a projectile travels per tick, used only for shot lead. */
    public double projectileSpeed = 1.5D;
    public int maxLeadTicks = 8;

    // Cast budget and burst rhythm (plan 3.11 item 1, 3.13 item 3).
    /** False turns the energy pool off (the Bombardier Mage uses its volley rhythm instead, plan 19.7). */
    public boolean castBudgetEnabled = true;
    public double budgetMax = 100.0D;
    public double budgetRegenPerSecond = 10.0D;
    public double costPerComplexity = 1.0D;
    public double holdBelowFraction = 0.25D;
    public double resumeFraction = 0.6D;

    // Summon upkeep (plan 3.12).
    public int upkeepIntervalTicks = 20;
    public double upkeepRadius = 48.0D;
    /** 0 means auto: max(1, ceil(castSize / 2)). */
    public int desiredActiveSpellBats;

    // Melee burst (plan 3.2).
    public double meleeTriggerRange = 3.0D;
    public double meleeReach = 2.5D;
    public int meleeAttackIntervalTicks = 14;
    public int meleeBurstMin = 2;
    public int meleeBurstMax = 3;
    public int meleeBurstMaxTicks = 60;
    public int meleeBurstCooldownTicks = 100;
    public int meleeRetreatTicks = 30;
    /** Chance, rolled once when the cast budget starts holding back, that the mage closes in for a melee burst (0 turns it off). */
    public double meleeCloseInChance = 0.6D;
    /** The mage only closes in when the target is within this many blocks. */
    public double meleeCloseInMaxRange = 14.0D;
    /** Of the close-in rolls, the share that blinks next to the target instead of walking (needs Blink ready). */
    public double meleeStrikeBlinkChance = 0.5D;
    /** The strike blink is only used with the target within this many blocks. */
    public double meleeStrikeBlinkMaxRange = 24.0D;
    /** Ticks the mage stands still after blinking in before its first blow (the player's reaction window). */
    public int meleeStrikeInWindowTicks = 10;
    /** Ticks it stays after its last blow before blinking out (the player's punish window). */
    public int meleeStrikeOutWindowTicks = 10;

    // Crowd-control resistance (plan 3.13 item 6).
    public double stunDurationScale = 0.5D;
    public int stunImmunityTicks = 80;

    // Ally heal (Swarm Mage, plan 18.7). Read by the variant that fills the escort and heal hooks.
    /** An escort below this health fraction is healed. */
    public double healAllyHpFraction = 0.6D;
    /** Escorts farther than this from the mage are ignored. */
    public double healAllyRange = 16.0D;
    /** Healing Aura is a radius-2 cloud on the caster: cast it when the ally is this close, otherwise blink next to it. */
    public double healAllyCastDistance = 2.5D;
    public int healAllyCooldownTicks = 60;
    /** The Swarm Mage's own pool of Healing Aura charges, separate from the self-heal budget: max and ticks per charge back. */
    public int healAllyBudgetMax = 5;
    public int healAllyBudgetRegenTicks = 100;

    // Healing Aura guard (Swarm Mage, plan 20): melee then Knock Back when a player closes on the mage's own heal cloud.
    /** A hostile player this close to the aura cloud's centre triggers the guard (the cloud radius 2 plus 1.5). */
    public double cloudGuardRange = 3.5D;
    /** ...and the mage must be this close to that player ("near"); beyond Knock Back's reach it walks in. */
    public double cloudGuardNearRange = 6.0D;
    /** The guard stays armed this many ticks after the mage casts the aura even if the cloud is not seen yet. */
    public int cloudGuardCastTicks = 20;
    public int cloudGuardMeleeMin = 1;
    public int cloudGuardMeleeMax = 2;
    /** Minimum ticks between two guard sequences. */
    public int cloudGuardCooldownTicks = 30;
    /** At most this many ticks of walking toward a player who is near the cloud but beyond Knock Back's reach. */
    public int cloudGuardCloseTicks = 20;

    // Blink (plan 3.10, shared by every mage that blinks): one cooldown bucket for all blink modes.
    /** Shared blink cooldown in ticks (plan 3.10 point 4: about 5 s). */
    public int blinkCooldownTicks = 100;
    /** Timed reposition blink (plan 3.10): every this many ticks the mage may blink just to change range. */
    public int repositionBlinkIntervalTicks = 200;
    /** Chance, rolled each interval, that it does. 0 turns it off. */
    public double repositionBlinkChance = 0.4D;
    /** Candidate points sampled per blink. */
    public int blinkSamples = 16;
    /** A blink never travels farther than this from where the mage stands. */
    public double blinkMaxTravel = 24.0D;
    /** {@code TOWARD_ALLY} ring around the ally: the mage lands this far from it at the least and the most. */
    public double blinkAllyMinDistance = 1.0D;
    public double blinkAllyMaxDistance = 2.0D;

    /** {@code REPOSITION_BAND} ring around the target (plan 19.8): the mage lands this far from the target at the least and the most. */
    public double blinkBandMinDistance = 8.0D;
    public double blinkBandMaxDistance = 14.0D;
    /** Extra blocks added to a cloud's radius when a blink must stay out of it. */
    public double cloudAvoidMargin = 0.75D;
    /** Score cost of landing inside an enemy cloud or mine (a soft cost: the least bad spot is still taken). */
    public double enemyCloudCost = 6.0D;

    // Volley casting (Bombardier Mage, plan 19.7).
    public int volleyGapTicks = 30;
    public int volleyPauseTicks = 80;
    public int volleyMaxTicks = 200;

    // Close-range response (Bombardier Mage, plan 19.8): melee 1 to 2 times, Knock Back, then blink.
    public int closeMeleeMin = 1;
    public int closeMeleeMax = 2;
    /** The whole close sequence gives up and starts over after this many ticks, so it can never wedge. */
    public int closeSequenceMaxTicks = 160;

    // ---- Stage 3 (plan 22): shield, heal budget, retreat, peel, sidestep, defensive blink, rift, unstick ----

    /** Runic shield (plan 3.7): +1 point per interval, the wait after it breaks, and how it widens the band. */
    public int shieldRechargeIntervalTicks = 40;
    public int shieldBreakWaitTicks = 40;
    public boolean shieldStartFull = true;
    /** While the shield is broken or still below {@link #shieldResumeFraction}, the preferred band moves out by this much. */
    public double shieldBrokenBandBonus = 3.0D;
    public double shieldResumeFraction = 0.5D;
    /** A hit this fraction of max health (or one that breaks the shield) staggers the mage briefly. */
    public int staggerTicks = 10;
    public double staggerDamageFraction = 0.15D;

    /** Heal charges (plan 3.12): a small regenerating pool, separate from the cast budget. */
    public int healBudgetMax = 3;
    public int healBudgetRegenTicks = 200;
    /** Heal in place below this health fraction. */
    public double healHpFraction = 0.6D;
    /** Withdraw to heal at or below this fraction; return at {@link #retreatResumeFraction} or after the max ticks. */
    public double retreatHpFraction = 0.5D;
    public double retreatResumeFraction = 0.75D;
    public int retreatMaxTicks = 160;
    public int retreatCooldownTicks = 200;

    /** Peel (Knock Back) when a player is this close; own cooldown so melee bursts still happen. */
    public double peelRange = 3.0D;
    public int peelCooldownTicks = 100;
    /** Sidestep a projectile or a drawn bow aimed at the mage. */
    public int sidestepCooldownTicks = 40;
    public double sidestepRange = 12.0D;
    public double sidestepImpulse = 0.55D;

    /** Strict defensive blink (plan 3.10): health below this, player within the close range, under pressure, Knock Back spent. */
    public double awayHpFraction = 0.25D;
    /** Below this health fraction, a mage exposed in a Ley-Rupture surge may only heal, blink, rift and melee (plan 6.15). */
    public double surgeLowHealthFraction = 0.10D;
    public double awayCloseRange = 4.0D;
    public int awayPressureTicks = 30;
    public int awayKnockBackWindowTicks = 100;
    public double awayBandMinDistance = 10.0D;
    public double awayBandMaxDistance = 18.0D;
    /** Chance to blink back to the firing band after a melee burst when Blink is ready. */
    public double blinkAfterMeleeChance = 0.5D;
    /** With no line of sight for this long the mage blinks to a spot that has one. */
    public int noLosBlinkTicks = 60;

    /** Miniboss phases (plan 3.13 item 2) and the reset rule (item 10). */
    public double phaseEmpoweredBelow = 0.66D;
    public double phaseDesperateBelow = 0.33D;
    public double phaseEmpoweredCooldownScale = 0.75D;
    public double phaseDesperateCooldownScale = 0.60D;
    public double phaseEmpoweredRegenScale = 1.25D;
    public double phaseDesperateRegenScale = 1.50D;
    public int phaseEmpoweredExtraBats = 1;
    public int phaseDesperateExtraBats = 1;
    public int phaseDesperateBlinkCooldownTicks = 60;
    public double phaseDesperateHealRegenFactor = 0.75D;
    /** With no player within this many blocks for this long, the mage resets to full. 0 turns the reset off. */
    public double resetRange = 48.0D;
    public int resetAfterTicks = 600;

    /** Rift (plan 3.14). */
    public boolean riftEnabled = true;
    public int riftMaxThickness = 12;
    public int riftCooldownMinTicks = 300;
    public int riftCooldownMaxTicks = 400;
    public int riftMaxHolesPerCast = 24;
    public double riftDetourFactor = 2.5D;
    public int riftPathProbeIntervalTicks = 20;
    public int riftHoleDurationTicks = 120;

    /** Stuck in a wall (plan 3.14): out at once after this many ticks inside a block, searching this far. */
    public int suffocationTicks = 3;
    public double emergencyRadius = 16.0D;

    /** Target re-pick (plan 3.11 item 3): every this many ticks, with a stickiness bonus for the current target. */
    public int targetRepickTicks = 20;

    /** Independent copy; every field is a primitive. */
    public AiTuning copy() {
        try {
            return (AiTuning) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException(e);
        }
    }
}
