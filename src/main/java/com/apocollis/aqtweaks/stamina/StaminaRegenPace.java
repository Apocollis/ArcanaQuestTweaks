package com.apocollis.aqtweaks.stamina;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig.RegenPace;
import com.apocollis.aqtweaks.comfort.RestedBonuses;
import com.apocollis.aqtweaks.stamina.StaminaRegenMath.Pace;
import com.elenai.elenaidodge2.api.FeathersHelper;
import com.elenai.elenaidodge2.init.PotionInit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.EnumAction;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * State-based stamina refill (plan 20261122-0610). Elenai counts regeneration on the client and resets its timer to
 * {@code rate + modifier}; this class sets the server-side modifier so the interval follows what the player is doing:
 * Resting (full stop, sneaking), Moving (in between) or Active (strenuous). The interval rules live in
 * {@link StaminaRegenMath}; this class only reads game state and applies the difference.
 *
 * <p>Layering: this class owns {@code AQTPaceDelta}; Moonfrost owns its own delta and the Replenishment potion's own
 * modifier is cancelled by {@code MixinReplenishmentPotion}. Rested Vigor is folded in as a cap on every pace.
 */
public final class StaminaRegenPace {

    private static final Logger LOGGER = LogManager.getLogger("AQTweaks");

    static final String NBT_DELTA = "AQTPaceDelta";
    private static final int RESOLVE_EVERY = 5;

    private static final class State {
        double lastX, lastZ;
        boolean hasPos;
        int stillTicks;
        int sneakTicks;
        long activeUntil;
        long itemUntil;
        Pace lastPace = Pace.MOVING;
    }

    private static final Map<UUID, State> STATES = new HashMap<>();

    private StaminaRegenPace() {}

    public static void register() {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new Events());
    }

    private static State state(EntityPlayer player) {
        return STATES.computeIfAbsent(player.getUniqueID(), id -> new State());
    }

    private static boolean applies(EntityPlayer player) {
        return player instanceof EntityPlayerMP && !player.world.isRemote && !player.capabilities.isCreativeMode
                && !player.isSpectator() && !(player instanceof net.minecraftforge.common.util.FakePlayer);
    }

    /** Keeps the player Active for {@code ticks} more ticks (a later, longer window is not shortened). */
    public static void markActive(EntityPlayer player, int ticks) {
        if (!applies(player) || ticks <= 0) return;
        State st = state(player);
        long until = player.world.getTotalWorldTime() + ticks;
        if (until > st.activeUntil) st.activeUntil = until;
    }

    /** A melee swing: Active for the base combo grace plus the weapon's attack period. */
    public static void markSwing(EntityPlayer player) {
        RegenPace cfg = ArcanaQuestTweaksConfig.StaminaModuleConfig.regenPace;
        if (!cfg.enable || !cfg.meleeActive) return;
        markActive(player, StaminaRegenMath.swingWindow(cfg.comboBaseGraceTicks,
                DssWeaponBalance.attackPeriodTicks(player)));
    }

    /** A digging tick: Active for the linger window. */
    public static void markMining(EntityPlayer player) {
        RegenPace cfg = ArcanaQuestTweaksConfig.StaminaModuleConfig.regenPace;
        if (!cfg.enable || !cfg.miningActive) return;
        markActive(player, Math.max(1, cfg.activeLingerTicks));
    }

    private static void markItemUse(EntityPlayer player) {
        if (!applies(player)) return;
        State st = state(player);
        long until = player.world.getTotalWorldTime()
                + Math.max(0, ArcanaQuestTweaksConfig.StaminaModuleConfig.regenPace.itemUseLingerTicks);
        if (until > st.itemUntil) st.itemUntil = until;
    }

    /** Re-evaluates now (a Rested tier or perk change), bypassing the resolve cadence. */
    public static void refresh(EntityPlayer player) {
        if (!applies(player)) return;
        resolve((EntityPlayerMP) player);
    }

    /** Drops the stored pace delta bookkeeping for a player (logout). */
    static void forget(UUID id) {
        STATES.remove(id);
    }

    private static int replenishmentInterval(EntityPlayer player) {
        RegenPace cfg = ArcanaQuestTweaksConfig.StaminaModuleConfig.regenPace;
        PotionEffect effect = player.getActivePotionEffect(PotionInit.REPLENISHMENT_EFFECT);
        int amp = effect == null ? -1 : effect.getAmplifier();
        return StaminaRegenMath.replenishment(amp, cfg.replenishmentInterval, cfg.replenishmentStep,
                cfg.replenishmentMinimum);
    }

    private static int vigorInterval(EntityPlayer player) {
        try {
            return RestedBonuses.vigorInterval(player);
        } catch (RuntimeException | LinkageError t) {
            return 30;
        }
    }

    private static int elenaiBase() {
        int base = 30;
        try {
            int rate = com.elenai.elenaidodge2.ModConfig.common.feathers.rate;
            if (rate > 0) base = rate;
        } catch (Throwable ignored) {
        }
        return base;
    }

    private static boolean strenuous(EntityPlayerMP player, NBTTagCompound pData, boolean marathon) {
        if (player.isSprinting() && !marathon) return true;
        if (player.isOnLadder() && !player.onGround) return true;
        if (pData.getInteger("StaminaTweaksLedgeClimbState") == 1) return true;
        if (OpenGliderHelper.isGliding(player)) return true;
        if (player.isInWater() && !player.onGround) return true;
        RegenPace cfg = ArcanaQuestTweaksConfig.StaminaModuleConfig.regenPace;
        if (cfg.grappleActive && GrappleHelper.isGrappling(player)) return true;
        if (player.isHandActive()) {
            net.minecraft.item.ItemStack active = player.getActiveItemStack();
            if (active.getItemUseAction() == EnumAction.BOW || StaminaModule.isThrowingWeapon(active)) return true;
        }
        return "ACTIVE".equalsIgnoreCase(cfg.shieldHold) && player.isActiveItemStackBlocking();
    }

    private static void track(EntityPlayerMP player, State st, boolean active) {
        RegenPace cfg = ArcanaQuestTweaksConfig.StaminaModuleConfig.regenPace;
        double dx = player.posX - st.lastX;
        double dz = player.posZ - st.lastZ;
        st.lastX = player.posX;
        st.lastZ = player.posZ;
        boolean still = st.hasPos && dx * dx + dz * dz < cfg.restSpeedThreshold && player.onGround;
        st.hasPos = true;
        if (active) {
            // The settle time counts from the end of the Active cooldown, never during it.
            st.stillTicks = 0;
            st.sneakTicks = 0;
            return;
        }
        st.stillTicks = still ? st.stillTicks + 1 : 0;
        st.sneakTicks = player.isSneaking() && player.onGround ? st.sneakTicks + 1 : 0;
        if (player.isHandActive() && !player.isActiveItemStackBlocking()) {
            long until = player.world.getTotalWorldTime() + Math.max(0, cfg.itemUseLingerTicks);
            if (until > st.itemUntil) st.itemUntil = until;
        }
    }

    private static void resolve(EntityPlayerMP player) {
        RegenPace cfg = ArcanaQuestTweaksConfig.StaminaModuleConfig.regenPace;
        NBTTagCompound pData = player.getEntityData();
        State st = state(player);
        long now = player.world.getTotalWorldTime();

        boolean marathon = StaminaPerks.marathonRunner(player);
        boolean cardio = StaminaPerks.cardioMaster(player);

        Pace pace;
        boolean sprintLike = false;
        if (!cfg.enable) {
            pace = Pace.ACTIVE;
        } else {
            if (strenuous(player, pData, marathon)) {
                long until = now + Math.max(0, cfg.activeLingerTicks);
                if (until > st.activeUntil) st.activeUntil = until;
            }
            if (now < st.activeUntil) {
                pace = Pace.ACTIVE;
            } else if (marathon && (player.isSprinting() || !player.onGround)) {
                pace = Pace.MOVING;
                sprintLike = true;
            } else {
                boolean itemBlock = now < st.itemUntil;
                boolean shield = player.isActiveItemStackBlocking();
                boolean shieldBlocksRest = shield && !"RESTING_ALLOWED".equalsIgnoreCase(cfg.shieldHold);
                boolean calm = !itemBlock && !shieldBlocksRest;
                boolean stopped = st.stillTicks >= cfg.restSettleTicks || st.sneakTicks >= cfg.restSettleTicks
                        || player.isPlayerSleeping();
                boolean walkingRest = marathon && player.onGround;
                pace = calm && (stopped || walkingRest) ? Pace.RESTING : Pace.MOVING;
            }
        }
        st.lastPace = pace;

        int interval = cfg.enable
                ? StaminaRegenMath.interval(pace, cardio, sprintLike, cfg.restingInterval, cfg.movingInterval,
                        cfg.activeInterval, vigorInterval(player), replenishmentInterval(player))
                : elenaiBase();
        apply(player, pData, interval - elenaiBase(), pace);
    }

    private static void apply(EntityPlayerMP player, NBTTagCompound pData, int target, Pace pace) {
        int applied = pData.getInteger(NBT_DELTA);
        int diff = target - applied;
        if (diff == 0) return;
        try {
            if (diff > 0) {
                FeathersHelper.increaseRegenModifier(player, diff);
            } else {
                FeathersHelper.decreaseRegenModifier(player, -diff);
            }
        } catch (RuntimeException | LinkageError t) {
            return;
        }
        if (target == 0) {
            pData.removeTag(NBT_DELTA);
        } else {
            pData.setInteger(NBT_DELTA, target);
        }
        LOGGER.debug("[AQ-STAMINA] regen pace {} -> modifier {} for {}", pace, target, player.getName());
    }

    public static final class Events {
        @SubscribeEvent
        public void onTick(TickEvent.PlayerTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            EntityPlayer player = event.player;
            if (!applies(player)) return;
            State st = state(player);
            long now = player.world.getTotalWorldTime();
            track((EntityPlayerMP) player, st, now < st.activeUntil);
            if ((player.ticksExisted + (player.getEntityId() & 3)) % RESOLVE_EVERY != 0) return;
            resolve((EntityPlayerMP) player);
        }

        @SubscribeEvent
        public void onRightClick(PlayerInteractEvent.RightClickItem event) {
            markItemUse(event.getEntityPlayer());
        }

        @SubscribeEvent
        public void onUseStart(LivingEntityUseItemEvent.Start event) {
            if (event.getEntityLiving() instanceof EntityPlayer) markItemUse((EntityPlayer) event.getEntityLiving());
        }

        @SubscribeEvent
        public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
            forget(event.player.getUniqueID());
        }

        @SubscribeEvent
        public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
            if (event.player instanceof EntityPlayerMP && applies(event.player)) {
                resolve((EntityPlayerMP) event.player);
            }
        }
    }
}
