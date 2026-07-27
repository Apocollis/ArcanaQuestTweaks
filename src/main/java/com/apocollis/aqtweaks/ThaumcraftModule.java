package com.apocollis.aqtweaks;

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
        if (player.world.isRemote) return;

        NBTTagCompound persisted = player.getEntityData().getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
        if (!persisted.hasKey("VisitedDimensions")) {
            // First time joining the server: initialize with the current dimension so they don't get warp instantly
            persisted.setIntArray("VisitedDimensions", new int[]{ player.dimension });
        }
    }

    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!ArcanaQuestTweaksConfig.thaumcraftModule.enableDimensionWarp) return;

        EntityPlayer player = event.player;
        if (player.world.isRemote) return;

        int toDim = event.toDim;
        NBTTagCompound persisted = player.getEntityData().getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
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
            // Run delayed thread, then schedule back on server main thread
            new Thread(() -> {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (player.getServer() != null) {
                    player.getServer().addScheduledTask(() -> {
                        // Check if player is still online and alive
                        if (player.isDead || !player.world.playerEntities.contains(player)) return;

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
                                ResourceLocation loc = new ResourceLocation(soundName);
                                SoundEvent sound = SoundEvent.REGISTRY.getObject(loc);
                                if (sound != null) {
                                    float volume = ArcanaQuestTweaksConfig.thaumcraftModule.dimensionEntrySoundVolume;
                                    player.world.playSound(null, player.posX, player.posY, player.posZ, sound, SoundCategory.PLAYERS, volume, 1.0F);
                                }
                            }

                            // Send chat message
                            String chatMsg = ArcanaQuestTweaksConfig.thaumcraftModule.dimensionChatMessageText;
                            if (chatMsg != null && !chatMsg.isEmpty()) {
                                player.sendMessage(new TextComponentString(chatMsg));
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
        if (player.world.isRemote || event.wakeImmediately()) return;

        // Verify they successfully slept (it is morning)
        if (player.world.isDaytime()) {
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
                    player.sendMessage(new TextComponentString(chatMsg));
                }
            }
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.player.world.isRemote && event.player.ticksExisted % 400 == 0) {
            evaluateExposureWarp(event.player);
        }
    }

    private void evaluateExposureWarp(EntityPlayer player) {
        if (!ArcanaQuestTweaksConfig.thaumcraftModule.enableExposureWarp) return;

        int shortestInterval = Integer.MAX_VALUE;

        // 1. Check dimension exposure
        int playerDim = player.dimension;
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
            player.posY <= ArcanaQuestTweaksConfig.thaumcraftModule.exposureUndergroundY) {
            shortestInterval = Math.min(shortestInterval, ArcanaQuestTweaksConfig.thaumcraftModule.exposureUndergroundInterval);
        }

        // 3. Check dungeon exposure
        if (ArcanaQuestTweaksConfig.thaumcraftModule.enableDungeonExposure) {
            if (RoguelikeDungeonSavedData.get(player.world).isInside(player.getPosition())) {
                shortestInterval = Math.min(shortestInterval, ArcanaQuestTweaksConfig.thaumcraftModule.exposureDungeonInterval);
            }
        }

        NBTTagCompound persisted = player.getEntityData().getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);

        if (shortestInterval != Integer.MAX_VALUE) {
            // Player is exposed
            int exposureProgress = persisted.getInteger("WarpExposureProgress") + 20; // 20 seconds elapsed
            
            if (exposureProgress >= shortestInterval) {
                exposureProgress = 0;
                // Add 1 temporary warp
                ThaumcraftHelper.addWarp(player, 1, 1);
                ThaumcraftHelper.syncWarp(player);
                
                if (ArcanaQuestTweaksConfig.thaumcraftModule.enableExposureSound) {
                    String soundName = ArcanaQuestTweaksConfig.thaumcraftModule.exposureSoundEffect;
                    if (soundName != null && !soundName.isEmpty()) {
                        ResourceLocation loc = new ResourceLocation(soundName);
                        SoundEvent sound = SoundEvent.REGISTRY.getObject(loc);
                        if (sound != null) {
                            float volume = ArcanaQuestTweaksConfig.thaumcraftModule.exposureSoundVolume;
                            player.world.playSound(null, player.posX, player.posY, player.posZ, sound, SoundCategory.PLAYERS, volume, 1.0F);
                        }
                    }
                }
            }
            persisted.setInteger("WarpExposureProgress", exposureProgress);
        } else {
            // Player is NOT exposed: slowly decay progress back to 0
            int exposureProgress = persisted.getInteger("WarpExposureProgress");
            if (exposureProgress > 0) {
                persisted.setInteger("WarpExposureProgress", Math.max(0, exposureProgress - 20));
            }
        }
    }
}
