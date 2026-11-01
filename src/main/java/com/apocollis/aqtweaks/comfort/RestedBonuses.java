package com.apocollis.aqtweaks.comfort;

import com.elenai.elenaidodge2.api.FeathersHelper;
import com.elenai.elenaidodge2.api.MaxFeathersEvent;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraftforge.event.entity.living.PotionEvent;
import net.minecraftforge.event.entity.player.PlayerPickupXpEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

/**
 * Insight, Vigor and Fortitude while {@code aqtweaks:homestead} is active.
 * Vigor's applied delta is {@code AQTVigorDelta} on entity data, not the persisted tag.
 * One reconcile writes the difference, so a tier change does not stack on Elenai's integer.
 */
public final class RestedBonuses {

    static final String VIGOR_DELTA = "AQTVigorDelta";
    static final String INSIGHT_CARRY = "AQTInsightXpCarry";
    static final String FORTITUDE_TIER = "AQTFortitudeTier";

    private static final Set<UUID> PENDING = new HashSet<>();
    private static final ThreadLocal<Boolean> REPLACING = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private RestedBonuses() {}

    public static void beginReplace() {
        REPLACING.set(Boolean.TRUE);
    }

    public static void endReplace() {
        REPLACING.set(Boolean.FALSE);
    }

    public static void flush(EntityPlayer player) {
        if (player == null || player.world == null || player.world.isRemote) return;
        if (!PENDING.remove(player.getUniqueID())) return;
        reconcile(player);
    }

    public static void reconcile(EntityPlayer player) {
        if (!(player instanceof EntityPlayerMP)) return;
        if (player.world == null || player.world.isRemote) return;
        EntityPlayerMP mp = (EntityPlayerMP) player;
        NBTTagCompound data = player.getEntityData();
        int target = vigorDelta(player);
        int applied = data.getInteger(VIGOR_DELTA);
        int diff = target - applied;
        if (diff > 0) {
            FeathersHelper.decreaseRegenModifier(mp, diff);
        } else if (diff < 0) {
            FeathersHelper.increaseRegenModifier(mp, -diff);
        }
        if (diff != 0) {
            data.setInteger(VIGOR_DELTA, target);
        }
        int tier = restedTier(player);
        if (data.getInteger(FORTITUDE_TIER) != tier) {
            data.setInteger(FORTITUDE_TIER, tier);
            FeathersHelper.syncMaxFeathers(mp);
        }
    }

    public static float insightRate(EntityPlayer player) {
        ComfortSettings.RestedConfig rested = ComfortSystemHandler.RESTED;
        int tier = restedTier(player);
        if (tier == 1) return rested.insight_1;
        if (tier == 2) return rested.insight_2;
        if (tier >= 3) return rested.insight_3;
        return 0.0f;
    }

    public static int staminaBonus(EntityPlayer player) {
        ComfortSettings.RestedConfig rested = ComfortSystemHandler.RESTED;
        int tier = restedTier(player);
        if (tier == 1) return rested.stamina_1;
        if (tier == 2) return rested.stamina_2;
        if (tier >= 3) return rested.stamina_3;
        return 0;
    }

    public static int vigorInterval(EntityPlayer player) {
        ComfortSettings.RestedConfig rested = ComfortSystemHandler.RESTED;
        int tier = restedTier(player);
        if (tier == 1) return rested.vigor_1;
        if (tier == 2) return rested.vigor_2;
        if (tier >= 3) return rested.vigor_3;
        return ComfortSystemHandler.VIGOR_BASE_INTERVAL;
    }

    public static int restedTier(EntityPlayer player) {
        PotionEffect effect = player.getActivePotionEffect(PotionHomestead.INSTANCE);
        if (effect == null) return 0;
        return effect.getAmplifier() + 1;
    }

    private static int vigorDelta(EntityPlayer player) {
        int tier = restedTier(player);
        if (tier <= 0) return 0;
        int interval = vigorInterval(player);
        return Math.max(0, ComfortSystemHandler.VIGOR_BASE_INTERVAL - interval);
    }

    @SubscribeEvent
    public static void onPotionEnd(PotionEvent.PotionRemoveEvent event) {
        if (REPLACING.get()) return;
        if (event.getPotion() != PotionHomestead.INSTANCE) return;
        EntityLivingBase living = event.getEntityLiving();
        if (!(living instanceof EntityPlayer player)) return;
        if (player.world == null || player.world.isRemote) return;
        PENDING.add(player.getUniqueID());
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.player == null || event.player.world == null || event.player.world.isRemote) return;
        reconcile(event.player);
    }

    @SubscribeEvent
    public static void onClone(net.minecraftforge.event.entity.player.PlayerEvent.Clone event) {
        if (event.isWasDeath()) return;
        EntityPlayer player = event.getEntityPlayer();
        if (player == null || player.world == null || player.world.isRemote) return;
        reconcile(player);
    }

    @SubscribeEvent
    public static void onPickupXp(PlayerPickupXpEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        if (player == null || player.world == null || player.world.isRemote) return;
        float rate = insightRate(player);
        if (rate <= 0.0f) return;
        EntityXPOrb orb = event.getOrb();
        if (orb == null) return;
        NBTTagCompound data = player.getEntityData();
        float carry = data.getFloat(INSIGHT_CARRY);
        float total = orb.xpValue * (1.0f + rate) + carry;
        int whole = (int) Math.floor(total);
        if (whole < 0) whole = 0;
        data.setFloat(INSIGHT_CARRY, total - whole);
        orb.xpValue = whole;
    }

    @SubscribeEvent
    public static void onMaxFeathers(MaxFeathersEvent event) {
        EntityPlayer player = event.getPlayer();
        if (player == null) return;
        int extra = staminaBonus(player);
        if (extra <= 0) return;
        int room = event.getAbsoluteMaximum() - event.getMaximum();
        if (room <= 0) return;
        event.addMaximum(Math.min(extra, room));
    }
}
