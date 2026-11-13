package com.apocollis.aqtweaks.twilightforest;

import net.darkhax.gamestages.GameStageHelper;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import twilightforest.block.BlockTFTowerDevice;
import twilightforest.enums.TowerDeviceVariant;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Handles the in-world Carminite Reactor Phlogiston Matrix synthesis ritual.
 * Right-clicking a Carminite Reactor (Inactive) with Thaumcraft Salis Mundus surrounded
 * by 6 Betweenlands Octine Blocks initiates a 200-tick charging sequence producing Phlogiston Matrix.
 */
public class CarminiteReactorRitualHandler {

    private static final Logger LOGGER = LogManager.getLogger("AQTweaks");

    private static final ResourceLocation TOWER_DEVICE_RL = new ResourceLocation("twilightforest", "tower_device");
    private static final ResourceLocation OCTINE_BLOCK_RL = new ResourceLocation("thebetweenlands", "octine_block");
    private static final ResourceLocation SALIS_MUNDUS_RL = new ResourceLocation("thaumcraft", "salis_mundus");
    private static final ResourceLocation PHLOGISTON_MATRIX_RL = new ResourceLocation("contenttweaker", "phlogiston_matrix");

    private static final Map<Integer, List<RitualProcess>> ACTIVE_RITUALS = new HashMap<>();
    private static boolean matrixMissingLogged;

    public static boolean isRitualActive(World world, BlockPos pos) {
        List<RitualProcess> rituals = ACTIVE_RITUALS.get(world.provider.getDimension());
        if (rituals == null || rituals.isEmpty()) {
            return false;
        }
        for (RitualProcess ritual : rituals) {
            if (ritual.pos.equals(pos)) {
                return true;
            }
        }
        return false;
    }

    public static void startRitual(World world, BlockPos pos) {
        int dim = world.provider.getDimension();
        ACTIVE_RITUALS.computeIfAbsent(dim, k -> new ArrayList<>()).add(new RitualProcess(pos));
    }

    private static boolean isInactiveReactor(IBlockState state) {
        Block block = state.getBlock();
        if (block instanceof BlockTFTowerDevice) {
            return state.getValue(BlockTFTowerDevice.VARIANT) == TowerDeviceVariant.REACTOR_INACTIVE;
        }
        ResourceLocation rl = block.getRegistryName();
        return rl != null && rl.equals(TOWER_DEVICE_RL) && block.getMetaFromState(state) == 12;
    }

    /** All six faces are {@code thebetweenlands:octine_block}. A missing registry entry is not a shell. */
    private static boolean isOctineShell(World world, BlockPos pos) {
        Block octineBlock = ForgeRegistries.BLOCKS.getValue(OCTINE_BLOCK_RL);
        if (octineBlock == null || octineBlock == Blocks.AIR) {
            return false;
        }
        for (EnumFacing facing : EnumFacing.VALUES) {
            if (world.getBlockState(pos.offset(facing)).getBlock() != octineBlock) {
                return false;
            }
        }
        return true;
    }

    private static Item matrixItem() {
        Item item = ForgeRegistries.ITEMS.getValue(PHLOGISTON_MATRIX_RL);
        if (item == null || item == Items.AIR) {
            return null;
        }
        return item;
    }

    private static void warnMatrixMissing() {
        if (matrixMissingLogged) {
            return;
        }
        matrixMissingLogged = true;
        LOGGER.warn("[AQ-TF] {} is not registered; Carminite phlogiston ritual will not start or consume the shell",
                PHLOGISTON_MATRIX_RL);
    }

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        World world = event.getWorld();
        BlockPos pos = event.getPos();
        IBlockState state = world.getBlockState(pos);

        if (!isInactiveReactor(state)) {
            return;
        }

        ItemStack held = event.getItemStack();
        if (held.isEmpty()) {
            return;
        }

        ResourceLocation heldRl = held.getItem().getRegistryName();
        if (heldRl == null || !heldRl.equals(SALIS_MUNDUS_RL)) {
            return;
        }

        // Incomplete shell, or Octine not in the registry: leave Twilight Forest's own right-click alone.
        if (!isOctineShell(world, pos)) {
            return;
        }

        EntityPlayer player = event.getEntityPlayer();
        if (!GameStageHelper.hasStage(player, "adamantine")) {
            if (!world.isRemote) {
                TextComponentTranslation lead = new TextComponentTranslation(
                        "chat.aqtweaks.twilightforest.reactor_unlearned");
                lead.getStyle().setColor(TextFormatting.DARK_PURPLE);
                TextComponentTranslation detail = new TextComponentTranslation(
                        "chat.aqtweaks.twilightforest.reactor_unlearned.detail");
                detail.getStyle().setColor(TextFormatting.GRAY);
                lead.appendSibling(new TextComponentString(" "));
                lead.appendSibling(detail);
                player.sendMessage(lead);
            }
            event.setCanceled(true);
            return;
        }

        if (matrixItem() == null) {
            if (!world.isRemote) {
                warnMatrixMissing();
            }
            return;
        }

        if (isRitualActive(world, pos)) {
            event.setCanceled(true);
            return;
        }

        if (!world.isRemote) {
            startRitual(world, pos);
            if (!player.capabilities.isCreativeMode) {
                held.shrink(1);
            }
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
        if (event.phase != TickEvent.Phase.END || event.world.isRemote) {
            return;
        }

        int dim = event.world.provider.getDimension();
        List<RitualProcess> rituals = ACTIVE_RITUALS.get(dim);
        if (rituals == null || rituals.isEmpty()) {
            return;
        }

        WorldServer worldServer = (WorldServer) event.world;
        Iterator<RitualProcess> iterator = rituals.iterator();
        while (iterator.hasNext()) {
            RitualProcess ritual = iterator.next();
            if (!ritual.tick(worldServer)) {
                iterator.remove();
            }
        }
    }

    /**
     * Count 0 makes the client treat offset as velocity ({@code motion = speed * offset}).
     * A positive count scatters instead, so directed motes have to be one packet each.
     */
    private static void spawnDirected(WorldServer world, EnumParticleTypes type,
            double x, double y, double z, double vx, double vy, double vz) {
        world.spawnParticle(type, x, y, z, 0, vx, vy, vz, 1.0D);
    }

    private static class RitualProcess {
        final BlockPos pos;
        int ticks = 0;

        RitualProcess(BlockPos pos) {
            this.pos = pos;
        }

        boolean tick(WorldServer world) {
            // Chunk unloaded: pause. Forgetting the ritual here would keep the spent Salis Mundus
            // and leave the shell, with no way to resume.
            if (!world.isBlockLoaded(pos)) {
                return true;
            }

            IBlockState centerState = world.getBlockState(pos);
            if (!isInactiveReactor(centerState) || !isOctineShell(world, pos)) {
                fizzle(world);
                return false;
            }

            double cx = pos.getX() + 0.5;
            double cy = pos.getY() + 0.5;
            double cz = pos.getZ() + 0.5;

            // Audio ramp: every 5 ticks, pitch from 0.5f toward 2.0f. Tick 195 is the last cue.
            if (ticks % 5 == 0) {
                float pitch = 0.5f + ((float) ticks / 200.0f) * 1.5f;
                world.playSound(null, cx, cy, cz, SoundEvents.BLOCK_PORTAL_TRIGGER, SoundCategory.BLOCKS, 1.0f, pitch);
            }

            for (EnumFacing facing : EnumFacing.VALUES) {
                double ox = cx + facing.getXOffset();
                double oy = cy + facing.getYOffset();
                double oz = cz + facing.getZOffset();

                // Portal motes travel from the Octine face to the core (they move from pos+motion toward pos).
                for (int i = 0; i < 3; i++) {
                    double jx = (world.rand.nextDouble() - 0.5) * 0.3;
                    double jy = (world.rand.nextDouble() - 0.5) * 0.3;
                    double jz = (world.rand.nextDouble() - 0.5) * 0.3;
                    spawnDirected(world, EnumParticleTypes.PORTAL, cx, cy, cz,
                            facing.getXOffset() * 0.9 + jx,
                            facing.getYOffset() * 0.9 + jy,
                            facing.getZOffset() * 0.9 + jz);
                }

                double progress = world.rand.nextDouble();
                double px = ox + (cx - ox) * progress;
                double py = oy + (cy - oy) * progress;
                double pz = oz + (cz - oz) * progress;
                spawnDirected(world, EnumParticleTypes.FLAME, px, py, pz,
                        (cx - px) * 0.15, (cy - py) * 0.15, (cz - pz) * 0.15);

                if (ticks % 6 == 0) {
                    world.spawnParticle(EnumParticleTypes.LAVA, ox, oy, oz, 1, 0.1, 0.1, 0.1, 0.0);
                }

                double angle = (ticks * 0.25) + facing.ordinal() * (Math.PI / 3.0);
                double radius = 0.6;
                double tx = -Math.sin(angle) * 0.08;
                double tz = Math.cos(angle) * 0.08;
                spawnDirected(world, EnumParticleTypes.FLAME,
                        cx + Math.cos(angle) * radius,
                        cy + Math.sin((ticks + facing.ordinal() * 10) * 0.15) * 0.35,
                        cz + Math.sin(angle) * radius,
                        tx, 0.01, tz);
            }

            ticks++;

            if (ticks >= 200) {
                Item matrix = matrixItem();
                if (matrix == null) {
                    warnMatrixMissing();
                    fizzle(world);
                    return false;
                }

                // Visual only. createExplosion still damages entities when the terrain flag is false.
                world.playSound(null, cx, cy, cz, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.BLOCKS, 1.0f, 1.0f);
                world.spawnParticle(EnumParticleTypes.EXPLOSION_LARGE, cx, cy, cz, 1, 0.0, 0.0, 0.0, 0.0);
                world.spawnParticle(EnumParticleTypes.EXPLOSION_NORMAL, cx, cy, cz, 20, 0.6, 0.6, 0.6, 0.0);

                world.setBlockToAir(pos);
                for (EnumFacing facing : EnumFacing.VALUES) {
                    world.setBlockToAir(pos.offset(facing));
                }

                int count = 5 + world.rand.nextInt(3); // 5, 6, or 7
                ItemStack dropStack = new ItemStack(matrix, count);
                EntityItem entityItem = new EntityItem(world, cx, cy, cz, dropStack);
                entityItem.motionX = (world.rand.nextDouble() - 0.5) * 0.08;
                entityItem.motionY = 0.25;
                entityItem.motionZ = (world.rand.nextDouble() - 0.5) * 0.08;
                entityItem.setDefaultPickupDelay();
                world.spawnEntity(entityItem);

                world.playSound(null, cx, cy, cz, SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.BLOCKS, 1.5f, 1.0f);
                world.playSound(null, cx, cy, cz, SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.BLOCKS, 1.5f, 1.2f);
                return false;
            }

            return true;
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
