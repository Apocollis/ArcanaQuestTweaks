package com.apocollis.aqtweaks.simpletomb;

import com.lothrazar.simpletomb.ConfigTomb;
import com.lothrazar.simpletomb.block.TileEntityTomb;
import com.lothrazar.simpletomb.helper.EntityHelper;
import com.lothrazar.simpletomb.helper.WorldHelper;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerDropsEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.*;

public final class SimpleTombModule {

    private static final ThreadLocal<DeathContext> CURRENT_DEATH = new ThreadLocal<>();

    private static final class DeathContext {
        final EntityPlayerMP player;
        final long timestamp;
        final int deathDim;
        final double deathX;
        final double deathY;
        final double deathZ;
        final List<TombSlotMaps.SlotMapping> slotMappings = new ArrayList<>();
        final List<TombBackupSaveData.BackupEntry> backupEntries = new ArrayList<>();
        TileEntityTomb createdTomb = null;

        DeathContext(EntityPlayerMP player) {
            this.player = player;
            this.timestamp = System.currentTimeMillis();
            this.deathDim = player.dimension;
            this.deathX = player.posX;
            this.deathY = player.posY;
            this.deathZ = player.posZ;
        }
    }

    public static void recordCapturedStack(int tombSlot, byte type, int targetSlot, ItemStack stripped) {
        DeathContext ctx = CURRENT_DEATH.get();
        if (ctx != null) {
            ctx.slotMappings.add(new TombSlotMaps.SlotMapping(tombSlot, type, targetSlot));
            ctx.backupEntries.add(new TombBackupSaveData.BackupEntry(type, targetSlot, stripped.copy()));
        }
    }

    public static void recordUnmappedStack(ItemStack stripped) {
        DeathContext ctx = CURRENT_DEATH.get();
        if (ctx != null) {
            ctx.backupEntries.add(new TombBackupSaveData.BackupEntry((byte) -1, -1, stripped.copy()));
        }
    }

    public static void onTombCreated(TileEntityTomb tomb, EntityPlayer player) {
        DeathContext ctx = CURRENT_DEATH.get();
        if (ctx != null && ctx.player == player) {
            ctx.createdTomb = tomb;
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onLivingDeath(LivingDeathEvent event) {
        if (event.isCanceled() || !(event.getEntityLiving() instanceof EntityPlayerMP)) {
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) event.getEntityLiving();
        if (player instanceof FakePlayer || player.world.isRemote) {
            return;
        }
        if (!ConfigTomb.INSTANCE.handlePlayerDeath || WorldHelper.isRuleKeepInventory(player)) {
            return;
        }

        DeathContext ctx = new DeathContext(player);
        CURRENT_DEATH.set(ctx);

        // 1. Vanilla Main Inventory (0-35)
        for (int i = 0; i < player.inventory.mainInventory.size(); i++) {
            ItemStack stack = player.inventory.mainInventory.get(i);
            if (!stack.isEmpty()) {
                TombSlotMaps.stampStack(stack, TombSlotMaps.TYPE_MAIN, i);
            }
        }

        // 2. Vanilla Armor (0-3)
        for (int i = 0; i < player.inventory.armorInventory.size(); i++) {
            ItemStack stack = player.inventory.armorInventory.get(i);
            if (!stack.isEmpty()) {
                TombSlotMaps.stampStack(stack, TombSlotMaps.TYPE_ARMOR, i);
            }
        }

        // 3. Vanilla Offhand (0)
        for (int i = 0; i < player.inventory.offHandInventory.size(); i++) {
            ItemStack stack = player.inventory.offHandInventory.get(i);
            if (!stack.isEmpty()) {
                TombSlotMaps.stampStack(stack, TombSlotMaps.TYPE_OFFHAND, i);
            }
        }

        // 4. Baubles (if loaded)
        if (Loader.isModLoaded("baubles")) {
            TombBaubleSlots.stampBaubles(player);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPlayerDrops(PlayerDropsEvent event) {
        DeathContext ctx = CURRENT_DEATH.get();
        try {
            if (ctx == null || ctx.player != event.getEntityPlayer()) {
                return;
            }

            boolean gravePlaced = (ctx.createdTomb != null);
            int graveDim = gravePlaced ? ctx.createdTomb.getWorld().provider.getDimension() : 0;
            BlockPos gravePos = gravePlaced ? ctx.createdTomb.getPos() : null;

            if (gravePlaced) {
                // If a grave was placed, sweep any remaining tagged drops (e.g. from Baubles if BaublesEX ran after Simple Tomb)
                ListIterator<EntityItem> it = event.getDrops().listIterator();
                while (it.hasNext()) {
                    EntityItem entityItem = it.next();
                    ItemStack stack = entityItem.getItem();
                    if (TombSlotMaps.hasGraveSlot(stack)) {
                        NBTTagCompound slotTag = TombSlotMaps.getGraveSlotTag(stack);
                        byte type = slotTag.getByte("type");
                        int targetSlot = slotTag.getInteger("slot");

                        ItemStack stripped = stack.copy();
                        TombSlotMaps.stripGraveSlotTag(stripped);

                        net.minecraftforge.items.IItemHandler inv = ctx.createdTomb.getCapability(
                                net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
                        if (inv != null) {
                            int emptySlot = TombSlotMaps.findFirstEmptySlot(inv);
                            if (emptySlot != -1) {
                                inv.insertItem(emptySlot, stripped, false);
                                ctx.slotMappings.add(new TombSlotMaps.SlotMapping(emptySlot, type, targetSlot));
                                ctx.backupEntries.add(new TombBackupSaveData.BackupEntry(type, targetSlot, stripped.copy()));
                                entityItem.setItem(ItemStack.EMPTY);
                                it.remove();
                            }
                        }
                    }
                }

                // Write slot map to tomb TileEntity
                NBTTagCompound tileNbt = new NBTTagCompound();
                ctx.createdTomb.writeToNBT(tileNbt);
                tileNbt.setTag(TombSlotMaps.NBT_SLOT_MAP, TombSlotMaps.writeSlotMap(ctx.slotMappings));
                ctx.createdTomb.readFromNBT(tileNbt);
                ctx.createdTomb.markDirty();
            } else {
                // No grave placed: copy still-tagged drops into backup, strip tags from remaining drops so they don't persist on ground
                for (EntityItem entityItem : event.getDrops()) {
                    ItemStack stack = entityItem.getItem();
                    if (TombSlotMaps.hasGraveSlot(stack)) {
                        NBTTagCompound slotTag = TombSlotMaps.getGraveSlotTag(stack);
                        byte type = slotTag.getByte("type");
                        int targetSlot = slotTag.getInteger("slot");

                        ItemStack copy = stack.copy();
                        TombSlotMaps.stripGraveSlotTag(copy);
                        ctx.backupEntries.add(new TombBackupSaveData.BackupEntry(type, targetSlot, copy));
                        TombSlotMaps.stripGraveSlotTag(stack);
                    }
                }
            }

            // Clean transient tag from baubles that remained equipped
            if (Loader.isModLoaded("baubles")) {
                TombBaubleSlots.cleanUndroppedBaubles(ctx.player);
            }

            // Save record to WorldSavedData (always Overworld storage)
            if (!ctx.backupEntries.isEmpty()) {
                TombBackupSaveData saveData = TombBackupSaveData.get(ctx.player.server);
                TombBackupSaveData.DeathBackupRecord record = new TombBackupSaveData.DeathBackupRecord(
                        ctx.timestamp, ctx.deathDim, ctx.deathX, ctx.deathY, ctx.deathZ,
                        gravePlaced, graveDim, gravePos, ctx.backupEntries
                );
                saveData.addBackup(ctx.player.getUniqueID(), record);
            }
        } finally {
            CURRENT_DEATH.remove();
        }
    }
}
