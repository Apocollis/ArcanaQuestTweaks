package com.apocollis.aqtweaks.reskillable;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.bewitchment.common.item.poppet.ItemPoppet;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.registries.IForgeRegistryEntry;

import java.lang.reflect.Field;

public class RecipeStitchPoppet extends IForgeRegistryEntry.Impl<IRecipe> implements IRecipe {

    private static final Field EVENT_HANDLER = ReflectionHelper.findField(InventoryCrafting.class, "eventHandler",
            "field_70465_c");

    @Override
    public boolean matches(InventoryCrafting inv, World world) {
        return find(inv) != null;
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inv) {
        ItemStack poppet = find(inv);
        if (poppet == null) {
            return ItemStack.EMPTY;
        }
        ItemStack out = poppet.copy();
        out.setCount(1);
        out.setItemDamage(0);
        if (poppet.hasTagCompound()) {
            NBTTagCompound tag = poppet.getTagCompound().copy();
            out.setTagCompound(tag);
        }
        return out;
    }

    @Override
    public boolean canFit(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public ItemStack getRecipeOutput() {
        return ItemStack.EMPTY;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(InventoryCrafting inv) {
        return ForgeHooks.defaultRecipeGetRemainingItems(inv);
    }

    @Override
    public boolean isDynamic() {
        return true;
    }

    private static ItemStack find(InventoryCrafting inv) {
        Item stitching = Item.getByNameOrId("bewitchment:witches_stitching");
        if (stitching == null) {
            return null;
        }
        ItemStack poppet = ItemStack.EMPTY;
        int stitchCount = 0;
        int extra = 0;
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.getItem() instanceof ItemPoppet) {
                if (!poppet.isEmpty()) {
                    return null;
                }
                poppet = stack;
            } else if (stack.getItem() == stitching) {
                stitchCount += stack.getCount();
            } else {
                extra++;
            }
        }
        if (poppet.isEmpty() || stitchCount != 1 || extra != 0) {
            return null;
        }
        EntityPlayer player = playerFrom(inv);
        if (player != null) {
            if (!MagicSchoolPresence.unlocked(player, "aqtweaks:stitch",
                    ArcanaQuestTweaksConfig.ReskillableModuleConfig.perks.stitch.enable)) {
                return null;
            }
        }
        return poppet;
    }

    private static EntityPlayer playerFrom(InventoryCrafting inv) {
        Container container = containerOf(inv);
        if (container == null) {
            return null;
        }
        for (Slot slot : container.inventorySlots) {
            if (slot != null && slot.inventory instanceof InventoryPlayer inventoryPlayer) {
                return inventoryPlayer.player;
            }
        }
        return null;
    }

    private static Container containerOf(InventoryCrafting inv) {
        try {
            return (Container) EVENT_HANDLER.get(inv);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }
}
