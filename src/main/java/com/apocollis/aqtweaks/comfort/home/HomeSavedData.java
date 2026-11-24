package com.apocollis.aqtweaks.comfort.home;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;

/**
 * Persists only the bed anchors: owner UUID, last-known name, dimension, bed position and the hearthkeeper flag.
 * Regions are rebuilt lazily on first occupancy, so the save stays tiny and heals itself.
 * One global file ({@code data/aqtweaks_homes.dat}) holds every dimension.
 */
public final class HomeSavedData extends WorldSavedData {
    static final String NAME = "aqtweaks_homes";

    final List<HomeRecord> loaded = new ArrayList<>();

    public HomeSavedData() {
        super(NAME);
    }

    public HomeSavedData(String name) {
        super(name);
    }

    static HomeSavedData get(World world) {
        MapStorage storage = world.getMapStorage();
        if (storage == null) {
            return null;
        }
        HomeSavedData data = (HomeSavedData) storage.getOrLoadData(HomeSavedData.class, NAME);
        if (data == null) {
            data = new HomeSavedData();
            storage.setData(NAME, data);
        }
        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        loaded.clear();
        NBTTagList list = nbt.getTagList("homes", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound tag = list.getCompoundTagAt(i);
            if (!tag.hasKey("OwnerMost")) {
                continue;
            }
            UUID owner = new UUID(tag.getLong("OwnerMost"), tag.getLong("OwnerLeast"));
            BlockPos bed = new BlockPos(tag.getInteger("x"), tag.getInteger("y"), tag.getInteger("z"));
            loaded.add(new HomeRecord(owner, tag.getString("name"), tag.getInteger("dim"), bed,
                    tag.getBoolean("hearthkeeper")));
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        NBTTagList list = new NBTTagList();
        for (HomeRecord r : HomeRegistry.allRecords()) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setLong("OwnerMost", r.owner.getMostSignificantBits());
            tag.setLong("OwnerLeast", r.owner.getLeastSignificantBits());
            tag.setString("name", r.ownerName);
            tag.setInteger("dim", r.dimension);
            tag.setInteger("x", r.bed.getX());
            tag.setInteger("y", r.bed.getY());
            tag.setInteger("z", r.bed.getZ());
            tag.setBoolean("hearthkeeper", r.hearthkeeper);
            list.appendTag(tag);
        }
        nbt.setTag("homes", list);
        return nbt;
    }
}
