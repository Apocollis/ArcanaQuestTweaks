package com.apocollis.aqtweaks.gamestages;

import com.blamejared.recipestages.handlers.Recipes;
import crafttweaker.api.item.IIngredient;
import crafttweaker.api.item.IItemStack;
import crafttweaker.api.minecraft.CraftTweakerMC;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.oredict.OreDictionary;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Captures Recipe Stages {@code setRecipeStage(stage, ingredient)} pairs.
 * Call only after {@code recipestages} (and CraftTweaker) are loaded.
 */
public final class GameStagesRecipeIndex {

    private record StagedIngredient(String stage, IIngredient ingredient) {
    }

    private static final List<StagedIngredient> STAGED = new CopyOnWriteArrayList<>();
    private static final ConcurrentHashMap<String, List<String>> MATCH_CACHE = new ConcurrentHashMap<>();

    private GameStagesRecipeIndex() {
    }

    public static void add(String stage, IIngredient ingredient) {
        if (stage == null || stage.isEmpty() || ingredient == null) {
            return;
        }
        STAGED.add(new StagedIngredient(stage, ingredient));
        MATCH_CACHE.clear();
    }

    public static List<String> matchingStages(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return List.of();
        }
        return MATCH_CACHE.computeIfAbsent(cacheKey(stack), k -> computeMatchingStages(stack));
    }

    private static List<String> computeMatchingStages(ItemStack stack) {
        LinkedHashSet<String> stages = new LinkedHashSet<>();
        IItemStack ctStack = CraftTweakerMC.getIItemStack(stack);
        if (ctStack != null) {
            for (StagedIngredient entry : STAGED) {
                if (entry.ingredient().matches(ctStack)) {
                    stages.add(entry.stage());
                }
            }
        }
        addStagesFromRecipes(stack, stages);
        return List.copyOf(stages);
    }

    private static String cacheKey(ItemStack stack) {
        ResourceLocation id = stack.getItem().getRegistryName();
        String name = id == null ? stack.getItem().getClass().getName() : id.toString();
        return name + "#" + stack.getMetadata();
    }

    private static void addStagesFromRecipes(ItemStack stack, Set<String> stages) {
        var recipesByStage = Recipes.recipes;
        if (recipesByStage == null || recipesByStage.isEmpty()) {
            return;
        }
        for (var stageEntry : recipesByStage.entrySet()) {
            String stage = stageEntry.getKey();
            if (stages.contains(stage)) {
                continue;
            }
            List<IRecipe> recipes = stageEntry.getValue();
            if (recipes == null) {
                continue;
            }
            for (IRecipe recipe : recipes) {
                if (recipe == null) {
                    continue;
                }
                ItemStack output = recipe.getRecipeOutput();
                if (output == null || output.isEmpty()) {
                    continue;
                }
                if (OreDictionary.itemMatches(output, stack, false) || ItemStack.areItemsEqual(output, stack)) {
                    stages.add(stage);
                    break;
                }
            }
        }
    }
}
