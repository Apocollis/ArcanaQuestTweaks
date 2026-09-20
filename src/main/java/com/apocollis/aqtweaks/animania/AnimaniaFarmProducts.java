package com.apocollis.aqtweaks.animania;

import com.animania.addons.farm.common.block.BlockAnimaniaWool;
import com.animania.addons.farm.common.entity.goats.EntityAnimaniaGoat;
import com.animania.addons.farm.common.entity.sheep.EntityAnimaniaSheep;
import com.animania.addons.farm.common.entity.sheep.SheepType;
import com.animania.addons.farm.common.handler.FarmAddonBlockHandler;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * Farm-only extra products. Loaded via {@link Class#forName} from Reskillable.
 */
public final class AnimaniaFarmProducts {

    private AnimaniaFarmProducts() {}

    public static boolean isWoolAnimal(Entity entity) {
        return entity instanceof EntityAnimaniaSheep || entity instanceof EntityAnimaniaGoat;
    }

    public static boolean isSheared(Entity entity) {
        if (entity instanceof EntityAnimaniaSheep sheep) {
            return sheep.getSheared();
        }
        if (entity instanceof EntityAnimaniaGoat goat) {
            return goat.getSheared();
        }
        return false;
    }

    public static ItemStack woolStack(Entity entity) {
        Block wool = FarmAddonBlockHandler.blockAnimaniaWool;
        if (wool == null) return ItemStack.EMPTY;
        Item item = Item.getItemFromBlock(wool);
        if (item == null) return ItemStack.EMPTY;
        int meta = metaFor(entity);
        return new ItemStack(item, 1, meta);
    }

    private static int metaFor(Entity entity) {
        if (entity instanceof EntityAnimaniaSheep sheep) {
            return metaForSheep(sheep);
        }
        return BlockAnimaniaWool.EnumType.MERINO_WHITE.getMetadata();
    }

    private static int metaForSheep(EntityAnimaniaSheep sheep) {
        SheepType type = sheep.sheepType;
        EnumDyeColor dye = sheep.getDyeColor();
        boolean brown = dye != null && dye != EnumDyeColor.WHITE && dye != EnumDyeColor.SILVER
                && dye != EnumDyeColor.LIGHT_BLUE && dye != EnumDyeColor.PINK;
        if (type == SheepType.JACOB) return BlockAnimaniaWool.EnumType.JACOB.getMetadata();
        if (type == SheepType.DORSET) return BlockAnimaniaWool.EnumType.DORSET_BROWN.getMetadata();
        if (type == SheepType.SUFFOLK) return BlockAnimaniaWool.EnumType.SUFFOLK_BROWN.getMetadata();
        if (type == SheepType.FRIESIAN) {
            return brown ? BlockAnimaniaWool.EnumType.FRIESIAN_BROWN.getMetadata()
                    : BlockAnimaniaWool.EnumType.FRIESIAN_BLACK.getMetadata();
        }
        if (type == SheepType.MERINO) {
            return brown ? BlockAnimaniaWool.EnumType.MERINO_BROWN.getMetadata()
                    : BlockAnimaniaWool.EnumType.MERINO_WHITE.getMetadata();
        }
        return BlockAnimaniaWool.EnumType.MERINO_WHITE.getMetadata();
    }
}
