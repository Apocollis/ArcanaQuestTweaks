package com.apocollis.aqtweaks.somnia;

import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;

/**
 * Invisible, non-colliding mount entity spawned on beds, hammocks, or sleeping bags when a player
 * cannot sleep (e.g. not tired in Somnia or wrong time of day) so they sit comfortably and gain resting bonuses.
 */
public class EntityBedSeat extends Entity {

    public EntityBedSeat(World world) {
        super(world);
        this.noClip = true;
        this.setSize(1.0E-4F, 0.0625F);
    }

    public EntityBedSeat(World world, double x, double y, double z) {
        this(world);
        this.setPosition(x, y, z);
    }

    @Override
    public double getMountedYOffset() {
        return 0.0D;
    }

    @Override
    public boolean canBePushed() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean isInvisible() {
        return true;
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (this.world.isRemote) return;

        if (!this.isBeingRidden()) {
            this.setDead();
            return;
        }

        BlockPos pos = new BlockPos(this.posX, this.posY, this.posZ);
        if (!isValidBedOrHammockBlock(this.world, pos)) {
            this.setDead();
        }
    }

    public static boolean isValidBedOrHammockBlock(World world, BlockPos pos) {
        if (world == null || pos == null) return false;
        IBlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        if (block.isBed(state, world, pos, null)) return true;
        String name = block.getClass().getName();
        return name.contains("BlockHammock") || name.contains("BlockSleepingBag") || name.contains("BlockBase");
    }

    public static boolean isSeatAt(World world, BlockPos pos) {
        if (world == null || pos == null) return false;
        AxisAlignedBB aabb = new AxisAlignedBB(pos).grow(0.2D);
        List<EntityBedSeat> seats = world.getEntitiesWithinAABB(EntityBedSeat.class, aabb);
        return seats != null && !seats.isEmpty();
    }

    /**
     * Attempts to mount the player on the bed, hammock, or sleeping bag at {@code headPos}.
     */
    public static boolean mount(World world, BlockPos headPos, EntityPlayer player) {
        if (world == null || headPos == null || player == null || world.isRemote) return false;
        if (player.isRiding()) return false;

        IBlockState state = world.getBlockState(headPos);
        Block block = state.getBlock();

        // Calculate seat position centered on the bed / hammock
        double seatX = headPos.getX() + 0.5D;
        double seatZ = headPos.getZ() + 0.5D;
        double seatY = headPos.getY();

        EnumFacing facing = null;
        if (state.getPropertyKeys().contains(BlockHorizontal.FACING)) {
            facing = state.getValue(BlockHorizontal.FACING);
        }

        String className = block.getClass().getName();
        boolean isHammock = className.contains("BlockHammock");
        boolean isBag = className.contains("BlockSleepingBag");

        if (isBag) {
            seatY += 0.20D;
        } else if (isHammock) {
            // Comforts EntityRest sits at y - 0.1825D; player eye height adjusted
            seatY += 0.35D;
        } else {
            // Vanilla or modded beds
            seatY += 0.52D;
        }

        // Check if there is already a seat at the head or foot
        if (isSeatAt(world, headPos)) return false;
        if (facing != null) {
            BlockPos footPos = headPos.offset(facing.getOpposite());
            if (isSeatAt(world, footPos)) return false;
            // Place seat closer to the middle of the bed / hammock
            seatX -= facing.getXOffset() * 0.4D;
            seatZ -= facing.getZOffset() * 0.4D;
        }

        EntityBedSeat seat = new EntityBedSeat(world, seatX, seatY, seatZ);
        if (facing != null) {
            seat.rotationYaw = facing.getHorizontalAngle();
        } else {
            seat.rotationYaw = player.rotationYaw;
        }

        if (world.spawnEntity(seat)) {
            player.startRiding(seat, true);
            return true;
        }
        return false;
    }

    @Override
    protected void entityInit() {
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound compound) {
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound compound) {
    }
}
