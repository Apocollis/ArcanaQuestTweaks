package com.apocollis.aqtweaks.simpledifficulty;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.charles445.simpledifficulty.api.temperature.ITemperatureDynamicModifier;
import com.charles445.simpledifficulty.api.temperature.ITemperatureModifier;
import com.charles445.simpledifficulty.api.temperature.TemperatureRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;

/**
 * Surface indoor insulation for Simple Difficulty.
 *
 * <p>Simple Difficulty's {@code ModifierBase.applyUndergroundEffect} returns the raw value as soon as
 * {@code Y >= 64}, so biome, time, and Serene Seasons stay at full strength in a surface house. Shade does not
 * cover nights either: {@code ModifierTime} only applies {@code timeTemperatureShade} when the time value is
 * already positive. This modifier pulls the environment-driven part of the temperature toward neutral whenever
 * the sampled position is sheltered.
 *
 * <p>Runs in {@code applyDynamicWorldInfluence} so thermometers, item-frame thermometers, the client world
 * readout, and the player target all agree. {@code applyDynamicPlayerInfluence} is a pass-through.
 *
 * <p>Design and do-not-regress rules: {@code docs/stamina.md}.
 */
public class DynamicModifierInsulation implements ITemperatureDynamicModifier {

    public static final String NAME = "AQTweaksInsulation";

    /** Simple Difficulty's neutral body temperature, i.e. the {@code Default} modifier's +12. */
    private static final float NEUTRAL = 12.0f;

    /** Modifier names whose world influence counts as "environment" and gets pulled toward neutral. */
    private static final String[] DAMPENED = { "Biome", "Time", "Altitude", "Snow", "SereneSeasons" };

    private enum CeilingType { NONE, GLASS, OPAQUE, GREENHOUSE }

    // Serene Seasons greenhouse glass, resolved by registry name. No Serene Seasons import, no jar in libs/.
    // volatile + "value before flag" ordering: TemperatureRegistry holds ONE instance shared by the client thread
    // and the integrated server thread, so a plain flag could publish `true` before the block reference.
    private static volatile boolean greenhouseResolved;
    private static volatile Block greenhouseGlass;

    /**
     * Shelter memo. One per side, picked by {@code world.isRemote}, because {@code dynamicModifiers} is a single
     * static map: this modifier is one object shared by the client thread and the integrated server thread. The
     * split confines each map to a single thread instead of sharing a HashMap across both.
     */
    private final Memo clientMemo = new Memo();
    private final Memo serverMemo = new Memo();

    private static final class Memo {
        final Map<Long, Shelter> entries = new HashMap<>();
        long tick = Long.MIN_VALUE;
    }

    private static final class Shelter {
        final boolean sheltered;
        final CeilingType ceiling;

        Shelter(boolean sheltered, CeilingType ceiling) {
            this.sheltered = sheltered;
            this.ceiling = ceiling;
        }
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public float applyDynamicWorldInfluence(World world, BlockPos pos, float currentTemp) {
        if (world == null || pos == null) return currentTemp;

        ArcanaQuestTweaksConfig.SimpleDifficulty cfg =
                ArcanaQuestTweaksConfig.StaminaModuleConfig.simpleDifficulty;
        if (!cfg.indoorInsulationEnabled) return currentTemp;

        Shelter shelter = shelter(world, pos, cfg);
        if (shelter == null || !shelter.sheltered) return currentTemp;

        float targetTemp = shelter.ceiling == CeilingType.GREENHOUSE
                ? (float) cfg.greenhouseGlassTargetTemperature
                : NEUTRAL;
        float targetOffset = targetTemp - NEUTRAL;

        float envOffset = 0.0f;
        for (String name : DAMPENED) {
            ITemperatureModifier modifier = TemperatureRegistry.modifiers.get(name);
            if (modifier == null) continue;
            try {
                envOffset += modifier.getWorldInfluence(world, pos);
            } catch (Throwable ignored) {}
        }

        float pull = (envOffset - targetOffset) * (float) cfg.indoorInsulationFactor;
        return currentTemp - pull;
    }

    @Override
    public float applyDynamicPlayerInfluence(EntityPlayer player, float currentTemp) {
        return currentTemp;
    }

    // ---- shelter ----

    private Shelter shelter(World world, BlockPos pos, ArcanaQuestTweaksConfig.SimpleDifficulty cfg) {
        if (world.provider == null || !world.provider.hasSkyLight()) return null;

        Memo memo = world.isRemote ? clientMemo : serverMemo;
        long tick = world.getTotalWorldTime();
        if (tick != memo.tick) {
            memo.entries.clear();
            memo.tick = tick;
        }
        long key = pos.toLong() ^ ((long) world.provider.getDimension() * 31L);
        Shelter cached = memo.entries.get(key);
        if (cached != null) return cached;

        CeilingType ceiling = checkCeiling(world, pos, cfg);
        Shelter computed = new Shelter(isSheltered(world, pos, cfg, ceiling), ceiling);
        memo.entries.put(key, computed);
        return computed;
    }

    private boolean isSheltered(World world, BlockPos pos, ArcanaQuestTweaksConfig.SimpleDifficulty cfg,
            CeilingType ceiling) {
        if (ceiling == CeilingType.NONE) return false;
        // No Y gate: Simple Difficulty's underground scale is already inside every getWorldInfluence value this
        // modifier reads and inside currentTemp, so there is nothing to double-count. See docs/stamina.md.
        if (cfg.indoorInsulationEnclosureStrictness <= 0) return true;

        int walled = 0;
        for (EnumFacing dir : EnumFacing.HORIZONTALS) {
            if (rayWalled(world, pos, dir, cfg)) walled++;
        }
        return cfg.indoorInsulationEnclosureStrictness >= 2 ? walled >= 4 : walled >= 3;
    }

    private boolean rayWalled(World world, BlockPos pos, EnumFacing dir,
            ArcanaQuestTweaksConfig.SimpleDifficulty cfg) {
        for (int d = 1; d <= cfg.indoorInsulationMaxDistance; d++) {
            BlockPos checkPos = pos.offset(dir, d);
            // Never force a chunk load or generation from a temperature sample.
            if (!world.isBlockLoaded(checkPos)) return false;
            if (collides(world, checkPos) || collides(world, checkPos.up())) return true;
            // hasRoof, not checkCeiling: rays never need the greenhouse scan.
            if (!hasRoof(world, checkPos, cfg)) return false;
        }
        return false;
    }

    private static boolean collides(World world, BlockPos pos) {
        if (!world.isBlockLoaded(pos)) return false;
        try {
            IBlockState state = world.getBlockState(pos);
            return state.getCollisionBoundingBox(world, pos) != Block.NULL_AABB;
        } catch (Throwable t) {
            return false;
        }
    }

    // ---- roof ----

    /**
     * The only roof scan. {@link #hasRoof} and {@link #checkCeiling} both go through this, so there is no second
     * pass that can drift out of sync.
     */
    private static CeilingType roofType(World world, BlockPos pos,
            ArcanaQuestTweaksConfig.SimpleDifficulty cfg) {
        // Cheap first: one heightmap pair covers every opaque house and every cave. Glass has lightOpacity 0, so
        // canSeeSky stays true under it, which is the only case the block scan below exists for.
        if (!world.canSeeSky(pos) && !world.canSeeSky(pos.up())) return CeilingType.OPAQUE;

        for (int dy = 2; dy <= cfg.indoorInsulationCeilingMaxHeight; dy++) {
            BlockPos checkCeil = pos.up(dy);
            if (!world.isBlockLoaded(checkCeil)) return CeilingType.NONE;
            IBlockState state = world.getBlockState(checkCeil);
            if (state.getMaterial() == Material.GLASS) return CeilingType.GLASS;
            // isPassable, not isOpaqueCube: BlockLeaves.isOpaqueCube tracks the client fancy/fast setting.
            try {
                if (!state.getBlock().isPassable(world, checkCeil)) return CeilingType.OPAQUE;
            } catch (Throwable ignored) {}
        }
        return CeilingType.NONE;
    }

    private static boolean hasRoof(World world, BlockPos pos, ArcanaQuestTweaksConfig.SimpleDifficulty cfg) {
        return roofType(world, pos, cfg) != CeilingType.NONE;
    }

    private static CeilingType checkCeiling(World world, BlockPos pos,
            ArcanaQuestTweaksConfig.SimpleDifficulty cfg) {
        CeilingType roof = roofType(world, pos, cfg);
        if (roof == CeilingType.NONE) return CeilingType.NONE;

        Block glass = greenhouseGlass();
        if (glass != null) {
            // Matches Serene Seasons' SeasonalCropGrowthHandler.isGreenhouseGlassAboveBlock: scan from dy=1 and
            // do not stop at intervening blocks, so warmth and crop fertility agree in the same greenhouse.
            for (int dy = 1; dy <= cfg.greenhouseGlassMaxHeight; dy++) {
                BlockPos checkCeil = pos.up(dy);
                if (!world.isBlockLoaded(checkCeil)) continue;
                if (world.getBlockState(checkCeil).getBlock() == glass) return CeilingType.GREENHOUSE;
            }
        }
        return roof;
    }

    /** Greenhouse glass by registry name. Null when Serene Seasons is absent; greenhouse handling then disables. */
    private static Block greenhouseGlass() {
        if (!greenhouseResolved) {
            Block resolved = null;
            try {
                if (Loader.isModLoaded("sereneseasons")) {
                    ResourceLocation rl = new ResourceLocation("sereneseasons", "greenhouse_glass");
                    if (ForgeRegistries.BLOCKS.containsKey(rl)) {
                        Block block = ForgeRegistries.BLOCKS.getValue(rl);
                        if (block != null && block != Blocks.AIR) resolved = block;
                    }
                }
            } catch (Throwable ignored) {}
            // Value before flag, so a concurrent reader never sees resolved == true with a null block.
            greenhouseGlass = resolved;
            greenhouseResolved = true;
        }
        return greenhouseGlass;
    }
}
