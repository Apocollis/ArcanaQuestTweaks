package com.apocollis.aqtweaks.portal;

import com.apocollis.aqtweaks.ArcanaQuestTweaks;
import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig.PortalModuleConfig;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;

import java.util.List;
import java.util.Random;

@Mod.EventBusSubscriber(modid = ArcanaQuestTweaks.MODID)
public final class PortalModule {

    public static final ItemSpatialRiftTear TEAR = new ItemSpatialRiftTear();
    public static final ItemSpatialRiftWild WILD = new ItemSpatialRiftWild();

    private PortalModule() {}

    public static void preInit() {
        PortalLang.register();
        ForgeChunkManager.setForcedChunkLoadingCallback(ArcanaQuestTweaks.instance, (tickets, world) -> {
            for (ForgeChunkManager.Ticket ticket : tickets) {
                ForgeChunkManager.releaseTicket(ticket);
            }
        });
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().registerAll(TEAR, WILD);
    }

    @SubscribeEvent
    public static void registerEntities(RegistryEvent.Register<EntityEntry> event) {
        event.getRegistry().register(EntityEntryBuilder.create()
                .entity(EntityArcaneRift.class)
                .id(new net.minecraft.util.ResourceLocation(ArcanaQuestTweaks.MODID, "arcane_rift"), 1)
                .name("arcane_rift")
                .tracker(64, 3, false)
                .build());
    }

    public static boolean spawnLinkedRifts(World sourceWorld, EntityPlayer player, BlockPos destStand, boolean wild) {
        return spawnLinkedRifts(sourceWorld, sourceWorld, player, destStand, wild);
    }

    public static boolean spawnLinkedRifts(World sourceWorld, World destWorld, EntityPlayer player, BlockPos destStand,
            boolean wild) {
        if (sourceWorld.isRemote || destWorld.isRemote || !PortalModuleConfig.general.enable) {
            return false;
        }
        destWorld.getChunk(destStand);
        Vec3d look = player.getLookVec();
        double spawn = PortalModuleConfig.general.spawnOffset;
        double sx = player.posX + look.x * spawn;
        double sz = player.posZ + look.z * spawn;
        BlockPos sourceFeet = snapStand(sourceWorld, MathHelper.floor(sx), MathHelper.floor(player.posY),
                MathHelper.floor(sz));
        double sy = sourceFeet != null ? sourceFeet.getY() : player.posY;
        BlockPos destFeet = snapStand(destWorld, destStand.getX(), destStand.getY(), destStand.getZ());
        if (destFeet == null) {
            destFeet = findStandFromY(destWorld, destStand.getX(), destStand.getY(), destStand.getZ());
        }
        if (destFeet == null) {
            return false;
        }
        EntityArcaneRift source = new EntityArcaneRift(sourceWorld);
        source.setWild(wild);
        source.setPositionAndRotation(sx, sy, sz, player.rotationYaw, 0.0F);
        EntityArcaneRift dest = new EntityArcaneRift(destWorld);
        dest.setWild(wild);
        dest.setPositionAndRotation(
                destFeet.getX() + 0.5,
                destFeet.getY(),
                destFeet.getZ() + 0.5,
                player.rotationYaw + 180.0F,
                0.0F);
        source.setLinked(dest.getUniqueID(), destWorld.provider.getDimension());
        dest.setLinked(source.getUniqueID(), sourceWorld.provider.getDimension());
        if (!sourceWorld.spawnEntity(source) || !destWorld.spawnEntity(dest)) {
            source.setDead();
            dest.setDead();
            return false;
        }
        forceChunk(source);
        forceChunk(dest);
        player.timeUntilPortal = PortalModuleConfig.general.cooldownTicks;
        playOpenSounds(sourceWorld, player.getPosition());
        playOpenSounds(destWorld, destFeet);
        return true;
    }

    private static void playOpenSounds(World world, BlockPos pos) {
        world.playSound(null, pos, SoundEvents.ENTITY_LIGHTNING_THUNDER, SoundCategory.WEATHER, 1.0F, 1.0F);
        world.playSound(null, pos, SoundEvents.BLOCK_PORTAL_TRIGGER, SoundCategory.BLOCKS, 0.6F, 1.2F);
    }

    public static BlockPos findRandomLand(World world, EntityPlayer player) {
        Random rand = world.rand;
        int min = Math.min(PortalModuleConfig.general.wildMinDistance, PortalModuleConfig.general.wildMaxDistance);
        int max = Math.max(PortalModuleConfig.general.wildMinDistance, PortalModuleConfig.general.wildMaxDistance);
        int originX = MathHelper.floor(player.posX);
        int originZ = MathHelper.floor(player.posZ);
        for (int i = 0; i < PortalModuleConfig.general.wildSearchAttempts; i++) {
            double angle = rand.nextDouble() * Math.PI * 2.0;
            double dist = min + rand.nextDouble() * (max - min);
            int x = originX + MathHelper.floor(Math.cos(angle) * dist);
            int z = originZ + MathHelper.floor(Math.sin(angle) * dist);
            BlockPos stand = findStandPos(world, x, z);
            if (stand != null) {
                return stand;
            }
        }
        return null;
    }

    public static BlockPos findStandPos(World world, int x, int z) {
        world.getChunk(x >> 4, z >> 4);
        int startY = Math.max(world.getHeight(x, z), 1);
        BlockPos feet = findStandFromY(world, x, startY, z);
        if (feet == null) {
            return null;
        }
        IBlockState groundState = world.getBlockState(feet.down());
        if (groundState.getMaterial().isLiquid()) {
            return null;
        }
        Biome biome = world.getBiome(feet);
        if (BiomeDictionary.hasType(biome, BiomeDictionary.Type.OCEAN)) {
            return null;
        }
        return feet;
    }

    public static Vec3d standAtOffset(World world, double destX, double destY, double destZ, float yaw) {
        double offset = PortalModuleConfig.general.exitOffset;
        int startY = MathHelper.floor(destY);
        for (int step = 0; step < 8; step++) {
            float heading = yaw + step * 45.0F;
            double rad = Math.toRadians(heading);
            double x = destX + (-Math.sin(rad)) * offset;
            double z = destZ + Math.cos(rad) * offset;
            BlockPos feet = findStandFromY(world, MathHelper.floor(x), startY, MathHelper.floor(z));
            if (feet != null) {
                return new Vec3d(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5);
            }
        }
        BlockPos destFeet = findStandFromY(world, MathHelper.floor(destX), startY, MathHelper.floor(destZ));
        if (destFeet != null) {
            return new Vec3d(destFeet.getX() + 0.5, destFeet.getY(), destFeet.getZ() + 0.5);
        }
        return new Vec3d(destX, destY, destZ);
    }

    static BlockPos snapStand(World world, int x, int startY, int z) {
        int y = MathHelper.clamp(startY, 0, world.getHeight() - 2);
        while (y > 0 && isPlant(world, new BlockPos(x, y, z))) {
            y--;
        }
        BlockPos at = new BlockPos(x, y, z);
        if (isSolidFloor(world, at)) {
            return climbForBody(world, at);
        }
        if (isStandBody(world, at) && isSolidFloor(world, at.down()) && isStandBody(world, at.up())) {
            return at;
        }
        return null;
    }

    static BlockPos findStandFromY(World world, int x, int startY, int z) {
        int y = MathHelper.clamp(startY, 0, world.getHeight() - 2);
        int minY = Math.max(0, y - 48);
        int top = Math.min(world.getHeight() - 2, y + 8);
        for (int cursor = y; cursor >= minY; cursor--) {
            BlockPos ground = new BlockPos(x, cursor, z);
            if (!isSolidFloor(world, ground)) {
                continue;
            }
            BlockPos feet = ground.up();
            if (isStandBody(world, feet) && isStandBody(world, feet.up())) {
                return feet;
            }
        }
        for (int cursor = y; cursor <= top; cursor++) {
            BlockPos climbed = climbForBody(world, new BlockPos(x, cursor, z));
            if (climbed != null) {
                return climbed;
            }
        }
        return null;
    }

    private static BlockPos climbForBody(World world, BlockPos solid) {
        if (!isSolidFloor(world, solid)) {
            return null;
        }
        int maxY = world.getHeight() - 2;
        for (int dy = 1; dy <= 8; dy++) {
            int feetY = solid.getY() + dy;
            if (feetY > maxY) {
                break;
            }
            BlockPos feet = new BlockPos(solid.getX(), feetY, solid.getZ());
            if (isStandBody(world, feet) && isStandBody(world, feet.up())) {
                return feet;
            }
        }
        return null;
    }

    private static boolean isPlant(World world, BlockPos pos) {
        if (world.isAirBlock(pos) || isCanopy(world, pos)) {
            return false;
        }
        IBlockState state = world.getBlockState(pos);
        if (state.getMaterial().isLiquid() || state.getMaterial() == Material.AIR) {
            return false;
        }
        return state.getBlock().isReplaceable(world, pos);
    }

    private static boolean isCanopy(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        return state.getBlock().isLeaves(state, world, pos)
                || state.getMaterial() == Material.LEAVES
                || state.getBlock().isFoliage(world, pos);
    }

    private static boolean isStandBody(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        if (state.getMaterial().isLiquid() || isCanopy(world, pos) || isSolidFloor(world, pos)) {
            return false;
        }
        return world.isAirBlock(pos) || state.getMaterial() == Material.AIR || isPlant(world, pos);
    }

    private static boolean isSolidFloor(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        if (state.getMaterial().isLiquid()) {
            return false;
        }
        if (isCanopy(world, pos)) {
            return false;
        }
        return state.getMaterial().blocksMovement();
    }

    static void forceChunk(EntityArcaneRift rift) {
        if (rift.world.isRemote) {
            return;
        }
        ForgeChunkManager.Ticket ticket = ForgeChunkManager.requestTicket(
                ArcanaQuestTweaks.instance, rift.world, ForgeChunkManager.Type.NORMAL);
        if (ticket == null) {
            return;
        }
        ForgeChunkManager.forceChunk(ticket, rift.getChunkPos());
        rift.setChunkTicket(ticket);
    }

    static void releaseTicket(ForgeChunkManager.Ticket ticket) {
        if (ticket != null) {
            ForgeChunkManager.releaseTicket(ticket);
        }
    }

    static List<net.minecraft.entity.Entity> collectCompanions(EntityPlayer player) {
        double r = PortalModuleConfig.general.companionRadius;
        net.minecraft.util.math.AxisAlignedBB box = player.getEntityBoundingBox().grow(r, r, r);
        java.util.ArrayList<net.minecraft.entity.Entity> out = new java.util.ArrayList<>();
        java.util.UUID owner = player.getUniqueID();
        for (net.minecraft.entity.Entity entity : player.world.getEntitiesWithinAABB(net.minecraft.entity.Entity.class, box)) {
            if (entity == player || entity.isDead) {
                continue;
            }
            if (player.isRiding() && entity == player.getRidingEntity()) {
                continue;
            }
            if (entity instanceof net.minecraft.entity.passive.EntityTameable tame) {
                if (!tame.isTamed() || tame.isSitting()) {
                    continue;
                }
                if (owner.equals(tame.getOwnerId())) {
                    out.add(entity);
                }
                continue;
            }
            if (entity instanceof net.minecraft.entity.EntityLiving living
                    && living.getLeashed()
                    && living.getLeashHolder() == player) {
                out.add(entity);
            }
        }
        return out;
    }
}
