package com.apocollis.aqtweaks.gaia;

import javax.annotation.Nullable;

import gaia.GaiaConfig;
import gaia.entity.EntityAttributes;
import gaia.entity.EntityMobHostileBase;
import gaia.entity.ai.EntityAIGaiaAttackRangedBow;
import gaia.entity.ai.EntityAIGaiaBreakDoor;
import gaia.entity.ai.GaiaIRangedAttackMob;
import gaia.entity.ai.Ranged;
import gaia.entity.monster.EntityGaiaDwarf;
import gaia.entity.monster.EntityGaiaOrc;
import gaia.init.GaiaItems;
import gaia.init.GaiaLootTables;
import gaia.init.GaiaSounds;
import gaia.items.ItemShard;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIAttackMelee;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAIWander;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.init.PotionTypes;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.pathfinding.PathNavigateGround;
import net.minecraft.potion.PotionEffect;
import net.minecraft.potion.PotionUtils;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import net.minecraft.world.storage.loot.LootTableList;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Hostile cave clone of Gaia’s dwarf. Combat and gear match {@link EntityGaiaDwarf};
 * targeting is {@link EntityMobHostileBase} (player on sight) plus orcs.
 */
public class EntityDeepDwarf extends EntityMobHostileBase implements GaiaIRangedAttackMob {

    private static final String MOB_TYPE_TAG = "MobType";

    private final EntityAIGaiaAttackRangedBow aiArrowAttack =
            new EntityAIGaiaAttackRangedBow(this, EntityAttributes.ATTACK_SPEED_2, 20, 15.0F);
    private final EntityAIAttackMelee aiAttackOnCollide = new EntityAIAttackMelee(this, EntityAttributes.ATTACK_SPEED_2, true) {
        @Override
        public void resetTask() {
            super.resetTask();
            EntityDeepDwarf.this.setSwingingArms(false);
        }

        @Override
        public void startExecuting() {
            super.startExecuting();
            EntityDeepDwarf.this.setSwingingArms(true);
        }
    };
    private final EntityAIGaiaBreakDoor breakDoor = new EntityAIGaiaBreakDoor(this);

    private static final DataParameter<Integer> SKIN = EntityDataManager.createKey(EntityDeepDwarf.class, DataSerializers.VARINT);
    private static final DataParameter<Boolean> SWINGING_ARMS = EntityDataManager.createKey(EntityDeepDwarf.class, DataSerializers.BOOLEAN);
    private static final ItemStack TIPPED_ARROW_CUSTOM = PotionUtils.addPotionToItemStack(new ItemStack(Items.TIPPED_ARROW), PotionTypes.SLOWNESS);
    private static final ItemStack TIPPED_ARROW_CUSTOM_2 = PotionUtils.addPotionToItemStack(new ItemStack(Items.TIPPED_ARROW), PotionTypes.WEAKNESS);

    public EntityDeepDwarf(World worldIn) {
        super(worldIn);
        setSize(0.5F, 1.5F);
        experienceValue = EntityAttributes.EXPERIENCE_VALUE_2;
        stepHeight = 1.0F;
        if (!worldIn.isRemote) {
            setCombatTask();
        }
    }

    @Override
    protected void initEntityAI() {
        tasks.addTask(0, new EntityAISwimming(this));
        tasks.addTask(3, new EntityAIWander(this, 1.0D));
        tasks.addTask(4, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
        tasks.addTask(4, new EntityAILookIdle(this));
        targetTasks.addTask(1, new EntityAIHurtByTarget(this, true));
        targetTasks.addTask(3, new EntityAINearestAttackableTarget<>(this, EntityGaiaOrc.class, true));
    }

    public void setBreakDoorsAItask(boolean enabled) {
        ((PathNavigateGround) this.getNavigator()).setBreakDoors(enabled);
        if (enabled) {
            this.tasks.addTask(1, this.breakDoor);
        } else {
            this.tasks.removeTask(this.breakDoor);
        }
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(EntityAttributes.MAX_HEALTH_2);
        getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(EntityAttributes.FOLLOW_RANGE);
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(EntityAttributes.MOVE_SPEED_2);
        getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(EntityAttributes.ATTACK_DAMAGE_2);
        getEntityAttribute(SharedMonsterAttributes.ARMOR).setBaseValue(EntityAttributes.RATE_ARMOR_2);
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float damage) {
        if (hasShield()) {
            Entity entity = source.getImmediateSource();
            return !(entity instanceof EntityArrow) && super.attackEntityFrom(source, Math.min(damage, EntityAttributes.BASE_DEFENSE_2));
        }
        return super.attackEntityFrom(source, Math.min(damage, EntityAttributes.BASE_DEFENSE_2));
    }

    private boolean hasShield() {
        ItemStack itemstack = this.getItemStackFromSlot(EntityEquipmentSlot.OFFHAND);
        return itemstack.getItem() == Items.SHIELD || itemstack.getItem() == GaiaItems.SHIELD_PROP;
    }

    @Override
    public void knockBack(Entity entityIn, float strength, double xRatio, double zRatio) {
        super.knockBack(xRatio, zRatio, EntityAttributes.KNOCKBACK_2);
    }

    @Override
    public boolean attackEntityAsMob(Entity entityIn) {
        if (super.attackEntityAsMob(entityIn)) {
            if (getMobType() == 1 && entityIn instanceof EntityLivingBase) {
                byte duration = 0;
                if (world.getDifficulty() == EnumDifficulty.NORMAL) {
                    duration = 10;
                } else if (world.getDifficulty() == EnumDifficulty.HARD) {
                    duration = 20;
                }
                if (duration > 0) {
                    ((EntityLivingBase) entityIn).addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, duration * 20));
                    ((EntityLivingBase) entityIn).addPotionEffect(new PotionEffect(MobEffects.MINING_FATIGUE, duration * 20));
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean isAIDisabled() {
        return false;
    }

    @Override
    public void setItemStackToSlot(EntityEquipmentSlot slotIn, ItemStack stack) {
        super.setItemStackToSlot(slotIn, stack);
        if (!world.isRemote && slotIn.getIndex() == 0) {
            setCombatTask();
        }
    }

    private void setCombatTask() {
        tasks.removeTask(aiAttackOnCollide);
        tasks.removeTask(aiArrowAttack);
        ItemStack itemstack = getHeldItemMainhand();
        if (itemstack.getItem() == Items.BOW) {
            tasks.addTask(2, aiArrowAttack);
        } else {
            tasks.addTask(2, aiAttackOnCollide);
        }
    }

    public int getTextureType() {
        return dataManager.get(SKIN);
    }

    public int getMobType() {
        return dataManager.get(SKIN);
    }

    private void setMobType(int type) {
        dataManager.set(SKIN, type);
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound compound) {
        super.writeEntityToNBT(compound);
        compound.setByte(MOB_TYPE_TAG, (byte) getMobType());
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound compound) {
        super.readEntityFromNBT(compound);
        if (compound.hasKey(MOB_TYPE_TAG)) {
            setMobType(compound.getByte(MOB_TYPE_TAG));
        }
        setCombatTask();
    }

    @Override
    public boolean canAttackClass(Class<? extends EntityLivingBase> cls) {
        return super.canAttackClass(cls) && cls != EntityDeepDwarf.class && cls != EntityGaiaDwarf.class;
    }

    @Override
    public void attackEntityWithRangedAttack(EntityLivingBase target, float distanceFactor) {
        if (!target.isDead) {
            Ranged.rangedAttack(target, this, distanceFactor);
        }
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(SKIN, 0);
        dataManager.register(SWINGING_ARMS, Boolean.FALSE);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean isSwingingArms() {
        return dataManager.get(SWINGING_ARMS);
    }

    @Override
    public void setSwingingArms(boolean swingingArms) {
        dataManager.set(SWINGING_ARMS, swingingArms);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return GaiaSounds.DWARF_SAY;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return GaiaSounds.DWARF_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return GaiaSounds.DWARF_DEATH;
    }

    @Nullable
    @Override
    protected ResourceLocation getLootTable() {
        return switch (getMobType()) {
            case 0 -> GaiaLootTables.ENTITIES_GAIA_DWARF_MELEE;
            case 1 -> GaiaLootTables.ENTITIES_GAIA_DWARF_RANGED;
            case 2 -> GaiaLootTables.ENTITIES_GAIA_DWARF_MINER;
            default -> LootTableList.EMPTY;
        };
    }

    @Override
    protected void dropFewItems(boolean wasRecentlyHit, int lootingModifier) {
        if (!wasRecentlyHit) {
            return;
        }
        int drop = rand.nextInt(3 + lootingModifier);
        switch (getMobType()) {
            case 0 -> {
                for (int i = 0; i < drop; ++i) {
                    dropItem(GaiaItems.FOOD_MEAT, 1);
                }
            }
            case 1 -> {
                for (int i = 0; i < drop; ++i) {
                    dropItem(Items.ARROW, 1);
                }
            }
            case 2 -> {
                for (int i = 0; i < drop; ++i) {
                    dropItem(Items.IRON_NUGGET, 1);
                }
            }
            default -> {
            }
        }
        int dropNugget = rand.nextInt(3) + 1;
        for (int i = 0; i < dropNugget; ++i) {
            dropItem(Items.GOLD_NUGGET, 1);
        }
        if (GaiaConfig.OPTIONS.additionalOre) {
            int dropNuggetAlt = rand.nextInt(3) + 1;
            for (int i = 0; i < dropNuggetAlt; ++i) {
                ItemShard.dropNugget(this, 5);
            }
        }
        if ((rand.nextInt(EntityAttributes.RATE_SEMI_RARE_DROP) == 0) && (getMobType() == 2)) {
            entityDropItem(new ItemStack(GaiaItems.BOX, 1, 0), 0.0F);
        }
        if (rand.nextInt(EntityAttributes.RATE_RARE_DROP) == 0) {
            switch (rand.nextInt(2)) {
                case 0 -> dropItem(GaiaItems.BOX_GOLD, 1);
                case 1 -> dropItem(GaiaItems.BAG_BOOK, 1);
            }
        }
        if ((rand.nextInt(EntityAttributes.RATE_UNIQUE_RARE_DROP) == 0) && (getMobType() == 1)) {
            dropItem(GaiaItems.BAG_ARROW, 1);
        }
    }

    @Override
    public IEntityLivingData onInitialSpawn(DifficultyInstance difficulty, @Nullable IEntityLivingData livingdata) {
        IEntityLivingData ret = super.onInitialSpawn(difficulty, livingdata);
        if (world.rand.nextInt(4) == 0) {
            mobClass(difficulty, 1);
        } else if (world.rand.nextInt(4) == 0) {
            mobClass(difficulty, 2);
        } else {
            mobClass(difficulty, 0);
        }
        setCombatTask();
        setBreakDoorsAItask(true);
        return ret;
    }

    private void mobClass(DifficultyInstance difficulty, int id) {
        switch (id) {
            case 0 -> {
                setItemStackToSlot(EntityEquipmentSlot.MAINHAND, new ItemStack(GaiaItems.WEAPON_PROP_AXE_STONE));
                setEnchantmentBasedOnDifficulty(difficulty);
                if (world.rand.nextInt(2) == 0) {
                    setItemStackToSlot(EntityEquipmentSlot.OFFHAND, new ItemStack(GaiaItems.SHIELD_PROP, 1, 1));
                    getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(0.25D);
                }
            }
            case 1 -> {
                ItemStack bowCustom = new ItemStack(Items.BOW);
                setItemStackToSlot(EntityEquipmentSlot.MAINHAND, bowCustom);
                bowCustom.addEnchantment(Enchantments.PUNCH, 1);
                if (world.rand.nextInt(2) == 0) {
                    if (world.rand.nextInt(2) == 0) {
                        setItemStackToSlot(EntityEquipmentSlot.OFFHAND, TIPPED_ARROW_CUSTOM.copy());
                    } else {
                        setItemStackToSlot(EntityEquipmentSlot.OFFHAND, TIPPED_ARROW_CUSTOM_2.copy());
                    }
                }
            }
            case 2 -> {
                setItemStackToSlot(EntityEquipmentSlot.OFFHAND, new ItemStack(Items.STONE_PICKAXE));
                setEnchantmentBasedOnDifficulty(difficulty);
            }
            default -> {
            }
        }
        setMobType(id);
    }

    @Override
    public int getMaxSpawnedInChunk() {
        return EntityAttributes.CHUNK_LIMIT_2;
    }
}
