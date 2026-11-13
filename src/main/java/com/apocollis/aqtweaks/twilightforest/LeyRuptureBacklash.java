package com.apocollis.aqtweaks.twilightforest;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.weather.GearTierRule;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Arcane backlash: using any magic item while exposed to a surge hurts the user. The item is classified by
 * {@link ArcaneItemClassifier}; the damage is the configured backlash scaled by the exposure factor and the
 * proper-gear tier, with a per-player cooldown. A Thaumcraft caster also adds the capped warp. The use itself
 * still succeeds unless {@code Backlash Cancels Use} is on.
 */
public class LeyRuptureBacklash {
    private final Map<UUID, Integer> cooldown = new HashMap<>();

    private boolean trigger(EntityPlayer player, ItemStack stack) {
        if (player.world.isRemote || !(player instanceof EntityPlayerMP) || player.isCreative() || player.isSpectator()) {
            return false;
        }
        ArcanaQuestTweaksConfig.LeyRupture c = ArcanaQuestTweaksConfig.TwilightForestModuleConfig.ley;
        LeyRuptureHandler handler = LeyChannels.handler;
        if (!c.enable || !c.enableBacklash || handler == null || !LeyChannels.isTf(player.world)) return false;
        if (!handler.active(player)) return false;
        if (ArcaneItemClassifier.reason(stack) == null) return false;
        double exposure = handler.exposureOf(player);
        if (exposure <= 0.01D) return false;
        int last = cooldown.getOrDefault(player.getUniqueID(), -1000);
        if (player.ticksExisted - last < c.backlashCooldown) return false;
        cooldown.put(player.getUniqueID(), player.ticksExisted);

        boolean proper = GearTierRule.allSlotsOnList(player, c.properGear);
        double damage = SurgeMath.backlash(c.backlashDamage, exposure, proper, c.scourUnprotected, c.scourProper);
        if (damage > 0.0D) {
            player.attackEntityFrom(LeyRuptureHandler.DAMAGE_LEY, (float) damage);
        }
        WorldServer world = (WorldServer) player.world;
        world.spawnParticle(EnumParticleTypes.SPELL_WITCH, player.posX, player.posY + 1.0D, player.posZ, 25, 0.3D, 0.3D,
                0.3D, 0.2D);
        world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.BLOCK_FIRE_EXTINGUISH,
                SoundCategory.PLAYERS, 0.7F, 1.6F);
        if (ArcaneItemClassifier.isCaster(stack) && c.backlashWarp > 0) {
            handler.giveBacklashWarp(player, c.backlashWarp);
        }
        return true;
    }

    private static boolean cancels() {
        return ArcanaQuestTweaksConfig.TwilightForestModuleConfig.ley.backlashCancelsUse;
    }

    @SubscribeEvent
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (trigger(event.getEntityPlayer(), event.getItemStack()) && cancels()) event.setCanceled(true);
    }

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (trigger(event.getEntityPlayer(), event.getItemStack()) && cancels()) event.setCanceled(true);
    }

    @SubscribeEvent
    public void onUseStart(LivingEntityUseItemEvent.Start event) {
        if (event.getEntityLiving() instanceof EntityPlayer
                && trigger((EntityPlayer) event.getEntityLiving(), event.getItem()) && cancels()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onAttack(AttackEntityEvent event) {
        trigger(event.getEntityPlayer(), event.getEntityPlayer().getHeldItemMainhand());
    }

    @SubscribeEvent
    public void onMount(EntityMountEvent event) {
        if (!event.isMounting() || !(event.getEntityMounting() instanceof EntityPlayer)) return;
        Entity mount = event.getEntityBeingMounted();
        ResourceLocation key = mount == null ? null : EntityList.getKey(mount);
        if (key == null || !ArcaneItemClassifier.matchesPatterns(key.toString(),
                ArcanaQuestTweaksConfig.TwilightForestModuleConfig.ley.magicItemPatterns)) {
            return;
        }
        EntityPlayer player = (EntityPlayer) event.getEntityMounting();
        // Mounting a broom entity counts as using a broom: charge the backlash with a stand-in classified stack.
        triggerMount(player);
    }

    private void triggerMount(EntityPlayer player) {
        if (player.world.isRemote || !(player instanceof EntityPlayerMP) || player.isCreative()) return;
        ArcanaQuestTweaksConfig.LeyRupture c = ArcanaQuestTweaksConfig.TwilightForestModuleConfig.ley;
        LeyRuptureHandler handler = LeyChannels.handler;
        if (!c.enable || !c.enableBacklash || handler == null || !LeyChannels.isTf(player.world) || !handler.active(player)) {
            return;
        }
        double exposure = handler.exposureOf(player);
        if (exposure <= 0.01D) return;
        int last = cooldown.getOrDefault(player.getUniqueID(), -1000);
        if (player.ticksExisted - last < c.backlashCooldown) return;
        cooldown.put(player.getUniqueID(), player.ticksExisted);
        boolean proper = GearTierRule.allSlotsOnList(player, c.properGear);
        player.attackEntityFrom(LeyRuptureHandler.DAMAGE_LEY,
                (float) SurgeMath.backlash(c.backlashDamage, exposure, proper, c.scourUnprotected, c.scourProper));
    }
}
