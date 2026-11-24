package com.apocollis.aqtweaks.thaumcraft.battlemage;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig.BattlemageConfig;
import com.apocollis.aqtweaks.potion.Stun;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import thaumcraft.common.entities.projectile.EntityFocusCloud;
import thaumcraft.common.lib.SoundsTC;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * The Swarm Mage ({@code aqtweaks:cultist_swarm_mage}, plan 18.7): a Tier 1 variant that carries a fixed loadout (both
 * bats, Healing Aura, Flux Bolt, Prismatic Bolt, no area foci; the roller is skipped) with a x2 selection weight on bolts.
 *
 * <p>It brings an escort (library {@code variants[swarm_mage].escort}: 2 Crimson Knights, a Cleric and an Archer) that
 * drops out of a cosmetic {@link EntityEscortPortal} a few ticks after the mage spawns, once per mage ({@code EscortSpawned}
 * NBT; {@code Escort:0b} turns it off). It keeps the escort's uuids, makes them share its target, and heals them: an escort
 * below {@code healAllyHpFraction} inside {@code healAllyRange} gets a Healing Aura, with a blink next to it first when the
 * aura (a radius 2 cloud on the caster) would not reach ({@link AllyHealPlanner}, {@link BattlemageBlink}). Healing Aura has
 * role {@code heal_ally}, which the normal selector never picks, so this rule is the only thing that casts it.
 */
public class EntityCultistSwarmMage extends EntityCultistBattlemage {

    public static final String VARIANT_ID = "swarm_mage";
    private static final Logger LOG = LogManager.getLogger("AQTweaks-Battlemage");
    private static final String AURA_ID = "healing_aura";
    /** Walk toward a hurt escort for this many ticks per decision when Blink is on cooldown. */
    private static final int WALK_TICKS = 20;

    private final List<UUID> escortIds = new ArrayList<>();
    private boolean escortSpawned;
    private boolean escortDisabled;

    private boolean arrivalActive;
    private int arrivalElapsed;
    private int arrivalSpawned;
    private boolean portalValid;
    private double portalX;
    private double portalY;
    private double portalZ;
    private UUID portalId;

    private long healReadyAt;
    /** Healing Aura charges (plan: Swarm Mage area heal has its own pool, 5 charges, one back per 100 ticks). */
    private HealBudget auraBudget;
    private UUID walkAllyId;
    private long walkUntil;

    // Healing Aura guard (plan 20).
    private static final String KNOCK_BACK_ID = "knock_back";
    /** Radius of the scan for the mage's own aura cloud (the aura spawns on the mage, so it is always close). */
    private static final double CLOUD_SCAN_RADIUS = 12.0D;
    /** The cloud scan only runs this long after an aura cast (the cloud cannot outlive it). */
    private static final int AURA_SCAN_WINDOW_TICKS = 400;
    private final CloudGuard guard = new CloudGuard();
    private final CloudGuard.Params guardParams = new CloudGuard.Params();
    private long lastAuraCastTick = Long.MIN_VALUE / 2;
    private long guardTick = -1L;
    private CloudGuard.Action guardPending = CloudGuard.Action.NONE;
    private EntityPlayer guardPlayer;
    private UUID guardPrevPlayer;
    private double guardPrevCloudDist = Double.NaN;
    private int lastGuardPathTick = -1000;

    public EntityCultistSwarmMage(World world) {
        super(world);
    }

    @Override
    public int getTier() {
        return 1;
    }

    @Override
    public String getVariantId() {
        return VARIANT_ID;
    }

    // ---- escort ids, release on death (plan 21) ----

    /** Escort ids already reported as missing this session, so each warning appears once. */
    private static final java.util.Set<String> WARNED_MISSING = new java.util.HashSet<>();

    private static void warnMissingOnce(String entityId) {
        if (WARNED_MISSING.add(entityId)) {
            LOG.warn("Swarm Mage escort entity '{}' is not a registered mob; it is skipped. Check the escort list in cultist_battlemages.json"
                    + " (Thaumcraft ids are thaumcraft:cultistknight and thaumcraft:cultistcleric, no underscore).", entityId);
        }
    }

    /** At the first spawn: warns once for every escort id that is malformed or not in the entity registry. */
    private static void checkEscortIds(List<EscortDef> defs) {
        for (String id : EscortSpawner.unresolved(defs, new java.util.function.Predicate<String>() {
            @Override
            public boolean test(String s) {
                return net.minecraftforge.fml.common.registry.ForgeRegistries.ENTITIES.containsKey(new ResourceLocation(s));
            }
        })) {
            warnMissingOnce(id);
        }
    }

    /** The mage died: its loaded escorts lose the despawn protection, and the id is recorded for the unloaded ones. */
    @Override
    public void onDeath(net.minecraft.util.DamageSource cause) {
        releaseEscorts();
        super.onDeath(cause);
    }

    private boolean escortsReleased;

    private void releaseEscorts() {
        if (escortsReleased || this.world.isRemote) {
            return;
        }
        escortsReleased = true;
        if (escortIds.isEmpty()) {
            return;
        }
        for (EntityLivingBase e : liveEscorts()) {
            e.getEntityData().removeTag(EscortRelease.TAG);
        }
        EscortReleasedData data = EscortReleasedData.get(this.world);
        if (data != null) {
            data.release(this.getUniqueID(), this.world.getTotalWorldTime());
        }
    }

    // ---- escort arrival ----

    @Override
    protected void onInitialised(LibraryData lib, TierDef tier, VariantDef variant) {
        if (escortSpawned || escortDisabled || variant == null || variant.escort.isEmpty()) {
            return;
        }
        List<String> ids = EscortSpawner.expand(variant.escort);
        if (ids.isEmpty()) {
            return;
        }
        checkEscortIds(variant.escort);
        escortSpawned = true;
        arrivalActive = true;
        arrivalElapsed = 0;
        arrivalSpawned = 0;
        findPortalSpot();
        // No casting until the last escort has dropped: the portal beat comes first.
        setNextCastTick(Math.max(getNextCastTick(), this.ticksExisted + EscortSpawner.spawnTick(ids.size() - 1) + 5));
    }

    /** Looks for a floor spot 4 to 6 blocks away with room for the 3 block tall portal; none leaves the escort to drop beside the mage. */
    private void findPortalSpot() {
        portalValid = false;
        for (double angle : EscortSpawner.portalAngles(this.getRNG(), 12)) {
            double dist = EscortSpawner.portalDistance(this.getRNG());
            double x = this.posX + Math.cos(angle) * dist;
            double z = this.posZ + Math.sin(angle) * dist;
            double y = floorY(x, z);
            if (!Double.isNaN(y)) {
                portalValid = true;
                portalX = Math.floor(x) + 0.5D;
                portalY = y;
                portalZ = Math.floor(z) + 0.5D;
                return;
            }
        }
    }

    /** Feet height of solid ground with 3 free blocks above it near the mage's height, or NaN. */
    private double floorY(double x, double z) {
        int bx = (int) Math.floor(x);
        int bz = (int) Math.floor(z);
        int top = (int) Math.floor(this.posY) + 2;
        for (int y = top; y >= top - 6; y--) {
            BlockPos feet = new BlockPos(bx, y, bz);
            if (!this.world.isBlockLoaded(feet)) {
                return Double.NaN;
            }
            BlockPos below = feet.down();
            if (!this.world.getBlockState(below).isSideSolid(this.world, below, net.minecraft.util.EnumFacing.UP)) {
                continue;
            }
            for (int k = 0; k < 3; k++) {
                if (this.world.getBlockState(feet.up(k)).getMaterial().blocksMovement()
                        || this.world.getBlockState(feet.up(k)).getMaterial().isLiquid()) {
                    return Double.NaN;
                }
            }
            return y;
        }
        return Double.NaN;
    }

    @Override
    protected void onVariantTick() {
        if (arrivalActive) {
            tickArrival();
        }
        if (this.ticksExisted % 10 == 0) {
            syncEscortTargets();
        }
    }

    private void tickArrival() {
        LibraryData lib = FocusLibrary.data();
        VariantDef variant = lib.variant(VARIANT_ID);
        List<String> ids = variant != null ? EscortSpawner.expand(variant.escort) : new ArrayList<String>();
        if (portalValid && arrivalElapsed < EscortSpawner.PORTAL_LIFE_TICKS && !portalAlive()) {
            openPortal(arrivalElapsed == 0);
        }
        int due = EscortSpawner.dueNow(arrivalElapsed, arrivalSpawned, ids.size());
        for (int i = 0; i < due; i++) {
            spawnEscort(ids.get(arrivalSpawned));
            arrivalSpawned++;
        }
        arrivalElapsed++;
        if (EscortSpawner.arrivalFinished(arrivalElapsed, arrivalSpawned, ids.size())) {
            arrivalActive = false;
        }
    }

    private boolean portalAlive() {
        if (portalId == null || !(this.world instanceof WorldServer)) {
            return false;
        }
        Entity e = ((WorldServer) this.world).getEntityFromUuid(portalId);
        return e != null && !e.isDead;
    }

    private void openPortal(boolean fresh) {
        EntityEscortPortal portal = new EntityEscortPortal(this.world);
        portal.setLocationAndAngles(portalX, portalY, portalZ, this.getRNG().nextFloat() * 360.0F, 0.0F);
        portal.setLifeTicks(EscortSpawner.PORTAL_LIFE_TICKS - arrivalElapsed);
        if (this.world.spawnEntity(portal)) {
            portalId = portal.getUniqueID();
            if (fresh) {
                this.world.playSound(null, portalX, portalY, portalZ, SoundsTC.evilportal, SoundCategory.HOSTILE, 1.0F, 1.0F);
            }
        }
    }

    private void spawnEscort(String entityId) {
        Entity created = EntityList.createEntityByIDFromName(new ResourceLocation(entityId), this.world);
        if (!(created instanceof EntityLiving)) {
            warnMissingOnce(entityId);
            return;
        }
        EntityLiving escort = (EntityLiving) created;
        double x;
        double y;
        double z;
        if (portalValid) {
            x = portalX + (this.getRNG().nextDouble() - 0.5D);
            y = portalY;
            z = portalZ + (this.getRNG().nextDouble() - 0.5D);
        } else {
            double a = this.getRNG().nextDouble() * 2.0D * Math.PI;
            x = this.posX + Math.cos(a) * 2.5D;
            y = this.posY;
            z = this.posZ + Math.sin(a) * 2.5D;
        }
        escort.setLocationAndAngles(x, y, z, this.getRNG().nextFloat() * 360.0F, 0.0F);
        escort.onInitialSpawn(this.world.getDifficultyForLocation(new BlockPos(escort)), null);
        // Plan 21.2: tagged with its owner so BattlemageDespawnGuard denies despawning while the mage lives.
        escort.getEntityData().setString(EscortRelease.TAG, this.getUniqueID().toString());
        if (!this.world.spawnEntity(escort)) {
            return;
        }
        escortIds.add(escort.getUniqueID());
        EntityLivingBase target = this.getAttackTarget();
        if (target != null && target.isEntityAlive()) {
            escort.setAttackTarget(target);
        }
        if (this.world instanceof WorldServer) {
            ((WorldServer) this.world).spawnParticle(EnumParticleTypes.PORTAL, x, y + 1.0D, z, 25, 0.3D, 0.6D, 0.3D, 0.1D);
        }
        if (portalAlive()) {
            Entity p = ((WorldServer) this.world).getEntityFromUuid(portalId);
            if (p instanceof EntityEscortPortal) {
                ((EntityEscortPortal) p).pulse();
            }
        }
    }

    /** A withdrawing Swarm Mage heads for its nearest living escort (plan 22), so the retreat is toward help. */
    @Override
    public net.minecraft.util.math.Vec3d retreatAnchor() {
        EntityLivingBase best = null;
        double bestD = Double.MAX_VALUE;
        for (EntityLivingBase e : liveEscorts()) {
            double d = this.getDistanceSq(e);
            if (d < bestD) {
                bestD = d;
                best = e;
            }
        }
        return best != null ? best.getPositionVector() : null;
    }

    // ---- tracking and shared target ----

    /** The escorts that are loaded and alive, in the order they were recorded; forgets the ones confirmed dead. */
    private List<EntityLivingBase> liveEscorts() {
        List<EntityLivingBase> out = new ArrayList<>();
        if (!(this.world instanceof WorldServer)) {
            return out;
        }
        WorldServer ws = (WorldServer) this.world;
        Iterator<UUID> it = escortIds.iterator();
        while (it.hasNext()) {
            Entity e = ws.getEntityFromUuid(it.next());
            if (e == null) {
                continue; // not loaded right now: keep the uuid
            }
            if (e.isDead || !(e instanceof EntityLivingBase) || !((EntityLivingBase) e).isEntityAlive()) {
                it.remove();
                continue;
            }
            out.add((EntityLivingBase) e);
        }
        return out;
    }

    /** Escorts without a target take the mage's; with no target of its own the mage takes the player an escort is fighting. */
    private void syncEscortTargets() {
        if (escortIds.isEmpty()) {
            return;
        }
        List<EntityLivingBase> escorts = liveEscorts();
        EntityLivingBase mine = this.getAttackTarget();
        if (mine != null && mine.isEntityAlive()) {
            for (EntityLivingBase e : escorts) {
                if (e instanceof EntityLiving && ((EntityLiving) e).getAttackTarget() == null) {
                    ((EntityLiving) e).setAttackTarget(mine);
                }
            }
            return;
        }
        for (EntityLivingBase e : escorts) {
            EntityLivingBase attacker = e.getRevengeTarget();
            if (attacker instanceof EntityPlayer && attacker.isEntityAlive()) {
                this.setAttackTarget(attacker);
                return;
            }
        }
    }

    // ---- ally heal ----

    /**
     * Decision tick, after Spell Bat upkeep: first the ally heal (casting the aura outranks the guard), then the aura guard,
     * which keeps the normal casts and the normal melee burst off while it owns the mage (plan 20.3).
     */
    @Override
    public boolean variantPreCast(EntityLivingBase target, double dist, boolean los) {
        if (healAlly(target, dist, los)) {
            return true;
        }
        updateGuard(getAi());
        return guard.active() || guardPending != CloudGuard.Action.NONE;
    }

    /** Healing Aura on a hurt escort, blinking next to it first when needed. */
    private boolean healAlly(EntityLivingBase target, double dist, boolean los) {
        int aura = indexOfFocus(AURA_ID);
        if (aura < 0 || escortIds.isEmpty()) {
            return false;
        }
        AiTuning t = getAi();
        List<EntityLivingBase> escorts = liveEscorts();
        if (escorts.isEmpty()) {
            return false;
        }
        List<AllyHealPlanner.Ally> allies = new ArrayList<>(escorts.size());
        for (int i = 0; i < escorts.size(); i++) {
            EntityLivingBase e = escorts.get(i);
            allies.add(new AllyHealPlanner.Ally(i, e.getHealth(), e.getMaxHealth(), this.getDistance(e)));
        }
        AllyHealPlanner.Params p = new AllyHealPlanner.Params();
        p.hpFraction = t.healAllyHpFraction;
        p.range = t.healAllyRange;
        p.castDistance = t.healAllyCastDistance;
        AllyHealPlanner.State s = new AllyHealPlanner.State();
        s.now = this.ticksExisted;
        if (auraBudget == null) {
            auraBudget = new HealBudget(t.healAllyBudgetMax, t.healAllyBudgetRegenTicks);
        }
        auraBudget.tick(this.ticksExisted);
        s.healReadyAt = auraBudget.canHeal() ? healReadyAt : Long.MAX_VALUE;
        s.auraReady = getReadyAt()[aura] <= this.ticksExisted;
        s.blinkReady = blinkReady();
        s.stunned = Stun.isStunned(this);
        s.playerInMeleeRange = target instanceof EntityPlayer && dist <= t.meleeTriggerRange;
        AllyHealPlanner.Decision d = AllyHealPlanner.plan(allies, p, s);
        if (d.action == AllyHealPlanner.Action.NONE) {
            walkAllyId = null;
            return false;
        }
        EntityLivingBase ally = escorts.get(d.allyIndex);
        switch (d.action) {
            case CAST:
                walkAllyId = null;
                castFocus(aura, ally, t);
                lastAuraCastTick = this.ticksExisted;
                auraBudget.spend(this.ticksExisted);
                healReadyAt = this.ticksExisted + t.healAllyCooldownTicks;
                if (BattlemageConfig.debug) {
                    LOG.info("Swarm Mage healed an escort ({} hp of {})", ally.getHealth(), ally.getMaxHealth());
                }
                return true;
            case BLINK:
                return BattlemageBlink.blinkToAlly(this, ally, t);
            case WALK:
                walkAllyId = ally.getUniqueID();
                walkUntil = this.ticksExisted + WALK_TICKS;
                return false;
            default:
                return false;
        }
    }

    // ---- Healing Aura guard (plan 20) ----

    /**
     * Steps the guard once per tick (the first caller of the tick does the work, the result waits in {@code guardPending}
     * for {@link #variantStep}). Cheap when no aura was cast recently: no scan, no player search.
     */
    private void updateGuard(AiTuning t) {
        long now = this.ticksExisted;
        if (guardTick == now) {
            return;
        }
        if (guard.active() && now - guardTick > 20L) {
            guard.reset(); // the AI did not run for a while (Stun, no target): drop a stale sequence
        }
        guardTick = now;
        guardPending = CloudGuard.Action.NONE;
        guardPlayer = null;
        boolean castRecent = now - lastAuraCastTick <= t.cloudGuardCastTicks;
        boolean mayHaveCloud = now - lastAuraCastTick <= AURA_SCAN_WINDOW_TICKS;
        if (!mayHaveCloud && !guard.active()) {
            guardPrevPlayer = null;
            return;
        }
        Vec3d centre = null;
        EntityFocusCloud cloud = ownedCloud();
        if (cloud != null) {
            centre = new Vec3d(cloud.posX, cloud.posY, cloud.posZ);
        } else if (castRecent) {
            centre = new Vec3d(this.posX, this.posY + 1.0D, this.posZ);
        }
        CloudGuard.Input in = new CloudGuard.Input();
        in.now = now;
        in.stunned = Stun.isStunned(this);
        in.armed = centre != null;
        if (centre != null) {
            EntityPlayer player = nearestPlayerToCloud(centre, t.cloudGuardNearRange);
            if (player != null) {
                in.playerPresent = true;
                in.playerToCloud = player.getDistance(centre.x, centre.y, centre.z);
                in.playerToMage = this.getDistance(player);
                UUID id = player.getUniqueID();
                in.approaching = id.equals(guardPrevPlayer) && in.playerToCloud < guardPrevCloudDist - 0.02D;
                guardPrevPlayer = id;
                guardPrevCloudDist = in.playerToCloud;
                guardPlayer = player;
            } else {
                guardPrevPlayer = null;
            }
        }
        int kb = indexOfFocus(KNOCK_BACK_ID);
        in.knockBackReady = kb >= 0 && getReadyAt()[kb] <= now;
        in.burstActive = getMelee().active();
        guardParams.range = t.cloudGuardRange;
        guardParams.nearRange = t.cloudGuardNearRange;
        guardParams.meleeReach = t.meleeReach;
        guardParams.touchReach = BattlemageConfig.touchReach;
        guardParams.meleeMin = t.cloudGuardMeleeMin;
        guardParams.meleeMax = t.cloudGuardMeleeMax;
        guardParams.cooldownTicks = t.cloudGuardCooldownTicks;
        guardParams.closeTicks = t.cloudGuardCloseTicks;
        guardParams.maxTicks = t.cloudGuardCloseTicks + t.meleeBurstMaxTicks + 20;
        guardPending = guard.step(guardParams, in, this.getRNG());
    }

    /** The nearest cloud this mage owns (Healing Aura is the only cloud focus it carries), or null. */
    private EntityFocusCloud ownedCloud() {
        AxisAlignedBB box = this.getEntityBoundingBox().grow(CLOUD_SCAN_RADIUS);
        EntityFocusCloud best = null;
        double bestSq = Double.MAX_VALUE;
        for (EntityFocusCloud c : this.world.getEntitiesWithinAABB(EntityFocusCloud.class, box)) {
            if (c.isDead || c.getOwner() != this) {
                continue;
            }
            double sq = c.getDistanceSq(this);
            if (sq < bestSq) {
                bestSq = sq;
                best = c;
            }
        }
        return best;
    }

    /** The survival-mode player nearest the cloud centre who is also within {@code near} blocks of the mage. */
    private EntityPlayer nearestPlayerToCloud(Vec3d centre, double near) {
        EntityPlayer best = null;
        double bestSq = Double.MAX_VALUE;
        for (EntityPlayer p : this.world.playerEntities) {
            if (!p.isEntityAlive() || p.isSpectator() || p.isCreative() || this.getDistance(p) > near) {
                continue;
            }
            double sq = p.getDistanceSq(centre.x, centre.y, centre.z);
            if (sq < bestSq) {
                bestSq = sq;
                best = p;
            }
        }
        return best;
    }

    /** Carries out the guard's action for this tick. @return true while the guard owns the tick */
    private boolean guardStep() {
        updateGuard(getAi());
        CloudGuard.Action a = guardPending;
        guardPending = CloudGuard.Action.NONE;
        EntityPlayer player = guardPlayer;
        AiTuning t = getAi();
        if (player != null && (a != CloudGuard.Action.NONE || guard.active())) {
            this.getLookHelper().setLookPositionWithEntity(player, 30.0F, 30.0F);
        }
        switch (a) {
            case MELEE:
                getMelee().start(this.ticksExisted, guard.burstSize());
                return player != null && driveGuardBurst(player, t);
            case KNOCK_BACK:
                castGuardKnockBack(player, t);
                return true;
            case WALK:
                walkToward(player, t);
                return true;
            case NONE:
            default:
                if (player != null && guard.state() == CloudGuard.State.MELEE && getMelee().active()) {
                    return driveGuardBurst(player, t);
                }
                return false;
        }
    }

    /** Casts Knock Back at the player without delaying the next normal cast (the cast gap would otherwise start). */
    private void castGuardKnockBack(EntityPlayer player, AiTuning t) {
        int kb = indexOfFocus(KNOCK_BACK_ID);
        if (kb < 0 || player == null) {
            return;
        }
        long keep = getNextCastTick();
        castFocus(kb, player, t);
        setNextCastTick(keep);
    }

    /** The guard's melee burst: swing when due, end when it has made its strikes (no cooldown). @return true while it owns the tick */
    private boolean driveGuardBurst(EntityPlayer player, AiTuning t) {
        MeleeBurst burst = getMelee();
        int now = this.ticksExisted;
        if (burst.finished(now, t.meleeBurstMaxTicks)) {
            burst.end(now, 0);
            this.getNavigator().clearPath();
            return true;
        }
        if (this.getDistance(player) > t.meleeReach) {
            walkToward(player, t);
            return true;
        }
        this.getNavigator().clearPath();
        if (burst.attackDue(now)) {
            this.swingArm(EnumHand.MAIN_HAND);
            this.attackEntityAsMob(player);
            burst.onAttack(now, t.meleeAttackIntervalTicks);
            if (burst.finished(now, t.meleeBurstMaxTicks)) {
                burst.end(now, 0);
            }
        }
        return true;
    }

    private void walkToward(EntityPlayer player, AiTuning t) {
        if (player == null) {
            return;
        }
        int now = this.ticksExisted;
        if (this.getNavigator().noPath() || now - lastGuardPathTick >= t.repositionIntervalTicks) {
            this.getNavigator().tryMoveToEntityLiving(player, t.moveSpeed * 1.2D);
            lastGuardPathTick = now;
        }
    }

    /** The guard first (a player near the live aura is the urgent thing), then the walk to a hurt escort while Blink is on cooldown. */
    @Override
    public boolean variantStep(EntityLivingBase target, double dist, boolean los) {
        if (guardStep()) {
            return true;
        }
        if (walkAllyId == null || this.ticksExisted >= walkUntil || !(this.world instanceof WorldServer)) {
            walkAllyId = null;
            return false;
        }
        Entity e = ((WorldServer) this.world).getEntityFromUuid(walkAllyId);
        if (!(e instanceof EntityLivingBase) || !e.isEntityAlive() || this.getDistance(e) <= getAi().healAllyCastDistance) {
            walkAllyId = null;
            return false;
        }
        if (this.getNavigator().noPath() || this.ticksExisted % 10 == 0) {
            this.getNavigator().tryMoveToEntityLiving(e, getAi().moveSpeed);
        }
        return true;
    }

    // ---- save and load ----

    @Override
    protected void writeVariantNbt(NBTTagCompound tag) {
        if (escortDisabled) {
            tag.setBoolean("Escort", false);
        }
        tag.setBoolean("EscortSpawned", escortSpawned);
        NBTTagList ids = new NBTTagList();
        for (UUID id : escortIds) {
            ids.appendTag(new NBTTagString(id.toString()));
        }
        tag.setTag("EscortIds", ids);
        if (arrivalActive) {
            NBTTagCompound a = new NBTTagCompound();
            a.setInteger("Elapsed", arrivalElapsed);
            a.setInteger("Spawned", arrivalSpawned);
            a.setBoolean("PortalValid", portalValid);
            a.setDouble("X", portalX);
            a.setDouble("Y", portalY);
            a.setDouble("Z", portalZ);
            tag.setTag("EscortArrival", a);
        }
    }

    @Override
    protected void readVariantNbt(NBTTagCompound tag) {
        if (tag.hasKey("Escort") && !tag.getBoolean("Escort")) {
            escortDisabled = true;
        }
        escortSpawned = tag.getBoolean("EscortSpawned");
        escortIds.clear();
        NBTTagList ids = tag.getTagList("EscortIds", 8);
        for (int i = 0; i < ids.tagCount(); i++) {
            try {
                escortIds.add(UUID.fromString(ids.getStringTagAt(i)));
            } catch (IllegalArgumentException ex) {
                LOG.warn("Swarm Mage has a bad escort uuid in its save: {}", ids.getStringTagAt(i));
            }
        }
        if (tag.hasKey("EscortArrival", 10)) {
            NBTTagCompound a = tag.getCompoundTag("EscortArrival");
            arrivalActive = true;
            arrivalElapsed = a.getInteger("Elapsed");
            arrivalSpawned = a.getInteger("Spawned");
            portalValid = a.getBoolean("PortalValid");
            portalX = a.getDouble("X");
            portalY = a.getDouble("Y");
            portalZ = a.getDouble("Z");
            portalId = null; // the portal is not saved: tickArrival re-opens it with the life that is left
        }
    }
}
