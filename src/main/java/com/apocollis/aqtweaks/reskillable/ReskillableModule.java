package com.apocollis.aqtweaks.reskillable;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
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
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerChangedDimensionEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.List;
import java.util.UUID;

public class ReskillableModule {

    @SubscribeEvent
    public void onLevelUp(LevelUpEvent.Post event) {
        applyAttributes(event.getEntityPlayer());
    }

    @SubscribeEvent
    public void onLogin(PlayerLoggedInEvent event) {
        applyAttributes(event.player);
    }

    @SubscribeEvent
    public void onRespawn(PlayerRespawnEvent event) {
        applyAttributes(event.player);
    }

    @SubscribeEvent
    public void onClone(PlayerEvent.Clone event) {
        applyAttributes(event.getEntityPlayer());
    }

    @SubscribeEvent
    public void onDimChange(PlayerChangedDimensionEvent event) {
        applyAttributes(event.player);
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        EntityPlayer player = event.player;
        if (player == null || player.world == null || player.world.isRemote) return;
        if (player.ticksExisted % 20 != 0) return;
        applyAttributes(player);
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
    public void onHarvest(BlockEvent.HarvestDropsEvent event) {
        if (!ReskillableBonuses.enabled()) return;
        EntityPlayer player = event.getHarvester();
        if (skipPlayer(player) || event.getWorld() == null || event.getWorld().isRemote) return;
        IBlockState state = event.getState();
        List<ItemStack> drops = event.getDrops();
        if (state == null || drops == null || drops.isEmpty()) return;

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
        if (!(target instanceof EntitySheep) || !ReskillableBonuses.isShearableLiving(target)) return;
        EntitySheep sheep = (EntitySheep) target;
        if (sheep.getSheared()) return;
        double k = ArcanaQuestTweaksConfig.ReskillableModuleConfig.gathering.extraDropChancePerLevel;
        if (!ReskillableBonuses.roll(event.getWorld(), player, "gathering", k)) return;
        int color = sheep.getFleeceColor().getMetadata();
        World world = event.getWorld();
        if (world.getMinecraftServer() == null) return;
        world.getMinecraftServer().addScheduledTask(() -> {
            if (sheep.isDead || !sheep.getSheared()) return;
            ItemStack wool = new ItemStack(Blocks.WOOL, 1, color);
            EntityItem dropped = new EntityItem(world, sheep.posX, sheep.posY, sheep.posZ, wool);
            world.spawnEntity(dropped);
        });
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void onHurt(LivingHurtEvent event) {
        if (!ReskillableBonuses.enabled()) return;
        EntityLivingBase victim = event.getEntityLiving();
        if (victim == null || victim.world == null || victim.world.isRemote) return;
        DamageSource source = event.getSource();
        Entity trueSource = source == null ? null : source.getTrueSource();
        boolean classified = ReskillableBonuses.isSpellLike(source);
        ReskillableBonuses.maybeLogMagic(source, trueSource, victim, classified);
        if (!classified) return;

        float amount = event.getAmount();
        if (trueSource instanceof EntityPlayer && !skipPlayer((EntityPlayer) trueSource)) {
            amount *= ReskillableBonuses.magicMultiplier((EntityPlayer) trueSource, true);
        }
        if (victim instanceof EntityPlayer && !skipPlayer((EntityPlayer) victim)) {
            amount *= ReskillableBonuses.magicMultiplier((EntityPlayer) victim, false);
        }
        event.setAmount(amount);
    }

    public static void applyAttributes(EntityPlayer player) {
        if (player == null || player.world == null || player.world.isRemote) return;
        if (player instanceof FakePlayer) return;
        if (!ReskillableBonuses.enabled()) {
            clearModifier(player, SharedMonsterAttributes.ATTACK_DAMAGE, ReskillableBonuses.UUID_ATTACK);
            clearModifier(player, SharedMonsterAttributes.ARMOR, ReskillableBonuses.UUID_ARMOR);
            clearModifier(player, SharedMonsterAttributes.MOVEMENT_SPEED, ReskillableBonuses.UUID_SPEED);
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
    }

    public static void restampOnlinePlayers() {
        ReskillableBonuses.restampOnlinePlayers();
    }

    private static void stampAdd(EntityPlayer player, IAttribute attribute, UUID uuid, String name,
            String skill, double k) {
        IAttributeInstance instance = player.getEntityAttribute(attribute);
        if (instance == null) return;
        instance.removeModifier(uuid);
        if (k <= 0.0) return;
        double amount = ReskillableBonuses.skillLevel(player, skill) * k;
        if (amount == 0.0) return;
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
