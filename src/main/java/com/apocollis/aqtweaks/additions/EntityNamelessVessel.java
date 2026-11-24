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
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.BossInfo;
import net.minecraft.world.BossInfoServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.builder.ILoopType.EDefaultLoopTypes;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

public class EntityNamelessVessel extends EntityMob implements IAnimatable {
    public static final int STATE_IDLE_OR_GLIDE = 0;
    public static final int STATE_ATTACK_CLAW_SWIPE = 1;
    public static final int STATE_ATTACK_CAST = 2;
    public static final int STATE_ERUPT = 3;

    // Animation durations in ticks (20 ticks = 1.0s)
    public static final int TICKS_ATTACK_CLAW_SWIPE = 19; // 0.95s
    public static final int TICKS_ATTACK_CAST = 32;       // 1.60s

    private static final DataParameter<Integer> ANIM_STATE =
            EntityDataManager.createKey(EntityNamelessVessel.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> ACTION_SEQ =
            EntityDataManager.createKey(EntityNamelessVessel.class, DataSerializers.VARINT);

    private final AnimationFactory factory = new AnimationFactory(this);
    private final BossInfoServer bossInfo = new BossInfoServer(
            this.getDisplayName(), BossInfo.Color.PURPLE, BossInfo.Overlay.PROGRESS);

    private int actionTimer = 0;
    private int attackCooldown = 0;
    private boolean erupting = false;
    private int eruptionTimer = 0;

    // Client-side tracking of last seen ACTION_SEQ to force controller reload on repeated attacks
    private int clientLastActionSeq = -1;

    public EntityNamelessVessel(World worldIn) {
        super(worldIn);
        this.setSize(0.85F, 2.45F);
        this.isImmuneToFire = true;
        this.experienceValue = 0; // Experience and loot are awarded by Stage 2 (The Nameless One)
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
        this.tasks.addTask(6, new EntityAIWatchClosest(this, EntityPlayer.class, 16.0F));
        this.tasks.addTask(7, new EntityAILookIdle(this));

        this.targetTasks.addTask(1, new EntityAIHurtByTarget(this, false));
        this.targetTasks.addTask(2, new EntityAINearestAttackableTarget<>(this, EntityPlayer.class, true));
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        ArcanaQuestTweaksConfig.NamelessVesselConfig cfg =
                ArcanaQuestTweaksConfig.AdditionsModuleConfig.namelessVessel;
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
        return 2.15F;
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

    public boolean isErupting() {
        return this.erupting || this.getAnimState() == STATE_ERUPT;
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();

        // Dampen fall speed for floating/hovering vessel feel
        if (!this.onGround && this.motionY < 0.0D) {
            this.motionY *= 0.6D;
        }

        if (this.world.isRemote) {
            if (this.getAnimState() == STATE_ERUPT) {
                for (int i = 0; i < 6; i++) {
                    double ox = (this.rand.nextDouble() - 0.5D) * 1.2D;
                    double oy = this.rand.nextDouble() * 2.2D;
                    double oz = (this.rand.nextDouble() - 0.5D) * 1.2D;
                    this.world.spawnParticle(EnumParticleTypes.PORTAL,
                            this.posX + ox, this.posY + oy, this.posZ + oz,
                            ox * 0.5D, 0.1D, oz * 0.5D);
                    this.world.spawnParticle(EnumParticleTypes.SPELL_WITCH,
                            this.posX + ox * 0.7D, this.posY + oy, this.posZ + oz * 0.7D,
                            0.0D, 0.08D, 0.0D);
                }
            } else if (this.ticksExisted % 3 == 0) {
                double ox = (this.rand.nextDouble() - 0.5D) * 0.7D;
                double oy = 0.2D + this.rand.nextDouble() * 1.8D;
                double oz = (this.rand.nextDouble() - 0.5D) * 0.7D;
                this.world.spawnParticle(EnumParticleTypes.TOWN_AURA,
                        this.posX + ox, this.posY + oy, this.posZ + oz,
                        0.0D, 0.01D, 0.0D);
            }
            return;
        }

        // Server-side eruption sequence (Stage 1 -> Stage 2)
        if (this.erupting) {
            this.motionX = 0.0D;
            this.motionY = 0.0D;
            this.motionZ = 0.0D;
            this.getNavigator().clearPath();
            this.eruptionTimer++;

            int maxEruptTicks = Math.max(10, ArcanaQuestTweaksConfig.AdditionsModuleConfig.namelessVessel.eruptionTicks);
            if (this.eruptionTimer == 1) {
                this.playSound(SoundEvents.ENTITY_WITHER_AMBIENT, 1.8F, 0.6F);
                this.playSound(SoundEvents.ENTITY_ILLAGER_PREPARE_BLINDNESS, 2.0F, 0.55F);
            } else if (this.eruptionTimer == maxEruptTicks / 2) {
                this.playSound(SoundEvents.ENTITY_ZOMBIE_VILLAGER_CURE, 1.8F, 0.55F);
            }

            if (this.world instanceof WorldServer) {
                WorldServer ws = (WorldServer) this.world;
                ws.spawnParticle(EnumParticleTypes.SPELL_WITCH,
                        this.posX, this.posY + 1.2D, this.posZ,
                        8, 0.45D, 0.8D, 0.45D, 0.04D);
                if (this.eruptionTimer >= maxEruptTicks - 10) {
                    ws.spawnParticle(EnumParticleTypes.END_ROD,
                            this.posX, this.posY + 1.3D, this.posZ,
                            12, 0.6D, 0.9D, 0.6D, 0.12D);
                }
            }

            if (this.eruptionTimer >= maxEruptTicks) {
                this.completeEruptionIntoStage2();
            }
            return;
        }

        if (this.attackCooldown > 0) {
            this.attackCooldown--;
        }

        EntityLivingBase target = this.getAttackTarget();
        if (this.actionTimer > 0) {
            this.actionTimer--;
            int currentState = this.getAnimState();
            // Deal melee claw swipe damage at the strike apex (tick 7 out of 17, ~0.35s into animation)
            if (currentState == STATE_ATTACK_CLAW_SWIPE
                    && this.actionTimer == (TICKS_ATTACK_CLAW_SWIPE - 7)
                    && target != null && target.isEntityAlive()) {
                double distSq = this.getDistanceSq(target);
                if (distSq <= 12.25D) { // 3.5 blocks reach
                    this.attackEntityAsMob(target);
                    this.playSound(SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 1.2F, 0.7F);
                }
            }
            if (this.actionTimer == 0) {
                this.dataManager.set(ANIM_STATE, STATE_IDLE_OR_GLIDE);
            }
        }

        // Baseline Phase D pursuit & melee AI
        if (target != null && target.isEntityAlive()) {
            this.getLookHelper().setLookPositionWithEntity(target, 30.0F, 30.0F);
            double distSq = this.getDistanceSq(target);
            if (distSq > 4.0D) {
                this.getNavigator().tryMoveToEntityLiving(target, 1.0D);
            } else {
                this.getNavigator().clearPath();
            }

            if (distSq <= 9.0D && this.attackCooldown <= 0 && this.actionTimer <= 0) {
                this.triggerActionState(STATE_ATTACK_CLAW_SWIPE, TICKS_ATTACK_CLAW_SWIPE);
                this.attackCooldown = 26;
                this.playSound(SoundEvents.VINDICATION_ILLAGER_AMBIENT, 1.2F, 0.65F);
            }
        }
    }

    private void startEruption() {
        if (this.erupting) {
            return;
        }
        this.erupting = true;
        this.eruptionTimer = 0;
        this.setHealth(1.0F);
        this.clearActivePotions();
        this.getNavigator().clearPath();
        int maxEruptTicks = Math.max(10, ArcanaQuestTweaksConfig.AdditionsModuleConfig.namelessVessel.eruptionTicks);
        this.triggerActionState(STATE_ERUPT, maxEruptTicks + 5);
        this.bossInfo.setPercent(0.0F);
    }

    private void completeEruptionIntoStage2() {
        if (!this.world.isRemote) {
            this.playSound(SoundEvents.ENTITY_GENERIC_EXPLODE, 2.0F, 0.55F);
            this.playSound(SoundEvents.ENTITY_WITHER_SPAWN, 2.0F, 0.65F);

            if (this.world instanceof WorldServer) {
                WorldServer ws = (WorldServer) this.world;
                ws.spawnParticle(EnumParticleTypes.EXPLOSION_HUGE,
                        this.posX, this.posY + 1.4D, this.posZ,
                        1, 0.0D, 0.0D, 0.0D, 0.0D);
                ws.spawnParticle(EnumParticleTypes.END_ROD,
                        this.posX, this.posY + 1.5D, this.posZ,
                        45, 0.9D, 1.3D, 0.9D, 0.18D);
                ws.spawnParticle(EnumParticleTypes.SPELL_WITCH,
                        this.posX, this.posY + 1.2D, this.posZ,
                        40, 0.8D, 1.1D, 0.8D, 0.10D);
            }

            if (ArcanaQuestTweaksConfig.AdditionsModuleConfig.drops.spawnStage2OnVesselDefeat
                    && ArcanaQuestTweaksConfig.AdditionsModuleConfig.namelessOne.enable) {
                EntityNamelessOne stage2 = new EntityNamelessOne(this.world);
                stage2.setLocationAndAngles(this.posX, this.posY, this.posZ, this.rotationYaw, this.rotationPitch);
                stage2.rotationYawHead = this.rotationYawHead;
                stage2.renderYawOffset = this.renderYawOffset;
                if (this.getAttackTarget() != null && this.getAttackTarget().isEntityAlive()) {
                    stage2.setAttackTarget(this.getAttackTarget());
                }
                stage2.startSpawnErupt();
                this.world.spawnEntity(stage2);
            }
        }
        // Remove Stage 1 cleanly without dropping loot or XP
        this.setDead();
    }

    @Override
    protected void updateAITasks() {
        super.updateAITasks();
        if (!this.erupting) {
            this.bossInfo.setPercent(this.getHealth() / this.getMaxHealth());
        }
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
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (source == DamageSource.OUT_OF_WORLD) {
            return super.attackEntityFrom(source, amount);
        }
        if (this.erupting || source == DamageSource.IN_WALL || source == DamageSource.FALL) {
            return false;
        }
        float cap = (float) ArcanaQuestTweaksConfig.AdditionsModuleConfig.namelessVessel.damageCap;
        if (cap > 0.0F && amount > cap) {
            amount = cap;
        }

        // Check if this hit would reduce the Vessel to <= 1 HP; if so, intercept death and trigger eruption!
        if (!this.world.isRemote && (this.getHealth() - amount) <= 1.0F
                && ArcanaQuestTweaksConfig.AdditionsModuleConfig.drops.spawnStage2OnVesselDefeat
                && ArcanaQuestTweaksConfig.AdditionsModuleConfig.namelessOne.enable) {
            this.startEruption();
            return true;
        }

        return super.attackEntityFrom(source, amount);
    }

    @Override
    public void onDeath(DamageSource cause) {
        // Safety net if lethal damage bypassed attackEntityFrom (except /kill / OUT_OF_WORLD)
        if (!this.world.isRemote && cause != DamageSource.OUT_OF_WORLD
                && ArcanaQuestTweaksConfig.AdditionsModuleConfig.drops.spawnStage2OnVesselDefeat
                && ArcanaQuestTweaksConfig.AdditionsModuleConfig.namelessOne.enable) {
            this.isDead = false;
            this.deathTime = 0;
            this.startEruption();
            return;
        }
        super.onDeath(cause);
    }

    @Override
    protected void dropFewItems(boolean wasRecentlyHit, int lootingModifier) {
        // Stage 1 drops nothing; Stage 2 is the true boss
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound compound) {
        super.writeEntityToNBT(compound);
        compound.setBoolean("Erupting", this.erupting);
        compound.setInteger("EruptionTimer", this.eruptionTimer);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound compound) {
        super.readEntityFromNBT(compound);
        this.erupting = compound.getBoolean("Erupting");
        this.eruptionTimer = compound.getInteger("EruptionTimer");
        if (this.erupting) {
            this.dataManager.set(ANIM_STATE, STATE_ERUPT);
        }
        if (this.hasCustomName()) {
            this.bossInfo.setName(this.getDisplayName());
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_EVOCATION_ILLAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return SoundEvents.ENTITY_EVOCATION_ILLAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_ILLAGER_DEATH;
    }

    @Override
    protected float getSoundPitch() {
        return 0.65F + (this.rand.nextFloat() * 0.1F);
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
        if (state == STATE_ERUPT) {
            controller.transitionLengthTicks = 3.0D;
            controller.setAnimation(new AnimationBuilder().addAnimation(
                    "animation.nameless_vessel.erupt", EDefaultLoopTypes.HOLD_ON_LAST_FRAME));
            return PlayState.CONTINUE;
        }
        if (state == STATE_ATTACK_CLAW_SWIPE) {
            controller.transitionLengthTicks = 2.0D;
            controller.setAnimation(new AnimationBuilder().addAnimation(
                    "animation.nameless_vessel.attack_claw_swipe", EDefaultLoopTypes.PLAY_ONCE));
            return PlayState.CONTINUE;
        }
        if (state == STATE_ATTACK_CAST) {
            controller.transitionLengthTicks = 3.0D;
            controller.setAnimation(new AnimationBuilder().addAnimation(
                    "animation.nameless_vessel.attack_cast", EDefaultLoopTypes.PLAY_ONCE));
            return PlayState.CONTINUE;
        }

        controller.transitionLengthTicks = 5.0D;
        double dx = this.posX - this.prevPosX;
        double dz = this.posZ - this.prevPosZ;
        boolean moving = event.isMoving() || (dx * dx + dz * dz) > 0.0008D;
        if (moving) {
            controller.setAnimation(new AnimationBuilder().addAnimation(
                    "animation.nameless_vessel.glide", EDefaultLoopTypes.LOOP));
        } else {
            controller.setAnimation(new AnimationBuilder().addAnimation(
                    "animation.nameless_vessel.idle", EDefaultLoopTypes.LOOP));
        }
        return PlayState.CONTINUE;
    }

    @Override
    public void registerControllers(AnimationData data) {
        data.addAnimationController(new AnimationController<>(this, "controller", 4.0F, this::predicate));
    }

    @Override
    public AnimationFactory getFactory() {
        return this.factory;
    }
}
