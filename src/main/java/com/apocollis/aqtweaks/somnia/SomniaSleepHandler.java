package com.apocollis.aqtweaks.somnia;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.kingrunes.somnia.Somnia;
import com.kingrunes.somnia.api.capability.CapabilityFatigue;
import com.kingrunes.somnia.api.capability.IFatigue;
import com.kingrunes.somnia.common.PacketHandler;
import com.kingrunes.somnia.common.SomniaConfig;
import com.kingrunes.somnia.common.SomniaPotions;
import com.kingrunes.somnia.common.util.ListUtils;
import com.kingrunes.somnia.common.util.SomniaState;
import com.kingrunes.somnia.server.ServerTickHandler;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.SPacketTimeUpdate;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SomniaSleepHandler {

    private static final Set<UUID> SLEEPING_PLAYERS = Collections.newSetFromMap(new ConcurrentHashMap<UUID, Boolean>());
    private static final Int2DoubleOpenHashMap DIM_OVERFLOW = new Int2DoubleOpenHashMap();

    static {
        DIM_OVERFLOW.defaultReturnValue(0.0);
    }

    public static void init() {
        applyConfigOverrides();
        MinecraftForge.EVENT_BUS.register(new SomniaSleepHandler());
    }

    public static void applyConfigOverrides() {
        if (!ArcanaQuestTweaksConfig.SomniaModuleConfig.enableSomniaModule) return;

        // Override Somnia fatigue replenish rate with configured points per hour / 1000.0
        if (SomniaConfig.FATIGUE != null) {
            SomniaConfig.FATIGUE.fatigueReplenishRate = ArcanaQuestTweaksConfig.SomniaModuleConfig.fatigueRecoveredPerHour / 1000.0;
        }

        // Disable Somnia's native time-skip logic to ensure AQTweaks precise Case A/B progression
        if (SomniaConfig.PERFORMANCE != null) {
            SomniaConfig.PERFORMANCE.fasterWorldTime = false;
        }
    }

    public static SomniaState getState(ServerTickHandler handler) {
        if (handler == null || handler.worldServer == null) return SomniaState.IDLE;
        WorldServer world = handler.worldServer;

        long timeOfDay = world.getWorldTime() % 24000L;
        if (Somnia.validSleepPeriod != null && !Somnia.validSleepPeriod.isTimeWithin(timeOfDay)) {
            return SomniaState.NOT_NOW;
        }

        if (world.playerEntities.isEmpty()) {
            return SomniaState.IDLE;
        }

        int totalNonSpectators = 0;
        int sleepingCount = 0;
        int somniaSleepCount = 0;
        int vanillaSleepCount = 0;

        for (EntityPlayer player : world.playerEntities) {
            if (player.isSpectator()) continue;
            totalNonSpectators++;

            boolean isSleeping = isConsideredSleeping(player);
            if (isSleeping) {
                sleepingCount++;
            }

            IFatigue fatigue = player.getCapability(CapabilityFatigue.FATIGUE_CAPABILITY, null);
            if (fatigue != null && fatigue.shouldSleepNormally()) {
                vanillaSleepCount++;
            } else {
                somniaSleepCount++;
            }
        }

        if (totalNonSpectators == 0) {
            return SomniaState.IDLE;
        }

        if (!ArcanaQuestTweaksConfig.SomniaModuleConfig.enableSomniaModule) {
            // Default Somnia logic: 100% sleeping required
            if (sleepingCount >= totalNonSpectators) {
                return somniaSleepCount >= vanillaSleepCount ? SomniaState.ACTIVE : SomniaState.IDLE;
            } else if (sleepingCount > 0) {
                return SomniaState.WAITING_PLAYERS;
            }
            return SomniaState.IDLE;
        }

        double ratio = (double) sleepingCount / (double) totalNonSpectators;
        double threshold = ArcanaQuestTweaksConfig.SomniaModuleConfig.sleepPercentage;

        if (ratio >= threshold && sleepingCount > 0) {
            if (somniaSleepCount >= vanillaSleepCount) {
                // Sleep simulation activates! Debounce transition broadcast
                if (handler.currentState != SomniaState.ACTIVE && ArcanaQuestTweaksConfig.SomniaModuleConfig.enableSleepNotifications) {
                    broadcastActivation(world, sleepingCount, totalNonSpectators);
                }
                return SomniaState.ACTIVE;
            } else {
                return SomniaState.IDLE;
            }
        } else if (sleepingCount > 0) {
            // Case C: resting in bed, recovering fatigue slowly
            return SomniaState.WAITING_PLAYERS;
        }

        return SomniaState.IDLE;
    }

    public static boolean isConsideredSleeping(EntityPlayer player) {
        if (player == null) return false;
        if (player.isPlayerSleeping()) return true;
        if (player instanceof EntityPlayerMP && Somnia.instance != null && Somnia.instance.ignoreList != null) {
            return ListUtils.containsRef((EntityPlayerMP) player, Somnia.instance.ignoreList);
        }
        return false;
    }

    public static boolean isPartialSleep(WorldServer world) {
        if (world == null || world.playerEntities.isEmpty()) return false;
        int totalNonSpectators = 0;
        int sleepingCount = 0;
        for (EntityPlayer player : world.playerEntities) {
            if (player.isSpectator()) continue;
            totalNonSpectators++;
            if (isConsideredSleeping(player)) {
                sleepingCount++;
            }
        }
        if (totalNonSpectators == 0) return false;
        double ratio = (double) sleepingCount / (double) totalNonSpectators;
        return ratio >= ArcanaQuestTweaksConfig.SomniaModuleConfig.sleepPercentage && ratio < 1.0;
    }

    public static synchronized boolean handleMultipliedTicking(ServerTickHandler handler) {
        if (!ArcanaQuestTweaksConfig.SomniaModuleConfig.enableSomniaModule) return false;
        if (handler == null || handler.worldServer == null) return false;
        WorldServer world = handler.worldServer;

        // If not partial sleep (e.g. 100% sleeping), allow Somnia to run full multiplied ticking (Case A)
        if (!isPartialSleep(world)) {
            return false;
        }

        // Case B: Partial sleep (50% - 99%)
        // Vanilla WorldServer.tick already applied +1 time and one fatigue recover.
        // Extra step is (multiplier - 1) so 2.0 = double per world tick, not triple.
        int dim = world.provider != null ? world.provider.getDimension() : 0;
        double mult = Math.max(1.0, ArcanaQuestTweaksConfig.SomniaModuleConfig.caseBTimeMultiplier);
        double overflow = DIM_OVERFLOW.get(dim);
        double extra = (mult - 1.0) + overflow;
        int step = (int) Math.floor(extra);
        DIM_OVERFLOW.put(dim, extra - step);

        if (step > 0) {
            world.setWorldTime(world.getWorldTime() + step);

            // Advance fatigue recovery for sleeping players matching the accelerated time speed
            double ratePerTick = ArcanaQuestTweaksConfig.SomniaModuleConfig.fatigueRecoveredPerHour / 1000.0;
            double fatigueDrop = step * ratePerTick;

            for (EntityPlayer player : world.playerEntities) {
                if (player.isPlayerSleeping()) {
                    IFatigue fatigue = player.getCapability(CapabilityFatigue.FATIGUE_CAPABILITY, null);
                    if (fatigue != null) {
                        double currentFatigue = fatigue.getFatigue();
                        double newFatigue = Math.max(0.0, currentFatigue - fatigueDrop);
                        fatigue.setFatigue(newFatigue);

                        // Clear potion effects if dropped below thresholds
                        if (SomniaConfig.FATIGUE != null) {
                            if (newFatigue < SomniaConfig.FATIGUE.fatigueSleepy && SomniaPotions.sleepyEffect != null) {
                                player.removePotionEffect(SomniaPotions.sleepyEffect);
                            }
                            if (newFatigue < SomniaConfig.FATIGUE.fatigueExhausted && SomniaPotions.exhaustedEffect != null) {
                                player.removePotionEffect(SomniaPotions.exhaustedEffect);
                            }
                            if (newFatigue < SomniaConfig.FATIGUE.fatigueFading && SomniaPotions.fadingEffect != null) {
                                player.removePotionEffect(SomniaPotions.fadingEffect);
                            }
                        }

                        // Sync fatigue to client
                        if (player instanceof EntityPlayerMP) {
                            FMLProxyPacket packet = PacketHandler.buildPropUpdatePacket(1, new Object[]{ 0, newFatigue });
                            Somnia.eventChannel.sendTo(packet, (EntityPlayerMP) player);
                        }
                    }
                }
            }

            // Sync world time to dimension skybox
            MinecraftServer server = world.getMinecraftServer();
            if (server != null) {
                server.getPlayerList().sendPacketToAllPlayersInDimension(
                        new SPacketTimeUpdate(world.getTotalWorldTime(), world.getWorldTime(), world.getGameRules().getBoolean("doDaylightCycle")),
                        dim
                );
            }
        }

        return true; // Suppress Somnia's full world/entity ticking for Case B
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.world.isRemote) return;
        EntityPlayer player = event.player;
        UUID id = player.getUniqueID();
        boolean isSleeping = player.isPlayerSleeping();
        boolean wasSleeping = SLEEPING_PLAYERS.contains(id);

        if (!wasSleeping && isSleeping) {
            SLEEPING_PLAYERS.add(id);
            if (ArcanaQuestTweaksConfig.SomniaModuleConfig.enableSleepNotifications && ArcanaQuestTweaksConfig.SomniaModuleConfig.enableSomniaModule) {
                broadcastBedEnter(player);
            }
        } else if (wasSleeping && !isSleeping) {
            SLEEPING_PLAYERS.remove(id);
        }
    }

    @SubscribeEvent
    public void onPlayerWakeUp(PlayerWakeUpEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        if (player.world.isRemote) return;
        UUID id = player.getUniqueID();
        SLEEPING_PLAYERS.remove(id);

        // If waking up before daytime and not an immediate wake/teleport, announce leaving bed
        if (!player.world.isDaytime() && !event.wakeImmediately()) {
            if (ArcanaQuestTweaksConfig.SomniaModuleConfig.enableSleepNotifications && ArcanaQuestTweaksConfig.SomniaModuleConfig.enableSomniaModule) {
                broadcastBedLeave(player);
            }
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        SLEEPING_PLAYERS.remove(event.player.getUniqueID());
    }

    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        SLEEPING_PLAYERS.remove(event.player.getUniqueID());
    }

    private static void broadcastBedEnter(EntityPlayer player) {
        World world = player.world;
        int total = 0;
        int sleeping = 0;
        for (EntityPlayer p : world.playerEntities) {
            if (p.isSpectator()) continue;
            total++;
            if (p.isPlayerSleeping() || p == player) sleeping++;
        }
        if (total == 0) return;
        int pct = Math.min(100, (int) Math.round(((double) sleeping / total) * 100.0));
        String msg = TextFormatting.GOLD + player.getDisplayName().getFormattedText() + TextFormatting.WHITE + " is now sleeping. [" + sleeping + "/" + total + " (" + pct + "%)]";
        broadcastToDimension(world, msg);
    }

    private static void broadcastBedLeave(EntityPlayer player) {
        World world = player.world;
        int total = 0;
        int sleeping = 0;
        for (EntityPlayer p : world.playerEntities) {
            if (p.isSpectator()) continue;
            total++;
            if (p != player && p.isPlayerSleeping()) sleeping++;
        }
        if (total == 0) return;
        int pct = Math.min(100, (int) Math.round(((double) sleeping / total) * 100.0));
        String msg = TextFormatting.GOLD + player.getDisplayName().getFormattedText() + TextFormatting.WHITE + " has left their bed. [" + sleeping + "/" + total + " (" + pct + "%)]";
        broadcastToDimension(world, msg);
    }

    private static void broadcastActivation(World world, int sleeping, int total) {
        if (total <= 0) return;
        int pct = Math.min(100, (int) Math.round(((double) sleeping / (double) total) * 100.0));
        String msg = TextFormatting.GOLD + "[Somnia] " + TextFormatting.YELLOW
                + sleeping + "/" + total + " (" + pct + "%) of players are sleeping! Fast-forwarding the night...";
        broadcastToDimension(world, msg);
    }

    private static void broadcastToDimension(World world, String msg) {
        TextComponentString text = new TextComponentString(msg);
        for (EntityPlayer p : world.playerEntities) {
            p.sendMessage(text);
        }
    }
}
