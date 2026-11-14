package com.apocollis.aqtweaks.aurorian;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.hazard.HazardEvent;
import com.apocollis.aqtweaks.hazard.HazardState;
import com.apocollis.aqtweaks.weather.LayerAccumulator;
import com.apocollis.aqtweaks.weather.Wildcard;

import net.minecraft.block.Block;
import net.minecraft.block.BlockSnow;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.MobEffects;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * Snow that piles up while a Moonfrost storm rages, and the burial hazard that goes with it. Server side.
 *
 * <p>Like vanilla snowfall it samples the loaded chunks around players, but faster and up to 16 layers deep, and
 * smoothly: a layer meant for a column goes to a lower neighbour when it would leave the column more than
 * {@code Snow Max Step} layers deeper ({@link com.apocollis.aqtweaks.weather.LayerLeveling}). It only ever places vanilla snow layers into air (never
 * replacing a block), keeps no state, and leaves melting to vanilla ({@code BlockSnow} melts near bright light).
 */
public class MoonfrostSnowHandler extends LayerAccumulator {

    public static final DamageSource DAMAGE_SNOW_BURIAL = new DamageSource("aqtweaks_snow_burial").setDamageBypassesArmor();
    static final String NBT_DEBT = "AQTSnowAirDebt";

    // ---- accumulation (the shared build-up in LayerAccumulator, with the snow specifics) ----

    @Override
    protected boolean worldMatches(World world) {
        return MoonfrostState.isAurorian(world);
    }

    @Override
    protected boolean enabled() {
        return ArcanaQuestTweaksConfig.AurorianModuleConfig.enableSnowAccumulation && MoonfrostState.enabled();
    }

    @Override
    protected float strength(WorldServer world) {
        HazardState state = MoonfrostState.get(world);
        return state.phase == HazardEvent.Phase.ACTIVE ? state.strength : 0.0F;
    }

    @Override
    protected double ratePerChunkPerSecond() {
        return ArcanaQuestTweaksConfig.AurorianModuleConfig.snowLayersPerChunkPerSecond;
    }

    @Override
    protected int maxChangesPerTick() {
        return ArcanaQuestTweaksConfig.AurorianModuleConfig.snowMaxChangesPerTick;
    }

    @Override
    protected int radius() {
        return ArcanaQuestTweaksConfig.AurorianModuleConfig.snowAccumulationRadius;
    }

    @Override
    protected int cap() {
        return ArcanaQuestTweaksConfig.AurorianModuleConfig.maxSnowLayers;
    }

    @Override
    protected int step() {
        return ArcanaQuestTweaksConfig.AurorianModuleConfig.snowMaxStep;
    }

    @Override
    protected boolean windShadow() {
        return ArcanaQuestTweaksConfig.AurorianModuleConfig.snowWindShadow;
    }

    @Override
    protected double shadowAngle() {
        return ArcanaQuestTweaksConfig.AurorianModuleConfig.snowFallAngle;
    }

    @Override
    protected Vec3d wind(WorldServer world) {
        return AurorianWindHelper.getWindVector(world, true);
    }

    @Override
    protected boolean isLayerBlock(IBlockState state) {
        return state.getBlock() == Blocks.SNOW_LAYER;
    }

    @Override
    protected int layersOf(IBlockState state) {
        return state.getValue(BlockSnow.LAYERS);
    }

    @Override
    protected IBlockState withLayers(IBlockState state, int layers) {
        return state.withProperty(BlockSnow.LAYERS, layers);
    }

    @Override
    protected IBlockState firstLayer() {
        return Blocks.SNOW_LAYER.getDefaultState();
    }

    @Override
    protected boolean canPlaceLayer(WorldServer world, BlockPos pos) {
        return Blocks.SNOW_LAYER.canPlaceBlockAt(world, pos);
    }

    // ---- replaceable plants ----

    private static Set<Block> replaceable;
    private static String[] replaceableFor;

    /** Air, or (when enabled) one of the listed wild ground plants. */
    @Override
    protected boolean canStartLayerIn(IBlockState state, WorldServer world, BlockPos pos) {
        Block block = state.getBlock();
        if (block.isAir(state, world, pos)) return true;
        if (!ArcanaQuestTweaksConfig.AurorianModuleConfig.snowReplacesPlants) return false;
        return replaceableBlocks().contains(block);
    }

    /** The listed blocks, resolved once per config array (a config sync replaces the array object). */
    private static Set<Block> replaceableBlocks() {
        String[] patterns = ArcanaQuestTweaksConfig.AurorianModuleConfig.snowReplaceablePlants;
        if (replaceable == null || replaceableFor != patterns) {
            Set<Block> set = Collections.newSetFromMap(new IdentityHashMap<>());
            for (Block block : ForgeRegistries.BLOCKS) {
                if (block.getRegistryName() != null
                        && Wildcard.matchesAny(block.getRegistryName().toString(), patterns)) {
                    set.add(block);
                }
            }
            replaceable = set;
            replaceableFor = patterns;
        }
        return replaceable;
    }

    // ---- burial ----

    /** Once a second per Aurorian player (from {@link MoonfrostHandler}): slowness at the chest, air loss at the head. */
    public static void burialSecond(EntityPlayerMP player) {
        NBTTagCompound data = player.getEntityData();
        if (!ArcanaQuestTweaksConfig.AurorianModuleConfig.enableSnowBurial || player.isCreative() || player.isSpectator()
                || player instanceof FakePlayer) {
            if (data.hasKey(NBT_DEBT)) data.removeTag(NBT_DEBT);
            return;
        }
        World world = player.world;
        int x = (int) Math.floor(player.posX);
        int z = (int) Math.floor(player.posZ);
        int y0 = Math.max(1, (int) Math.floor(player.posY) - 1);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        double top = Double.NEGATIVE_INFINITY;
        for (int y = y0; y <= y0 + 4 && y < 255; y++) {
            p.setPos(x, y, z);
            if (!world.isBlockLoaded(p)) break;
            IBlockState st = world.getBlockState(p);
            if (st.getBlock() == Blocks.SNOW_LAYER) {
                int layers = st.getValue(BlockSnow.LAYERS);
                top = y + layers * 0.125D;
                if (layers < 8) break;
            } else if (top > Double.NEGATIVE_INFINITY) {
                break;
            }
        }

        SnowBurial.Level level = top == Double.NEGATIVE_INFINITY ? SnowBurial.Level.NONE
                : SnowBurial.level(player.posY + 0.8D, player.posY + player.getEyeHeight(), top);
        int debt = data.getInteger(NBT_DEBT);
        if (level == SnowBurial.Level.NONE) {
            debt = SnowBurial.debtWithHeadFree(debt);
        } else {
            if (ArcanaQuestTweaksConfig.AurorianModuleConfig.snowBurialSlowness) {
                player.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 40, 1, false, false));
            }
            if (level == SnowBurial.Level.HEAD) {
                int drain = ArcanaQuestTweaksConfig.AurorianModuleConfig.snowBurialAirDrainPerSecond;
                debt = SnowBurial.debtWithHeadUnder(debt, drain);
                if (drain > 0 && SnowBurial.outOfAir(debt)) {
                    float dmg = SnowBurial.damage(player.getHealth(), (float) ArcanaQuestTweaksConfig.AurorianModuleConfig.snowBurialDamage);
                    if (dmg > 0.0F) player.attackEntityFrom(DAMAGE_SNOW_BURIAL, dmg);
                }
            } else {
                debt = SnowBurial.debtWithHeadFree(debt);
            }
        }
        if (debt <= 0) {
            if (data.hasKey(NBT_DEBT)) data.removeTag(NBT_DEBT);
        } else {
            data.setInteger(NBT_DEBT, debt);
        }
    }

    /** Every tick: vanilla refills the air bar each tick, so hold it at the burial level while the debt lasts. */
    public static void holdAir(EntityPlayerMP player) {
        NBTTagCompound data = player.getEntityData();
        if (!data.hasKey(NBT_DEBT)) return;
        int debt = data.getInteger(NBT_DEBT);
        if (debt > 0) player.setAir(Math.max(0, SnowBurial.AIR_MAX - debt));
    }

    public static void clearBurial(EntityPlayerMP player) {
        NBTTagCompound data = player.getEntityData();
        if (data.hasKey(NBT_DEBT)) data.removeTag(NBT_DEBT);
    }
}
