package com.apocollis.aqtweaks.simpledifficulty;

import com.apocollis.aqtweaks.ArcanaQuestTweaks;
import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.util.Reflect;
import com.charles445.simpledifficulty.api.SDCapabilities;
import com.charles445.simpledifficulty.api.SDDamageSources;
import com.charles445.simpledifficulty.api.SDItems;
import com.charles445.simpledifficulty.api.SDPotions;
import com.charles445.simpledifficulty.api.thirst.IThirstCapability;
import com.elenai.elenaidodge2.api.FeathersHelper;
import com.elenai.elenaidodge2.api.MaxFeathersEvent;
import com.elenai.elenaidodge2.util.Utils;
import com.elenai.elenaidodge2.capability.absorption.AbsorptionProvider;
import com.elenai.elenaidodge2.capability.absorption.IAbsorption;
import com.elenai.elenaidodge2.network.PacketHandler;
import com.elenai.elenaidodge2.network.message.CUpdateAbsorptionMessage;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Water Collector glass-bottle fill, plus hypothermia and hyperthermia.
 * Registered only when Simple Difficulty is loaded.
 */
public class SimpleDifficultyModule {

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        EntityPlayer player = event.getEntityPlayer();
        if (player == null || player instanceof FakePlayer) return;
        World world = event.getWorld();
        if (world == null) return;
        ItemStack held = event.getItemStack();
        if (held.isEmpty() || held.getItem() != Items.GLASS_BOTTLE) return;
        if (!ArcanaQuestTweaksConfig.ReskillableModuleConfig.perks.waterCollector.enable) return;
        if (!Reflect.hasUnlockable(player, "aqtweaks:water_collector")) return;
        RayTraceResult hit = rayWater(world, player);
        if (hit == null || hit.typeOfHit != RayTraceResult.Type.BLOCK) return;
        BlockPos pos = hit.getBlockPos();
        if (world.getBlockState(pos).getMaterial() != Material.WATER) return;
        event.setCanceled(true);
        event.setCancellationResult(EnumActionResult.SUCCESS);
        if (world.isRemote) return;
        world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.ITEM_BOTTLE_FILL,
                SoundCategory.NEUTRAL, 1.0f, 1.0f);
        if (!player.capabilities.isCreativeMode) {
            held.shrink(1);
        }
        ItemStack purified = new ItemStack(SDItems.purifiedWaterBottle);
        if (held.isEmpty()) {
            player.setHeldItem(event.getHand(), purified);
        } else if (!player.inventory.addItemStackToInventory(purified)) {
            player.dropItem(purified, false);
        }
        EnumHand hand = event.getHand();
        if (hand != null) player.swingArm(hand);
    }

    private static RayTraceResult rayWater(World world, EntityPlayer player) {
        float reach = (float) player.getEntityAttribute(EntityPlayer.REACH_DISTANCE).getAttributeValue();
        Vec3d start = player.getPositionEyes(1.0f);
        Vec3d look = player.getLook(1.0f);
        Vec3d end = start.add(look.x * reach, look.y * reach, look.z * reach);
        return world.rayTraceBlocks(start, end, true, false, false);
    }

    private static final int SLOWNESS_DURATION = 60;
    private static final int SLOWNESS_REFRESH_TICKS = 20;
    private static final int RECOVERY_TICKS = 10;
    private static final String NBT_HYPO_TICKS = "aqt_hypothermia_ticks";
    private static final String NBT_HYPO_SLOWNESS = "aqt_hypothermia_slowness";
    private static final String NBT_HYPO_PENALTY = "aqt_hypo_max_penalty";
    private static final String NBT_HYPO_RECOVER = "aqt_hypo_recover_ticks";
    private static final String NBT_HYPER_TICKS = "aqt_hyperthermia_ticks";
    private static final String NBT_HYPER_PENALTY = "aqt_hyper_max_penalty";
    private static final String NBT_HYPER_THIRST = "aqt_hyper_thirst_ticks";
    private static final String NBT_HYPER_RECOVER = "aqt_hyper_recover_ticks";
    private static final String NBT_LETHAL_FREEZE = "aqt_lethal_freeze";
    private static final String NBT_LETHAL_HEAT = "aqt_lethal_heat";

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onThermiaAttack(LivingAttackEvent event) {
        var cfg = ArcanaQuestTweaksConfig.StaminaModuleConfig.simpleDifficulty;
        if (!cfg.enableTemperatureEffects || !cfg.disableThermiaDamage) return;
        if (!(event.getEntityLiving() instanceof EntityPlayer player)) return;
        DamageSource source = event.getSource();
        if (source == null) return;
        NBTTagCompound data = player.getEntityData();
        if (source == SDDamageSources.HYPOTHERMIA) {
            if (!data.getBoolean(NBT_LETHAL_FREEZE)) event.setCanceled(true);
        } else if (source == SDDamageSources.HYPERTHERMIA) {
            if (!data.getBoolean(NBT_LETHAL_HEAT)) event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        EntityPlayer player = event.player;
        if (player == null || player.world.isRemote || player instanceof FakePlayer) return;
        if (!(player instanceof EntityPlayerMP playerMP)) return;
        if (playerMP.capabilities.isCreativeMode || playerMP.isSpectator() || !playerMP.isEntityAlive()) return;

        var cfg = ArcanaQuestTweaksConfig.StaminaModuleConfig.simpleDifficulty;
        NBTTagCompound data = playerMP.getEntityData();
        if (!cfg.enableTemperatureEffects) {
            clearHypothermiaState(playerMP, data);
            clearHyperthermiaState(playerMP, data);
            return;
        }

        if (playerMP.isPotionActive(SDPotions.hypothermia)) {
            data.setInteger(NBT_HYPO_RECOVER, 0);
            applyHypothermia(playerMP, data, cfg);
        } else {
            easeHypothermia(playerMP, data, cfg);
        }

        if (playerMP.isPotionActive(SDPotions.hyperthermia)) {
            data.setInteger(NBT_HYPER_RECOVER, 0);
            applyHyperthermia(playerMP, data, cfg);
        } else {
            easeHyperthermia(playerMP, data, cfg);
        }
    }

    @SubscribeEvent
    public void onMaxFeathers(MaxFeathersEvent event) {
        var cfg = ArcanaQuestTweaksConfig.StaminaModuleConfig.simpleDifficulty;
        if (!cfg.enableTemperatureEffects) return;
        EntityPlayer player = event.getPlayer();
        if (player == null) return;
        NBTTagCompound data = player.getEntityData();
        int penalty = 0;
        if (player.isPotionActive(SDPotions.hypothermia)) {
            penalty = data.getInteger(NBT_HYPO_TICKS) / Math.max(1, cfg.hypothermiaMaxStaminaReductionRampTicks);
        } else if (player.isPotionActive(SDPotions.hyperthermia)) {
            penalty = data.getInteger(NBT_HYPER_TICKS) / Math.max(1, cfg.hyperthermiaMaxStaminaReductionRampTicks);
        } else {
            penalty = Math.max(data.getInteger(NBT_HYPO_PENALTY), data.getInteger(NBT_HYPER_PENALTY));
        }
        if (penalty > 0) {
            event.setMaximum(Math.max(1, event.getMaximum() - penalty));
        }
    }

    private static void applyHypothermia(EntityPlayerMP player, NBTTagCompound data,
            ArcanaQuestTweaksConfig.SimpleDifficulty cfg) {
        int ticks = data.getInteger(NBT_HYPO_TICKS) + 1;
        data.setInteger(NBT_HYPO_TICKS, ticks);
        if (cfg.hypothermiaSlowness) {
            int ramp = Math.max(1, cfg.hypothermiaSlownessRampTicks);
            int amp = Math.min(cfg.hypothermiaMaxSlownessAmplifier, ticks / ramp);
            PotionEffect active = player.getActivePotionEffect(MobEffects.SLOWNESS);
            if (active == null || active.getAmplifier() < amp || active.getDuration() <= SLOWNESS_REFRESH_TICKS) {
                player.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, SLOWNESS_DURATION, amp, true, false));
                data.setInteger(NBT_HYPO_SLOWNESS, amp);
            }
        }
        clearGoldFeathers(player);
        int penalty = ticks / Math.max(1, cfg.hypothermiaMaxStaminaReductionRampTicks);
        stepMaxStamina(player, data, NBT_HYPO_PENALTY, penalty,
                cfg.hypothermiaLethalZeroMaxStamina, NBT_LETHAL_FREEZE, SDDamageSources.HYPOTHERMIA);
    }

    private static void applyHyperthermia(EntityPlayerMP player, NBTTagCompound data,
            ArcanaQuestTweaksConfig.SimpleDifficulty cfg) {
        int ticks = data.getInteger(NBT_HYPER_TICKS) + 1;
        data.setInteger(NBT_HYPER_TICKS, ticks);
        int thirstTicks = data.getInteger(NBT_HYPER_THIRST) + 1;
        if (thirstTicks >= 20) {
            thirstTicks = 0;
            IThirstCapability thirst = SDCapabilities.getThirstData(player);
            if (thirst != null && cfg.hyperthermiaThirstExhaustionPerSecond > 0.0) {
                thirst.addThirstExhaustion((float) cfg.hyperthermiaThirstExhaustionPerSecond);
            }
        }
        data.setInteger(NBT_HYPER_THIRST, thirstTicks);

        clearGoldFeathers(player);
        int penalty = ticks / Math.max(1, cfg.hyperthermiaMaxStaminaReductionRampTicks);
        stepMaxStamina(player, data, NBT_HYPER_PENALTY, penalty,
                cfg.hyperthermiaLethalZeroMaxStamina, NBT_LETHAL_HEAT, SDDamageSources.HYPERTHERMIA);
    }

    private static void clearGoldFeathers(EntityPlayerMP player) {
        IAbsorption absorption = player.getCapability(AbsorptionProvider.ABSORPTION_CAP, null);
        boolean changed = false;
        if (absorption != null && absorption.getAbsorption() > 0) {
            absorption.set(0);
            changed = true;
        }
        Potion feathers = Potion.getPotionFromResourceLocation("elenaidodge2:feathers");
        if (feathers != null && player.isPotionActive(feathers)) {
            player.removePotionEffect(feathers);
            changed = true;
        }
        if (changed) {
            PacketHandler.instance.sendTo(new CUpdateAbsorptionMessage(0), player);
        }
    }

    private static void lethalThermia(EntityPlayerMP player, NBTTagCompound data, String flag, DamageSource source) {
        data.setBoolean(flag, true);
        player.attackEntityFrom(source, player.getMaxHealth() * 2.0f);
        data.setBoolean(flag, false);
    }

    private static void clearHypothermiaState(EntityPlayerMP player, NBTTagCompound data) {
        clearOwnedSlowness(player, data);
        boolean hadCap = data.getInteger(NBT_HYPO_PENALTY) > 0 || data.getInteger(NBT_HYPO_TICKS) > 0;
        data.setInteger(NBT_HYPO_TICKS, 0);
        data.setInteger(NBT_HYPO_PENALTY, 0);
        data.setInteger(NBT_HYPO_RECOVER, 0);
        if (hadCap) {
            syncMaxToClient(player);
        }
    }

    private static void easeHypothermia(EntityPlayerMP player, NBTTagCompound data,
            ArcanaQuestTweaksConfig.SimpleDifficulty cfg) {
        clearOwnedSlowness(player, data);
        easePenalty(player, data, NBT_HYPO_TICKS, NBT_HYPO_PENALTY, NBT_HYPO_RECOVER,
                cfg.hypothermiaMaxStaminaReductionRampTicks);
    }

    private static void easeHyperthermia(EntityPlayerMP player, NBTTagCompound data,
            ArcanaQuestTweaksConfig.SimpleDifficulty cfg) {
        data.setInteger(NBT_HYPER_THIRST, 0);
        easePenalty(player, data, NBT_HYPER_TICKS, NBT_HYPER_PENALTY, NBT_HYPER_RECOVER,
                cfg.hyperthermiaMaxStaminaReductionRampTicks);
    }

    private static void easePenalty(EntityPlayerMP player, NBTTagCompound data,
            String ticksKey, String penaltyKey, String recoverKey, int rampTicks) {
        int penalty = data.getInteger(penaltyKey);
        if (penalty <= 0 && data.getInteger(ticksKey) <= 0) {
            data.setInteger(recoverKey, 0);
            return;
        }
        int recover = data.getInteger(recoverKey) + 1;
        if (recover < RECOVERY_TICKS) {
            data.setInteger(recoverKey, recover);
            return;
        }
        data.setInteger(recoverKey, 0);
        int next = Math.max(0, penalty - 1);
        int ramp = Math.max(1, rampTicks);
        int ticks = Math.max(0, data.getInteger(ticksKey) - ramp);
        data.setInteger(ticksKey, next == 0 ? 0 : ticks);
        data.setInteger(penaltyKey, next);
        if (next >= Utils.getBaseDodges()) {
            ArcanaQuestTweaks.NETWORK.sendTo(new PacketTemperatureMax(0), player);
        } else {
            syncMaxToClient(player);
        }
    }

    private static void clearOwnedSlowness(EntityPlayer player, NBTTagCompound data) {
        if (!data.hasKey(NBT_HYPO_SLOWNESS)) return;
        PotionEffect active = player.getActivePotionEffect(MobEffects.SLOWNESS);
        int owned = data.getInteger(NBT_HYPO_SLOWNESS);
        if (active != null
                && active.getIsAmbient()
                && active.getAmplifier() == owned
                && active.getDuration() <= SLOWNESS_DURATION) {
            player.removePotionEffect(MobEffects.SLOWNESS);
        }
        data.removeTag(NBT_HYPO_SLOWNESS);
    }

    private static void clearHyperthermiaState(EntityPlayerMP player, NBTTagCompound data) {
        boolean hadCap = data.getInteger(NBT_HYPER_PENALTY) > 0 || data.getInteger(NBT_HYPER_TICKS) > 0;
        data.setInteger(NBT_HYPER_TICKS, 0);
        data.setInteger(NBT_HYPER_PENALTY, 0);
        data.setInteger(NBT_HYPER_THIRST, 0);
        data.setInteger(NBT_HYPER_RECOVER, 0);
        if (hadCap) {
            syncMaxToClient(player);
        }
    }

    private static void stepMaxStamina(EntityPlayerMP player, NBTTagCompound data, String penaltyKey, int penalty,
            boolean lethalEnabled, String lethalFlag, DamageSource source) {
        if (data.getInteger(penaltyKey) == penalty) return;
        data.setInteger(penaltyKey, penalty);
        if (penalty >= Utils.getBaseDodges()) {
            int level = FeathersHelper.getFeatherLevel(player);
            if (level > 0) {
                FeathersHelper.decreaseFeathers(player, level);
            }
            ArcanaQuestTweaks.NETWORK.sendTo(new PacketTemperatureMax(0), player);
            if (lethalEnabled) {
                lethalThermia(player, data, lethalFlag, source);
            }
            return;
        }
        int max = Utils.getMaxDodges(player);
        int level = FeathersHelper.getFeatherLevel(player);
        int drop = level - max;
        if (drop > 0) {
            FeathersHelper.decreaseFeathers(player, drop);
        }
        ArcanaQuestTweaks.NETWORK.sendTo(new PacketTemperatureMax(max), player);
    }

    private static void syncMaxToClient(EntityPlayerMP player) {
        ArcanaQuestTweaks.NETWORK.sendTo(new PacketTemperatureMax(Utils.getMaxDodges(player)), player);
    }
}
