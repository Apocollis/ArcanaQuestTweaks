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
 * Client telegraph and presence of the Ley-Rupture: during the warning the ambient forest sounds fall silent, a
 * resonance hum builds and columns of runes, wisps, sparks and dots begin to rise through the ground around the
 * player from the very start, thickening as the surge nears; during the surge the particles scale with the exposure
 * factor the server sent. The columns start at a spawn floor (24 blocks below the player, never above Y30), climb to
 * Y50 to Y55 and pass through ordinary blocks, stopping only in sheltered structures. The warning comes from the
 * shared hazard event, so a command-started rupture telegraphs exactly like a natural one.
 */
@SideOnly(Side.CLIENT)
public class LeyRuptureClient {
    private final Random rand = new Random();
    private HazardLoopSound hum;
    private float humScale;

    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        ClientLey.clear();
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
        if (p <= 0.01F) {
            if (ClientLey.shelterBoxes.length > 0) ClientLey.shelterBoxes = new int[0];
            return;
        }

        if (c.enableAudio) {
            // Audible from the first second of the warning, meeting the surge level at its end.
            humScale = state.phase == HazardEvent.Phase.ACTIVE ? 0.5F : 0.18F + 0.32F * p;
            if (hum == null || hum.isDonePlaying() || !mc.getSoundHandler().isSoundPlaying(hum)) {
                final float pitchBase = 1.0F;
                hum = new HazardLoopSound(player, SoundEvents.BLOCK_PORTAL_AMBIENT, SoundCategory.AMBIENT,
                        pitchBase + 0.9F * p, () -> humScale);
                mc.getSoundHandler().playSound(hum);
            }
        }
        if (!c.enableParticles) return;

        boolean active = state.phase == HazardEvent.Phase.ACTIVE;
        float scale = active ? Math.max(0.15F, ClientLey.exposure) : 0.15F + 0.45F * p;
        spawnParticles(mc, world, player, c, active, scale);
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (event.getWorld() != null && event.getWorld().isRemote) ParticleLeyEruption.resetCounts();
    }

    /**
     * Columns of runes (about 30 percent) and wisps, sparks and dots (about 70 percent) start at the spawn floor in a
     * wide square around the player, about half of them within 18 blocks so the player is surrounded and not only
     * ringed, with the density per square block held at the old 32 by 32 level; plus vent plumes in the surge. Every
     * particle climbs from the floor to its own peak at Y50 to Y55. No ground scan is needed: the columns pass through
     * the terrain, and none start inside a sheltered structure's footprint.
     */
    private void spawnParticles(Minecraft mc, World world, EntityPlayerSP player,
                                ArcanaQuestTweaksConfig.LeyRupture c, boolean active, float scale) {
        double radius = c.particleRadius;
        double density = c.particleDensity;
        int cap = c.maxParticles;
        int floorY = LeyEruptionShape.floorY(player.posY);
        net.minecraft.util.math.BlockPos.MutableBlockPos pos = new net.minecraft.util.math.BlockPos.MutableBlockPos();

        if (active) {
            // Vents first: they keep a reserved share of the cap so the ground pass cannot starve the plumes.
            int vents = LeyEruptionShape.vents(scale, radius, density);
            for (int v = 0; v < vents; v++) {
                if (!LeyEruptionShape.ventAllowed(ParticleLeyEruption.vent(), ParticleLeyEruption.active(), cap)) break;
                double[] o = LeyEruptionShape.spreadOffset(rand, radius, LeyEruptionShape.VENT_KEEP_OUT);
                double dist = Math.sqrt(o[0] * o[0] + o[1] * o[1]);
                double x = player.posX + o[0];
                double z = player.posZ + o[1];
                if (!spawnable(world, pos, x, floorY, z)) continue;
                float dScale = LeyEruptionShape.distanceScale(dist, radius);
                int bolts = 4 + rand.nextInt(4) + (dist > 24.0D ? 1 : 0);
                for (int b = 0; b < bolts; b++) {
                    if (!LeyEruptionShape.ventAllowed(ParticleLeyEruption.vent(), ParticleLeyEruption.active(), cap)) break;
                    addColumn(mc, world, x + (rand.nextDouble() - 0.5D) * 0.8D, floorY, z + (rand.nextDouble() - 0.5D) * 0.8D,
                            true, dScale);
                }
            }
        }

        int attempts = LeyEruptionShape.attempts(scale, active ? 36.0D : 14.0D, radius, density);
        for (int i = 0; i < attempts; i++) {
            if (!LeyEruptionShape.groundAllowed(ParticleLeyEruption.vent(), ParticleLeyEruption.active(), cap)) break;
            double[] o = LeyEruptionShape.spreadOffset(rand, radius, LeyEruptionShape.WARNING_KEEP_OUT);
            double dist = Math.sqrt(o[0] * o[0] + o[1] * o[1]);
            double x = player.posX + o[0];
            double z = player.posZ + o[1];
            if (!spawnable(world, pos, x, floorY, z)) continue;
            addColumn(mc, world, x, floorY, z, false, LeyEruptionShape.distanceScale(dist, radius));
        }
    }

    /** The chunk must be loaded and the column outside every sheltered structure's footprint. */
    private static boolean spawnable(World world, net.minecraft.util.math.BlockPos.MutableBlockPos pos, double x,
                                     int floorY, double z) {
        pos.setPos(x, floorY, z);
        return world.isBlockLoaded(pos) && !ClientLey.inShelterFootprint(x, z);
    }

    private void addColumn(Minecraft mc, World world, double x, int floorY, double z, boolean vent, float dScale) {
        boolean rune = LeyEruptionShape.isRune(rand);
        double rise = LeyEruptionShape.riseSpeed(rand, rune);
        double peak = LeyEruptionShape.peakY(rand);
        mc.effectRenderer.addEffect(new ParticleLeyEruption(world, x, floorY + rand.nextDouble() * 0.5D, z, rune, vent,
                dScale, peak, rise));
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
