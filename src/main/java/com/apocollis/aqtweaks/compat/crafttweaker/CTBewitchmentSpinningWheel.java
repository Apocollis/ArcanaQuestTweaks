package com.apocollis.aqtweaks.compat.crafttweaker;

import com.bewitchment.api.registry.SpinningWheelRecipe;
import crafttweaker.CraftTweakerAPI;
import crafttweaker.IAction;
import crafttweaker.annotations.ModOnly;
import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.item.IIngredient;
import crafttweaker.api.item.IItemStack;
import crafttweaker.api.minecraft.CraftTweakerMC;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryModifiable;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@ZenRegister
@ModOnly("bewitchment")
@ZenClass("mods.bewitchment.SpinningWheel")
public final class CTBewitchmentSpinningWheel {

    private CTBewitchmentSpinningWheel() {
    }

    @ZenMethod
    public static void addRecipe(String name, IItemStack output, IIngredient[] inputs) {
        if (name == null || name.trim().isEmpty()) {
            CraftTweakerAPI.logError("Spinning Wheel addRecipe: name must not be empty.");
            return;
        }
        if (output == null) {
            CraftTweakerAPI.logError("Spinning Wheel addRecipe: output must not be null.");
            return;
        }
        if (inputs == null || inputs.length < 1 || inputs.length > 4) {
            CraftTweakerAPI.logError("Spinning Wheel addRecipe: inputs must be 1 to 4 ingredients.");
            return;
        }

        List<Ingredient> inputList = new ArrayList<>(inputs.length);
        for (IIngredient input : inputs) {
            if (input == null) {
                CraftTweakerAPI.logError("Spinning Wheel addRecipe: inputs must not contain null.");
                return;
            }
            inputList.add(CraftTweakerMC.getIngredient(input));
        }

        ResourceLocation id = parseName(name);
        ItemStack outStack = CraftTweakerMC.getItemStack(output);
        if (outStack.isEmpty()) {
            CraftTweakerAPI.logError("Spinning Wheel addRecipe: output must not be empty.");
            return;
        }
        List<ItemStack> outputList = Collections.singletonList(outStack.copy());
        CraftTweakerAPI.apply(new AddAction(id, inputList, outputList));
    }

    @ZenMethod
    public static void removeRecipe(IItemStack output) {
        if (output == null) {
            CraftTweakerAPI.logError("Spinning Wheel removeRecipe: output must not be null.");
            return;
        }
        ItemStack match = CraftTweakerMC.getItemStack(output);
        if (match.isEmpty()) {
            CraftTweakerAPI.logError("Spinning Wheel removeRecipe: output must not be empty.");
            return;
        }
        CraftTweakerAPI.apply(new RemoveByOutputAction(match.copy()));
    }

    @ZenMethod
    public static void removeRecipe(String name) {
        if (name == null || name.trim().isEmpty()) {
            CraftTweakerAPI.logError("Spinning Wheel removeRecipe: name must not be empty.");
            return;
        }
        CraftTweakerAPI.apply(new RemoveByNameAction(parseName(name)));
    }

    static ResourceLocation parseName(String name) {
        String trimmed = name.trim();
        if (trimmed.indexOf(':') >= 0) {
            return new ResourceLocation(trimmed);
        }
        return new ResourceLocation("crafttweaker", trimmed);
    }

    static IForgeRegistry<SpinningWheelRecipe> registry() {
        return GameRegistry.findRegistry(SpinningWheelRecipe.class);
    }

    static void removeId(IForgeRegistry<SpinningWheelRecipe> registry, ResourceLocation id) {
        if (!(registry instanceof IForgeRegistryModifiable)) {
            CraftTweakerAPI.logError("Spinning Wheel: registry is not modifiable; cannot remove " + id);
            return;
        }
        try {
            ((IForgeRegistryModifiable<SpinningWheelRecipe>) registry).remove(id);
        } catch (IllegalStateException e) {
            CraftTweakerAPI.logError("Spinning Wheel: failed to remove " + id + " (registry may not allowModification).", e);
        }
    }

    static boolean outputMatches(SpinningWheelRecipe recipe, ItemStack match) {
        if (recipe.output == null || recipe.output.isEmpty()) {
            return false;
        }
        ItemStack first = recipe.output.get(0);
        return ItemStack.areItemsEqual(first, match) && ItemStack.areItemStackTagsEqual(first, match);
    }

    private static final class AddAction implements IAction {
        private final ResourceLocation id;
        private final List<Ingredient> inputs;
        private final List<ItemStack> outputs;

        private AddAction(ResourceLocation id, List<Ingredient> inputs, List<ItemStack> outputs) {
            this.id = id;
            this.inputs = inputs;
            this.outputs = outputs;
        }

        @Override
        public void apply() {
            IForgeRegistry<SpinningWheelRecipe> registry = registry();
            if (registry == null) {
                CraftTweakerAPI.logError("Spinning Wheel: registry not found; cannot add " + id);
                return;
            }
            registry.register(new SpinningWheelRecipe(id, inputs, outputs));
        }

        @Override
        public String describe() {
            return "Adding Bewitchment Spinning Wheel recipe " + id;
        }
    }

    private static final class RemoveByOutputAction implements IAction {
        private final ItemStack match;

        private RemoveByOutputAction(ItemStack match) {
            this.match = match;
        }

        @Override
        public void apply() {
            IForgeRegistry<SpinningWheelRecipe> registry = registry();
            if (registry == null) {
                CraftTweakerAPI.logError("Spinning Wheel: registry not found; cannot remove by output.");
                return;
            }
            List<ResourceLocation> toRemove = new ArrayList<>();
            for (SpinningWheelRecipe recipe : registry) {
                if (outputMatches(recipe, match) && recipe.getRegistryName() != null) {
                    toRemove.add(recipe.getRegistryName());
                }
            }
            if (toRemove.isEmpty()) {
                CraftTweakerAPI.logError("Spinning Wheel: no recipe produces " + match);
                return;
            }
            for (ResourceLocation id : toRemove) {
                removeId(registry, id);
            }
        }

        @Override
        public String describe() {
            return "Removing Bewitchment Spinning Wheel recipes that output " + match;
        }
    }

    private static final class RemoveByNameAction implements IAction {
        private final ResourceLocation id;

        private RemoveByNameAction(ResourceLocation id) {
            this.id = id;
        }

        @Override
        public void apply() {
            IForgeRegistry<SpinningWheelRecipe> registry = registry();
            if (registry == null) {
                CraftTweakerAPI.logError("Spinning Wheel: registry not found; cannot remove " + id);
                return;
            }
            if (registry.getValue(id) == null) {
                CraftTweakerAPI.logError("Spinning Wheel: no recipe named " + id);
                return;
            }
            removeId(registry, id);
        }

        @Override
        public String describe() {
            return "Removing Bewitchment Spinning Wheel recipe " + id;
        }
    }
}
