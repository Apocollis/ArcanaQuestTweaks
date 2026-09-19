package com.apocollis.aqtweaks.thaumcraft;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;

import com.apocollis.aqtweaks.util.Reflect;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import thaumcraft.common.lib.potions.PotionWarpWard;

public class ThaumcraftModule {

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        EntityPlayer player = event.player;
        if (Reflect.isRemote(player)) return;

        NBTTagCompound persisted = Reflect.getPersistedTag(player);
        if (!Reflect.hasKey(persisted, "VisitedDimensions")) {
            // First time joining the server: initialize with the current dimension so they don't get warp instantly
            Reflect.setIntArray(persisted, "VisitedDimensions", new int[]{ Reflect.getDimension(player) });
        }
    }

    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!ArcanaQuestTweaksConfig.ThaumcraftConfig.enableDimensionWarp) return;

        EntityPlayer player = event.player;
        if (Reflect.isRemote(player)) return;

        int toDim = event.toDim;
        NBTTagCompound persisted = Reflect.getPersistedTag(player);
        int[] visited = Reflect.getIntArray(persisted, "VisitedDimensions");

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
            Reflect.setIntArray(persisted, "VisitedDimensions", newVisited);
            // Run delayed thread, then schedule back on server main thread
            new Thread(() -> {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                net.minecraft.server.MinecraftServer server = Reflect.getServer(player);
                if (server != null) {
                    server.addScheduledTask(() -> {
                        // Check if player is still online and alive
                        net.minecraft.world.World pWorld = Reflect.getWorld(player);
                        if (Reflect.isDead(player) || pWorld == null || !pWorld.playerEntities.contains(player)) return;

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
                                SoundEvent sound = Reflect.getSoundEvent(soundName);
                                if (sound != null && pWorld != null) {
                                    float volume = ArcanaQuestTweaksConfig.ThaumcraftConfig.dimensionEntrySoundVolume;
                                    Reflect.playSound(pWorld, null, Reflect.getPosX(player), Reflect.getPosY(player), Reflect.getPosZ(player), sound, SoundCategory.PLAYERS, volume, 1.0F);
                                }
                            }

                            // Send chat message
                            String chatMsg = ArcanaQuestTweaksConfig.ThaumcraftConfig.dimensionChatMessageText;
                            if (chatMsg != null && !chatMsg.isEmpty()) {
                                Reflect.sendMessage(player, new TextComponentString(chatMsg));
                            }
                        }
                    });
                }
            }).start();
        }
    }

    @SubscribeEvent
    public void onPlayerWakeUp(PlayerWakeUpEvent event) {
        if (!ArcanaQuestTweaksConfig.ThaumcraftConfig.enableWarpCleansing) return;

        EntityPlayer player = event.getEntityPlayer();
        if (Reflect.isRemote(player) || event.wakeImmediately()) return;

        net.minecraft.world.World pWorld = Reflect.getWorld(player);

        // Verify they successfully slept (it is morning)
        if (pWorld != null && Reflect.isDaytime(pWorld)) {
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

            // Reduce Temporary Warp
            if (ArcanaQuestTweaksConfig.ThaumcraftConfig.clearTempWarp) {
                int currentTemp = ThaumcraftHelper.getWarp(player, 1);
                if (currentTemp > 0) {
                    ThaumcraftHelper.reduceWarp(player, 1, ArcanaQuestTweaksConfig.ThaumcraftConfig.tempWarpReduction);
                    clearedTemp = ThaumcraftHelper.getWarp(player, 1) < currentTemp;
                }
            }

            // Sync and notify player
            if (clearedNormal || clearedTemp) {
                ThaumcraftHelper.syncWarp(player);

                String chatMsg = ArcanaQuestTweaksConfig.ThaumcraftConfig.chatMessageText;
                if (ArcanaQuestTweaksConfig.ThaumcraftConfig.enableChatMessage && chatMsg != null && !chatMsg.isEmpty()) {
                    Reflect.sendMessage(player, new TextComponentString(chatMsg));
                }
            }
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || Reflect.isRemote(event.player)) return;
        int period = ArcanaQuestTweaksConfig.ThaumcraftConfig.exposureTickSeconds * 20;
        if (period <= 0) return;
        if (Reflect.getTicksExisted(event.player) % period != 0) return;
        evaluateExposureWarp(event.player);
    }

    private void evaluateExposureWarp(EntityPlayer player) {
        if (!ArcanaQuestTweaksConfig.ThaumcraftConfig.enableExposureWarp) return;

        Potion warpWard = PotionWarpWard.instance;
        if (warpWard != null && player.isPotionActive(warpWard)) return;

        ExposureMatch winner = pickWinningExposure(player);
        NBTTagCompound persisted = Reflect.getPersistedTag(player);
        NBTTagCompound banks = Reflect.getCompoundTag(persisted, "WarpExposureBySource");
        if (banks == null) {
            banks = new NBTTagCompound();
        }

        int leftover = Reflect.getInteger(persisted, "WarpExposureProgress");
        if (leftover > 0 && winner != null) {
            int existing = Reflect.getInteger(banks, winner.key);
            Reflect.setInteger(banks, winner.key, existing + leftover);
            Reflect.removeTag(persisted, "WarpExposureProgress");
        }

        if (winner == null || winner.g <= 0) {
            Reflect.setTag(persisted, "WarpExposureBySource", banks);
            return;
        }

        int tickSeconds = ArcanaQuestTweaksConfig.ThaumcraftConfig.exposureTickSeconds;
        int grantSeconds = ArcanaQuestTweaksConfig.ThaumcraftConfig.exposureGrantSeconds;
        int progress = Reflect.getInteger(banks, winner.key) + tickSeconds;
        if (progress >= grantSeconds) {
            progress = 0;
            ThaumcraftHelper.addWarp(player, 1, winner.g);
            ThaumcraftHelper.syncWarp(player);
            playExposureSound(player);
        }
        Reflect.setInteger(banks, winner.key, progress);
        Reflect.setTag(persisted, "WarpExposureBySource", banks);
    }

    private void playExposureSound(EntityPlayer player) {
        if (!ArcanaQuestTweaksConfig.ThaumcraftConfig.enableExposureSound) return;
        String soundName = ArcanaQuestTweaksConfig.ThaumcraftConfig.exposureSoundEffect;
        if (soundName == null || soundName.isEmpty()) return;
        SoundEvent sound = Reflect.getSoundEvent(soundName);
        net.minecraft.world.World pWorld = Reflect.getWorld(player);
        if (sound == null || pWorld == null) return;
        float volume = ArcanaQuestTweaksConfig.ThaumcraftConfig.exposureSoundVolume;
        Reflect.playSound(pWorld, null, Reflect.getPosX(player), Reflect.getPosY(player), Reflect.getPosZ(player),
                sound, SoundCategory.PLAYERS, volume, 1.0F);
    }

    private ExposureMatch pickWinningExposure(EntityPlayer player) {
        ExposureMatch winner = null;
        int playerDim = Reflect.getDimension(player);
        for (String entry : ArcanaQuestTweaksConfig.ThaumcraftConfig.exposureDimensionGrants) {
            String[] parts = entry.split("=");
            if (parts.length != 2) continue;
            try {
                int dim = Integer.parseInt(parts[0].trim());
                int g = Integer.parseInt(parts[1].trim());
                if (dim == playerDim && g > 0) {
                    winner = better(winner, new ExposureMatch("dim:" + dim, g, PRI_DIM));
                }
            } catch (NumberFormatException ignored) {
            }
        }

        if (ArcanaQuestTweaksConfig.ThaumcraftConfig.enableUndergroundExposure) {
            int y = (int) Math.floor(Reflect.getPosY(player));
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

        net.minecraft.world.World pWorld = Reflect.getWorld(player);
        if (ArcanaQuestTweaksConfig.ThaumcraftConfig.enableDungeonExposure && pWorld != null) {
            net.minecraft.world.chunk.IChunkProvider provider = pWorld.getChunkProvider();
            if (provider instanceof ChunkProviderServer
                    && ((ChunkProviderServer) provider).isInsideStructure(pWorld, "RoguelikeDungeon", Reflect.getPosition(player))) {
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
