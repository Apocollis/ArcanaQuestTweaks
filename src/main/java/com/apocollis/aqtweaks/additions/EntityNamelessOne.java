package com.apocollis.aqtweaks.additions;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAIWanderAvoidWater;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BossInfo;
import net.minecraft.world.BossInfoServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.ForgeEventFactory;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.builder.ILoopType.EDefaultLoopTypes;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

public class EntityNamelessOne extends EntityMob implements IAnimatable {
    public static final int STATE_IDLE_OR_GLIDE = 0;
    public static final int STATE_ATTACK_CLAW_SWIPE = 1;
    public static final int STATE_ATTACK_CLAW_COMBO = 2;
    public static final int STATE_ATTACK_HAND_CAST = 3;
    public static final int STATE_ATTACK_VOID_CAST = 4;
    public static final int STATE_PHASE_SHIFT = 5;
    public static final int STATE_STAGGER = 6;
    public static final int STATE_DEATH = 7;
    public static final int STATE_SPAWN_ERUPT = 8;
    public static final int STATE_ATTACK_IMPETUS_BEAM = 9;

    // Animation durations in ticks (20 ticks = 1.0s)
    public static final int TICKS_ATTACK_CLAW_SWIPE = 18;           // 0.90s
    public static final int TICKS_ATTACK_CLAW_COMBO = 31;           // 1.55s
    public static final int TICKS_ATTACK_HAND_CAST = 44;            // 2.20s
    public static final int TICKS_ATTACK_VOID_CAST = 78;            // 3.90s
    public static final int TICKS_PHASE_SHIFT = 40;                 // 2.00s
    public static final int TICKS_STAGGER = 90;                     // 4.50s (0..15t recoil/drop, 15..65t slump hold, 65..90t slow recover)
    public static final int TICKS_STAGGER_SLUMP_START = 15;         // 0.75s
    public static final int TICKS_STAGGER_SLUMP_END = 65;           // 3.25s (50 ticks / 2.50s slump window)
    public static final int TICKS_SPAWN_ERUPT = 44;                 // 2.20s
    public static final int TICKS_ATTACK_IMPETUS_BEAM = 120;        // 6.00s (0..53t void-cast-style warning/charge, 53..103t active 2.50s beam sweep, 103..120t recover)
    public static final int TICKS_IMPETUS_BEAM_FIRE_START = 53;     // 2.65s
    public static final int TICKS_IMPETUS_BEAM_FIRE_END = 103;      // 5.15s (50 ticks / 2.50s held beam pose)
    public static final int TICKS_DEATH_DEFAULT = 90;               // 4.50s

    private static final DataParameter<Integer> ANIM_STATE =
            EntityDataManager.createKey(EntityNamelessOne.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> ACTION_SEQ =
            EntityDataManager.createKey(EntityNamelessOne.class, DataSerializers.VARINT);

    private final AnimationFactory factory = new AnimationFactory(this);
    private final BossInfoServer bossInfo = new BossInfoServer(
            this.getDisplayName(), BossInfo.Color.YELLOW, BossInfo.Overlay.NOTCHED_6);

    private int actionTimer = 0;
    private int attackCooldown = 0;
    private int spawnInvulnTimer = 0;
    private int customDeathTicks = 0;

    // Client-side tracking of last seen ACTION_SEQ to force controller reload on repeated attacks
    // and elapsed ticks within the current animation state for synced visual/particle cues
    private int clientLastActionSeq = -1;
    private int clientStateTrack = -1;
    private int clientActionSeqTrack = -1;
    private int clientActionTicks = 0;

    public EntityNamelessOne(World worldIn) {
        super(worldIn);
        this.setSize(2.0F, 5.4F);
        this.isImmuneToFire = true;
        this.experienceValue = 500;
        this.enablePersistence();
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(ANIM_STATE, STATE_IDLE_OR_GLIDE);
        this.dataManager.register(ACTION_SEQ, 0);
    }

    @Override
    protected void initEntityAI() {
        this.tasks.addTask(0, new EntityAISwimming(this));
        this.tasks.addTask(5, new EntityAIWanderAvoidWater(this, 0.75D));
        this.tasks.addTask(6, new EntityAIWatchClosest(this, EntityPlayer.class, 24.0F));
        this.tasks.addTask(7, new EntityAILookIdle(this));

        this.targetTasks.addTask(1, new EntityAIHurtByTarget(this, false));
        this.targetTasks.addTask(2, new EntityAINearestAttackableTarget<>(this, EntityPlayer.class, true));
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        ArcanaQuestTweaksConfig.NamelessOneConfig cfg =
                ArcanaQuestTweaksConfig.AdditionsModuleConfig.namelessOne;
        this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(cfg.maxHealth);
        this.getEntityAttribute(SharedMonsterAttributes.ARMOR).setBaseValue(cfg.armor);
        this.getEntityAttribute(SharedMonsterAttributes.ARMOR_TOUGHNESS).setBaseValue(cfg.armorToughness);
        this.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(cfg.knockbackResistance);
        this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(cfg.attackDamage);
        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(cfg.moveSpeed);
        this.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(cfg.followRange);
    }

    @Override
    public float getEyeHeight() {
        return 5.2F;
    }

    public int getAnimState() {
        return this.dataManager.get(ANIM_STATE);
    }

    public int getActionSeq() {
        return this.dataManager.get(ACTION_SEQ);
    }

    public void triggerActionState(int state, int durationTicks) {
        this.dataManager.set(ANIM_STATE, state);
        this.dataManager.set(ACTION_SEQ, this.dataManager.get(ACTION_SEQ) + 1);
        this.actionTimer = durationTicks;
    }

    /**
     * Triggered when emerging from The Nameless Vessel (Stage 1 -> Stage 2 eruption).
     */
    public void startSpawnErupt() {
        this.spawnInvulnTimer = Math.max(0,
                ArcanaQuestTweaksConfig.AdditionsModuleConfig.namelessOne.spawnInvulnTicks);
        this.triggerActionState(STATE_SPAWN_ERUPT, TICKS_SPAWN_ERUPT);
        this.attackCooldown = TICKS_SPAWN_ERUPT + 10;
    }

    /**
     * Cancels the current action and drops The Nameless One into its 90-tick (4.50s) stagger animation.
     */
    public void triggerStagger() {
        this.getNavigator().clearPath();
        this.motionX = 0.0D;
        this.motionZ = 0.0D;
        this.triggerActionState(STATE_STAGGER, TICKS_STAGGER);
    }

    /**
     * True only while the boss is in the held slump portion (ticks 15..64, 50 ticks / 2.50s)
     * of {@link #STATE_STAGGER}, after recoil/drop and before recovery.
     */
    public boolean isInStaggerSlump() {
        if (this.getAnimState() != STATE_STAGGER || this.actionTimer <= 0) {
            return false;
        }
        int elapsed = TICKS_STAGGER - this.actionTimer;
        return elapsed >= TICKS_STAGGER_SLUMP_START && elapsed < TICKS_STAGGER_SLUMP_END;
    }

    public int getClientActionTicks() {
        return this.clientActionTicks;
    }

    /**
     * True while The Nameless One's heart_core should appear enlarged and blackened
     * during {@link #STATE_ATTACK_IMPETUS_BEAM} (warning charge + beam fire ticks 0..102).
     */
    public boolean isImpetusHeartBlackened() {
        if (this.getAnimState() != STATE_ATTACK_IMPETUS_BEAM) {
            return false;
        }
        int elapsed = this.world.isRemote
                ? this.clientActionTicks
                : (TICKS_ATTACK_IMPETUS_BEAM - this.actionTimer);
        return elapsed >= 0 && elapsed < TICKS_IMPETUS_BEAM_FIRE_END;
    }

    /**
     * Smooth 0..1 factor driving heart_core blackening on the renderer and suppressing its glowmask
     * across {@link #STATE_ATTACK_IMPETUS_BEAM}: ramps dark over ticks 0..12, holds pitch-black
     * across warning coil and firing sweep (ticks 12..103), and restores over ticks 103..120.
     */
    public float getImpetusHeartDarkness(float partialTicks) {
        if (this.getAnimState() != STATE_ATTACK_IMPETUS_BEAM) {
            return 0.0F;
        }
        float elapsed = this.clientActionTicks + partialTicks;
        if (elapsed <= 0.0F) {
            return 0.0F;
        }
        if (elapsed < 12.0F) {
            return MathHelper.clamp(elapsed / 12.0F, 0.0F, 1.0F);
        }
        if (elapsed < TICKS_IMPETUS_BEAM_FIRE_END) {
            return 1.0F;
        }
        if (elapsed < TICKS_ATTACK_IMPETUS_BEAM) {
            return MathHelper.clamp(
                    1.0F - ((elapsed - TICKS_IMPETUS_BEAM_FIRE_END) / (float) (TICKS_ATTACK_IMPETUS_BEAM - TICKS_IMPETUS_BEAM_FIRE_END)),
                    0.0F, 1.0F);
        }
        return 0.0F;
    }

    /**
     * World-space origin of the chest heart_core cube (pivot [0, 73.5, -1.6], arched forward during impetus beam).
     */
    public Vec3d getImpetusHeartWorldPos(float partialTicks) {
        double ix = this.prevPosX + (this.posX - this.prevPosX) * partialTicks;
        double iy = this.prevPosY + (this.posY - this.prevPosY) * partialTicks;
        double iz = this.prevPosZ + (this.posZ - this.prevPosZ) * partialTicks;
        float yaw = this.prevRenderYawOffset + (this.renderYawOffset - this.prevRenderYawOffset) * partialTicks;
        float yawRad = yaw * ((float) Math.PI / 180.0F);
        double forwardX = -MathHelper.sin(yawRad);
        double forwardZ = MathHelper.cos(yawRad);
        double forwardOffset = this.getAnimState() == STATE_ATTACK_IMPETUS_BEAM ? 0.35D : 0.10D;
        double heightOffset = this.getAnimState() == STATE_ATTACK_IMPETUS_BEAM ? 4.45D : 4.59D;
        return new Vec3d(ix + forwardX * forwardOffset, iy + heightOffset, iz + forwardZ * forwardOffset);
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();

        // Keep boss hovering smoothly when airborne
        if (!this.onGround && this.motionY < 0.0D) {
            this.motionY *= 0.55D;
        }

        if (this.world.isRemote) {
            int state = this.getAnimState();
            int seq = this.getActionSeq();
            if (state != this.clientStateTrack || seq != this.clientActionSeqTrack) {
                this.clientStateTrack = state;
                this.clientActionSeqTrack = seq;
                this.clientActionTicks = 0;
            } else {
                this.clientActionTicks++;
            }

            if (state == STATE_SPAWN_ERUPT) {
                for (int i = 0; i < 6; i++) {
                    double ox = (this.rand.nextDouble() - 0.5D) * 2.2D;
                    double oy = this.rand.nextDouble() * 5.0D;
                    double oz = (this.rand.nextDouble() - 0.5D) * 2.2D;
                    this.world.spawnParticle(EnumParticleTypes.PORTAL,
                            this.posX + ox, this.posY + oy, this.posZ + oz,
                            ox * 0.4D, 0.15D, oz * 0.4D);
                }
            } else if (state == STATE_ATTACK_IMPETUS_BEAM && this.clientActionTicks < TICKS_IMPETUS_BEAM_FIRE_END) {
                Vec3d heart = this.getImpetusHeartWorldPos(1.0F);
                if (this.clientActionTicks < TICKS_IMPETUS_BEAM_FIRE_START) {
                    // Impetus charge: dark portal & void sparks converging directly into heart_core
                    int count = 3 + (this.clientActionTicks / 8);
                    for (int i = 0; i < count; i++) {
                        double ox = (this.rand.nextDouble() - 0.5D) * 1.9D;
                        double oy = (this.rand.nextDouble() - 0.5D) * 1.9D;
                        double oz = (this.rand.nextDouble() - 0.5D) * 1.9D;
                        this.world.spawnParticle(EnumParticleTypes.PORTAL,
                                heart.x, heart.y, heart.z,
                                ox, oy, oz);
                        if ((i & 1) == 0) {
                            this.world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL,
                                    heart.x + ox * 0.45D, heart.y + oy * 0.45D, heart.z + oz * 0.45D,
                                    -ox * 0.06D, -oy * 0.06D, -oz * 0.06D);
                        }
                    }
                } else {
                    // Active raking beam: turbulent void crackling around the blackened heart_core
                    for (int i = 0; i < 4; i++) {
                        double ox = (this.rand.nextDouble() - 0.5D) * 0.7D;
                        double oy = (this.rand.nextDouble() - 0.5D) * 0.7D;
                        double oz = (this.rand.nextDouble() - 0.5D) * 0.7D;
                        this.world.spawnParticle(EnumParticleTypes.SPELL_WITCH,
                                heart.x + ox, heart.y + oy, heart.z + oz,
                                ox * 0.12D, oy * 0.12D, oz * 0.12D);
                    }
                }
            } else if (state != STATE_DEATH && this.ticksExisted % 2 == 0) {
                double ox = (this.rand.nextDouble() - 0.5D) * 1.6D;
                double oy = 0.6D + this.rand.nextDouble() * 4.4D;
                double oz = (this.rand.nextDouble() - 0.5D) * 1.6D;
                this.world.spawnParticle(EnumParticleTypes.TOWN_AURA,
                        this.posX + ox, this.posY + oy, this.posZ + oz,
                        0.0D, 0.01D, 0.0D);
            }
            return;
        }

        if (this.getHealth() <= 0.0F) {
            return;
        }

        if (this.spawnInvulnTimer > 0) {
            this.spawnInvulnTimer--;
        }
        if (this.attackCooldown > 0) {
            this.attackCooldown--;
        }

        int currentState = this.getAnimState();
        EntityLivingBase target = this.getAttackTarget();

        if (this.actionTimer > 0) {
            this.actionTimer--;
            if (currentState == STATE_SPAWN_ERUPT || currentState == STATE_STAGGER) {
                this.motionX = 0.0D;
                this.motionZ = 0.0D;
                this.getNavigator().clearPath();
            } else if (currentState == STATE_ATTACK_CLAW_SWIPE) {
                // Single swipe hit at 0.40s (tick 8 of 18)
                if (this.actionTimer == (TICKS_ATTACK_CLAW_SWIPE - 8)
                        && target != null && target.isEntityAlive()
                        && this.getDistanceSq(target) <= 20.25D) { // 4.5 blocks reach
                    this.attackEntityAsMob(target);
                    this.playSound(SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 1.5F, 0.55F);
                }
            } else if (currentState == STATE_ATTACK_CLAW_COMBO) {
                // Double swipe hits at 0.40s (tick 8) and 0.75s (tick 15)
                int elapsed = TICKS_ATTACK_CLAW_COMBO - this.actionTimer;
                if ((elapsed == 8 || elapsed == 15)
                        && target != null && target.isEntityAlive()
                        && this.getDistanceSq(target) <= 20.25D) {
                    this.attackEntityAsMob(target);
                    this.playSound(SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 1.6F, elapsed == 8 ? 0.55F : 0.45F);
                }
            }

            if (this.actionTimer == 0 && this.getHealth() > 0.0F) {
                this.dataManager.set(ANIM_STATE, STATE_IDLE_OR_GLIDE);
            }
        }

        // Baseline Phase D pursuit & melee AI (Phase E replaces/extends this with AINamelessOneCombat)
        if (currentState != STATE_SPAWN_ERUPT && currentState != STATE_STAGGER
                && target != null && target.isEntityAlive()) {
            this.getLookHelper().setLookPositionWithEntity(target, 30.0F, 30.0F);
            double distSq = this.getDistanceSq(target);
            if (distSq > 9.0D) {
                this.getNavigator().tryMoveToEntityLiving(target, 1.0D);
            } else {
                this.getNavigator().clearPath();
            }

            if (distSq <= 16.0D && this.attackCooldown <= 0 && this.actionTimer <= 0) {
                if (this.rand.nextFloat() < 0.40F) {
                    this.triggerActionState(STATE_ATTACK_CLAW_COMBO, TICKS_ATTACK_CLAW_COMBO);
                    this.attackCooldown = 38;
                } else {
                    this.triggerActionState(STATE_ATTACK_CLAW_SWIPE, TICKS_ATTACK_CLAW_SWIPE);
                    this.attackCooldown = 26;
                }
                this.playSound(SoundEvents.ENTITY_WITHER_SHOOT, 1.2F, 0.55F);
            }
        }
    }

    @Override
    protected void onDeathUpdate() {
        this.customDeathTicks++;
        this.deathTime = Math.min(18, this.customDeathTicks); // Prevent vanilla EntityLivingBase removal at deathTime == 20
        this.motionX = 0.0D;
        this.motionY = 0.0D;
        this.motionZ = 0.0D;

        if (!this.world.isRemote && this.customDeathTicks == 1) {
            this.triggerActionState(STATE_DEATH, 200);
            this.bossInfo.setPercent(0.0F);
            this.playSound(SoundEvents.ENTITY_WITHER_DEATH, 2.0F, 0.55F);
        }

        int maxDeathTicks = Math.max(20,
                ArcanaQuestTweaksConfig.AdditionsModuleConfig.namelessOne.deathTicks);

        // Beat 1: Halo shatters (~ticks 19-31 / 0.95s-1.55s)
        if (this.customDeathTicks == 20) {
            this.playSound(SoundEvents.BLOCK_GLASS_BREAK, 2.0F, 0.55F);
            this.playSound(SoundEvents.ENTITY_ITEM_BREAK, 1.8F, 0.5F);
            if (this.world.isRemote) {
                for (int i = 0; i < 24; i++) {
                    double ox = (this.rand.nextDouble() - 0.5D) * 2.4D;
                    double oz = (this.rand.nextDouble() - 0.5D) * 2.4D;
                    this.world.spawnParticle(EnumParticleTypes.END_ROD,
                            this.posX + ox, this.posY + 4.8D, this.posZ + oz,
                            ox * 0.15D, 0.12D, oz * 0.15D);
                }
            }
        }

        // Beat 2: Heart core ruptures (~ticks 31-52 / 1.55s-2.60s)
        if (this.customDeathTicks == 32) {
            this.playSound(SoundEvents.ENTITY_ZOMBIE_VILLAGER_CURE, 2.0F, 0.5F);
            if (this.world.isRemote) {
                for (int i = 0; i < 30; i++) {
                    double ox = (this.rand.nextDouble() - 0.5D) * 1.5D;
                    double oy = 3.2D + (this.rand.nextDouble() - 0.5D) * 1.2D;
                    double oz = (this.rand.nextDouble() - 0.5D) * 1.5D;
                    this.world.spawnParticle(EnumParticleTypes.SPELL_WITCH,
                            this.posX + ox, this.posY + oy, this.posZ + oz,
                            ox * 0.2D, 0.1D, oz * 0.2D);
                }
            }
        }

        // Beat 3: Collapse into the void (~ticks 52-90 / 2.60s-4.50s)
        if (this.world.isRemote && this.customDeathTicks >= 52) {
            for (int i = 0; i < 8; i++) {
                double ox = (this.rand.nextDouble() - 0.5D) * 2.2D;
                double oy = this.rand.nextDouble() * 4.5D;
                double oz = (this.rand.nextDouble() - 0.5D) * 2.2D;
                this.world.spawnParticle(EnumParticleTypes.SMOKE_LARGE,
                        this.posX + ox, this.posY + oy, this.posZ + oz,
                        0.0D, 0.04D, 0.0D);
                this.world.spawnParticle(EnumParticleTypes.PORTAL,
                        this.posX + ox, this.posY + oy, this.posZ + oz,
                        -ox * 0.25D, -0.1D, -oz * 0.25D);
            }
        }

        if (this.customDeathTicks >= maxDeathTicks) {
            if (!this.world.isRemote && (this.isPlayer() || this.recentlyHit > 0 && this.canDropLoot()
                    && this.world.getGameRules().getBoolean("doMobLoot"))) {
                int i = this.getExperiencePoints(this.attackingPlayer);
                i = ForgeEventFactory.getExperienceDrop(this, this.attackingPlayer, i);
                while (i > 0) {
                    int j = EntityXPOrb.getXPSplit(i);
                    i -= j;
                    this.world.spawnEntity(new EntityXPOrb(this.world, this.posX, this.posY, this.posZ, j));
                }
            }

            this.setDead();
            for (int k = 0; k < 25; k++) {
                double d2 = this.rand.nextGaussian() * 0.05D;
                double d0 = this.rand.nextGaussian() * 0.05D;
                double d1 = this.rand.nextGaussian() * 0.05D;
                this.world.spawnParticle(EnumParticleTypes.EXPLOSION_NORMAL,
                        this.posX + (double) (this.rand.nextFloat() * this.width * 2.0F) - (double) this.width,
                        this.posY + (double) (this.rand.nextFloat() * this.height),
                        this.posZ + (double) (this.rand.nextFloat() * this.width * 2.0F) - (double) this.width,
                        d2, d0, d1);
            }
        }
    }

    public int getCustomDeathTicks() {
        return this.customDeathTicks;
    }

    @Override
    protected void dropFewItems(boolean wasRecentlyHit, int lootingModifier) {
        super.dropFewItems(wasRecentlyHit, lootingModifier);
        String[] specs = ArcanaQuestTweaksConfig.AdditionsModuleConfig.drops.namelessOneDrops;
        if (specs == null) {
            return;
        }
        for (String raw : specs) {
            if (raw == null) {
                continue;
            }
            String entry = raw.trim();
            if (entry.isEmpty()) {
                continue;
            }
            int count = 1;
            int starIdx = entry.indexOf('*');
            if (starIdx > 0) {
                try {
                    count = Math.max(1, Integer.parseInt(entry.substring(starIdx + 1).trim()));
                } catch (NumberFormatException ignored) {
                }
                entry = entry.substring(0, starIdx).trim();
            }
            int meta = 0;
            String[] parts = entry.split(":");
            if (parts.length < 2) {
                continue;
            }
            if (parts.length >= 3) {
                try {
                    meta = Integer.parseInt(parts[2].trim());
                } catch (NumberFormatException ignored) {
                }
            }
            Item item = Item.REGISTRY.getObject(new ResourceLocation(parts[0].trim(), parts[1].trim()));
            if (item != null) {
                this.entityDropItem(new ItemStack(item, count, meta), 0.5F);
            }
        }
    }

    @Override
    protected void updateAITasks() {
        super.updateAITasks();
        this.bossInfo.setPercent(Math.max(0.0F, this.getHealth() / this.getMaxHealth()));
    }

    @Override
    public void addTrackingPlayer(EntityPlayerMP player) {
        super.addTrackingPlayer(player);
        this.bossInfo.addPlayer(player);
    }

    @Override
    public void removeTrackingPlayer(EntityPlayerMP player) {
        super.removeTrackingPlayer(player);
        this.bossInfo.removePlayer(player);
    }

    @Override
    public void setCustomNameTag(String name) {
        super.setCustomNameTag(name);
        this.bossInfo.setName(this.getDisplayName());
    }

    @Override
    public boolean isNonBoss() {
        return false;
    }

    @Override
    protected boolean canDespawn() {
        return false;
    }

    @Override
    public void fall(float distance, float damageMultiplier) {
        // Immune to fall damage
    }

    @Override
    protected void updateFallState(double y, boolean onGroundIn, IBlockState state, BlockPos pos) {
        // Do not accumulate fall distance
    }

    @Override
    public void knockBack(Entity entityIn, float strength, double xRatio, double zRatio) {
        if (this.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).getAttributeValue() >= 1.0D) {
            return;
        }
        super.knockBack(entityIn, strength, xRatio, zRatio);
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (source == DamageSource.OUT_OF_WORLD) {
            return super.attackEntityFrom(source, amount);
        }
        if (this.spawnInvulnTimer > 0
                || this.getAnimState() == STATE_SPAWN_ERUPT
                || source == DamageSource.IN_WALL
                || source == DamageSource.FALL) {
            return false;
        }
        if (this.isInStaggerSlump()) {
            amount *= (float) ArcanaQuestTweaksConfig.AdditionsModuleConfig.namelessOne.staggerDamageMultiplier;
        }
        float cap = (float) ArcanaQuestTweaksConfig.AdditionsModuleConfig.namelessOne.damageCap;
        if (cap > 0.0F && amount > cap) {
            amount = cap;
        }
        return super.attackEntityFrom(source, amount);
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound compound) {
        super.writeEntityToNBT(compound);
        compound.setInteger("SpawnInvulnTimer", this.spawnInvulnTimer);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound compound) {
        super.readEntityFromNBT(compound);
        this.spawnInvulnTimer = compound.getInteger("SpawnInvulnTimer");
        if (this.hasCustomName()) {
            this.bossInfo.setName(this.getDisplayName());
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_WITHER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return SoundEvents.ENTITY_WITHER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_WITHER_DEATH;
    }

    @Override
    protected float getSoundPitch() {
        return 0.55F + (this.rand.nextFloat() * 0.1F);
    }

    // --- SauriaLib 3 (GeckoLib) Animation Controller ---

    private <E extends IAnimatable> PlayState predicate(AnimationEvent<E> event) {
        AnimationController<?> controller = event.getController();
        int seq = this.getActionSeq();
        if (seq != this.clientLastActionSeq) {
            this.clientLastActionSeq = seq;
            controller.markNeedsReload();
        }

        int state = this.getAnimState();
        if (state == STATE_DEATH || this.getHealth() <= 0.0F) {
            controller.transitionLengthTicks = 3.0D;
            controller.setAnimation(new AnimationBuilder().addAnimation(
                    "animation.nameless_one.death", EDefaultLoopTypes.HOLD_ON_LAST_FRAME));
            return PlayState.CONTINUE;
        }
        if (state == STATE_SPAWN_ERUPT) {
            controller.transitionLengthTicks = 1.0D;
            controller.setAnimation(new AnimationBuilder().addAnimation(
                    "animation.nameless_one.spawn_erupt", EDefaultLoopTypes.PLAY_ONCE));
            return PlayState.CONTINUE;
        }
        if (state == STATE_STAGGER) {
            controller.transitionLengthTicks = 2.0D;
            controller.setAnimation(new AnimationBuilder().addAnimation(
                    "animation.nameless_one.stagger", EDefaultLoopTypes.PLAY_ONCE));
            return PlayState.CONTINUE;
        }
        if (state == STATE_PHASE_SHIFT) {
            controller.transitionLengthTicks = 3.0D;
            controller.setAnimation(new AnimationBuilder().addAnimation(
                    "animation.nameless_one.phase_shift", EDefaultLoopTypes.PLAY_ONCE));
            return PlayState.CONTINUE;
        }
        if (state == STATE_ATTACK_IMPETUS_BEAM) {
            controller.transitionLengthTicks = 3.0D;
            controller.setAnimation(new AnimationBuilder().addAnimation(
                    "animation.nameless_one.attack_impetus_beam", EDefaultLoopTypes.PLAY_ONCE));
            return PlayState.CONTINUE;
        }
        if (state == STATE_ATTACK_VOID_CAST) {
            controller.transitionLengthTicks = 3.0D;
            controller.setAnimation(new AnimationBuilder().addAnimation(
                    "animation.nameless_one.attack_void_cast", EDefaultLoopTypes.PLAY_ONCE));
            return PlayState.CONTINUE;
        }
        if (state == STATE_ATTACK_HAND_CAST) {
            controller.transitionLengthTicks = 3.0D;
            controller.setAnimation(new AnimationBuilder().addAnimation(
                    "animation.nameless_one.attack_hand_cast", EDefaultLoopTypes.PLAY_ONCE));
            return PlayState.CONTINUE;
        }
        if (state == STATE_ATTACK_CLAW_COMBO) {
            controller.transitionLengthTicks = 2.0D;
            controller.setAnimation(new AnimationBuilder().addAnimation(
                    "animation.nameless_one.attack_claw_combo", EDefaultLoopTypes.PLAY_ONCE));
            return PlayState.CONTINUE;
        }
        if (state == STATE_ATTACK_CLAW_SWIPE) {
            controller.transitionLengthTicks = 2.0D;
            controller.setAnimation(new AnimationBuilder().addAnimation(
                    "animation.nameless_one.attack_claw_swipe", EDefaultLoopTypes.PLAY_ONCE));
            return PlayState.CONTINUE;
        }

        controller.transitionLengthTicks = 6.0D;
        double dx = this.posX - this.prevPosX;
        double dz = this.posZ - this.prevPosZ;
        boolean moving = event.isMoving() || (dx * dx + dz * dz) > 0.0008D;
        if (moving) {
            controller.setAnimation(new AnimationBuilder().addAnimation(
                    "animation.nameless_one.glide", EDefaultLoopTypes.LOOP));
        } else {
            controller.setAnimation(new AnimationBuilder().addAnimation(
                    "animation.nameless_one.idle", EDefaultLoopTypes.LOOP));
        }
        return PlayState.CONTINUE;
    }

    @Override
    public void registerControllers(AnimationData data) {
        data.addAnimationController(new AnimationController<>(this, "controller", 5.0F, this::predicate));
    }

    @Override
    public AnimationFactory getFactory() {
        return this.factory;
    }
}
