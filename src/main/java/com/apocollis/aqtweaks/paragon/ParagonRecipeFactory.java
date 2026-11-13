package com.apocollis.aqtweaks.paragon;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.crafting.IRecipeFactory;
import net.minecraftforge.common.crafting.JsonContext;

/**
 * {@code aqtweaks:paragon_shaped}. A recipe file names the trait and the Tartarite item:
 * {@code {"type": "aqtweaks:paragon_shaped", "trait": "fiery", "item": "metallurgy:tartarite_sword"}}.
 */
public class ParagonRecipeFactory implements IRecipeFactory {
    @Override
    public IRecipe parse(JsonContext context, JsonObject json) {
        String traitId = JsonUtils.getString(json, "trait");
        String itemName = JsonUtils.getString(json, "item");
        ParagonTrait trait = ParagonRegistry.get(traitId);
        if (trait == null) {
            throw new JsonSyntaxException("Unknown or unavailable Paragon trait '" + traitId
                    + "'; the recipe needs a mod_loaded condition for the metal's mod");
        }
        Item item = Item.REGISTRY.getObject(new ResourceLocation(itemName));
        if (item == null) throw new JsonSyntaxException("Unknown item '" + itemName + "'");
        TartariteKind kind = ParagonRegistry.kindOf(new ItemStack(item));
        if (kind == null) throw new JsonSyntaxException("'" + itemName + "' is not one of the nine Tartarite items");
        if (!trait.appliesTo(kind)) {
            throw new JsonSyntaxException("Trait '" + traitId + "' does not apply to " + itemName);
        }
        ResourceLocation group = new ResourceLocation("aqtweaks", "paragon_" + traitId);
        return new ParagonRecipe(group, trait, kind, new ItemStack(item));
    }
}
