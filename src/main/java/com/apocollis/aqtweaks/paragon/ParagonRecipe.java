package com.apocollis.aqtweaks.paragon;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.oredict.OreIngredient;
import net.minecraftforge.oredict.ShapedOreRecipe;

/**
 * Four metal blocks around one of the nine Tartarite items, the shape of Metallurgy's own Paragon recipes. Extending the
 * shaped ore recipe keeps the ingredients and output visible to JEI.
 */
public class ParagonRecipe extends ShapedOreRecipe {
    private final ParagonTrait trait;
    private final TartariteKind kind;

    public ParagonRecipe(ResourceLocation group, ParagonTrait trait, TartariteKind kind, ItemStack tartarite) {
        super(group, preview(trait, tartarite), primer(trait, tartarite));
        this.trait = trait;
        this.kind = kind;
    }

    private static CraftingHelper.ShapedPrimer primer(ParagonTrait trait, ItemStack tartarite) {
        CraftingHelper.ShapedPrimer primer = new CraftingHelper.ShapedPrimer();
        primer.width = 3;
        primer.height = 3;
        primer.mirrored = false;
        primer.input = NonNullList.withSize(9, Ingredient.EMPTY);
        Ingredient block = new OreIngredient(trait.getOreName());
        primer.input.set(1, block);
        primer.input.set(3, block);
        primer.input.set(5, block);
        primer.input.set(7, block);
        ItemStack any = tartarite.copy();
        any.setItemDamage(net.minecraftforge.oredict.OreDictionary.WILDCARD_VALUE);
        primer.input.set(4, Ingredient.fromStacks(any));
        return primer;
    }

    /** What JEI shows: the item carrying the trait. */
    private static ItemStack preview(ParagonTrait trait, ItemStack tartarite) {
        ItemStack out = tartarite.copy();
        out.setItemDamage(0);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString(ParagonRegistry.NBT_KEY, trait.getId());
        out.setTagCompound(tag);
        return out;
    }

    @Override
    public boolean matches(InventoryCrafting inv, World world) {
        if (!super.matches(inv, world)) return false;
        if (!ParagonRegistry.isEnabled(trait)) return false;
        ItemStack item = inv.getStackInSlot(4);
        ParagonRules.Absorb outcome = ParagonRules.absorb(ParagonRegistry.storedId(item),
                ParagonRegistry.hasMetallurgyParagon(item), trait.getId(), trait.appliesTo(kind));
        return outcome == ParagonRules.Absorb.ACCEPT || outcome == ParagonRules.Absorb.REPLACE;
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inv) {
        ItemStack result = inv.getStackInSlot(4).copy();
        result.setCount(1);
        String old = ParagonRegistry.storedId(result);
        if (old != null) {
            ParagonTrait previous = ParagonRegistry.get(old);
            if (previous != null) previous.onRemoved(result);
        }
        NBTTagCompound tag = result.hasTagCompound() ? result.getTagCompound() : new NBTTagCompound();
        tag.setString(ParagonRegistry.NBT_KEY, trait.getId());
        result.setTagCompound(tag);
        trait.onAbsorbed(result, kind);
        return result;
    }

    @Override
    public boolean isDynamic() {
        return false;
    }
}
