package com.apocollis.aqtweaks;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.util.Constants;
import java.util.ArrayList;
import java.util.List;

public class RoguelikeDungeonSavedData extends WorldSavedData {

    private static final String DATA_NAME = "aqtweaks_roguelike_dungeons";
    private final List<DungeonBoundingBox> dungeons = new ArrayList<>();

    public RoguelikeDungeonSavedData(String name) {
        super(name);
    }

    public static RoguelikeDungeonSavedData get(World world) {
        RoguelikeDungeonSavedData instance = (RoguelikeDungeonSavedData) world.getMapStorage()
                .getOrLoadData(RoguelikeDungeonSavedData.class, DATA_NAME);
        if (instance == null) {
            instance = new RoguelikeDungeonSavedData(DATA_NAME);
            world.getMapStorage().setData(DATA_NAME, instance);
        }
        return instance;
    }

    public void addDungeonBoxes(List<DungeonBoundingBox> newBoxes) {
        dungeons.addAll(newBoxes);
        markDirty();
    }

    public boolean isInside(BlockPos pos) {
        return getDungeonLevel(pos) != -2;
    }

    public int getDungeonLevel(BlockPos pos) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        
        // 1. Check specific floor levels first (level >= 0) for precise room/corridor match
        for (DungeonBoundingBox box : dungeons) {
            if (box.level >= 0) {
                if (x >= box.minX && x <= box.maxX &&
                    y >= box.minY && y <= box.maxY &&
                    z >= box.minZ && z <= box.maxZ) {
                    return box.level;
                }
            }
        }

        // 2. Check tower entrance box (level == -1) for staircase/tower shaft
        for (DungeonBoundingBox box : dungeons) {
            if (box.level == -1) {
                if (x >= box.minX && x <= box.maxX &&
                    y >= box.minY && y <= box.maxY &&
                    z >= box.minZ && z <= box.maxZ) {
                    return box.level;
                }
            }
        }

        return -2; // Not inside
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        dungeons.clear();
        NBTTagList list = nbt.getTagList("Dungeons", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound tag = list.getCompoundTagAt(i);
            int lvl = tag.hasKey("level") ? tag.getInteger("level") : 0;
            dungeons.add(new DungeonBoundingBox(
                tag.getInteger("minX"),
                tag.getInteger("minY"),
                tag.getInteger("minZ"),
                tag.getInteger("maxX"),
                tag.getInteger("maxY"),
                tag.getInteger("maxZ"),
                lvl
            ));
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        NBTTagList list = new NBTTagList();
        for (DungeonBoundingBox box : dungeons) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setInteger("minX", box.minX);
            tag.setInteger("minY", box.minY);
            tag.setInteger("minZ", box.minZ);
            tag.setInteger("maxX", box.maxX);
            tag.setInteger("maxY", box.maxY);
            tag.setInteger("maxZ", box.maxZ);
            tag.setInteger("level", box.level);
            list.appendTag(tag);
        }
        nbt.setTag("Dungeons", list);
        return nbt;
    }

    public static class DungeonBoundingBox {
        public final int minX, minY, minZ, maxX, maxY, maxZ;
        public final int level; // -1 for tower, 0 to 4 for floors 1 to 5

        public DungeonBoundingBox(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, int level) {
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
            this.level = level;
        }
    }
}
