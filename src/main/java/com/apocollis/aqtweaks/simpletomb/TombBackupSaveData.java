package com.apocollis.aqtweaks.simpletomb;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.util.Constants;

import java.util.*;

public class TombBackupSaveData extends WorldSavedData {

    public static final String DATA_NAME = "aqtweaks_death_backups";
    private static final int MAX_BACKUPS_PER_PLAYER = 3;

    private final Map<UUID, List<DeathBackupRecord>> playerBackups = new HashMap<>();

    public TombBackupSaveData(String name) {
        super(name);
    }

    public TombBackupSaveData() {
        super(DATA_NAME);
    }

    public static final class BackupEntry {
        public final byte invType;
        public final int targetSlot;
        public final ItemStack stack;

        public BackupEntry(byte invType, int targetSlot, ItemStack stack) {
            this.invType = invType;
            this.targetSlot = targetSlot;
            this.stack = stack;
        }

        public NBTTagCompound writeToNBT() {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setByte("InvType", invType);
            tag.setInteger("TargetSlot", targetSlot);
            tag.setTag("Item", stack.serializeNBT());
            return tag;
        }

        public static BackupEntry readFromNBT(NBTTagCompound tag) {
            byte invType = tag.getByte("InvType");
            int targetSlot = tag.getInteger("TargetSlot");
            ItemStack stack = new ItemStack(tag.getCompoundTag("Item"));
            return new BackupEntry(invType, targetSlot, stack);
        }
    }

    public static final class DeathBackupRecord {
        public final long timestamp;
        public final int deathDim;
        public final double deathX;
        public final double deathY;
        public final double deathZ;
        public final boolean gravePlaced;
        public final int graveDim;
        public final BlockPos gravePos;
        public final List<BackupEntry> entries;

        public DeathBackupRecord(long timestamp, int deathDim, double deathX, double deathY, double deathZ,
                                 boolean gravePlaced, int graveDim, BlockPos gravePos, List<BackupEntry> entries) {
            this.timestamp = timestamp;
            this.deathDim = deathDim;
            this.deathX = deathX;
            this.deathY = deathY;
            this.deathZ = deathZ;
            this.gravePlaced = gravePlaced;
            this.graveDim = graveDim;
            this.gravePos = gravePos;
            this.entries = entries;
        }

        public NBTTagCompound writeToNBT() {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setLong("Time", timestamp);
            tag.setInteger("DeathDim", deathDim);
            tag.setDouble("DeathX", deathX);
            tag.setDouble("DeathY", deathY);
            tag.setDouble("DeathZ", deathZ);
            tag.setBoolean("GravePlaced", gravePlaced);
            if (gravePlaced && gravePos != null) {
                tag.setInteger("GraveDim", graveDim);
                tag.setInteger("GraveX", gravePos.getX());
                tag.setInteger("GraveY", gravePos.getY());
                tag.setInteger("GraveZ", gravePos.getZ());
            }
            NBTTagList entryList = new NBTTagList();
            for (BackupEntry entry : entries) {
                entryList.appendTag(entry.writeToNBT());
            }
            tag.setTag("Entries", entryList);
            return tag;
        }

        public static DeathBackupRecord readFromNBT(NBTTagCompound tag) {
            long timestamp = tag.getLong("Time");
            int deathDim = tag.getInteger("DeathDim");
            double deathX = tag.getDouble("DeathX");
            double deathY = tag.getDouble("DeathY");
            double deathZ = tag.getDouble("DeathZ");
            boolean gravePlaced = tag.getBoolean("GravePlaced");
            int graveDim = tag.getInteger("GraveDim");
            BlockPos gravePos = null;
            if (gravePlaced && tag.hasKey("GraveX")) {
                gravePos = new BlockPos(tag.getInteger("GraveX"), tag.getInteger("GraveY"), tag.getInteger("GraveZ"));
            }
            List<BackupEntry> entries = new ArrayList<>();
            NBTTagList entryList = tag.getTagList("Entries", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < entryList.tagCount(); i++) {
                entries.add(BackupEntry.readFromNBT(entryList.getCompoundTagAt(i)));
            }
            return new DeathBackupRecord(timestamp, deathDim, deathX, deathY, deathZ, gravePlaced, graveDim, gravePos, entries);
        }
    }

    public static TombBackupSaveData get(MinecraftServer server) {
        WorldServer overworld = server.getWorld(0);
        MapStorage storage = overworld.getMapStorage();
        TombBackupSaveData data = (TombBackupSaveData) storage.getOrLoadData(TombBackupSaveData.class, DATA_NAME);
        if (data == null) {
            data = new TombBackupSaveData();
            storage.setData(DATA_NAME, data);
        }
        return data;
    }

    public void addBackup(UUID playerUuid, DeathBackupRecord record) {
        List<DeathBackupRecord> list = playerBackups.computeIfAbsent(playerUuid, k -> new ArrayList<>());
        list.add(0, record); // newest at index 0
        while (list.size() > MAX_BACKUPS_PER_PLAYER) {
            list.remove(list.size() - 1);
        }
        markDirty();
    }

    public List<DeathBackupRecord> getBackups(UUID playerUuid) {
        List<DeathBackupRecord> list = playerBackups.get(playerUuid);
        return list != null ? Collections.unmodifiableList(list) : Collections.emptyList();
    }

    public DeathBackupRecord removeBackup(UUID playerUuid, int index) {
        List<DeathBackupRecord> list = playerBackups.get(playerUuid);
        if (list != null && index >= 0 && index < list.size()) {
            DeathBackupRecord rec = list.remove(index);
            markDirty();
            return rec;
        }
        return null;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        playerBackups.clear();
        NBTTagList playerList = nbt.getTagList("Players", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < playerList.tagCount(); i++) {
            NBTTagCompound playerTag = playerList.getCompoundTagAt(i);
            UUID uuid = UUID.fromString(playerTag.getString("UUID"));
            NBTTagList recordsList = playerTag.getTagList("Records", Constants.NBT.TAG_COMPOUND);
            List<DeathBackupRecord> records = new ArrayList<>();
            for (int j = 0; j < recordsList.tagCount(); j++) {
                records.add(DeathBackupRecord.readFromNBT(recordsList.getCompoundTagAt(j)));
            }
            playerBackups.put(uuid, records);
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        NBTTagList playerList = new NBTTagList();
        for (Map.Entry<UUID, List<DeathBackupRecord>> entry : playerBackups.entrySet()) {
            NBTTagCompound playerTag = new NBTTagCompound();
            playerTag.setString("UUID", entry.getKey().toString());
            NBTTagList recordsList = new NBTTagList();
            for (DeathBackupRecord record : entry.getValue()) {
                recordsList.appendTag(record.writeToNBT());
            }
            playerTag.setTag("Records", recordsList);
            playerList.appendTag(playerTag);
        }
        compound.setTag("Players", playerList);
        return compound;
    }
}
