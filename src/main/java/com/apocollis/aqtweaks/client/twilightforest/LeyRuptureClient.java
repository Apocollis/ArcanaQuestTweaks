package com.apocollis.aqtweaks.client.twilightforest;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.client.hazard.HazardLoopSound;
import com.apocollis.aqtweaks.hazard.HazardEvent;
import com.apocollis.aqtweaks.hazard.HazardState;
import com.apocollis.aqtweaks.hazard.Hazards;
import com.apocollis.aqtweaks.twilightforest.ClientLey;
import com.apocollis.aqtweaks.twilightforest.LeyChannels;
import com.apocollis.aqtweaks.twilightforest.LeyEruptionShape;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.world.World;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

/**
 * Client telegraph and presence of the Ley-Rupture: during the 30 s warning the ambient forest sounds fall silent
 * and a resonance hum builds, with glyph wisps and violet motes rising from the ground in the last third; during
 * the surge the particles scale with the exposure factor the server sent. The warning comes from the shared hazard
 * event, so a command-started rupture telegraphs exactly like a natural one.
 */
@SideOnly(Side.CLIENT)
public class LeyRuptureClient {
    private final Random rand = new Random();
    private HazardLoopSound hum;
    private float humScale;

    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        ClientLey.exposure = 0.0F;
        hum = null;
        ParticleLeyEruption.resetCounts();
    }

    /** 0..1 presence: the warning rises, the surge holds at 1 scaled by exposure. */
    private static float presence(HazardState state) {
        if (state.event == null) return 0.0F;
        if (state.phase == HazardEvent.Phase.WARNING) return state.warning;
        if (state.phase == HazardEvent.Phase.ACTIVE) return 1.0F;
        return 0.0F;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        World world = mc.world;
        EntityPlayerSP player = mc.player;
        humScale = 0.0F;
        if (world != null && !LeyChannels.isTf(world)) ParticleLeyEruption.resetCounts();
        if (world == null || player == null || mc.isGamePaused() || !LeyChannels.isTf(world)) return;
        ArcanaQuestTweaksConfig.LeyRupture c = ArcanaQuestTweaksConfig.TwilightForestModuleConfig.ley;
        if (!c.enable) return;
        HazardState state = Hazards.state(world, LeyChannels.RUPTURE);
        float p = presence(state);
        if (p <= 0.01F) return;

        if (c.enableAudio) {
            humScale = p * 0.5F;
            if (hum == null || hum.isDonePlaying() || !mc.getSoundHandler().isSoundPlaying(hum)) {
                final float pitchBase = 1.0F;
                hum = new HazardLoopSound(player, SoundEvents.BLOCK_PORTAL_AMBIENT, SoundCategory.AMBIENT,
                        pitchBase + 0.9F * p, () -> humScale);
                mc.getSoundHandler().playSound(hum);
            }
        }
        if (!c.enableParticles) return;

        boolean active = state.phase == HazardEvent.Phase.ACTIVE;
        boolean wisps = active
                || state.event.ticksToStart(state.tick) <= 200L; // the last ~10 s of the warning
        if (!wisps) return;
        float scale = active ? Math.max(0.15F, ClientLey.exposure) : p * 0.6F;
        spawnParticles(mc, world, player, c, active, scale);
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (event.getWorld() != null && event.getWorld().isRemote) ParticleLeyEruption.resetCounts();
    }

    /**
     * Glyphs and motes spread over a wide square around the player (never on top of the camera), with the density
     * per square block held at the old 32 by 32 level, plus vent plumes on a ring in the surge.
     */
    private void spawnParticles(Minecraft mc, World world, EntityPlayerSP player,
                                ArcanaQuestTweaksConfig.LeyRupture c, boolean active, float scale) {
        double radius = c.particleRadius;
        double density = c.particleDensity;
        int cap = c.maxParticles;
        int py = (int) Math.floor(player.posY);

        if (active) {
            // Vents first: they keep a reserved share of the cap so the ground pass cannot starve the plumes.
            int vents = LeyEruptionShape.vents(scale, radius, density);
            for (int v = 0; v < vents; v++) {
                if (!LeyEruptionShape.ventAllowed(ParticleLeyEruption.vent(), ParticleLeyEruption.active(), cap)) break;
                double[] o = LeyEruptionShape.offset(rand, radius, LeyEruptionShape.VENT_KEEP_OUT);
                double dist = Math.sqrt(o[0] * o[0] + o[1] * o[1]);
                double x = player.posX + o[0];
                double z = player.posZ + o[1];
                double y = groundY(world, (int) Math.floor(x), py, (int) Math.floor(z), dist);
                if (y < 0.0D) continue;
                float dScale = LeyEruptionShape.distanceScale(dist, radius);
                int bolts = 4 + rand.nextInt(4) + (dist > 24.0D ? 1 : 0);
                for (int b = 0; b < bolts; b++) {
                    if (!LeyEruptionShape.ventAllowed(ParticleLeyEruption.vent(), ParticleLeyEruption.active(), cap)) break;
                    boolean eruption = rand.nextFloat() < 0.70F;
                    mc.effectRenderer.addEffect(new ParticleLeyEruption(world, x + (rand.nextDouble() - 0.5D) * 0.8D,
                            y + 0.25D, z + (rand.nextDouble() - 0.5D) * 0.8D, eruption, true, true, dScale));
                }
                int sparks = 1 + rand.nextInt(2);
                for (int k = 0; k < sparks; k++) {
                    world.spawnParticle(EnumParticleTypes.CRIT_MAGIC, true, x, y + 0.4D, z,
                            (rand.nextDouble() - 0.5D) * 0.4D, 0.3D + rand.nextDouble() * 0.5D,
                            (rand.nextDouble() - 0.5D) * 0.4D);
                }
            }
        }

        int attempts = LeyEruptionShape.attempts(scale, active ? 36.0D : 14.0D, radius, density);
        for (int i = 0; i < attempts; i++) {
            if (!LeyEruptionShape.groundAllowed(ParticleLeyEruption.vent(), ParticleLeyEruption.active(), cap)) break;
            double[] o = LeyEruptionShape.offset(rand, radius, LeyEruptionShape.WARNING_KEEP_OUT);
            double dist = Math.sqrt(o[0] * o[0] + o[1] * o[1]);
            double x = player.posX + o[0];
            double z = player.posZ + o[1];
            double y = groundY(world, (int) Math.floor(x), py, (int) Math.floor(z), dist);
            if (y < 0.0D) continue;
            float dScale = LeyEruptionShape.distanceScale(dist, radius);
            if (active) {
                float roll = rand.nextFloat();
                if (roll < 0.35F) {
                    mc.effectRenderer.addEffect(new ParticleLeyEruption(world, x, y + 0.25D, z, false, false, true, dScale));
                } else if (roll < 0.70F) {
                    mc.effectRenderer.addEffect(new ParticleLeyEruption(world, x, y + 0.25D, z, true, false, true, dScale));
                } else {
                    world.spawnParticle(EnumParticleTypes.SPELL_WITCH, true, x, y + 0.2D, z, 0.0D,
                            0.30D + rand.nextDouble() * 0.16D, 0.0D);
                }
            } else {
                mc.effectRenderer.addEffect(new ParticleLeyEruption(world, x, y + 0.25D, z, false, false, false, dScale));
                if (rand.nextInt(2) == 0) {
                    world.spawnParticle(EnumParticleTypes.SPELL_WITCH, true, x, y + 0.2D, z, 0.0D,
                            0.26D + rand.nextDouble() * 0.12D, 0.0D);
                }
            }
        }
    }

    /**
     * Standing surface for a spawn column, or -1. Near the player it scans around the player's height; beyond 16
     * blocks hills and valleys differ too much for that, so the scan starts from the column's height map value and
     * walks down past leaves to the first solid block with air above it.
     */
    private static double groundY(World world, int x, int nearY, int z, double dist) {
        net.minecraft.util.math.BlockPos.MutableBlockPos pos = new net.minecraft.util.math.BlockPos.MutableBlockPos();
        if (dist <= 16.0D) {
            for (int y = nearY + 12; y >= nearY - 12; y--) {
                pos.setPos(x, y, z);
                if (!world.isBlockLoaded(pos)) return -1.0D;
                if (world.getBlockState(pos).getMaterial().isSolid()) {
                    pos.setPos(x, y + 1, z);
                    return world.isAirBlock(pos) ? y + 1 : -1.0D;
                }
            }
            return -1.0D;
        }
        pos.setPos(x, 64, z);
        if (!world.isBlockLoaded(pos)) return -1.0D;
        int top = world.getHeight(pos).getY();
        for (int y = top; y >= Math.max(1, top - 40); y--) {
            pos.setPos(x, y, z);
            net.minecraft.block.state.IBlockState state = world.getBlockState(pos);
            if (!state.getMaterial().isSolid() || state.getBlock().isLeaves(state, world, pos)) continue;
            pos.setPos(x, y + 1, z);
            return world.isAirBlock(pos) ? y + 1 : -1.0D;
        }
        return -1.0D;
    }

    /** Forest crickets and ambient wildlife fall dead silent from the start of the warning. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onPlaySound(PlaySoundEvent event) {
        if (!ArcanaQuestTweaksConfig.TwilightForestModuleConfig.ley.enableAudio) return;
        ISound sound = event.getSound();
        Minecraft mc = Minecraft.getMinecraft();
        World world = mc.world;
        if (sound == null || world == null || !LeyChannels.isTf(world)) return;
        HazardState state = Hazards.state(world, LeyChannels.RUPTURE);
        if (state.event == null || state.phase == HazardEvent.Phase.CLEAR) return;
        ResourceLocation loc = sound.getSoundLocation();
        if (loc == null) return;
        String domain = loc.getNamespace().toLowerCase();
        String path = loc.getPath().toLowerCase();
        if (domain.equals("aqtweaks")) return;
        boolean ambientMod = domain.equals("dsurround") || domain.equals("ambientsounds");
        boolean wildlife = path.contains("bird") || path.contains("cricket") || path.contains("cicada")
                || path.contains("frog") || path.contains("owl") || path.contains("crow") || path.contains("insect")
                || path.contains("nightingale") || path.contains("wolf.ambient") || path.contains("ambient.cave");
        if ((ambientMod && (wildlife || path.contains("wind"))) || (domain.equals("minecraft") && wildlife)) {
            event.setResultSound(null);
        }
    }
}
