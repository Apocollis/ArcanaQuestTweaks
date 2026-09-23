package com.apocollis.aqtweaks.reskillable;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import codersafterdark.reskillable.api.event.CacheInvalidatedEvent;
import codersafterdark.reskillable.api.event.LevelUpEvent;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemShears;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import com.apocollis.aqtweaks.util.Reflect;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerChangedDimensionEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent;

import java.util.List;
import java.util.UUID;

public class ReskillableModule {

    @SubscribeEvent
    public void onLevelUp(LevelUpEvent.Post event) {
        refresh(event.getEntityPlayer());
    }

    /**
     * Reskillable posts this on both sides — the server after a level or unlockable change, the
     * client when the matching invalidate packet lands — so it is the only signal that catches a
     * client-side level change.
     */
    @SubscribeEvent
    public void onCacheInvalidated(CacheInvalidatedEvent event) {
        refresh(event.getPlayer());
    }

    @SubscribeEvent
    public void onLogin(PlayerLoggedInEvent event) {
        refresh(event.player);
    }

    @SubscribeEvent
    public void onLogout(PlayerLoggedOutEvent event) {
        ReskillableBonuses.invalidateLevels(event.player);
    }

    @SubscribeEvent
    public void onRespawn(PlayerRespawnEvent event) {
        refresh(event.player);
    }

    @SubscribeEvent
    public void onClone(PlayerEvent.Clone event) {
        refresh(event.getEntityPlayer());
    }

    @SubscribeEvent
    public void onDimChange(PlayerChangedDimensionEvent event) {
        refresh(event.player);
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        World world = event.getWorld();
        if (world == null) return;
        ReskillableBonuses.invalidateLevels(world.isRemote);
    }

    @SubscribeEvent
    public void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (!ReskillableBonuses.enabled()) return;
        EntityPlayer player = event.getEntityPlayer();
        if (skipPlayer(player) || player.world.isRemote) return;
        double k = ArcanaQuestTweaksConfig.ReskillableModuleConfig.mining.breakSpeedPerLevel;
        if (k <= 0.0) return;
        int level = ReskillableBonuses.skillLevel(player, "mining");
        if (level <= 0) return;
        event.setNewSpeed((float) (event.getNewSpeed() * (1.0 + level * k)));
    }

    @SubscribeEvent
    public void onHarvestCheck(PlayerEvent.HarvestCheck event) {
        if (event.canHarvest()) return;
        EntityPlayer player = event.getEntityPlayer();
        if (skipPlayer(player)) return;
        if (!Reflect.hasUnlockable(player, "aqtweaks:mining_expert")) return;
        if (!ArcanaQuestTweaksConfig.ReskillableModuleConfig.perks.miningExpert.enable) return;
        ItemStack held = player.getHeldItemMainhand();
        if (held.isEmpty()) return;
        java.util.Set<String> classes = held.getItem().getToolClasses(held);
        if (classes == null || !classes.contains("pickaxe")) return;
        IBlockState state = event.getTargetBlock();
        if (state == null) return;
        String tool = state.getBlock().getHarvestTool(state);
        if (tool != null && !"pickaxe".equals(tool)) return;
        int floor = ArcanaQuestTweaksConfig.ReskillableModuleConfig.mining.expertHarvestFloor;
        if (state.getBlock().getHarvestLevel(state) > floor) return;
        event.setCanHarvest(true);
    }

    @SubscribeEvent
    public void onHarvest(BlockEvent.HarvestDropsEvent event) {
        if (!ReskillableBonuses.enabled()) return;
        EntityPlayer player = event.getHarvester();
        if (skipPlayer(player) || event.getWorld() == null || event.getWorld().isRemote) return;
        IBlockState state = event.getState();
        List<ItemStack> drops = event.getDrops();
        if (state == null || drops == null) return;

        if (Reflect.hasUnlockable(player, "aqtweaks:glass_cutter")
                && ArcanaQuestTweaksConfig.ReskillableModuleConfig.perks.glassCutter.enable
                && ReskillableBonuses.isCuttableGlass(event.getWorld(), event.getPos(), state)) {
            ItemStack self = ReskillableBonuses.glassSelfDrop(state);
            if (!self.isEmpty() && !ReskillableBonuses.hasSilkTouch(player)
                    && !ReskillableBonuses.dropsContain(drops, self)) {
                drops.add(self);
                event.setDropChance(1.0f);
            }
        }

        if (drops.isEmpty()) return;

        if (Reflect.hasUnlockable(player, "aqtweaks:herbalist")
                && ArcanaQuestTweaksConfig.ReskillableModuleConfig.perks.herbalist.enable
                && ReskillableBonuses.isHerbalistBlock(state)) {
            addOneExtra(drops);
            return;
        }

        if (ReskillableBonuses.isBountifulCrop(state)
                && Reflect.hasUnlockable(player, "aqtweaks:bountiful_harvest")
                && ArcanaQuestTweaksConfig.ReskillableModuleConfig.perks.bountifulHarvest.enable) {
            addOneExtra(drops);
            return;
        }

        if (ReskillableBonuses.isMatureCrop(state)) {
            double k = ArcanaQuestTweaksConfig.ReskillableModuleConfig.farming.extraDropChancePerLevel;
            if (ReskillableBonuses.roll(event.getWorld(), player, "farming", k)) {
                addOneExtra(drops);
            }
            return;
        }

        if (ReskillableBonuses.hasSilkTouch(player)) return;
        if (!ReskillableBonuses.isForageBlock(event.getWorld(), state)) return;
        double k = ArcanaQuestTweaksConfig.ReskillableModuleConfig.gathering.extraDropChancePerLevel;
        if (ReskillableBonuses.roll(event.getWorld(), player, "gathering", k)) {
            addOneExtra(drops);
        }
    }

    @SubscribeEvent
    public void onFish(ItemFishedEvent event) {
        if (!ReskillableBonuses.enabled()) return;
        EntityPlayer player = event.getEntityPlayer();
        if (skipPlayer(player) || player.world == null || player.world.isRemote) return;
        List<ItemStack> drops = event.getDrops();
        if (drops == null || drops.isEmpty()) return;
        double k = ArcanaQuestTweaksConfig.ReskillableModuleConfig.gathering.extraDropChancePerLevel;
        if (!ReskillableBonuses.roll(player.world, player, "gathering", k)) return;
        addOneExtra(drops);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onShear(PlayerInteractEvent.EntityInteract event) {
        if (!ReskillableBonuses.enabled()) return;
        if (event.getWorld() == null || event.getWorld().isRemote) return;
        EntityPlayer player = event.getEntityPlayer();
        if (skipPlayer(player)) return;
        ItemStack held = player.getHeldItem(event.getHand());
        if (held.isEmpty() || !(held.getItem() instanceof ItemShears)) return;
        Entity target = event.getTarget();
        if (event.isCanceled()) return;
        World world = event.getWorld();
        if (world.getMinecraftServer() == null) return;
        if (Reflect.hasUnlockable(player, "aqtweaks:herd_abundance")
                && ArcanaQuestTweaksConfig.ReskillableModuleConfig.perks.herdAbundance.enable) {
            extraFarmWoolAfterShear(world, target);
        }
        if (!(target instanceof EntitySheep) || !ReskillableBonuses.isShearableLiving(target)) return;
        if (isFarmWoolAnimal(target)) return;
        EntitySheep sheep = (EntitySheep) target;
        if (sheep.getSheared()) return;
        double k = ArcanaQuestTweaksConfig.ReskillableModuleConfig.gathering.extraDropChancePerLevel;
        if (!ReskillableBonuses.roll(world, player, "gathering", k)) return;
        int color = sheep.getFleeceColor().getMetadata();
        world.getMinecraftServer().addScheduledTask(() -> {
            if (sheep.isDead || !sheep.getSheared()) return;
            ItemStack wool = new ItemStack(Blocks.WOOL, 1, color);
            EntityItem dropped = new EntityItem(world, sheep.posX, sheep.posY, sheep.posZ, wool);
            world.spawnEntity(dropped);
        });
    }

    private static void extraFarmWoolAfterShear(World world, Entity target) {
        if (!isFarmWoolAnimal(target)) return;
        if (isEntitySheared(target)) return;
        world.getMinecraftServer().addScheduledTask(() -> {
            if (target.isDead || !isEntitySheared(target)) return;
            ItemStack wool = farmWoolStack(target);
            if (wool.isEmpty()) return;
            EntityItem dropped = new EntityItem(world, target.posX, target.posY, target.posZ, wool);
            world.spawnEntity(dropped);
        });
    }

    private static boolean isFarmWoolAnimal(Entity entity) {
        try {
            Object out = Class.forName("com.apocollis.aqtweaks.animania.AnimaniaFarmProducts")
                    .getMethod("isWoolAnimal", Entity.class)
                    .invoke(null, entity);
            return Boolean.TRUE.equals(out);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean isEntitySheared(Entity entity) {
        if (entity instanceof EntitySheep) {
            return ((EntitySheep) entity).getSheared();
        }
        try {
            Object out = Class.forName("com.apocollis.aqtweaks.animania.AnimaniaFarmProducts")
                    .getMethod("isSheared", Entity.class)
                    .invoke(null, entity);
            return Boolean.TRUE.equals(out);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static ItemStack farmWoolStack(Entity entity) {
        try {
            Object out = Class.forName("com.apocollis.aqtweaks.animania.AnimaniaFarmProducts")
                    .getMethod("woolStack", Entity.class)
                    .invoke(null, entity);
            return out instanceof ItemStack ? (ItemStack) out : ItemStack.EMPTY;
        } catch (Throwable ignored) {
            return ItemStack.EMPTY;
        }
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void onHurt(LivingHurtEvent event) {
        EntityLivingBase victim = event.getEntityLiving();
        if (victim == null || victim.world == null || victim.world.isRemote) return;
        DamageSource source = event.getSource();
        Entity trueSource = source == null ? null : source.getTrueSource();
        Entity immediate = source == null ? null : source.getImmediateSource();
        if (immediate instanceof net.minecraft.entity.projectile.EntityArrow arrow
                && arrow.getEntityData().getBoolean(NBT_PRECISION_ARROW)) {
            event.setAmount(event.getAmount() * 2.0f);
            arrow.getEntityData().removeTag(NBT_PRECISION_ARROW);
        }
        boolean classified = ReskillableBonuses.isSpellLike(source);
        ReskillableBonuses.maybeLogMagic(source, trueSource, victim, classified);
        if (ReskillableBonuses.enabled() && classified) {
            float amount = event.getAmount();
            if (trueSource instanceof EntityPlayer && !skipPlayer((EntityPlayer) trueSource)) {
                amount = ReskillableBonuses.scaleOutgoingMagic((EntityPlayer) trueSource, amount);
            }
            if (victim instanceof EntityPlayer && !skipPlayer((EntityPlayer) victim)) {
                amount *= ReskillableBonuses.magicMultiplier((EntityPlayer) victim, false);
            }
            event.setAmount(amount);
        }
        if (trueSource instanceof EntityPlayer attacker && !skipPlayer(attacker)) {
            MagicSchoolEffects.onOutgoingHurt(attacker, event);
        }
        if (victim instanceof EntityPlayer victimPlayer && !skipPlayer(victimPlayer)) {
            MagicSchoolEffects.onIncomingHurt(victimPlayer, event);
        }
    }

    @SubscribeEvent
    public void onSchoolAttack(net.minecraftforge.event.entity.living.LivingAttackEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer player) || skipPlayer(player)) return;
        if (player.world == null || player.world.isRemote) return;
        MagicSchoolEffects.onIncomingAttack(player, event);
    }

    @SubscribeEvent
    public void onSchoolTick(net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent event) {
        if (skipPlayer(event.player)) return;
        MagicSchoolEffects.onPlayerTick(event.player, event.phase);
    }

    @SubscribeEvent
    public void onArrowLoose(net.minecraftforge.event.entity.player.ArrowLooseEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        if (skipPlayer(player) || player.world == null || player.world.isRemote) return;
        if (!Reflect.hasUnlockable(player, "aqtweaks:precision_shot")) return;
        if (!ArcanaQuestTweaksConfig.ReskillableModuleConfig.perks.precisionShot.enable) return;
        if (!(event.getBow().getItem() instanceof net.minecraft.item.ItemBow)) return;
        if (event.getCharge() < 20) return;
        player.getEntityData().setBoolean(NBT_PRECISION_PENDING, true);
    }

    @SubscribeEvent
    public void onArrowJoin(net.minecraftforge.event.entity.EntityJoinWorldEvent event) {
        if (event.getWorld() == null || event.getWorld().isRemote) return;
        if (!(event.getEntity() instanceof net.minecraft.entity.projectile.EntityArrow arrow)) return;
        Entity shooter = arrow.shootingEntity;
        if (!(shooter instanceof EntityPlayer player) || skipPlayer(player)) return;
        if (!player.getEntityData().getBoolean(NBT_PRECISION_PENDING)) return;
        player.getEntityData().removeTag(NBT_PRECISION_PENDING);
        arrow.getEntityData().setBoolean(NBT_PRECISION_ARROW, true);
    }

    private static final String NBT_PRECISION_PENDING = "AqtweaksPrecisionPending";
    private static final String NBT_PRECISION_ARROW = "AqtweaksPrecisionShot";

    private static void refresh(EntityPlayer player) {
        ReskillableBonuses.invalidateLevels(player);
        applyAttributes(player);
    }

    public static void applyAttributes(EntityPlayer player) {
        if (player == null || player.world == null || player.world.isRemote) return;
        if (player instanceof FakePlayer) return;
        if (!ReskillableBonuses.enabled()) {
            clearModifier(player, SharedMonsterAttributes.ATTACK_DAMAGE, ReskillableBonuses.UUID_ATTACK);
            clearModifier(player, SharedMonsterAttributes.ARMOR, ReskillableBonuses.UUID_ARMOR);
            clearModifier(player, SharedMonsterAttributes.MOVEMENT_SPEED, ReskillableBonuses.UUID_SPEED);
            clearModifier(player, SharedMonsterAttributes.MAX_HEALTH, ReskillableBonuses.UUID_MAX_HEALTH);
            return;
        }
        stampAdd(player, SharedMonsterAttributes.ATTACK_DAMAGE, ReskillableBonuses.UUID_ATTACK,
                ReskillableBonuses.MOD_ATTACK, "attack",
                ArcanaQuestTweaksConfig.ReskillableModuleConfig.attack.damagePerLevel);
        stampAdd(player, SharedMonsterAttributes.ARMOR, ReskillableBonuses.UUID_ARMOR,
                ReskillableBonuses.MOD_ARMOR, "defense",
                ArcanaQuestTweaksConfig.ReskillableModuleConfig.defense.armorPerLevel);
        stampAdd(player, SharedMonsterAttributes.MOVEMENT_SPEED, ReskillableBonuses.UUID_SPEED,
                ReskillableBonuses.MOD_SPEED, "agility",
                ArcanaQuestTweaksConfig.ReskillableModuleConfig.agility.speedPerLevel);
        stampBloodPactHealth(player);
    }

    private static void stampBloodPactHealth(EntityPlayer player) {
        double amount = 0.0;
        if (ArcanaQuestTweaksConfig.ReskillableModuleConfig.perks.bloodPact.enable
                && Reflect.hasUnlockable(player, "aqtweaks:blood_pact")) {
            amount = ArcanaQuestTweaksConfig.ReskillableModuleConfig.magic.bloodPactMaxHealth;
        }
        IAttributeInstance instance = player.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH);
        if (instance == null) return;
        UUID uuid = ReskillableBonuses.UUID_MAX_HEALTH;
        AttributeModifier existing = instance.getModifier(uuid);
        if (amount == 0.0) {
            if (existing != null) instance.removeModifier(uuid);
            return;
        }
        if (existing != null && existing.getAmount() == amount && existing.getOperation() == 0
                && !existing.isSaved()) {
            clampHealth(player);
            return;
        }
        instance.removeModifier(uuid);
        instance.applyModifier(new AttributeModifier(uuid, ReskillableBonuses.MOD_MAX_HEALTH, amount, 0)
                .setSaved(false));
        clampHealth(player);
    }

    private static void clampHealth(EntityPlayer player) {
        float max = player.getMaxHealth();
        if (player.getHealth() > max) {
            player.setHealth(max);
        }
    }

    public static void restampOnlinePlayers() {
        ReskillableBonuses.restampOnlinePlayers();
    }

    private static void stampAdd(EntityPlayer player, IAttribute attribute, UUID uuid, String name,
            String skill, double k) {
        IAttributeInstance instance = player.getEntityAttribute(attribute);
        if (instance == null) return;
        double amount = k <= 0.0 ? 0.0 : ReskillableBonuses.skillLevel(player, skill) * k;
        AttributeModifier existing = instance.getModifier(uuid);
        if (amount == 0.0) {
            if (existing != null) instance.removeModifier(uuid);
            return;
        }
        // Re-applying an identical modifier still flags the attribute dirty, which resends
        // SPacketEntityProperties, so only touch it when the stamped value actually moved.
        if (existing != null && existing.getAmount() == amount && existing.getOperation() == 0
                && !existing.isSaved()) {
            return;
        }
        instance.removeModifier(uuid);
        instance.applyModifier(new AttributeModifier(uuid, name, amount, 0).setSaved(false));
    }

    private static void clearModifier(EntityPlayer player, IAttribute attribute, UUID uuid) {
        IAttributeInstance instance = player.getEntityAttribute(attribute);
        if (instance == null) return;
        instance.removeModifier(uuid);
    }

    private static boolean skipPlayer(EntityPlayer player) {
        return player == null || player instanceof FakePlayer;
    }

    private static void addOneExtra(List<ItemStack> drops) {
        for (ItemStack stack : drops) {
            if (stack == null || stack.isEmpty()) continue;
            if (stack.getCount() < stack.getMaxStackSize()) {
                stack.setCount(stack.getCount() + 1);
                return;
            }
        }
        for (ItemStack stack : drops) {
            if (stack == null || stack.isEmpty()) continue;
            ItemStack extra = stack.copy();
            extra.setCount(1);
            drops.add(extra);
            return;
        }
    }
}
