package com.apocollis.aqtweaks.thaumcraft.battlemage;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig.BattlemageConfig;
import com.apocollis.aqtweaks.potion.IStunResistant;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAIWanderAvoidWater;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EnumHand;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.BossInfo;
import net.minecraft.world.BossInfoServer;
import net.minecraft.world.WorldServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import thaumcraft.api.casters.FocusEngine;
import thaumcraft.api.casters.FocusPackage;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.common.entities.ai.combat.AICultistHurtByTarget;
import thaumcraft.common.entities.monster.cult.EntityCultist;
import thaumcraft.common.items.casters.ItemCaster;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The Crimson Battlemage (plan section 3, 18.8): the stock Crimson Cultist body that casts real Thaumcraft focus packages.
 * This class is the shared base and is itself the registered Tier 1 Battlemage ({@code aqtweaks:cultist_battlemage}).
 * The other entities are thin subclasses that change {@link #getTier()} and {@link #getVariantId()}:
 * {@link EntityCultistMasterBattlemage} (Tier 2), {@link EntityCultistSwarmMage} (Tier 1 variant) and
 * {@link EntityCultistBombardierMage} (Tier 2 variant). There is no tier or archetype NBT: the entity id is the identity.
 *
 * <p>It carries a loadout (rolled, or the variant's fixed list, or an explicit {@code Foci} NBT list), fights with
 * {@link AIBattlemageCombat} and never spends vis. Weapon in the main hand, a Thaumcraft caster in the off hand; the caster
 * shows a light display focus so its orb colour follows the active spell (vanilla syncs changed equipment to clients).
 *
 * <p>Variant hooks (all no-ops here, overridden by variants): {@link #onInitialised}, {@link #onVariantTick},
 * {@link #writeVariantNbt}, {@link #readVariantNbt}, {@link #variantStep}, {@link #variantPreCast},
 * {@link #handlesCasting} and {@link #variantCast}.
 */
public class EntityCultistBattlemage extends EntityCultist implements IStunResistant {

    private static final UUID MELEE_SCALE_ID = UUID.fromString("5c2f7b0e-8a31-4c64-9d57-0b7a3e1f6a42");
    private static final Logger LOG = LogManager.getLogger("AQTweaks-Battlemage");

    private final List<BattlemageFocus> foci = new ArrayList<>();
    private long[] readyAt = new long[0];
    private boolean initialised;
    private long nextCastTick;
    private long blinkReadyAt;
    private String lastFocusId;
    private CastBudget budget;
    private AiTuning ai;
    private final OwnedSummonTracker tracker = new OwnedSummonTracker();
    private final MeleeBurst melee = new MeleeBurst();
    private final StunGate stunGate = new StunGate();
    /** Set from spawn NBT {Foci:[...]}: bypasses the roll (plan 3.15, explicit loadouts win). */
    private List<String> explicitFoci;
    private boolean meleeScaleApplied;
    private BattlemageShield shield;
    private HealBudget healBudget;
    private BattlemageDefense defense;
    private long riftReadyAt;
    private int stuckTicks;
    private boolean stuckWarned;
    /** Miniboss phase (plan 3.13 item 2), 0 normal, 1 Empowered, 2 Desperate; only ever rises until the reset rule. */
    private int phase;
    private BossInfoServer bossInfo;
    private long lastPlayerNearTick;

    public EntityCultistBattlemage(World world) {
        super(world);
        this.experienceValue = 40;
        this.enablePersistence();
    }

    /** Never despawns on its own (plan 21.2); {@link BattlemageDespawnGuard} also denies the Forge despawn event. */
    @Override
    protected boolean canDespawn() {
        return false;
    }

    @Override
    protected void initEntityAI() {
        this.tasks.addTask(0, new EntityAISwimming(this));
        this.tasks.addTask(1, new AIBattlemageCombat(this));
        this.tasks.addTask(7, new EntityAIWanderAvoidWater(this, 0.8D));
        this.tasks.addTask(8, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
        this.tasks.addTask(8, new EntityAILookIdle(this));
        this.targetTasks.addTask(1, new AICultistHurtByTarget(this, true));
        this.targetTasks.addTask(2, new EntityAINearestAttackableTarget<>(this, EntityPlayer.class, true));
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(40.0D);
    }

    @Override
    protected void setLoot(DifficultyInstance difficulty) {
        // Equipment comes from the tier data in ensureInitialised, not the stock cultist loot.
    }

    /** The tier (1 or 2): fixed per entity class. Tier data (body, equipment, drops, roller) comes from the library. */
    public int getTier() {
        return 1;
    }

    /** Library variant id ({@code swarm_mage}, {@code bombardier_mage}), or null for the plain Battlemage and Master Battlemage. */
    public String getVariantId() {
        return null;
    }

    /** The variant's library entry, or null. */
    public VariantDef getVariant() {
        return FocusLibrary.data().variant(getVariantId());
    }

    /** This mage's resolved tuning: global {@code ai}, then its tier's overrides, then its variant's (schema 3). */
    public AiTuning getAi() {
        if (ai == null) {
            ai = FocusLibrary.data().aiFor(getTier(), getVariantId());
        }
        return ai;
    }

    /** Selection weight multiplier for a focus role (the variant's {@code weights}, 1.0 for everyone else). */
    public double getRoleWeight(String role) {
        VariantDef v = getVariant();
        return v != null ? v.roleWeight(role) : 1.0D;
    }

    /** The cast power multiplier of this mage's tier, or of its variant when it overrides it. */
    public double getFocusPower() {
        TierDef td = FocusLibrary.data().tier(getTier());
        VariantDef v = getVariant();
        double base = v != null && v.focusPower != null ? v.focusPower : (td != null ? td.focusPower : 1.0D);
        return base * BattlemageConfig.focusDamageScale;
    }

    public List<BattlemageFocus> getFoci() {
        return foci;
    }

    public long[] getReadyAt() {
        return readyAt;
    }

    public long getNextCastTick() {
        return nextCastTick;
    }

    /** Delays the next decision tick (the gap between two casts); variants use it to hold their rhythm. */
    public void setNextCastTick(long tick) {
        this.nextCastTick = tick;
    }

    public String getLastFocusId() {
        return lastFocusId;
    }

    /** Earliest tick (entity age) the next blink may happen: one cooldown bucket for every blink mode (plan 3.10). */
    public long getBlinkReadyAt() {
        return blinkReadyAt;
    }

    public void setBlinkReadyAt(long tick) {
        this.blinkReadyAt = tick;
    }

    public boolean blinkReady() {
        return this.ticksExisted >= blinkReadyAt;
    }

    /** Index of a carried focus by id, or -1. */
    public int indexOfFocus(String id) {
        for (int i = 0; i < foci.size(); i++) {
            if (foci.get(i).def.id.equals(id)) {
                return i;
            }
        }
        return -1;
    }

    /** The cast budget, created on first use from this mage's castBudget numbers. */
    public CastBudget getBudget() {
        if (budget == null) {
            AiTuning t = getAi();
            budget = new CastBudget(t.budgetMax, t.budgetRegenPerSecond, t.holdBelowFraction, t.resumeFraction);
        }
        return budget;
    }

    public OwnedSummonTracker getTracker() {
        return tracker;
    }

    public MeleeBurst getMelee() {
        return melee;
    }

    public BattlemageShield getShield() {
        if (shield == null) {
            shield = new BattlemageShield();
        }
        return shield;
    }

    public HealBudget getHealBudget() {
        if (healBudget == null) {
            AiTuning t = getAi();
            healBudget = new HealBudget(t.healBudgetMax, t.healBudgetRegenTicks);
        }
        return healBudget;
    }

    public BattlemageDefense getDefense() {
        if (defense == null) {
            defense = new BattlemageDefense(this);
        }
        return defense;
    }

    /** Earliest tick (entity age) the next Rift may happen: its own cooldown, separate from Blink (plan 3.14). */
    public long getRiftReadyAt() {
        return riftReadyAt;
    }

    public void setRiftReadyAt(long tick) {
        this.riftReadyAt = tick;
    }

    /** The runic shield capacity: the variant's when it sets one, else the tier's. */
    public double getShieldMax() {
        TierDef td = FocusLibrary.data().tier(getTier());
        VariantDef v = getVariant();
        if (v != null && v.shieldMax != null) {
            return v.shieldMax;
        }
        return td != null ? td.shieldMax : 0.0D;
    }

    /** A brief pause in casting after a heavy hit or a broken shield (plan 3.5). */
    public void stagger(int ticks) {
        this.nextCastTick = Math.max(this.nextCastTick, this.ticksExisted + ticks);
    }

    /** True when this mage uses the generic Knock Back peel (the Bombardier has its own close sequence). */
    /** Surge low-health rule (plan 6.15): only heal, blink, rift and melee remain. */
    public boolean surgeCastRestricted() {
        AiTuning t = getAi();
        return SurgeCastRules.restricted(
                com.apocollis.aqtweaks.thaumcraft.OverchannelHooks.overchanneling(this),
                getHealth() / Math.max(1.0F, getMaxHealth()), t.surgeLowHealthFraction);
    }

    public boolean usesGenericPeel() {
        return true;
    }

    /** Where a withdrawing mage heads, or null to just move away from the target (the Swarm Mage retreats to its escorts). */
    public net.minecraft.util.math.Vec3d retreatAnchor() {
        return null;
    }

    /** True when this mage may withdraw to heal at low health (the Bombardier holds its ground at range). */
    public boolean canRetreat() {
        return true;
    }

    @Override
    public int adjustStun(int ticks) {
        AiTuning t = getAi();
        return stunGate.adjust(this.ticksExisted, ticks, t.stunDurationScale, t.stunImmunityTicks);
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (this.world.isRemote) {
            return;
        }
        if (!initialised) {
            ensureInitialised();
        }
        if (!meleeScaleApplied) {
            applyMeleeScale();
        }
        if (initialised) {
            AiTuning t = getAi();
            getBudget().regen(this.ticksExisted);
            if (this.getAttackTarget() != null) {
                tracker.tick(this, this.ticksExisted, t.upkeepIntervalTicks, t.upkeepRadius);
            }
            getShield().tick(this);
            checkStuck(t);
            miniboss(t);
            capGroundSpeed();
            onVariantTick();
        }
    }

    /**
     * No faster than a walking player (plan: user, 2026-11-20). The speed attribute does not map cleanly to a player's
     * (a mob's input and the navigator multipliers differ), so the real horizontal step of the tick is limited: over the
     * cap the step is shortened along its own path and the velocity scaled. A teleport (blink, rift) moves further than a
     * walk could, so steps over 2 blocks are left alone; so are hits (knockback), the air and water (dodge hops, falls).
     */
    private void capGroundSpeed() {
        double cap = BattlemageConfig.maxMoveSpeed / 20.0D;
        if (cap <= 0.0D || !this.onGround || this.hurtTime > 0 || this.isInWater() || this.isRiding()) {
            return;
        }
        double dx = this.posX - this.prevPosX;
        double dz = this.posZ - this.prevPosZ;
        double step = Math.sqrt(dx * dx + dz * dz);
        if (step <= cap * 1.02D || step > 2.0D) {
            return;
        }
        double ratio = cap / step;
        this.setPosition(this.prevPosX + dx * ratio, this.posY, this.prevPosZ + dz * ratio);
        this.motionX *= ratio;
        this.motionZ *= ratio;
    }

    // ---- miniboss profile (plan 3.13, Stage 4) ----

    private boolean blinkedFlag;

    /** Called by the blink code on every successful blink. */
    public void noteBlink() {
        blinkedFlag = true;
    }

    /** True once after a blink; the combat AI uses it to open with the area focus in the Desperate phase. */
    public boolean consumeBlinkFlag() {
        boolean b = blinkedFlag;
        blinkedFlag = false;
        return b;
    }

    public int getPhase() {
        return phase;
    }

    public double phaseCooldownScale() {
        AiTuning t = getAi();
        return BattlemagePhase.cooldownScale(phase, t.phaseEmpoweredCooldownScale, t.phaseDesperateCooldownScale);
    }

    public int phaseExtraBats() {
        AiTuning t = getAi();
        return BattlemagePhase.extraBats(phase, t.phaseEmpoweredExtraBats, t.phaseDesperateExtraBats);
    }

    /** The Blink cooldown for this phase (Desperate caps it). */
    public int effectiveBlinkCooldown(AiTuning t) {
        return BattlemagePhase.blinkCooldown(phase, t.blinkCooldownTicks, t.phaseDesperateBlinkCooldownTicks);
    }

    private void miniboss(AiTuning t) {
        if (this.ticksExisted % 10 == 0) {
            updatePhase(t);
        }
        if (BattlemageConfig.bossBar) {
            updateBossBar();
        }
        resetCheck(t);
    }

    private void updatePhase(AiTuning t) {
        int next = BattlemagePhase.next(phase, this.getHealth() / Math.max(1.0F, this.getMaxHealth()),
                t.phaseEmpoweredBelow, t.phaseDesperateBelow);
        if (next == phase) {
            return;
        }
        phase = next;
        applyPhase(t);
        if (BattlemageConfig.shieldEnabled && getShieldMax() > 0.0D) {
            this.setAbsorptionAmount((float) getShieldMax());
        }
        phaseCue();
    }

    /** Pushes the phase numbers into the pools that hold them. */
    private void applyPhase(AiTuning t) {
        getBudget().setRegenScale(BattlemagePhase.regenScale(phase, t.phaseEmpoweredRegenScale, t.phaseDesperateRegenScale));
        HealBudget hb = getHealBudget();
        hb.setRegenTicks(BattlemagePhase.healRegenTicks(phase, hb.baseRegenTicks(), t.phaseDesperateHealRegenFactor));
        if (bossInfo != null) {
            bossInfo.setColor(phase >= BattlemagePhase.DESPERATE ? BossInfo.Color.WHITE
                    : (phase >= BattlemagePhase.EMPOWERED ? BossInfo.Color.PURPLE : BossInfo.Color.RED));
        }
    }

    /** A sound, a particle ring and a line to nearby players so the phase change is obvious. */
    private void phaseCue() {
        if (!(this.world instanceof WorldServer)) {
            return;
        }
        WorldServer w = (WorldServer) this.world;
        w.playSound(null, this.posX, this.posY, this.posZ, SoundEvents.ENTITY_ENDERDRAGON_GROWL, SoundCategory.HOSTILE, 0.8F, 1.4F);
        for (int i = 0; i < 32; i++) {
            double a = i * (Math.PI * 2.0D / 32.0D);
            w.spawnParticle(EnumParticleTypes.SPELL_WITCH, this.posX + Math.cos(a) * 2.0D, this.posY + 1.0D,
                    this.posZ + Math.sin(a) * 2.0D, 2, 0.0D, 0.2D, 0.0D, 0.05D);
        }
        for (EntityPlayer p : w.getEntitiesWithinAABB(EntityPlayer.class, this.getEntityBoundingBox().grow(48.0D))) {
            p.sendStatusMessage(new TextComponentTranslation("aqtweaks.battlemage.phase." + phase, this.getDisplayName()), true);
        }
    }

    private void updateBossBar() {
        if (bossInfo == null) {
            bossInfo = new BossInfoServer(this.getDisplayName(), BossInfo.Color.RED, BossInfo.Overlay.PROGRESS);
            applyPhase(getAi());
        }
        bossInfo.setPercent(this.getHealth() / Math.max(1.0F, this.getMaxHealth()));
    }

    @Override
    public void addTrackingPlayer(EntityPlayerMP player) {
        super.addTrackingPlayer(player);
        if (BattlemageConfig.bossBar) {
            if (bossInfo == null) {
                updateBossBar();
            }
            bossInfo.addPlayer(player);
        }
    }

    @Override
    public void removeTrackingPlayer(EntityPlayerMP player) {
        super.removeTrackingPlayer(player);
        if (bossInfo != null) {
            bossInfo.removePlayer(player);
        }
    }

    @Override
    public void setCustomNameTag(String name) {
        super.setCustomNameTag(name);
        if (bossInfo != null) {
            bossInfo.setName(this.getDisplayName());
        }
    }

    /** Reset rule (plan 3.13 item 10): no player within the range for a while and the fight is not fresh, so start over. */
    private void resetCheck(AiTuning t) {
        if (t.resetRange <= 0.0D) {
            return;
        }
        if (lastPlayerNearTick == 0L || this.world.getClosestPlayerToEntity(this, t.resetRange) != null) {
            lastPlayerNearTick = this.ticksExisted;
            return;
        }
        if (this.ticksExisted - lastPlayerNearTick < t.resetAfterTicks) {
            return;
        }
        boolean worn = this.getHealth() < this.getMaxHealth() - 0.01F || phase > 0
                || (getShieldMax() > 0.0D && this.getAbsorptionAmount() < getShieldMax() - 0.01D);
        lastPlayerNearTick = this.ticksExisted;
        if (!worn) {
            return;
        }
        this.setHealth(this.getMaxHealth());
        if (BattlemageConfig.shieldEnabled && getShieldMax() > 0.0D) {
            this.setAbsorptionAmount((float) getShieldMax());
        }
        phase = 0;
        budget = null;
        healBudget = null;
        applyPhase(t);
        this.setAttackTarget(null);
        getDefense().reset();
    }

    /** Stuck in a wall (plan 3.14): out at once after a few ticks inside a block, ignoring cooldown, Stun and health. */
    private void checkStuck(AiTuning t) {
        if (!this.isEntityInsideOpaqueBlock()) {
            stuckTicks = 0;
            return;
        }
        stuckTicks++;
        if (stuckTicks < t.suffocationTicks) {
            return;
        }
        if (BattlemageBlink.unstick(this, t.emergencyRadius)) {
            stuckTicks = 0;
        } else if (!stuckWarned) {
            stuckWarned = true;
            LOG.warn("{} is inside a block at {} and found no safe spot within {} blocks", this.getClass().getSimpleName(), this.getPosition(), t.emergencyRadius);
        }
    }

    /** Applies Battlemage Melee Damage Scale as an unsaved multiplier on the attack damage attribute. */
    private void applyMeleeScale() {
        meleeScaleApplied = true;
        double scale = BattlemageConfig.meleeDamageScale;
        IAttributeInstance attack = this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE);
        if (attack == null || scale == 1.0D || attack.getModifier(MELEE_SCALE_ID) != null) {
            return;
        }
        attack.applyModifier(new AttributeModifier(MELEE_SCALE_ID, "aqtweaks battlemage melee scale", scale - 1.0D, 2).setSaved(false));
    }

    /** Builds the tier body, equipment and loadout once (first server tick, so /summon with NBT works too). */
    private void ensureInitialised() {
        initialised = true;
        LibraryData lib = FocusLibrary.data();
        TierDef tier = lib.tier(getTier());
        VariantDef variant = lib.variant(getVariantId());
        AiTuning t = getAi();
        if (tier != null) {
            double health = variant != null && variant.health != null ? variant.health : tier.health;
            double armor = variant != null && variant.armor != null ? variant.armor : tier.armor;
            this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(health);
            this.getEntityAttribute(SharedMonsterAttributes.ARMOR).setBaseValue(armor);
            this.setHealth((float) health);
            this.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(t.followRange);
            equip(tier, variant);
        }
        for (EntityEquipmentSlot slot : EntityEquipmentSlot.values()) {
            this.setDropChance(slot, 0.0F);
        }
        buildLoadout(chooseLoadout(lib, tier, variant));
        if (BattlemageConfig.shieldEnabled && t.shieldStartFull && getShieldMax() > 0.0D) {
            this.setAbsorptionAmount((float) getShieldMax());
        }
        nextCastTick = this.ticksExisted + t.startupDelayTicks;
        if (BattlemageConfig.soulSightPotion) {
            applySoulSight();
        }
        onInitialised(lib, tier, variant);
    }

    /**
     * Equipment from the tier (plus the variant's slot overrides): item, optional SNBT tag, optional enchantments by
     * registry id, then the stack's Quality Tools modifiers applied straight to the mob ({@link GearAttributes}). The off
     * hand must be a Thaumcraft caster; anything else (or no entry) falls back to the basic caster.
     */
    private void equip(TierDef tier, VariantDef variant) {
        Map<String, EquipmentDef> eq = new LinkedHashMap<>(tier.equipment);
        if (variant != null) {
            eq.putAll(variant.equipment);
        }
        boolean offhandSet = false;
        for (Map.Entry<String, EquipmentDef> e : eq.entrySet()) {
            EntityEquipmentSlot slot = slotFor(e.getKey());
            if (slot == null) {
                continue;
            }
            ItemStack stack = buildStack(e.getKey(), e.getValue());
            if (stack.isEmpty()) {
                continue;
            }
            if (slot == EntityEquipmentSlot.OFFHAND) {
                if (!(stack.getItem() instanceof ItemCaster)) {
                    LOG.warn("Battlemage offhand {} is not a Thaumcraft caster; using the basic caster", e.getValue().item);
                    continue;
                }
                offhandSet = true;
            }
            this.setItemStackToSlot(slot, stack);
            GearAttributes.applyQuality(this, stack);
        }
        if (!offhandSet) {
            this.setItemStackToSlot(EntityEquipmentSlot.OFFHAND, new ItemStack(ItemsTC.casterBasic));
        }
    }

    private ItemStack buildStack(String slotName, EquipmentDef def) {
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(def.item));
        if (item == null) {
            LOG.warn("Battlemage equipment {} not registered: {}", slotName, def.item);
            return ItemStack.EMPTY;
        }
        ItemStack stack = new ItemStack(item);
        if (def.nbt != null && !def.nbt.isEmpty()) {
            try {
                stack.setTagCompound(JsonToNBT.getTagFromJson(def.nbt));
            } catch (NBTException ex) {
                LOG.warn("Battlemage equipment {} has a bad nbt tag ({}); using the plain item", slotName, ex.getMessage());
            }
        }
        for (EquipmentDef.Ench en : def.enchantments) {
            Enchantment enchantment = Enchantment.getEnchantmentByLocation(en.id);
            if (enchantment == null) {
                LOG.warn("Battlemage equipment {} lists unregistered enchantment {}", slotName, en.id);
                continue;
            }
            stack.addEnchantment(enchantment, en.level);
        }
        return stack;
    }

    /**
     * Loadout source, in order: explicit {@code Foci} NBT; the variant's fixed list (the roller is skipped); the variant's
     * own roller config, else the tier's; the tier's fixed fallback list.
     */
    private List<String> chooseLoadout(LibraryData lib, TierDef tier, VariantDef variant) {
        if (explicitFoci != null && !explicitFoci.isEmpty()) {
            return explicitFoci;
        }
        if (variant != null && variant.fixedLoadout != null && !variant.fixedLoadout.isEmpty()) {
            return variant.fixedLoadout;
        }
        LoadoutRoller.Config cfg = variant != null && variant.loadout != null ? variant.loadout : (tier != null ? tier.loadout : null);
        if (cfg != null) {
            List<FocusDef> pool = new ArrayList<>();
            for (FocusDef d : lib.foci.values()) {
                if (FocusLibrary.resolved(d.id) != null) {
                    pool.add(d);
                }
            }
            List<String> rolled = LoadoutRoller.roll(pool, cfg, this.world.provider.getDimension(), this.getRNG());
            if (!rolled.isEmpty()) {
                return rolled;
            }
            LOG.warn("Battlemage loadout roll came back empty for {}, using the fallback list", this.getClass().getSimpleName());
        }
        return tier != null ? tier.stage1Foci : new ArrayList<String>();
    }

    /** Fallback for packs that do not list the mage in Dynamic Stealth's Natural Soul Sight (plan 3.14). */
    private void applySoulSight() {
        Potion soul = Potion.getPotionFromResourceLocation("dynamicstealth:soulsight");
        if (soul != null) {
            this.addPotionEffect(new PotionEffect(soul, 1000000, 0, true, false));
        }
    }

    private static EntityEquipmentSlot slotFor(String name) {
        switch (name) {
            case "head":
                return EntityEquipmentSlot.HEAD;
            case "chest":
                return EntityEquipmentSlot.CHEST;
            case "legs":
                return EntityEquipmentSlot.LEGS;
            case "feet":
                return EntityEquipmentSlot.FEET;
            case "mainhand":
                return EntityEquipmentSlot.MAINHAND;
            case "offhand":
                return EntityEquipmentSlot.OFFHAND;
            default:
                return null;
        }
    }

    private void buildLoadout(List<String> ids) {
        foci.clear();
        for (String id : ids) {
            BattlemageFocus f = FocusLibrary.resolved(id);
            if (f != null) {
                foci.add(f);
            }
        }
        readyAt = new long[foci.size()];
        if (BattlemageConfig.debug) {
            StringBuilder sb = new StringBuilder(this.getClass().getSimpleName()).append(" loadout:");
            for (BattlemageFocus f : foci) {
                sb.append(' ').append(f.def.id);
            }
            LOG.info(sb.toString());
        }
    }

    /** Shows the focus's colour on the off-hand caster (light display focus, not the package). */
    private void showFocus(BattlemageFocus f) {
        ItemStack gauntlet = this.getHeldItemOffhand();
        if (f.item == null || !(gauntlet.getItem() instanceof ItemCaster)) {
            return;
        }
        ItemStack display = new ItemStack(f.item);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("color", f.color);
        display.setTagCompound(tag);
        ((ItemCaster) gauntlet.getItem()).setFocus(gauntlet, display);
    }

    /**
     * Casts a loadout focus at the target: aimed copy of the package, no vis, off-hand swing. The caller has already
     * checked cooldown, range and Stun. Variants call this too (ally heal, volley casts).
     */
    public void castFocus(int index, EntityLivingBase target, AiTuning ai) {
        BattlemageFocus f = foci.get(index);
        if (f.template == null) {
            return;
        }
        showFocus(f);
        FocusPackage pkg = f.template.copy(this);
        double power = getFocusPower();
        if (power != 1.0D) {
            pkg.multiplyPower((float) power);
        }
        BattlemageAim.aim(this, pkg, target, f.def, ai);
        FocusEngine.castFocusPackage(this, pkg, true);
        this.swingArm(EnumHand.OFF_HAND);
        int cd = FocusCooldowns.effective(f.def.cooldownTicks, f.def.complexity, BattlemageConfig.focusCooldownScale, phaseCooldownScale());
        readyAt[index] = this.ticksExisted + cd;
        nextCastTick = this.ticksExisted + ai.globalCastGapTicks;
        lastFocusId = f.def.id;
        boolean upkeep = "summon_upkeep".equals(f.def.role);
        if (!upkeep && ai.castBudgetEnabled) {
            getBudget().spend(f.def.complexity * ai.costPerComplexity);
        }
        if (upkeep || "aoe".equals(f.def.role) || "area_denial".equals(f.def.role)) {
            tracker.invalidate();
        }
        if (BattlemageConfig.debug) {
            LOG.info("{} cast {} at {} (cooldown {} ticks)", this.getClass().getSimpleName(), f.def.id, target.getName(), cd);
        }
    }

    // ---- variant hooks (plan 18.7, 19.7): no-ops in the plain Battlemage and Master Battlemage ----

    /** Called once on a fresh spawn at the end of initialisation (equipment, body and loadout are ready). Not called on reload. */
    protected void onInitialised(LibraryData lib, TierDef tier, VariantDef variant) {}

    /** Called every server tick after initialisation (tracking escorts, timers). */
    protected void onVariantTick() {}

    /** Variant state to save (escort uuids, flags); called from {@code writeEntityToNBT}. */
    protected void writeVariantNbt(NBTTagCompound tag) {}

    /** Variant state to restore; called from {@code readEntityFromNBT}. */
    protected void readVariantNbt(NBTTagCompound tag) {}

    /**
     * Called every AI tick while the mage has a target, after the decision-tick casts and before melee and movement.
     * Return true to own this tick (the default melee and movement are skipped).
     */
    public boolean variantStep(EntityLivingBase target, double dist, boolean los) {
        return false;
    }

    /**
     * Called on a decision tick after Spell Bat upkeep and before the normal cast. Return true when it acted (cast,
     * blinked, or committed a move): the normal cast is skipped this decision tick.
     */
    public boolean variantPreCast(EntityLivingBase target, double dist, boolean los) {
        return false;
    }

    /** True when the variant replaces the normal cast choice: the brain then calls {@link #variantCast} instead. */
    public boolean handlesCasting() {
        return false;
    }

    /** The variant's own cast choice on a decision tick (only called when {@link #handlesCasting()}); true when it acted. */
    public boolean variantCast(EntityLivingBase target, double dist, boolean los) {
        return false;
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        tag.setBoolean("BattlemageInit", initialised);
        NBTTagList ids = new NBTTagList();
        for (BattlemageFocus f : foci) {
            ids.appendTag(new NBTTagString(f.def.id));
        }
        tag.setTag("FociIds", ids);
        writeVariantNbt(tag);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        initialised = tag.getBoolean("BattlemageInit");
        if (tag.hasKey("Foci", 9)) {
            explicitFoci = new ArrayList<>();
            NBTTagList named = tag.getTagList("Foci", 8);
            for (int i = 0; i < named.tagCount(); i++) {
                explicitFoci.add(named.getStringTagAt(i));
            }
        }
        if (initialised && tag.hasKey("FociIds", 9)) {
            List<String> ids = new ArrayList<>();
            NBTTagList list = tag.getTagList("FociIds", 8);
            for (int i = 0; i < list.tagCount(); i++) {
                ids.add(list.getStringTagAt(i));
            }
            buildLoadout(ids);
        }
        readVariantNbt(tag);
    }
}
