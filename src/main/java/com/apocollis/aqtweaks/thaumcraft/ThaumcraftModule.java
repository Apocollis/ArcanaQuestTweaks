package com.apocollis.aqtweaks.thaumcraft;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;


import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import thaumcraft.common.lib.potions.PotionWarpWard;

public class ThaumcraftModule {

    /** The player's persisted-data compound ({@code ForgeData.PlayerPersisted}), created if missing. */
    private static NBTTagCompound persistedTag(EntityPlayer player) {
        NBTTagCompound data = player.getEntityData();
        if (!data.hasKey(EntityPlayer.PERSISTED_NBT_TAG)) {
            data.setTag(EntityPlayer.PERSISTED_NBT_TAG, new NBTTagCompound());
        }
        return data.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
    }

    private static SoundEvent soundEvent(String name) {
        if (name == null || name.trim().isEmpty()) return null;
        return SoundEvent.REGISTRY.getObject(new ResourceLocation(name.trim()));
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        EntityPlayer player = event.player;
        if (player.world.isRemote) return;

        NBTTagCompound persisted = persistedTag(player);
        if (!persisted.hasKey("VisitedDimensions")) {
            // First time joining the server: initialize with the current dimension so they don't get warp instantly
            persisted.setIntArray("VisitedDimensions", new int[]{ player.dimension });
        }
    }

    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!ArcanaQuestTweaksConfig.ThaumcraftConfig.enableDimensionWarp) return;

        EntityPlayer player = event.player;
        if (player.world.isRemote) return;

        int toDim = event.toDim;
        NBTTagCompound persisted = persistedTag(player);
        int[] visited = persisted.getIntArray("VisitedDimensions");

        boolean alreadyVisited = false;
        for (int v : visited) {
            if (v == toDim) {
                alreadyVisited = true;
                break;
            }
        }

        if (!alreadyVisited) {
            // Add to visited list
            int[] newVisited = new int[visited.length + 1];
            System.arraycopy(visited, 0, newVisited, 0, visited.length);
            newVisited[visited.length] = toDim;
            persisted.setIntArray("VisitedDimensions", newVisited);
            // Two seconds later, on the server thread (the old code slept on a new Thread per dimension).
            DELAYED.add(new Delayed(serverTicks + 40, () -> {
                // Check if player is still online and alive
                net.minecraft.world.World pWorld = player.world;
                if (player.isDead || pWorld == null || !pWorld.playerEntities.contains(player)) return;

                boolean addedNormal = false;
                boolean addedTemp = false;

                // Award warp
                if (ArcanaQuestTweaksConfig.ThaumcraftConfig.dimensionNormalWarp > 0) {
                    ThaumcraftHelper.addWarp(player, 0, ArcanaQuestTweaksConfig.ThaumcraftConfig.dimensionNormalWarp);
                    addedNormal = true;
                }

                if (ArcanaQuestTweaksConfig.ThaumcraftConfig.dimensionTempWarp > 0) {
                    ThaumcraftHelper.addWarp(player, 1, ArcanaQuestTweaksConfig.ThaumcraftConfig.dimensionTempWarp);
                    addedTemp = true;
                }

                if (addedNormal || addedTemp) {
                    ThaumcraftHelper.syncWarp(player);

                    // Play sound effect
                    String soundName = ArcanaQuestTweaksConfig.ThaumcraftConfig.dimensionEntrySound;
                    if (soundName != null && !soundName.isEmpty()) {
                        SoundEvent sound = soundEvent(soundName);
                        if (sound != null && pWorld != null) {
                            float volume = ArcanaQuestTweaksConfig.ThaumcraftConfig.dimensionEntrySoundVolume;
                            pWorld.playSound(null, player.posX, player.posY, player.posZ, sound, SoundCategory.PLAYERS, volume, 1.0F);
                        }
                    }

                    // Send chat message
                    String chatMsg = ArcanaQuestTweaksConfig.ThaumcraftConfig.dimensionChatMessageText;
                    if (chatMsg != null && !chatMsg.isEmpty()) {
                        player.sendMessage(new TextComponentString(chatMsg));
                    }
                }
            }, player));
        }
    }

    private static final class Delayed {
        final long due;
        final Runnable task;
        final EntityPlayer player;

        Delayed(long due, Runnable task, EntityPlayer player) {
            this.due = due;
            this.task = task;
            this.player = player;
        }
    }

    private static final java.util.List<Delayed> DELAYED = new java.util.ArrayList<>();
    private static long serverTicks;

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        serverTicks++;
        if (DELAYED.isEmpty()) return;
        for (java.util.Iterator<Delayed> it = DELAYED.iterator(); it.hasNext(); ) {
            Delayed d = it.next();
            if (d.due > serverTicks) continue;
            it.remove();
            d.task.run();
        }
    }

    /** Per sleeping player: {world time, ticksExisted} when the sleep was first seen. */
    private static final java.util.Map<java.util.UUID, long[]> SLEEP_START = new java.util.concurrent.ConcurrentHashMap<>();

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        SLEEP_START.remove(event.player.getUniqueID());
        DELAYED.removeIf(d -> d.player == event.player);
    }

    @SubscribeEvent
    public void onPlayerDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        SLEEP_START.remove(event.player.getUniqueID());
    }

    /**
     * Sleep cleanse. The requirement is time slept, not the time of day: at least
     * {@code sleepMinHours} in-game hours between going to bed and waking (Somnia fast-forward counts,
     * because the world clock advances). Waking early by leaving the bed, or waking before morning, no
     * longer disqualifies a long sleep.
     */
    @SubscribeEvent
    public void onPlayerWakeUp(PlayerWakeUpEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        if (player.world.isRemote) return;
        long[] start = SLEEP_START.remove(player.getUniqueID());
        if (!ArcanaQuestTweaksConfig.ThaumcraftConfig.enableWarpCleansing) return;
        if (start == null) return;

        long worldSlept = Math.max(0L, player.world.getWorldTime() - start[0]);
        long minTicks = ArcanaQuestTweaksConfig.ThaumcraftConfig.sleepMinHours * 1000L;
        if (worldSlept < minTicks) return;

        // A broken warp capability reads as 0 warp, which would otherwise look like a
        // successful cleanse and print the chat line for nothing.
        if (!ThaumcraftHelper.available()) return;

        boolean clearedNormal = false;
        boolean clearedTemp = false;

        // Reduce Normal Warp. Confirm the value actually moved before claiming success.
        if (ArcanaQuestTweaksConfig.ThaumcraftConfig.clearNormalWarp) {
            int currentNormal = ThaumcraftHelper.getWarp(player, 0);
            if (currentNormal > 0) {
                ThaumcraftHelper.reduceWarp(player, 0, ArcanaQuestTweaksConfig.ThaumcraftConfig.normalWarpReduction);
                clearedNormal = ThaumcraftHelper.getWarp(player, 0) < currentNormal;
            }
        }

        // Reduce Temporary Warp (flat amount, off by default)
        if (ArcanaQuestTweaksConfig.ThaumcraftConfig.clearTempWarp) {
            int currentTemp = ThaumcraftHelper.getWarp(player, 1);
            if (currentTemp > 0) {
                ThaumcraftHelper.reduceWarp(player, 1, ArcanaQuestTweaksConfig.ThaumcraftConfig.tempWarpReduction);
                clearedTemp = ThaumcraftHelper.getWarp(player, 1) < currentTemp;
            }
        }

        // Comfort rate over the sleep time the player ticks did not cover (Somnia time-only skip).
        if (ArcanaQuestTweaksConfig.ThaumcraftConfig.sleepComfortTempClear) {
            long playerTicks = Math.max(0L, player.ticksExisted - start[1]);
            long uncovered = worldSlept - playerTicks;
            if (uncovered > 0L
                    && com.apocollis.aqtweaks.comfort.ComfortSystemHandler.applyAcceleratedWarpCleanse(player, uncovered) > 0) {
                clearedTemp = true;
            }
        }

        // Sync and notify player
        if (clearedNormal || clearedTemp) {
            ThaumcraftHelper.syncWarp(player);

            String chatMsg = ArcanaQuestTweaksConfig.ThaumcraftConfig.chatMessageText;
            if (ArcanaQuestTweaksConfig.ThaumcraftConfig.enableChatMessage && chatMsg != null && !chatMsg.isEmpty()) {
                player.sendMessage(new TextComponentString(chatMsg));
            }
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.world.isRemote) return;
        if (event.player.isPlayerSleeping()) {
            SLEEP_START.computeIfAbsent(event.player.getUniqueID(),
                    id -> new long[] {event.player.world.getWorldTime(), event.player.ticksExisted});
        }
        int period = ArcanaQuestTweaksConfig.ThaumcraftConfig.exposureTickSeconds * 20;
        if (period <= 0) return;
        if (event.player.ticksExisted % period != 0) return;
        evaluateExposureWarp(event.player);
    }

    private void evaluateExposureWarp(EntityPlayer player) {
        if (!ArcanaQuestTweaksConfig.ThaumcraftConfig.enableExposureWarp) return;

        Potion warpWard = PotionWarpWard.instance;
        if (warpWard != null && player.isPotionActive(warpWard)) return;

        ExposureMatch winner = pickWinningExposure(player);
        NBTTagCompound persisted = persistedTag(player);
        NBTTagCompound banks = persisted.getCompoundTag("WarpExposureBySource");
        if (banks == null) {
            banks = new NBTTagCompound();
        }

        int leftover = persisted.getInteger("WarpExposureProgress");
        if (leftover > 0 && winner != null) {
            int existing = banks.getInteger(winner.key);
            banks.setInteger(winner.key, existing + leftover);
            persisted.removeTag("WarpExposureProgress");
        }

        if (winner == null || winner.g <= 0) {
            persisted.setTag("WarpExposureBySource", banks);
            return;
        }

        int tickSeconds = ArcanaQuestTweaksConfig.ThaumcraftConfig.exposureTickSeconds;
        int grantSeconds = ArcanaQuestTweaksConfig.ThaumcraftConfig.exposureGrantSeconds;
        int step = tickSeconds;
        if (("under".equals(winner.key) || "underDeep".equals(winner.key))
                && com.apocollis.aqtweaks.reskillable.SpelunkerComfort.underground(player)
                && com.apocollis.aqtweaks.reskillable.PerkAccess.on(player, "aqtweaks:spelunker",
                ArcanaQuestTweaksConfig.ReskillableModuleConfig.perks.spelunker.enable)) {
            step = Math.max(1, tickSeconds / 2);
        }
        int progress = banks.getInteger(winner.key) + step;
        if (progress >= grantSeconds) {
            progress = 0;
            ThaumcraftHelper.addWarp(player, 1, winner.g);
            ThaumcraftHelper.syncWarp(player);
            playExposureSound(player);
        }
        banks.setInteger(winner.key, progress);
        persisted.setTag("WarpExposureBySource", banks);
    }

    private void playExposureSound(EntityPlayer player) {
        if (!ArcanaQuestTweaksConfig.ThaumcraftConfig.enableExposureSound) return;
        String soundName = ArcanaQuestTweaksConfig.ThaumcraftConfig.exposureSoundEffect;
        if (soundName == null || soundName.isEmpty()) return;
        SoundEvent sound = soundEvent(soundName);
        net.minecraft.world.World pWorld = player.world;
        if (sound == null || pWorld == null) return;
        float volume = ArcanaQuestTweaksConfig.ThaumcraftConfig.exposureSoundVolume;
        pWorld.playSound(null, player.posX, player.posY, player.posZ,
                sound, SoundCategory.PLAYERS, volume, 1.0F);
    }

    private static String[] grantsSource;
    private static int[] grantsParsed = new int[0];

    /** {@code dim, grant} pairs parsed from the config array; re-parsed only when the array changes. */
    private static int[] dimensionGrants() {
        String[] src = ArcanaQuestTweaksConfig.ThaumcraftConfig.exposureDimensionGrants;
        if (src == grantsSource && grantsParsed != null && grantsParsedLen == src.length) return grantsParsed;
        java.util.List<Integer> out = new java.util.ArrayList<>();
        for (String entry : src) {
            String[] parts = entry.split("=");
            if (parts.length != 2) continue;
            try {
                out.add(Integer.parseInt(parts[0].trim()));
                out.add(Integer.parseInt(parts[1].trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        int[] arr = new int[out.size()];
        for (int i = 0; i < arr.length; i++) arr[i] = out.get(i);
        grantsSource = src;
        grantsParsedLen = src.length;
        grantsParsed = arr;
        return arr;
    }

    private static int grantsParsedLen = -1;

    private ExposureMatch pickWinningExposure(EntityPlayer player) {
        ExposureMatch winner = null;
        int playerDim = player.dimension;
        int[] grants = dimensionGrants();
        for (int i = 0; i + 1 < grants.length; i += 2) {
            if (grants[i] == playerDim && grants[i + 1] > 0) {
                winner = better(winner, new ExposureMatch("dim:" + grants[i], grants[i + 1], PRI_DIM));
            }
        }

        if (ArcanaQuestTweaksConfig.ThaumcraftConfig.enableUndergroundExposure) {
            int y = (int) Math.floor(player.posY);
            int yMin = ArcanaQuestTweaksConfig.ThaumcraftConfig.exposureUndergroundYMin;
            int yMax = ArcanaQuestTweaksConfig.ThaumcraftConfig.exposureUndergroundYMax;
            if (y < yMin) {
                int g = ArcanaQuestTweaksConfig.ThaumcraftConfig.exposureDeepUndergroundWarp;
                if (g > 0) {
                    winner = better(winner, new ExposureMatch("underDeep", g, PRI_UNDER_DEEP));
                }
            } else if (y >= yMin && y <= yMax) {
                int g = ArcanaQuestTweaksConfig.ThaumcraftConfig.exposureUndergroundWarp;
                if (g > 0) {
                    winner = better(winner, new ExposureMatch("under", g, PRI_UNDER));
                }
            }
        }

        net.minecraft.world.World pWorld = player.world;
        if (ArcanaQuestTweaksConfig.ThaumcraftConfig.enableDungeonExposure && pWorld != null) {
            net.minecraft.world.chunk.IChunkProvider provider = pWorld.getChunkProvider();
            if (provider instanceof ChunkProviderServer
                    && ((ChunkProviderServer) provider).isInsideStructure(pWorld, "RoguelikeDungeon", player.getPosition())) {
                int g = ArcanaQuestTweaksConfig.ThaumcraftConfig.exposureDungeonWarp;
                if (g > 0) {
                    winner = better(winner, new ExposureMatch("dungeon", g, PRI_DUNGEON));
                }
            }
        }
        return winner;
    }

    private static ExposureMatch better(ExposureMatch current, ExposureMatch candidate) {
        if (candidate == null) return current;
        if (current == null) return candidate;
        if (candidate.g > current.g) return candidate;
        if (candidate.g < current.g) return current;
        return candidate.priority < current.priority ? candidate : current;
    }

    private static final int PRI_UNDER_DEEP = 0;
    private static final int PRI_DUNGEON = 1;
    private static final int PRI_UNDER = 2;
    private static final int PRI_DIM = 3;

    private static final class ExposureMatch {
        final String key;
        final int g;
        final int priority;

        ExposureMatch(String key, int g, int priority) {
            this.key = key;
            this.g = g;
            this.priority = priority;
        }
    }
}
