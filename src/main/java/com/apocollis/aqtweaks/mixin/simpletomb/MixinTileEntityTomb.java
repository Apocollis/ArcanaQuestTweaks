package com.apocollis.aqtweaks.mixin.simpletomb;

import com.apocollis.aqtweaks.simpletomb.SimpleTombModule;
import com.apocollis.aqtweaks.simpletomb.TombSlotMaps;
import com.lothrazar.simpletomb.block.TileEntityTomb;
import com.lothrazar.simpletomb.data.MessageType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = TileEntityTomb.class, remap = false)
public abstract class MixinTileEntityTomb extends TileEntity {

    @Shadow
    @Final
    protected ItemStackHandler inventory;

    @Invoker("removeGraveBy")
    public abstract void callRemoveGraveBy(EntityPlayer player);

    @Unique
    private List<TombSlotMaps.SlotMapping> aqtweaks$slotMap = new ArrayList<>();

    @Inject(method = "initTombstoneOwner", at = @At("RETURN"))
    private void aqtweaks$onInitTombstoneOwner(EntityPlayer player, CallbackInfo ci) {
        SimpleTombModule.onTombCreated((TileEntityTomb) (Object) this, player);
    }

    @Inject(method = "func_189515_b", at = @At("RETURN"))
    private void aqtweaks$writeNBT(NBTTagCompound compound, CallbackInfoReturnable<NBTTagCompound> cir) {
        if (!aqtweaks$slotMap.isEmpty()) {
            compound.setTag(TombSlotMaps.NBT_SLOT_MAP, TombSlotMaps.writeSlotMap(aqtweaks$slotMap));
        }
    }

    @Inject(method = "func_145839_a", at = @At("RETURN"))
    private void aqtweaks$readNBT(NBTTagCompound compound, CallbackInfo ci) {
        if (compound.hasKey(TombSlotMaps.NBT_SLOT_MAP, Constants.NBT.TAG_LIST)) {
            aqtweaks$slotMap = TombSlotMaps.readSlotMap(compound.getTagList(TombSlotMaps.NBT_SLOT_MAP, Constants.NBT.TAG_COMPOUND));
        } else {
            aqtweaks$slotMap.clear();
        }
    }

    @Inject(method = "func_189517_E_", at = @At("RETURN"))
    private void aqtweaks$stripUpdateTag(CallbackInfoReturnable<NBTTagCompound> cir) {
        NBTTagCompound tag = cir.getReturnValue();
        if (tag != null && tag.hasKey(TombSlotMaps.NBT_SLOT_MAP)) {
            tag.removeTag(TombSlotMaps.NBT_SLOT_MAP);
        }
    }

    @Inject(method = "giveInventory", at = @At("HEAD"), cancellable = true)
    private void aqtweaks$customGiveInventory(EntityPlayer player, CallbackInfo ci) {
        if (this.world.isRemote || player instanceof FakePlayer) {
            return;
        }

        if (aqtweaks$slotMap == null || aqtweaks$slotMap.isEmpty()) {
            // Missing map: fallback to stock Simple Tomb auto-equip and dump
            return;
        }

        // Pass 1: restore mapped slots
        for (TombSlotMaps.SlotMapping mapping : aqtweaks$slotMap) {
            if (mapping.tombSlot >= 0 && mapping.tombSlot < inventory.getSlots()) {
                ItemStack stack = inventory.getStackInSlot(mapping.tombSlot);
                if (!stack.isEmpty()) {
                    inventory.setStackInSlot(mapping.tombSlot, ItemStack.EMPTY);
                    TombSlotMaps.restoreItemToSlot(player, stack, mapping.invType, mapping.targetSlot);
                }
            }
        }

        // Pass 2: unmapped items (e.g. ground pickups vacuumed into tomb)
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                inventory.setStackInSlot(i, ItemStack.EMPTY);
                ItemHandlerHelper.giveItemToPlayer(player, stack);
            }
        }

        callRemoveGraveBy(player);
        player.inventoryContainer.detectAndSendChanges();
        MessageType.MESSAGE_OPEN_GRAVE_SUCCESS.sendSpecialMessage(player, new Object[0]);
        ci.cancel();
    }
}
