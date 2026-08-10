package com.apocollis.aqtweaks.thaumcraft;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;

import com.apocollis.aqtweaks.roguelike.RoguelikeDungeonSavedData;

import com.apocollis.aqtweaks.util.Reflect;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

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
        if (!ArcanaQuestTweaksConfig.thaumcraftModule.enableDimensionWarp) return;

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
                        if (ArcanaQuestTweaksConfig.thaumcraftModule.dimensionNormalWarp > 0) {
                            ThaumcraftHelper.addWarp(player, 0, ArcanaQuestTweaksConfig.thaumcraftModule.dimensionNormalWarp);
                            addedNormal = true;
                        }

                        if (ArcanaQuestTweaksConfig.thaumcraftModule.dimensionTempWarp > 0) {
                            ThaumcraftHelper.addWarp(player, 1, ArcanaQuestTweaksConfig.thaumcraftModule.dimensionTempWarp);
                            addedTemp = true;
                        }

                        if (addedNormal || addedTemp) {
                            ThaumcraftHelper.syncWarp(player);

                            // Play sound effect
                            String soundName = ArcanaQuestTweaksConfig.thaumcraftModule.dimensionEntrySound;
                            if (soundName != null && !soundName.isEmpty()) {
                                SoundEvent sound = Reflect.getSoundEvent(soundName);
                                if (sound != null && pWorld != null) {
                                    float volume = ArcanaQuestTweaksConfig.thaumcraftModule.dimensionEntrySoundVolume;
                                    Reflect.playSound(pWorld, null, Reflect.getPosX(player), Reflect.getPosY(player), Reflect.getPosZ(player), sound, SoundCategory.PLAYERS, volume, 1.0F);
                                }
                            }

                            // Send chat message
                            String chatMsg = ArcanaQuestTweaksConfig.thaumcraftModule.dimensionChatMessageText;
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
        if (!ArcanaQuestTweaksConfig.thaumcraftModule.enableWarpCleansing) return;

        EntityPlayer player = event.getEntityPlayer();
        if (Reflect.isRemote(player) || event.wakeImmediately()) return;

        net.minecraft.world.World pWorld = Reflect.getWorld(player);

        // Verify they successfully slept (it is morning)
        if (pWorld != null && Reflect.isDaytime(pWorld)) {
            boolean clearedNormal = false;
            boolean clearedTemp = false;

            // Reduce Normal Warp
            if (ArcanaQuestTweaksConfig.thaumcraftModule.clearNormalWarp) {
                int currentNormal = ThaumcraftHelper.getWarp(player, 0);
                if (currentNormal > 0) {
                    ThaumcraftHelper.reduceWarp(player, 0, ArcanaQuestTweaksConfig.thaumcraftModule.normalWarpReduction);
                    clearedNormal = true;
                }
            }

            // Reduce Temporary Warp
            if (ArcanaQuestTweaksConfig.thaumcraftModule.clearTempWarp) {
                int currentTemp = ThaumcraftHelper.getWarp(player, 1);
                if (currentTemp > 0) {
                    ThaumcraftHelper.reduceWarp(player, 1, ArcanaQuestTweaksConfig.thaumcraftModule.tempWarpReduction);
                    clearedTemp = true;
                }
            }

            // Sync and notify player
            if (clearedNormal || clearedTemp) {
                ThaumcraftHelper.syncWarp(player);

                String chatMsg = ArcanaQuestTweaksConfig.thaumcraftModule.chatMessageText;
                if (ArcanaQuestTweaksConfig.thaumcraftModule.enableChatMessage && chatMsg != null && !chatMsg.isEmpty()) {
                    Reflect.sendMessage(player, new TextComponentString(chatMsg));
                }
            }
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !Reflect.isRemote(event.player) && Reflect.getTicksExisted(event.player) % 400 == 0) {
            evaluateExposureWarp(event.player);
        }
    }

    private void evaluateExposureWarp(EntityPlayer player) {
        if (!ArcanaQuestTweaksConfig.thaumcraftModule.enableExposureWarp) return;

        int shortestInterval = Integer.MAX_VALUE;

        // 1. Check dimension exposure
        int playerDim = Reflect.getDimension(player);
        for (String entry : ArcanaQuestTweaksConfig.thaumcraftModule.exposureDimensionsConfig) {
            String[] parts = entry.split("=");
            if (parts.length == 2) {
                try {
                    int dim = Integer.parseInt(parts[0].trim());
                    int seconds = Integer.parseInt(parts[1].trim());
                    if (dim == playerDim) {
                        shortestInterval = Math.min(shortestInterval, seconds);
                    }
                } catch (Exception e) {
                    // Ignore malformed entry
                }
            }
        }

        // 2. Check deep underground exposure
        if (ArcanaQuestTweaksConfig.thaumcraftModule.enableUndergroundExposure &&
            Reflect.getPosY(player) <= ArcanaQuestTweaksConfig.thaumcraftModule.exposureUndergroundY) {
            shortestInterval = Math.min(shortestInterval, ArcanaQuestTweaksConfig.thaumcraftModule.exposureUndergroundInterval);
        }

        // 3. Check dungeon exposure
        net.minecraft.world.World pWorld = Reflect.getWorld(player);
        if (ArcanaQuestTweaksConfig.thaumcraftModule.enableDungeonExposure && pWorld != null) {
            if (RoguelikeDungeonSavedData.get(pWorld).isInside(Reflect.getPosition(player))) {
                shortestInterval = Math.min(shortestInterval, ArcanaQuestTweaksConfig.thaumcraftModule.exposureDungeonInterval);
            }
        }

        NBTTagCompound persisted = Reflect.getPersistedTag(player);

        if (shortestInterval != Integer.MAX_VALUE) {
            // Player is exposed
            int exposureProgress = Reflect.getInteger(persisted, "WarpExposureProgress") + 20; // 20 seconds elapsed
            
            if (exposureProgress >= shortestInterval) {
                exposureProgress = 0;
                // Add 1 temporary warp
                ThaumcraftHelper.addWarp(player, 1, 1);
                ThaumcraftHelper.syncWarp(player);
                
                if (ArcanaQuestTweaksConfig.thaumcraftModule.enableExposureSound) {
                    String soundName = ArcanaQuestTweaksConfig.thaumcraftModule.exposureSoundEffect;
                    if (soundName != null && !soundName.isEmpty()) {
                        SoundEvent sound = Reflect.getSoundEvent(soundName);
                        if (sound != null && pWorld != null) {
                            float volume = ArcanaQuestTweaksConfig.thaumcraftModule.exposureSoundVolume;
                            Reflect.playSound(pWorld, null, Reflect.getPosX(player), Reflect.getPosY(player), Reflect.getPosZ(player), sound, SoundCategory.PLAYERS, volume, 1.0F);
                        }
                    }
                }
            }
            Reflect.setInteger(persisted, "WarpExposureProgress", exposureProgress);
        } else {
            // Player is NOT exposed: slowly decay progress back to 0
            int exposureProgress = Reflect.getInteger(persisted, "WarpExposureProgress");
            if (exposureProgress > 0) {
                Reflect.setInteger(persisted, "WarpExposureProgress", Math.max(0, exposureProgress - 20));
            }
        }
    }
}
