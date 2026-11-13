package com.apocollis.aqtweaks.hazard;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.storage.WorldSavedData;

import java.util.HashMap;
import java.util.Map;

/** Per-dimension saved hazard state: for each channel the event index, the last end tick and the event. */
public class HazardData extends WorldSavedData {
    public static final String ID = "aqtweaks_hazards";

    private final Map<Integer, NBTTagCompound> channels = new HashMap<>();

    public HazardData() {
        super(ID);
    }

    public HazardData(String name) {
        super(name);
    }

    public void load(int channel, HazardTimeline timeline) {
        NBTTagCompound tag = channels.get(channel);
        if (tag == null) return;
        HazardEvent event = null;
        if (tag.hasKey("event", 10)) {
            NBTTagCompound e = tag.getCompoundTag("event");
            event = new HazardEvent(channel, e.getLong("id"), e.getLong("warn"), e.getLong("start"),
                    e.getLong("end"), e.getInteger("up"), e.getInteger("down"), e.getLong("seed"),
                    e.getFloat("intensity"), e.getInteger("flags"));
        }
        timeline.restore(tag.getLong("index"), tag.getLong("lastEnd"), event);
    }

    public void store(int channel, HazardTimeline timeline) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setLong("index", timeline.index());
        tag.setLong("lastEnd", timeline.lastEnd());
        HazardEvent event = timeline.event();
        if (event != null) {
            NBTTagCompound e = new NBTTagCompound();
            e.setLong("id", event.id);
            e.setLong("warn", event.warningStart);
            e.setLong("start", event.start);
            e.setLong("end", event.end);
            e.setInteger("up", event.rampUp);
            e.setInteger("down", event.rampDown);
            e.setLong("seed", event.seed);
            e.setFloat("intensity", event.intensity);
            e.setInteger("flags", event.flags);
            tag.setTag("event", e);
        }
        channels.put(channel, tag);
        markDirty();
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        channels.clear();
        NBTTagList list = nbt.getTagList("channels", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound tag = list.getCompoundTagAt(i);
            channels.put(tag.getInteger("channel"), tag);
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        NBTTagList list = new NBTTagList();
        for (Map.Entry<Integer, NBTTagCompound> entry : channels.entrySet()) {
            NBTTagCompound tag = entry.getValue().copy();
            tag.setInteger("channel", entry.getKey());
            list.appendTag(tag);
        }
        compound.setTag("channels", list);
        return compound;
    }

    public static HazardData get(World world) {
        HazardData data = (HazardData) world.getPerWorldStorage().getOrLoadData(HazardData.class, ID);
        if (data == null) {
            data = new HazardData();
            world.getPerWorldStorage().setData(ID, data);
        }
        return data;
    }
}
