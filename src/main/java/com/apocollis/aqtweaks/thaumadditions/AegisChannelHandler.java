package com.apocollis.aqtweaks.thaumadditions;

import com.apocollis.aqtweaks.ArcanaQuestTweaks;
import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.network.PacketAegisShield;
import com.apocollis.aqtweaks.potion.PotionAegisBroken;
import com.apocollis.aqtweaks.thaumadditions.foci.FocusEffectAegis;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import thaumcraft.common.items.casters.ItemCaster;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.lib.network.PacketHandler;
import thaumcraft.common.lib.network.fx.PacketFXFocusPartImpact;

/**
 * The Aegis channel: raise (started from the cast hook), vis upkeep each interval, the directional block that
 * spends the pool, and the break that locks Aegis foci out. Server side; clients only draw it (see
 * {@link PacketAegisShield}).
 */
public class AegisChannelHandler {
    private static final Map<UUID, Integer> LAST_DENIED = new HashMap<>();

    // ---- Hooks called from MixinItemCaster ----

    /** True when this cast must be refused: an Aegis focus while Shield Broken is active. */
    public static boolean refuse(EntityPlayer player, ItemStack caster) {
        if (!ArcanaQuestTweaksConfig.ThaumAdditionsConfig.enableAegisFocus) return false;
        if (!PotionAegisBroken.isBroken(player) || !AegisFocus.has(caster)) return false;
        if (!player.world.isRemote) {
            Integer last = LAST_DENIED.get(player.getUniqueID());
            if (last == null || player.ticksExisted - last >= 10 || player.ticksExisted < last) {
                LAST_DENIED.put(player.getUniqueID(), player.ticksExisted);
                float volume = (float) ArcanaQuestTweaksConfig.ThaumAdditionsConfig.aegisSoundVolume;
                if (volume > 0.0F) {
                    player.world.playSound(null, player.posX, player.posY, player.posZ,
                            SoundEvents.BLOCK_DISPENSER_FAIL, SoundCategory.PLAYERS, 0.5F * volume, 1.4F);
                }
            }
        }
        return true;
    }

    /** The use action of a caster stack: a raised-shield pose while its selected focus holds Aegis. */
    public static EnumAction useAction(ItemStack caster) {
        return ArcanaQuestTweaksConfig.ThaumAdditionsConfig.enableAegisFocus && AegisFocus.has(caster)
                ? EnumAction.BLOCK : null;
    }

    /** After a successful stock cast: start holding the shield (both sides) and open the channel (server). */
    public static void onCast(EntityPlayer player, EnumHand hand) {
        if (!ArcanaQuestTweaksConfig.ThaumAdditionsConfig.enableAegisFocus) return;
        FocusEffectAegis effect = AegisFocus.find(player.getHeldItem(hand));
        if (effect == null) return;
        if (player.world.isRemote) {
            player.setActiveHand(hand);
            return;
        }
        if (AegisChannels.isRaised(player)) return;
        player.setActiveHand(hand);
        if (!player.isHandActive()) return;
        AegisChannels.Channel channel = AegisChannels.open(player,
                effect.pool(com.apocollis.aqtweaks.thaumcraft.OverchannelHooks.aegisMultiplier(player)), hand);
        send(player, PacketAegisShield.STATE, channel);
        PacketHandler.INSTANCE.sendToAllAround(
                new PacketFXFocusPartImpact(player.posX, player.posY + player.height * 0.5D, player.posZ,
                        new String[] {FocusEffectAegis.KEY}),
                new NetworkRegistry.TargetPoint(player.world.provider.getDimension(), player.posX, player.posY,
                        player.posZ, 64.0D));
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new AegisChannelHandler());
    }

    // ---- Events ----

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.world.isRemote) return;
        EntityPlayer player = event.player;
        AegisChannels.Channel channel = AegisChannels.get(player);
        if (channel == null) return;
        ItemStack active = player.getActiveItemStack();
        if (!player.isEntityAlive() || !player.isHandActive() || player.getActiveHand() != channel.hand
                || !AegisFocus.has(active)) {
            end(player, PacketAegisShield.ENDED);
            return;
        }
        channel.held++;
        com.apocollis.aqtweaks.thaumcraft.OverchannelHooks.aegisHold(player, channel.held);
        if (AegisMath.intervalDue(channel.held, ArcanaQuestTweaksConfig.ThaumAdditionsConfig.aegisIntervalTicks)) {
            float cost = (float) ArcanaQuestTweaksConfig.ThaumAdditionsConfig.aegisVisPerInterval;
            if (cost > 0.0F && active.getItem() instanceof ItemCaster
                    && !((ItemCaster) active.getItem()).consumeVis(active, player, cost, false, false)) {
                end(player, PacketAegisShield.ENDED);
                player.resetActiveHand();
                return;
            }
        }
        if (channel.held % 10 == 0) {
            send(player, PacketAegisShield.STATE, channel);
        }
    }

    /** Blocks hits that come from the front. Cancelled before vanilla's own shield code runs. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onAttack(LivingAttackEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer) || event.getEntityLiving().world.isRemote) return;
        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        AegisChannels.Channel channel = AegisChannels.get(player);
        if (channel == null || channel.pool <= 0.0F
                || channel.held < ArcanaQuestTweaksConfig.ThaumAdditionsConfig.aegisRaiseDelayTicks) return;

        DamageSource source = event.getSource();
        Entity attacker = source.getTrueSource() != null ? source.getTrueSource() : source.getImmediateSource();
        if (attacker == player) return;
        Vec3d at = source.getDamageLocation();
        if (at == null && attacker != null) {
            at = new Vec3d(attacker.posX, attacker.posY, attacker.posZ);
        }
        if (at == null) return;
        Vec3d look = player.getLook(1.0F);
        if (!AegisMath.isFrontal(look.x, look.z, at.x - player.posX, at.z - player.posZ)) return;

        float amount = event.getAmount();
        if (amount <= 0.0F) return;
        event.setCanceled(true);

        int since = player.ticksExisted - channel.lastBlockTick;
        float counted = AegisMath.effectiveDamage(amount, channel.lastDamage, since);
        if (since >= AegisMath.HURT_WINDOW_TICKS) {
            channel.lastBlockTick = player.ticksExisted;
            channel.lastDamage = amount;
        } else if (counted > 0.0F) {
            channel.lastDamage = amount;
        }
        if (counted <= 0.0F) return;

        boolean broke = AegisMath.breaks(channel.pool, counted);
        channel.pool = AegisMath.remaining(channel.pool, counted);
        MinecraftForge.EVENT_BUS.post(new AegisBlockEvent(player, attacker, counted, channel.pool, channel.held, broke));
        if (broke) {
            shatter(player);
            return;
        }
        float volume = (float) ArcanaQuestTweaksConfig.ThaumAdditionsConfig.aegisSoundVolume;
        if (volume > 0.0F) {
            player.world.playSound(null, player.posX, player.posY, player.posZ, SoundsTC.runicShieldEffect,
                    SoundCategory.PLAYERS, 0.7F * volume, 1.0F + player.world.rand.nextFloat() * 0.2F);
        }
        send(player, PacketAegisShield.HIT, channel);
    }

    @SubscribeEvent
    public void onDeath(LivingDeathEvent event) {
        if (event.getEntityLiving() instanceof EntityPlayer && !event.getEntityLiving().world.isRemote) {
            end(event.getEntityLiving(), PacketAegisShield.ENDED);
        }
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        AegisChannels.close(event.player.getUniqueID());
        LAST_DENIED.remove(event.player.getUniqueID());
    }

    // ---- Internals ----

    private static void shatter(EntityPlayer player) {
        end(player, PacketAegisShield.BROKE);
        player.resetActiveHand();
        int lockout = ArcanaQuestTweaksConfig.ThaumAdditionsConfig.aegisBreakLockoutTicks;
        if (lockout > 0) {
            player.addPotionEffect(new PotionEffect(PotionAegisBroken.BROKEN, lockout, 0, false, true));
        }
        float volume = (float) ArcanaQuestTweaksConfig.ThaumAdditionsConfig.aegisSoundVolume;
        if (volume > 0.0F) {
            player.world.playSound(null, player.posX, player.posY, player.posZ, SoundsTC.runicShieldEffect,
                    SoundCategory.PLAYERS, 1.0F * volume, 0.6F);
        }
    }

    private static void end(EntityLivingBase entity, byte mode) {
        AegisChannels.Channel channel = AegisChannels.close(entity);
        if (channel == null) return;
        send(entity, mode, channel);
    }

    private static void send(EntityLivingBase entity, byte mode, AegisChannels.Channel channel) {
        ArcanaQuestTweaks.NETWORK.sendToAllAround(
                new PacketAegisShield(entity.getEntityId(), mode, channel.pool, channel.maxPool),
                new NetworkRegistry.TargetPoint(entity.world.provider.getDimension(), entity.posX, entity.posY,
                        entity.posZ, 64.0D));
    }
}
