package com.apocollis.aqtweaks.twilightforest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig.TwilightForestModuleConfig;
import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig.VoidBreach;

import net.darkhax.gamestages.GameStageHelper;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureStart;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import thaumcraft.common.lib.SoundsTC;
import thecodex6824.thaumicaugmentation.api.world.TADimensions;
import thecodex6824.thaumicaugmentation.api.world.capability.CapabilityFractureLocations;
import thecodex6824.thaumicaugmentation.api.world.capability.IFractureLocations;
import thecodex6824.thaumicaugmentation.common.entity.EntityDimensionalFracture;
import thecodex6824.thaumicaugmentation.common.util.FractureLocatorSearchManager;
import thecodex6824.thaumicaugmentation.common.world.ChunkGeneratorEmptiness;
import thecodex6824.thaumicaugmentation.common.world.feature.FractureUtils;

/**
 * The Void Breach ritual: a Carminite Reactor encased in the configured casing (default Void Metal) and right-clicked
 * with Salis Mundus charges for a while, then collapses into a permanent pair of Thaumic Augmentation fractures to The
 * Emptiness. The fractures are plain TA entities, so guardians, linking and the locator behave exactly as TA's own.
 * See docs/twilightforest.md.
 */
public class CarminiteReactorVoidBreachHandler {

    private static final Logger LOGGER = LogManager.getLogger("AQTweaks");

    private static final ResourceLocation TOWER_DEVICE_RL = new ResourceLocation("twilightforest", "tower_device");
    private static final ResourceLocation SALIS_MUNDUS_RL = new ResourceLocation("thaumcraft", "salis_mundus");
    private static final EnumFacing[] FACES = {EnumFacing.UP, EnumFacing.DOWN, EnumFacing.NORTH, EnumFacing.SOUTH,
            EnumFacing.EAST, EnumFacing.WEST};
    private static final String SPIRE_STRUCTURE = "EldritchSpire";

    private static final Map<Integer, List<RitualProcess>> ACTIVE_RITUALS = new HashMap<>();

    private static String cachedCasing;
    private static String cachedCap;
    private static VoidBreachShell.Resolved cachedShell;

    private static synchronized VoidBreachShell.Resolved shell() {
        VoidBreach cfg = TwilightForestModuleConfig.voidBreach;
        if (cachedShell == null || !Objects.equals(cachedCasing, cfg.casingBlock) || !Objects.equals(cachedCap, cfg.capBlock)) {
            cachedCasing = cfg.casingBlock;
            cachedCap = cfg.capBlock;
            cachedShell = VoidBreachShell.resolve(cfg.casingBlock, cfg.capBlock);
            for (String w : cachedShell.warnings) LOGGER.warn("[AQ-TF] Void Breach: {}", w);
        }
        return cachedShell;
    }

    private static boolean isInactiveReactor(IBlockState state) {
        Block block = state.getBlock();
        ResourceLocation rl = block.getRegistryName();
        return rl != null && rl.equals(TOWER_DEVICE_RL) && block.getMetaFromState(state) == 12;
    }

    /** Fills the six face names and metadata in UP, DOWN, NORTH, SOUTH, EAST, WEST order. */
    private static void readFaces(World world, BlockPos pos, String[] names, int[] metas) {
        for (int i = 0; i < FACES.length; i++) {
            IBlockState s = world.getBlockState(pos.offset(FACES[i]));
            ResourceLocation rl = s.getBlock().getRegistryName();
            names[i] = rl == null ? null : rl.toString();
            metas[i] = s.getBlock().getMetaFromState(s);
        }
    }

    private static boolean isVoidShell(World world, BlockPos pos) {
        String[] names = new String[VoidBreachShell.FACE_COUNT];
        int[] metas = new int[VoidBreachShell.FACE_COUNT];
        readFaces(world, pos, names, metas);
        return VoidBreachShell.matches(names, metas, shell());
    }

    private static boolean isOctineShell(World world, BlockPos pos) {
        String[] names = new String[VoidBreachShell.FACE_COUNT];
        int[] metas = new int[VoidBreachShell.FACE_COUNT];
        readFaces(world, pos, names, metas);
        return VoidBreachShell.isOctineShell(names);
    }

    private static boolean isRitualActive(World world, BlockPos pos) {
        List<RitualProcess> rituals = ACTIVE_RITUALS.get(world.provider.getDimension());
        if (rituals == null) return false;
        for (RitualProcess r : rituals) {
            if (r.pos.equals(pos)) return true;
        }
        return false;
    }

    private static boolean emptinessAvailableFrom(World world) {
        return TADimensions.EMPTINESS != null && world.provider.getDimension() != TADimensions.EMPTINESS.getId();
    }

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!TwilightForestModuleConfig.voidBreach.enable) return;
        if (event.getHand() != EnumHand.MAIN_HAND) return;
        World world = event.getWorld();
        BlockPos pos = event.getPos();
        if (!isInactiveReactor(world.getBlockState(pos))) return;

        ItemStack held = event.getItemStack();
        if (held.isEmpty()) return;
        ResourceLocation heldRl = held.getItem().getRegistryName();
        if (heldRl == null || !heldRl.equals(SALIS_MUNDUS_RL)) return;

        // The Octine shell belongs to the Phlogiston ritual; anything short of the Void shell is Twilight Forest's.
        if (isOctineShell(world, pos)) return;
        if (!isVoidShell(world, pos)) return;

        EntityPlayer player = event.getEntityPlayer();
        String stage = TwilightForestModuleConfig.voidBreach.requiredStage;
        if (stage != null && !stage.trim().isEmpty() && !GameStageHelper.hasStage(player, stage.trim())) {
            if (!world.isRemote) {
                TextComponentTranslation lead = new TextComponentTranslation("chat.aqtweaks.twilightforest.reactor_void_unlearned");
                lead.getStyle().setColor(TextFormatting.DARK_PURPLE);
                TextComponentTranslation detail = new TextComponentTranslation("chat.aqtweaks.twilightforest.reactor_void_unlearned.detail");
                detail.getStyle().setColor(TextFormatting.GRAY);
                lead.appendSibling(new TextComponentString(" "));
                lead.appendSibling(detail);
                player.sendMessage(lead);
            }
            event.setCanceled(true);
            return;
        }

        if (!emptinessAvailableFrom(world)) {
            if (!world.isRemote) {
                TextComponentTranslation msg = new TextComponentTranslation("chat.aqtweaks.twilightforest.reactor_void_unavailable");
                msg.getStyle().setColor(TextFormatting.GRAY);
                player.sendMessage(msg);
            }
            event.setCanceled(true);
            return;
        }

        if (isRitualActive(world, pos)) {
            event.setCanceled(true);
            return;
        }

        if (!world.isRemote) {
            ACTIVE_RITUALS.computeIfAbsent(world.provider.getDimension(), k -> new ArrayList<>()).add(new RitualProcess(pos));
            if (!player.capabilities.isCreativeMode) held.shrink(1);
        }
        player.swingArm(event.getHand());
        event.setCanceled(true);
        event.setCancellationResult(EnumActionResult.SUCCESS);
    }

    /** A ritual does not outlive its world: forget it on unload so it cannot tick in a later world with the same dimension id. */
    @SubscribeEvent
    public void onWorldUnload(net.minecraftforge.event.world.WorldEvent.Unload event) {
        if (event.getWorld().isRemote) return;
        int dim = event.getWorld().provider.getDimension();
        if (dim == 0) {
            ACTIVE_RITUALS.clear();
        } else {
            ACTIVE_RITUALS.remove(dim);
        }
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.world.isRemote) return;
        List<RitualProcess> rituals = ACTIVE_RITUALS.get(event.world.provider.getDimension());
        if (rituals == null || rituals.isEmpty()) return;
        WorldServer world = (WorldServer) event.world;
        Iterator<RitualProcess> it = rituals.iterator();
        while (it.hasNext()) {
            if (!it.next().tick(world)) it.remove();
        }
    }

    /** Count 0 makes the client read the offsets as a velocity, so directed motes are one packet each. */
    private static void spawnDirected(WorldServer world, EnumParticleTypes type, double x, double y, double z,
            double vx, double vy, double vz) {
        world.spawnParticle(type, x, y, z, 0, vx, vy, vz, 1.0D);
    }

    private static void refund(WorldServer world, BlockPos pos) {
        Item item = ForgeRegistries.ITEMS.getValue(SALIS_MUNDUS_RL);
        if (item == null || item == Items.AIR) return;
        EntityItem drop = new EntityItem(world, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, new ItemStack(item));
        drop.setDefaultPickupDelay();
        world.spawnEntity(drop);
    }

    private static WorldServer emptinessWorld() {
        if (TADimensions.EMPTINESS == null) return null;
        int id = TADimensions.EMPTINESS.getId();
        WorldServer w = DimensionManager.getWorld(id);
        if (w == null) {
            try {
                DimensionManager.initDimension(id);
            } catch (RuntimeException e) {
                LOGGER.error("[AQ-TF] Could not initialise The Emptiness", e);
            }
            w = DimensionManager.getWorld(id);
        }
        return w;
    }

    private static boolean blocksStanding(WorldServer w, int x, int y, int z) {
        IBlockState s = w.getBlockState(new BlockPos(x, y, z));
        return s.getMaterial().blocksMovement() || s.getMaterial().isLiquid();
    }

    /** The top standable Y in a column at or below {@code topY}, or -1. */
    private static int standableY(WorldServer w, int x, int z, int topY) {
        int minY = 1;
        int maxY = Math.min(250, Math.max(minY + 4, topY));
        boolean[] solid = new boolean[maxY - minY + 1];
        for (int i = 0; i < solid.length; i++) solid[i] = blocksStanding(w, x, minY + i, z);
        return ColumnSearch.topStandableY(solid, minY);
    }

    private static boolean fractureNear(WorldServer w, BlockPos p) {
        return !w.getEntitiesWithinAABB(EntityDimensionalFracture.class, new AxisAlignedBB(p).grow(4.0D)).isEmpty();
    }

    /** A standable spot in The Emptiness beside the nearest Eldritch Spire (or at the scaled position), or null. */
    private static BlockPos chooseDestination(WorldServer origin, WorldServer empt, BlockPos pos) {
        double ratio = FractureUtils.movementRatio(origin);
        BlockPos scaled = FractureUtils.scaleBlockPosToEmptiness(pos, ratio, empt.getSeed());
        BlockPos spire = empt.getChunkProvider().getNearestStructurePos(empt, SPIRE_STRUCTURE, scaled, true);

        List<int[]> columns = new ArrayList<>();
        int refY = 64;
        if (spire != null) {
            ChunkProviderServer provider = empt.getChunkProvider();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) provider.provideChunk((spire.getX() >> 4) + dx, (spire.getZ() >> 4) + dz);
            }
            int cx = spire.getX();
            int cz = spire.getZ();
            int radius = 10;
            if (provider.chunkGenerator instanceof ChunkGeneratorEmptiness) {
                StructureStart start = ((ChunkGeneratorEmptiness) provider.chunkGenerator).getSpireStart(spire);
                if (start != null && start.getBoundingBox() != null) {
                    StructureBoundingBox bb = start.getBoundingBox();
                    cx = (bb.minX + bb.maxX) / 2;
                    cz = (bb.minZ + bb.maxZ) / 2;
                    radius = Math.max(bb.getXSize(), bb.getZSize()) / 2 + 3;
                    refY = bb.minY;
                }
            }
            columns.add(new int[] {cx + radius, cz});
            columns.add(new int[] {cx - radius, cz});
            columns.add(new int[] {cx, cz + radius});
            columns.add(new int[] {cx, cz - radius});
            columns.add(new int[] {cx + radius, cz + radius});
            columns.add(new int[] {cx - radius, cz - radius});
        } else {
            int[][] ring = {{0, 0}, {6, 0}, {-6, 0}, {0, 6}, {0, -6}, {6, 6}, {-6, -6}};
            for (int[] r : ring) columns.add(new int[] {scaled.getX() + r[0], scaled.getZ() + r[1]});
        }

        BlockPos best = null;
        for (int[] c : columns) {
            empt.getChunkProvider().provideChunk(c[0] >> 4, c[1] >> 4);
            int y = standableY(empt, c[0], c[1], refY + 48);
            if (y < 0) continue;
            BlockPos candidate = new BlockPos(c[0], y, c[1]);
            if (fractureNear(empt, candidate)) continue;
            if (best == null || Math.abs(y - refY) < Math.abs(best.getY() - refY)) best = candidate;
            if (spire == null) break;
        }
        return best;
    }

    private static EntityDimensionalFracture spawnFracture(WorldServer w, BlockPos at, int linkDim, BlockPos linkPos, Biome biome) {
        EntityDimensionalFracture f = new EntityDimensionalFracture(w);
        f.setLocationAndAngles(at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, w.rand.nextFloat() * 360.0F, 0.0F);
        f.setLinkedDimension(linkDim);
        f.setLinkedPosition(linkPos);
        f.setDestinationBiome(biome);
        f.setLinkLocated(true);
        if (!w.spawnEntity(f)) return null;
        f.open(true);
        return f;
    }

    private static void register(WorldServer w, BlockPos at) {
        Chunk chunk = w.getChunk(at);
        IFractureLocations cap = chunk.getCapability(CapabilityFractureLocations.FRACTURE_LOCATIONS, null);
        if (cap != null) cap.addFractureLocation(at);
        chunk.markDirty();
        FractureLocatorSearchManager.addFractureLocation(w, at);
    }

    private static class RitualProcess {
        final BlockPos pos;
        int ticks = 0;

        RitualProcess(BlockPos pos) {
            this.pos = pos;
        }

        boolean tick(WorldServer world) {
            // Chunk unloaded: pause, so a reload resumes the ritual instead of losing the spent Salis Mundus.
            if (!world.isBlockLoaded(pos)) return true;

            if (!isInactiveReactor(world.getBlockState(pos)) || !isVoidShell(world, pos)) {
                fizzle(world);
                return false;
            }

            int duration = TwilightForestModuleConfig.voidBreach.chargeDurationTicks;
            double cx = pos.getX() + 0.5;
            double cy = pos.getY() + 0.5;
            double cz = pos.getZ() + 0.5;

            if (ticks % 20 == 0) {
                world.playSound(null, cx, cy, cz, SoundEvents.AMBIENT_CAVE, SoundCategory.BLOCKS, 1.0f, 0.5f + world.rand.nextFloat() * 0.1f);
            }
            if (ticks % 6 == 0) {
                float pitch = 0.4f + ((float) ticks / (float) duration) * 0.3f;
                world.playSound(null, cx, cy, cz, SoundEvents.BLOCK_PORTAL_TRIGGER, SoundCategory.BLOCKS, 1.0f, pitch);
            }

            for (EnumFacing facing : EnumFacing.VALUES) {
                for (int i = 0; i < 2; i++) {
                    double jx = (world.rand.nextDouble() - 0.5) * 0.3;
                    double jy = (world.rand.nextDouble() - 0.5) * 0.3;
                    double jz = (world.rand.nextDouble() - 0.5) * 0.3;
                    spawnDirected(world, EnumParticleTypes.PORTAL, cx, cy, cz,
                            facing.getXOffset() * 0.9 + jx, facing.getYOffset() * 0.9 + jy, facing.getZOffset() * 0.9 + jz);
                }
                if (ticks % 5 == 0 && facing.getAxis() != EnumFacing.Axis.Y) {
                    world.spawnParticle(EnumParticleTypes.DRAGON_BREATH, cx + facing.getXOffset() * 0.9, cy + (world.rand.nextDouble() - 0.5) * 0.6,
                            cz + facing.getZOffset() * 0.9, 1, 0.1, 0.1, 0.1, 0.01);
                }
            }
            if (ticks % 2 == 0) {
                // Deep eldritch purple: with count 0 the offsets are the red, green and blue of the mote.
                spawnDirected(world, EnumParticleTypes.SPELL_MOB, cx, cy, cz, 0.4, 0.0, 0.6);
                double angle = ticks * 0.3;
                spawnDirected(world, EnumParticleTypes.SMOKE_LARGE, cx + Math.cos(angle) * 0.65, cy, cz + Math.sin(angle) * 0.65,
                        -Math.sin(angle) * 0.05, 0.01, Math.cos(angle) * 0.05);
            }

            ticks++;
            if (ticks >= duration) {
                climax(world);
                return false;
            }
            return true;
        }

        private void climax(WorldServer world) {
            WorldServer empt = emptinessWorld();
            if (empt == null) {
                LOGGER.error("[AQ-TF] Failed to load The Emptiness dimension; Void Breach aborted and the Salis Mundus refunded");
                refund(world, pos);
                return;
            }
            BlockPos dest = chooseDestination(world, empt, pos);
            if (dest == null) {
                LOGGER.error("[AQ-TF] No standable spot found in The Emptiness for {} ({}); Void Breach aborted and refunded", pos, world.provider.getDimension());
                refund(world, pos);
                return;
            }

            int originDim = world.provider.getDimension();
            int emptDim = empt.provider.getDimension();
            EntityDimensionalFracture remote = null;
            EntityDimensionalFracture local = null;
            try {
                remote = spawnFracture(empt, dest, originDim, pos, world.getBiome(pos));
                if (remote != null) local = spawnFracture(world, pos, emptDim, dest, empt.getBiome(dest));
            } catch (RuntimeException e) {
                LOGGER.error("[AQ-TF] Void Breach fracture spawn failed", e);
            }
            if (remote == null || local == null) {
                if (remote != null) remote.setDead();
                if (local != null) local.setDead();
                refund(world, pos);
                return;
            }

            register(world, pos);
            register(empt, dest);

            double cx = pos.getX() + 0.5;
            double cy = pos.getY() + 0.5;
            double cz = pos.getZ() + 0.5;
            world.setBlockToAir(pos);
            for (EnumFacing facing : EnumFacing.VALUES) world.setBlockToAir(pos.offset(facing));

            // Visual only. createExplosion still damages entities when the terrain flag is false.
            world.playSound(null, cx, cy, cz, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.BLOCKS, 1.0f, 0.8f);
            world.spawnParticle(EnumParticleTypes.EXPLOSION_LARGE, cx, cy, cz, 1, 0.0, 0.0, 0.0, 0.0);
            world.spawnParticle(EnumParticleTypes.EXPLOSION_NORMAL, cx, cy, cz, 25, 0.6, 0.6, 0.6, 0.02);
            world.playSound(null, cx, cy, cz, SoundEvents.BLOCK_END_PORTAL_SPAWN, SoundCategory.BLOCKS, 2.0f, 0.7f);
            empt.playSound(null, dest.getX() + 0.5, dest.getY() + 0.5, dest.getZ() + 0.5, SoundEvents.BLOCK_END_PORTAL_SPAWN, SoundCategory.BLOCKS, 2.0f, 0.7f);
            if (SoundsTC.shock != null) {
                world.playSound(null, cx, cy, cz, SoundsTC.shock, SoundCategory.BLOCKS, 1.5f, 0.7f);
                empt.playSound(null, dest.getX() + 0.5, dest.getY() + 0.5, dest.getZ() + 0.5, SoundsTC.shock, SoundCategory.BLOCKS, 1.5f, 0.7f);
            }
        }

        private void fizzle(WorldServer world) {
            double cx = pos.getX() + 0.5;
            double cy = pos.getY() + 0.5;
            double cz = pos.getZ() + 0.5;
            world.playSound(null, cx, cy, cz, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 1.0f, 1.0f);
            world.spawnParticle(EnumParticleTypes.SMOKE_LARGE, cx, cy, cz, 25, 0.5, 0.5, 0.5, 0.05);
        }
    }
}
