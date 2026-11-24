package com.apocollis.aqtweaks.thaumadditions.foci;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.thaumadditions.AegisMath;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.casters.FocusEffect;
import thaumcraft.api.casters.NodeSetting;
import thaumcraft.api.casters.Trajectory;
import thaumcraft.common.lib.SoundsTC;

/**
 * Aegis: a shield held up while the caster keeps casting. The focus itself does nothing when cast; the channel
 * (raise, vis upkeep, directional block, break) lives in {@code AegisChannelHandler}, started from the cast hook in
 * {@code MixinItemCaster}, because the focus API has no per-tick callback. Ordinary instance created through
 * {@code FocusEngine.registerElement} (public no-arg constructor required).
 */
public class FocusEffectAegis extends FocusEffect {
    public static final String KEY = "focus.aqtweaks.aegis";
    public static final String RESEARCH = "AQ_FOCUS_AEGIS";
    /** Registered element colour: Protect's teal. */
    public static final int COLOR = 0x00C0C0;

    @Override
    public String getResearch() {
        return RESEARCH;
    }

    @Override
    public String getKey() {
        return KEY;
    }

    @Override
    public Aspect getAspect() {
        return Aspect.PROTECT;
    }

    /** Focus cast cost is complexity / 5 vis; sized so the raise costs exactly one interval's bill. */
    @Override
    public int getComplexity() {
        return AegisMath.complexityFor(ArcanaQuestTweaksConfig.ThaumAdditionsConfig.aegisVisPerInterval);
    }

    @Override
    public NodeSetting[] createSettings() {
        return new NodeSetting[] {
                new NodeSetting("power", "focus.common.power", new NodeSetting.NodeSettingIntRange(1, 5))
        };
    }

    /** Damage the shield can take before it breaks, for this focus's power setting. */
    public float pool() {
        return pool(1.0D);
    }

    /** The pool with the per-level value and the cap both scaled (the Ley-Rupture overchannel doubles both). */
    public float pool(double scale) {
        int perLevel = (int) Math.max(1L, Math.round(ArcanaQuestTweaksConfig.ThaumAdditionsConfig.aegisPoolPerLevel * scale));
        int max = (int) Math.max(1L, Math.round(ArcanaQuestTweaksConfig.ThaumAdditionsConfig.aegisMaxPool * scale));
        return AegisMath.pool(getSettingValue("power"), perLevel, max);
    }

    @Override
    public boolean execute(RayTraceResult target, Trajectory trajectory, float finalPower, int num) {
        return target != null && target.entityHit != null && target.entityHit == getPackage().getCaster();
    }

    @Override
    public void onCast(Entity caster) {
        float volume = (float) ArcanaQuestTweaksConfig.ThaumAdditionsConfig.aegisSoundVolume;
        if (!(caster instanceof EntityPlayer) || caster.world.isRemote || volume <= 0.0F) return;
        caster.world.playSound(null, caster.posX, caster.posY, caster.posZ, SoundsTC.runicShieldCharge,
                SoundCategory.PLAYERS, 0.6F * volume, 1.0F);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderParticleFX(World world, double x, double y, double z, double vx, double vy, double vz) {
        float r = ((COLOR >> 16) & 255) / 255.0F;
        float g = ((COLOR >> 8) & 255) / 255.0F;
        float b = (COLOR & 255) / 255.0F;
        for (int i = 0; i < 6; i++) {
            double dx = (world.rand.nextDouble() - 0.5D) * 1.0D;
            double dz = (world.rand.nextDouble() - 0.5D) * 1.0D;
            world.spawnParticle(EnumParticleTypes.SPELL_MOB, x + dx, y + world.rand.nextDouble() * 0.6D, z + dz, r, g, b);
        }
    }
}
