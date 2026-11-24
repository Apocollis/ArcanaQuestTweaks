package com.apocollis.aqtweaks.thaumcraft.battlemage;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The owner mages that have died (plan 21.3), so an escort that was not loaded at that moment is still released the
 * next time the despawn check reaches it. One small global file ({@code data/aqtweaks_released_escort_owners.dat});
 * entries older than {@link EscortRelease#MAX_AGE_TICKS} are dropped.
 */
public final class EscortReleasedData extends WorldSavedData {
    static final String NAME = "aqtweaks_released_escort_owners";

    private final Map<UUID, Long> released = new HashMap<>();

    public EscortReleasedData() {
        super(NAME);
    }

    public EscortReleasedData(String name) {
        super(name);
    }

    /** @return the shared data for this world's storage, or null when the world has no map storage */
    public static EscortReleasedData get(World world) {
        MapStorage storage = world.getMapStorage();
        if (storage == null) {
            return null;
        }
        EscortReleasedData data = (EscortReleasedData) storage.getOrLoadData(EscortReleasedData.class, NAME);
        if (data == null) {
            data = new EscortReleasedData();
            storage.setData(NAME, data);
        }
        return data;
    }

    /** Live view for {@link EscortRelease#verdict}; do not modify. */
    public Map<UUID, Long> released() {
        return released;
    }

    public void release(UUID owner, long now) {
        released.put(owner, now);
        EscortRelease.trim(released, now, EscortRelease.MAX_AGE_TICKS);
        markDirty();
    }

    public void trim(long now) {
        if (EscortRelease.trim(released, now, EscortRelease.MAX_AGE_TICKS) > 0) {
            markDirty();
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        released.clear();
        NBTTagList list = nbt.getTagList("owners", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound tag = list.getCompoundTagAt(i);
            released.put(new UUID(tag.getLong("Most"), tag.getLong("Least")), tag.getLong("at"));
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        NBTTagList list = new NBTTagList();
        for (Map.Entry<UUID, Long> e : released.entrySet()) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setLong("Most", e.getKey().getMostSignificantBits());
            tag.setLong("Least", e.getKey().getLeastSignificantBits());
            tag.setLong("at", e.getValue());
            list.appendTag(tag);
        }
        nbt.setTag("owners", list);
        return nbt;
    }
}
