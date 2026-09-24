package com.apocollis.aqtweaks.reskillable;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.lycanitesmobs.PotionEffects;
import com.tmtravlr.potioncore.potion.PotionBrokenMagicShield;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.SPacketSoundEffect;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.entity.projectile.EntityPotion;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.init.Blocks;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemSeedFood;
import net.minecraft.item.ItemSeeds;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.oredict.OreDictionary;
import rustic.common.potions.PotionFeather;

import java.util.List;
import java.util.UUID;

public class PerkWishlist {

    private static final UUID KNOCKBACK = UUID.fromString("91b2c4de-6a10-4e55-8f3a-2d7c0b6e44c1");
    private static final String STILL = "AqtweaksFortifyStill";
    private static final String PROSPECT = "AqtweaksProspectorUntil";
    private static final String TUNNEL = "AqtweaksTunnelUntil";

    @SubscribeEvent
    public void onBreak(BlockEvent.BreakEvent event) {
        if (event.getWorld().isRemote) return;
        EntityPlayer player = event.getPlayer();
        PerkDrops.markFreeBreak(player, event.getState());
        prospect(event);
    }

    @SubscribeEvent
    public void onTrample(BlockEvent.FarmlandTrampleEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof EntityPlayer player)) return;
        if (PerkAccess.on(player, "aqtweaks:soft_step",
                ArcanaQuestTweaksConfig.ReskillableModuleConfig.perks.softStep.enable)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onFall(LivingFallEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer player)) return;
        if (player.world.isRemote) return;
        if (!PerkAccess.on(player, "aqtweaks:tumble",
                ArcanaQuestTweaksConfig.ReskillableModuleConfig.perks.tumble.enable)) return;
        event.setDamageMultiplier(event.getDamageMultiplier() * 0.5f);
    }

    @SubscribeEvent
    public void onFinishEat(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer player)) return;
        if (player.world.isRemote) return;
        if (!(event.getItem().getItem() instanceof ItemFood)) return;
        if (!PerkAccess.on(player, "aqtweaks:hearty_meal",
                ArcanaQuestTweaksConfig.ReskillableModuleConfig.perks.heartyMeal.enable)) return;
        player.getFoodStats().addStats(2, 0.0f);
    }

    @SubscribeEvent
    public void onPlant(PlayerInteractEvent.RightClickBlock event) {
        if (event.getWorld().isRemote || event.getHand() == null) return;
        EntityPlayer player = event.getEntityPlayer();
        if (!PerkAccess.on(player, "aqtweaks:sower",
                ArcanaQuestTweaksConfig.ReskillableModuleConfig.perks.sower.enable)) return;
        ItemStack seeds = player.getHeldItem(event.getHand());
        if (!isSeed(seeds)) return;
        BlockPos clicked = event.getPos();
        if (event.getWorld().getBlockState(clicked).getBlock() != Blocks.FARMLAND) return;
        World world = event.getWorld();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                if (seeds.isEmpty()) return;
                BlockPos farm = clicked.add(dx, 0, dz);
                if (world.getBlockState(farm).getBlock() != Blocks.FARMLAND) continue;
                if (!world.isAirBlock(farm.up())) continue;
                seeds.onItemUse(player, world, farm, event.getHand(), EnumFacing.UP, 0.5f, 1.0f, 0.5f);
                seeds = player.getHeldItem(event.getHand());
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void onPickFruit(PlayerInteractEvent.RightClickBlock event) {
        if (event.getWorld().isRemote) return;
        EntityPlayer player = event.getEntityPlayer();
        if (!PerkDrops.orchardRoll(event.getWorld(), player)) return;
        IBlockState state = event.getWorld().getBlockState(event.getPos());
        ItemStack fruit = OrchardFruit.bonus(event.getWorld(), event.getPos(), state);
        OrchardFruit.give(player, fruit);
    }

    @SubscribeEvent
    public void onHurt(LivingHurtEvent event) {
        if (event.getEntityLiving() == null || event.getEntityLiving().world.isRemote) return;
        Entity trueSource = event.getSource().getTrueSource();
        Entity immediate = event.getSource().getImmediateSource();
        if (trueSource instanceof EntityPlayer player) {
            boolean melee = immediate == trueSource;
            if (melee) {
                finisher(player, event);
                aura(player, event.getEntityLiving());
                bleed(player, event.getEntityLiving());
            } else {
                pin(player, event);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onHurtRecord(LivingHurtEvent event) {
        if (event.getEntityLiving() == null || event.getEntityLiving().world.isRemote) return;
        if (event.getSource().getTrueSource() instanceof EntityLivingBase attacker) {
            PerkOpportunistic.record(event.getEntityLiving(), attacker);
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.world.isRemote) return;
        EntityPlayer player = event.player;
        darkVision(player);
        tunnel(player);
        fortify(player);
        unyielding(player);
        slowFall(player);
        if (player.ticksExisted % 20 == 0) {
            husbandry(player);
        }
    }

    private static void finisher(EntityPlayer player, LivingHurtEvent event) {
        if (!(event.getEntityLiving() instanceof EntityLiving)) return;
        if (event.getEntityLiving() instanceof EntityPlayer) return;
        if (!PerkAccess.on(player, "aqtweaks:finisher",
                ArcanaQuestTweaksConfig.ReskillableModuleConfig.perks.finisher.enable)) return;
        float max = event.getEntityLiving().getMaxHealth();
        if (max <= 0.0f) return;
        if (event.getEntityLiving().getHealth() / max > 0.25f) return;
        event.setAmount(event.getAmount() * 1.5f);
    }

    private static void aura(EntityPlayer player, EntityLivingBase target) {
        if (!PerkAccess.on(player, "aqtweaks:aura_breaker",
                ArcanaQuestTweaksConfig.ReskillableModuleConfig.perks.auraBreaker.enable)) return;
        Potion potion = potion("potioncore", "broken_magic_shield");
        if (!(potion instanceof PotionBrokenMagicShield)) return;
        target.addPotionEffect(new PotionEffect(potion, 100, 2));
    }

    private static void bleed(EntityPlayer player, EntityLivingBase target) {
        if (!PerkAccess.on(player, "aqtweaks:bleeding_edge",
                ArcanaQuestTweaksConfig.ReskillableModuleConfig.perks.bleedingEdge.enable)) return;
        Potion potion = potion("lycanitesmobs", "bleed");
        if (potion == null) return;
        if (PotionEffects.class == null) return;
        target.addPotionEffect(new PotionEffect(potion, 320, 0));
    }

    private static void pin(EntityPlayer player, LivingHurtEvent event) {
        if (!PerkAccess.on(player, "aqtweaks:pinning_shot",
                ArcanaQuestTweaksConfig.ReskillableModuleConfig.perks.pinningShot.enable)) return;
        if (event.getSource().isMagicDamage()) return;
        Entity immediate = event.getSource().getImmediateSource();
        if (immediate instanceof EntityFireball || immediate instanceof EntityPotion) return;
        if (!(immediate instanceof EntityArrow) && !(immediate instanceof EntityThrowable)) return;
        event.getEntityLiving().addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 40, 1));
    }

    private static void darkVision(EntityPlayer player) {
        boolean on = PerkAccess.on(player, "aqtweaks:dark_vision",
                ArcanaQuestTweaksConfig.ReskillableModuleConfig.perks.darkVision.enable);
        int light = player.world.getLight(new BlockPos(player.posX, player.posY, player.posZ));
        if (on && light <= 7) {
            player.addPotionEffect(new PotionEffect(MobEffects.NIGHT_VISION, 220, 0, true, false));
            return;
        }
        if (light >= 9) {
            PotionEffect active = player.getActivePotionEffect(MobEffects.NIGHT_VISION);
            if (active != null && active.getDuration() <= 230) {
                player.removePotionEffect(MobEffects.NIGHT_VISION);
            }
        }
    }

    private static void tunnel(EntityPlayer player) {
        if (!PerkAccess.on(player, "aqtweaks:tunnel_sense",
                ArcanaQuestTweaksConfig.ReskillableModuleConfig.perks.tunnelSense.enable)) return;
        if (player.ticksExisted % 10 != 0) return;
        long now = player.world.getTotalWorldTime();
        if (player.getEntityData().getLong(TUNNEL) > now) return;
        List<EntityMob> mobs = player.world.getEntitiesWithinAABB(EntityMob.class,
                player.getEntityBoundingBox().grow(10.0));
        boolean hit = false;
        for (EntityMob mob : mobs) {
            if (mob.getDistance(player) > 10.0) continue;
            mob.addPotionEffect(new PotionEffect(MobEffects.GLOWING, 100, 0, true, false));
            hit = true;
        }
        if (!hit) return;
        if (player instanceof EntityPlayerMP mp) {
            mp.connection.sendPacket(new SPacketSoundEffect(SoundEvents.BLOCK_NOTE_PLING, SoundCategory.PLAYERS,
                    player.posX, player.posY, player.posZ, 0.6f, 1.0f));
        }
        player.getEntityData().setLong(TUNNEL, now + 100);
    }

    private static void fortify(EntityPlayer player) {
        boolean on = PerkAccess.on(player, "aqtweaks:fortify",
                ArcanaQuestTweaksConfig.ReskillableModuleConfig.perks.fortify.enable);
        if (!on || !player.isActiveItemStackBlocking()) {
            player.getEntityData().setInteger(STILL, 0);
            clearShort(player, MobEffects.RESISTANCE, 50);
            return;
        }
        BlockPos now = new BlockPos(player.posX, player.posY, player.posZ);
        BlockPos last = new BlockPos(player.getEntityData().getInteger("AqtweaksFortifyX"),
                player.getEntityData().getInteger("AqtweaksFortifyY"),
                player.getEntityData().getInteger("AqtweaksFortifyZ"));
        int still = player.getEntityData().getInteger(STILL);
        if (now.equals(last)) {
            still++;
        } else {
            still = 0;
        }
        player.getEntityData().setInteger(STILL, still);
        player.getEntityData().setInteger("AqtweaksFortifyX", now.getX());
        player.getEntityData().setInteger("AqtweaksFortifyY", now.getY());
        player.getEntityData().setInteger("AqtweaksFortifyZ", now.getZ());
        if (still >= 20) {
            player.addPotionEffect(new PotionEffect(MobEffects.RESISTANCE, 40, 1, true, false));
        } else {
            clearShort(player, MobEffects.RESISTANCE, 50);
        }
    }

    private static void unyielding(EntityPlayer player) {
        var attribute = player.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE);
        if (attribute == null) return;
        attribute.removeModifier(KNOCKBACK);
        if (!PerkAccess.on(player, "aqtweaks:unyielding",
                ArcanaQuestTweaksConfig.ReskillableModuleConfig.perks.unyielding.enable)) return;
        if (!player.isActiveItemStackBlocking()) return;
        attribute.applyModifier(new AttributeModifier(KNOCKBACK, "aqtweaks.unyielding", 1.0, 0));
    }

    private static void slowFall(EntityPlayer player) {
        boolean on = PerkAccess.on(player, "aqtweaks:slow_fall",
                ArcanaQuestTweaksConfig.ReskillableModuleConfig.perks.slowFall.enable);
        Potion feather = potion("rustic", "feather");
        if (!(feather instanceof PotionFeather)) return;
        boolean wall = on && player.motionY < 0.0 && !player.onGround && besideSolid(player);
        if (wall) {
            player.addPotionEffect(new PotionEffect(feather, 8, 0, true, false));
        } else {
            PotionEffect active = player.getActivePotionEffect(feather);
            if (active != null && active.getDuration() <= 10) {
                player.removePotionEffect(feather);
            }
        }
    }

    private static void husbandry(EntityPlayer player) {
        if (!PerkAccess.on(player, "aqtweaks:husbandry",
                ArcanaQuestTweaksConfig.ReskillableModuleConfig.perks.husbandry.enable)) return;
        AxisAlignedBB box = player.getEntityBoundingBox().grow(16.0);
        for (EntityAgeable animal : player.world.getEntitiesWithinAABB(EntityAgeable.class, box)) {
            int age = animal.getGrowingAge();
            if (age < 0) animal.setGrowingAge(Math.min(0, age + 20));
            else if (age > 0) animal.setGrowingAge(Math.max(0, age - 20));
        }
    }

    private static void prospect(BlockEvent.BreakEvent event) {
        EntityPlayer player = event.getPlayer();
        if (!PerkAccess.on(player, "aqtweaks:prospector",
                ArcanaQuestTweaksConfig.ReskillableModuleConfig.perks.prospector.enable)) return;
        Block block = event.getState().getBlock();
        if (block != Blocks.STONE && block != Blocks.COBBLESTONE && block != Blocks.STONEBRICK) return;
        long now = event.getWorld().getTotalWorldTime();
        if (player.getEntityData().getLong(PROSPECT) > now) return;
        player.getEntityData().setLong(PROSPECT, now + 300);
        if (!(event.getWorld() instanceof WorldServer server)) return;
        BlockPos origin = event.getPos();
        for (BlockPos pos : BlockPos.getAllInBoxMutable(origin.add(-5, -5, -5), origin.add(5, 5, 5))) {
            if (!ReskillableBonuses.isOreBlock(event.getWorld(), event.getWorld().getBlockState(pos))) continue;
            server.spawnParticle(EnumParticleTypes.VILLAGER_HAPPY, false,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    4, 0.2, 0.2, 0.2, 0.01);
        }
    }

    private static boolean besideSolid(EntityPlayer player) {
        BlockPos feet = new BlockPos(player.posX, player.posY, player.posZ);
        BlockPos[] around = {feet.north(), feet.south(), feet.east(), feet.west(),
                feet.up().north(), feet.up().south(), feet.up().east(), feet.up().west()};
        for (BlockPos pos : around) {
            IBlockState state = player.world.getBlockState(pos);
            if (state.getMaterial().isLiquid()) continue;
            if (!state.isFullCube()) continue;
            if (player.getDistanceSq(pos) > 2.25) continue;
            return true;
        }
        return false;
    }

    private static boolean isSeed(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getItem() instanceof ItemSeeds || stack.getItem() instanceof ItemSeedFood) return true;
        int[] ids = OreDictionary.getOreIDs(stack);
        for (int id : ids) {
            String name = OreDictionary.getOreName(id);
            if (name != null && (name.startsWith("seed") || "listAllseed".equals(name))) return true;
        }
        return false;
    }

    private static Potion potion(String namespace, String path) {
        return Potion.REGISTRY.getObject(new ResourceLocation(namespace, path));
    }

    private static void clearShort(EntityPlayer player, Potion potion, int maxDuration) {
        PotionEffect active = player.getActivePotionEffect(potion);
        if (active != null && active.getDuration() <= maxDuration) {
            player.removePotionEffect(potion);
        }
    }

}
