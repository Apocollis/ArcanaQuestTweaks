package com.apocollis.aqtweaks.roguelike;

import com.apocollis.aqtweaks.util.Reflect;

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
        net.minecraft.world.storage.MapStorage storage = Reflect.getMapStorage(world);
        if (storage == null) return new RoguelikeDungeonSavedData(DATA_NAME);
        RoguelikeDungeonSavedData instance = Reflect.getOrLoadData(storage, RoguelikeDungeonSavedData.class, DATA_NAME);
        if (instance == null) {
            instance = new RoguelikeDungeonSavedData(DATA_NAME);
            Reflect.setData(storage, DATA_NAME, instance);
        }
        return instance;
    }

    public void addDungeonBoxes(List<DungeonBoundingBox> newBoxes) {
        dungeons.addAll(newBoxes);
        Reflect.markDirty(this);
    }

    public boolean isInside(BlockPos pos) {
        return getDungeonLevel(pos) != -2;
    }

    public int getDungeonLevel(BlockPos pos) {
        int x = Reflect.getX(pos);
        int y = Reflect.getY(pos);
        int z = Reflect.getZ(pos);
        
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
        NBTTagList list = Reflect.getTagList(nbt, "Dungeons", Constants.NBT.TAG_COMPOUND);
        int count = Reflect.tagCount(list);
        for (int i = 0; i < count; i++) {
            NBTTagCompound tag = Reflect.getCompoundTagAt(list, i);
            int lvl = Reflect.hasKey(tag, "level") ? Reflect.getInteger(tag, "level") : 0;
            dungeons.add(new DungeonBoundingBox(
                Reflect.getInteger(tag, "minX"),
                Reflect.getInteger(tag, "minY"),
                Reflect.getInteger(tag, "minZ"),
                Reflect.getInteger(tag, "maxX"),
                Reflect.getInteger(tag, "maxY"),
                Reflect.getInteger(tag, "maxZ"),
                lvl
            ));
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        NBTTagList list = new NBTTagList();
        for (DungeonBoundingBox box : dungeons) {
            NBTTagCompound tag = new NBTTagCompound();
            Reflect.setInteger(tag, "minX", box.minX);
            Reflect.setInteger(tag, "minY", box.minY);
            Reflect.setInteger(tag, "minZ", box.minZ);
            Reflect.setInteger(tag, "maxX", box.maxX);
            Reflect.setInteger(tag, "maxY", box.maxY);
            Reflect.setInteger(tag, "maxZ", box.maxZ);
            Reflect.setInteger(tag, "level", box.level);
            Reflect.appendTag(list, tag);
        }
        Reflect.setTag(nbt, "Dungeons", list);
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
