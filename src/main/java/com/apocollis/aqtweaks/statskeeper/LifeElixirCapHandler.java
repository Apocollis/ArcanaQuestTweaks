package com.apocollis.aqtweaks.statskeeper;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;

import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import terrails.statskeeper.api.capabilities.IHealth;
import terrails.statskeeper.api.capabilities.SKCapabilities;
import terrails.statskeeper.config.configs.SKHealthConfig;

public class LifeElixirCapHandler {

    private static final ResourceLocation LIFE_ELIXIR = new ResourceLocation("contenttweaker", "life_elixir");

    @SubscribeEvent
    public void onItemUseStart(LivingEntityUseItemEvent.Start event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer player)) {
            return;
        }
        if (!shouldBlock(player, event.getItem())) {
            return;
        }
        event.setCanceled(true);
        if (!player.world.isRemote) {
            player.sendStatusMessage(new TextComponentTranslation("chat.aqtweaks.life_elixir.max_health")
                    .setStyle(new Style().setColor(TextFormatting.RED)), true);
        }
    }

    @SubscribeEvent
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        EntityPlayer player = event.getEntityPlayer();
        if (!shouldBlock(player, event.getItemStack())) {
            return;
        }
        event.setCanceled(true);
    }

    @SubscribeEvent
    public void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer player)) {
            return;
        }
        if (player.world.isRemote || player.isSpectator() || !isLifeElixir(event.getItem())) {
            return;
        }
        if (!SKHealthConfig.enabled) {
            return;
        }
        SoundEvent sound = resolveDrinkSound();
        if (sound == null) {
            return;
        }
        player.world.playSound(null, player.posX, player.posY, player.posZ,
                sound, SoundCategory.PLAYERS, 0.75F, 1.0F);
    }

    private static SoundEvent resolveDrinkSound() {
        String id = ArcanaQuestTweaksConfig.StatsKeeperModuleConfig.general.elixirDrinkSound;
        if (id == null || id.trim().isEmpty()) {
            return null;
        }
        ResourceLocation loc;
        try {
            loc = new ResourceLocation(id.trim());
        } catch (Exception e) {
            return SoundEvents.ENTITY_PLAYER_LEVELUP;
        }
        SoundEvent sound = SoundEvent.REGISTRY.getObject(loc);
        return sound != null ? sound : SoundEvents.ENTITY_PLAYER_LEVELUP;
    }

    private static boolean isLifeElixir(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        ResourceLocation name = stack.getItem().getRegistryName();
        return name != null && LIFE_ELIXIR.equals(name);
    }

    private static boolean shouldBlock(EntityPlayer player, ItemStack stack) {
        if (player.isSpectator() || !isLifeElixir(stack)) {
            return false;
        }
        if (!SKHealthConfig.enabled) {
            return false;
        }
        IHealth cap = SKCapabilities.getCapability(player);
        if (cap == null) {
            return false;
        }
        IAttributeInstance attr = player.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH);
        if (attr == null) {
            return false;
        }
        int current = (int) attr.getBaseValue() + cap.getAdditionalHealth();
        return current >= SKHealthConfig.max_health;
    }
}
