package com.apocollis.aqtweaks.thaumadditions.foci;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.potion.Stun;
import com.apocollis.aqtweaks.thaumadditions.PoweredCreeper;
import com.apocollis.aqtweaks.thaumadditions.ShockMath;
import com.apocollis.aqtweaks.thaumadditions.ShockTargets;
import com.apocollis.aqtweaks.thaumcraft.ThaumcraftFocusHooks;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSourceIndirect;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.casters.FocusEffect;
import thaumcraft.api.casters.NodeSetting;
import thaumcraft.api.casters.Trajectory;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.lib.network.PacketHandler;
import thaumcraft.common.lib.network.fx.PacketFXFocusPartImpact;
import thaumcraft.common.lib.network.fx.PacketFXZap;

/**
 * Shock: Potentia lightning. Frost's damage formula, a wet bonus, a chain to nearby targets (wider through water)
 * and a short {@link Stun} scaled by the {@code duration} setting. Ordinary instance created through
 * {@code FocusEngine.registerElement} (public no-arg constructor required).
 */
public class FocusEffectShock extends FocusEffect {
    public static final String KEY = "focus.aqtweaks.shock";
    public static final String RESEARCH = "AQ_FOCUS_SHOCK";
    /** Registered element colour (also the arc colour). */
    public static final int COLOR = 0xFFE150;

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
        return Aspect.ENERGY;
    }

    @Override
    public int getComplexity() {
        return getSettingValue("duration") + getSettingValue("power") * 2;
    }

    @Override
    public float getDamageForDisplay(float finalPower) {
        return ShockMath.baseDamage(ArcanaQuestTweaksConfig.ThaumAdditionsConfig.baseDamageOffset,
                getSettingValue("power"), finalPower);
    }

    @Override
    public NodeSetting[] createSettings() {
        return new NodeSetting[] {
                new NodeSetting("power", "focus.common.power", new NodeSetting.NodeSettingIntRange(1, 5)),
                new NodeSetting("duration", "focus.common.duration", new NodeSetting.NodeSettingIntRange(1, 5))
        };
    }

    @Override
    public boolean execute(RayTraceResult target, Trajectory trajectory, float finalPower, int num) {
        if (target == null || target.hitVec == null) return false;
        World world = getPackage().world;
        if (world == null || world.isRemote) return false;
        EntityLivingBase caster = getPackage().getCaster();

        impact(world, target.hitVec);
        if (target.typeOfHit != RayTraceResult.Type.ENTITY || !(target.entityHit instanceof EntityLivingBase)) {
            return false;
        }

        float primary = getDamageForDisplay(finalPower);
        int duration = getSettingValue("duration");
        int stun = ShockMath.stunTicks(ArcanaQuestTweaksConfig.ThaumAdditionsConfig.stunBaseTicks,
                ArcanaQuestTweaksConfig.ThaumAdditionsConfig.stunTicksPerLevel, duration);

        List<Entity> hit = new ArrayList<>();
        EntityLivingBase current = (EntityLivingBase) target.entityHit;
        hit.add(current);
        strike(caster, current, primary, true, stun);

        int jumps = ArcanaQuestTweaksConfig.ThaumAdditionsConfig.chainJumps;
        for (int jump = 1; jump <= jumps; jump++) {
            EntityLivingBase next = nextTarget(world, caster, current, hit);
            if (next == null) break;
            hit.add(next);
            float chain = ShockMath.chainDamage(primary,
                    (float) ArcanaQuestTweaksConfig.ThaumAdditionsConfig.chainFalloff, jump);
            arc(world, center(current), center(next));
            strike(caster, next, chain, false, stun);
            current = next;
        }
        return true;
    }

    private void strike(EntityLivingBase caster, EntityLivingBase victim, float rawDamage, boolean primary, int stun) {
        boolean wet = ShockTargets.wet(victim);
        float damage = ShockMath.wetBonus(rawDamage, wet, (float) ArcanaQuestTweaksConfig.ThaumAdditionsConfig.wetDamageMultiplier);
        DamageSource source = caster == null
                ? new DamageSource("aqtweaks_shock_wild")
                : new EntityDamageSourceIndirect("aqtweaks_shock", caster, caster);
        ThaumcraftFocusHooks.markMagic(source);
        impact(victim.world, center(victim));
        victim.attackEntityFrom(source, damage);
        if (victim.isEntityAlive()) {
            if (stun > 0 && ShockMath.stuns(primary, wet,
                    ArcanaQuestTweaksConfig.ThaumAdditionsConfig.stunWetChained)) {
                Stun.apply(victim, stun);
            }
            if (ArcanaQuestTweaksConfig.ThaumAdditionsConfig.chargeCreepers && victim instanceof EntityCreeper
                    && !((EntityCreeper) victim).getPowered()) {
                ((PoweredCreeper) victim).aqtweaks$setPowered();
            }
        }
    }

    private static EntityLivingBase nextTarget(World world, EntityLivingBase caster, EntityLivingBase from,
                                               List<Entity> already) {
        double normal = ArcanaQuestTweaksConfig.ThaumAdditionsConfig.chainRadius;
        double wetR = ArcanaQuestTweaksConfig.ThaumAdditionsConfig.wetChainRadius;
        boolean fromWet = ShockTargets.wet(from);
        double search = Math.max(normal, fromWet ? wetR : normal);
        AxisAlignedBB box = from.getEntityBoundingBox().grow(search);
        EntityLivingBase best = null;
        double bestDist = Double.MAX_VALUE;
        for (EntityLivingBase e : world.getEntitiesWithinAABB(EntityLivingBase.class, box)) {
            if (already.contains(e) || !ShockTargets.validChainTarget(caster, e)) continue;
            double r = ShockMath.jumpRadius(fromWet, ShockTargets.wet(e), normal, wetR);
            double d = from.getDistanceSq(e);
            if (d <= r * r && d < bestDist) {
                bestDist = d;
                best = e;
            }
        }
        return best;
    }

    private static Vec3d center(Entity e) {
        return new Vec3d(e.posX, e.posY + e.height * 0.5D, e.posZ);
    }

    private void impact(World world, Vec3d at) {
        PacketHandler.INSTANCE.sendToAllAround(new PacketFXFocusPartImpact(at.x, at.y, at.z, new String[] {getKey()}),
                new NetworkRegistry.TargetPoint(world.provider.getDimension(), at.x, at.y, at.z, 64.0D));
        float volume = (float) ArcanaQuestTweaksConfig.ThaumAdditionsConfig.soundVolume;
        if (volume > 0.0F) {
            world.playSound(null, at.x, at.y, at.z, SoundsTC.shock, SoundCategory.PLAYERS, 0.45F * volume, 1.0F);
        }
    }

    private static void arc(World world, Vec3d from, Vec3d to) {
        PacketHandler.INSTANCE.sendToAllAround(new PacketFXZap(from, to, COLOR, 0.45F),
                new NetworkRegistry.TargetPoint(world.provider.getDimension(), from.x, from.y, from.z, 64.0D));
    }

    @Override
    public void onCast(Entity caster) {
        float volume = (float) ArcanaQuestTweaksConfig.ThaumAdditionsConfig.soundVolume;
        if (caster == null || caster.world.isRemote || volume <= 0.0F) return;
        caster.world.playSound(null, caster.getPosition().up(), SoundsTC.zap, SoundCategory.PLAYERS, 0.5F * volume,
                1.0F + (float) (caster.world.rand.nextGaussian() * 0.05D));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderParticleFX(World world, double x, double y, double z, double vx, double vy, double vz) {
        float r = ((COLOR >> 16) & 255) / 255.0F;
        float g = ((COLOR >> 8) & 255) / 255.0F;
        float b = (COLOR & 255) / 255.0F;
        for (int i = 0; i < 2; i++) {
            double dx = (world.rand.nextDouble() - 0.5D) * 0.9D;
            double dy = (world.rand.nextDouble() - 0.3D) * 0.9D;
            double dz = (world.rand.nextDouble() - 0.5D) * 0.9D;
            thaumcraft.client.fx.FXDispatcher.INSTANCE.arcLightning(x, y, z, x + dx, y + dy, z + dz, r, g, b, 0.3F);
        }
    }
}
