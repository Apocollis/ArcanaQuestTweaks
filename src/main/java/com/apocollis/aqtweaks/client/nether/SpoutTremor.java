package com.apocollis.aqtweaks.client.nether;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.hazard.HazardEvent;
import com.apocollis.aqtweaks.hazard.HazardState;
import com.apocollis.aqtweaks.hazard.Hazards;
import com.apocollis.aqtweaks.client.hazard.HazardLoopSound;
import com.apocollis.aqtweaks.nether.NetherHazards;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

/**
 * The tremor that warns of a spout event: a rising rumble, bubbling lava surfaces near the player and a very
 * small camera shake. It reads the client copy of the {@code spout} hazard event, so a command-started event
 * shakes exactly like a natural one, and it runs for every player in the Nether.
 */
@SideOnly(Side.CLIENT)
public class SpoutTremor {
    private final Random rand = new Random();
    private HazardLoopSound sound;
    private float scale;

    /** 0..1: the warning ramps up, then the event holds at half and fades as it ends. */
    private static float tremorScale(HazardState state) {
        if (state.event == null) return 0.0F;
        if (state.phase == HazardEvent.Phase.WARNING) return state.warning;
        if (state.phase == HazardEvent.Phase.ACTIVE) {
            float total = Math.max(1.0F, state.event.end - state.event.start);
            return 0.5F * Math.min(1.0F, state.ticksToEnd / total);
        }
        return 0.0F;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        World world = mc.world;
        EntityPlayerSP player = mc.player;
        scale = 0.0F;
        if (world == null || player == null || mc.isGamePaused() || !NetherHazards.isNether(world)
                || !ArcanaQuestTweaksConfig.NetherModuleConfig.enableSpoutEffects) {
            return;
        }
        scale = tremorScale(Hazards.state(world, NetherHazards.SPOUT));
        if (scale <= 0.01F) return;

        if (sound == null || sound.isDonePlaying() || !mc.getSoundHandler().isSoundPlaying(sound)) {
            sound = new HazardLoopSound(player, SoundEvents.BLOCK_LAVA_AMBIENT, SoundCategory.AMBIENT, 0.5F,
                    () -> Math.min(1.0F, scale) * 0.9F);
            mc.getSoundHandler().playSound(sound);
        }

        int attempts = (int) (scale * ArcanaQuestTweaksConfig.NetherModuleConfig.tremorMaxParticles / 4.0F);
        int range = ArcanaQuestTweaksConfig.NetherModuleConfig.spoutRange;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int i = 0; i < attempts; i++) {
            int x = (int) Math.floor(player.posX) + rand.nextInt(range * 2 + 1) - range;
            int z = (int) Math.floor(player.posZ) + rand.nextInt(range * 2 + 1) - range;
            for (int y = (int) player.posY + 8; y >= (int) player.posY - 12; y--) {
                pos.setPos(x, y, z);
                if (!world.isBlockLoaded(pos)) break;
                IBlockState state = world.getBlockState(pos);
                if (state.getBlock() == Blocks.LAVA || state.getBlock() == Blocks.FLOWING_LAVA) {
                    world.spawnParticle(EnumParticleTypes.LAVA, x + rand.nextDouble(), y + 1.0D, z + rand.nextDouble(),
                            0.0D, 0.0D, 0.0D);
                    if (rand.nextInt(3) == 0) {
                        world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, x + rand.nextDouble(), y + 1.1D,
                                z + rand.nextDouble(), 0.0D, 0.04D, 0.0D);
                    }
                    break;
                }
            }
        }
    }

    @SubscribeEvent
    public void onCameraSetup(EntityViewRenderEvent.CameraSetup event) {
        if (!ArcanaQuestTweaksConfig.NetherModuleConfig.tremorCameraShake || scale <= 0.01F) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.currentScreen != null || mc.world == null) return;
        double t = mc.world.getTotalWorldTime() + event.getRenderPartialTicks();
        float amp = 0.28F * Math.min(1.0F, scale);
        event.setPitch(event.getPitch() + (float) (Math.sin(t * 1.9D) + 0.6D * Math.sin(t * 3.1D)) * amp * 0.6F);
        event.setRoll(event.getRoll() + (float) (Math.sin(t * 2.3D + 1.0D) + 0.6D * Math.sin(t * 3.7D)) * amp * 0.6F);
    }
}
