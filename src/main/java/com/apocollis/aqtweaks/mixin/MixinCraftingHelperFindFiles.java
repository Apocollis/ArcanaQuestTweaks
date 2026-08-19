package com.apocollis.aqtweaks.mixin;

import com.apocollis.aqtweaks.recipe.RecipeJsonSkip;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.fml.common.ModContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.nio.file.Path;
import java.util.function.BiFunction;
import java.util.function.Function;

@Mixin(value = CraftingHelper.class, remap = false)
public class MixinCraftingHelperFindFiles {

    @ModifyVariable(
            method = "findFiles(Lnet/minecraftforge/fml/common/ModContainer;Ljava/lang/String;Ljava/util/function/Function;Ljava/util/function/BiFunction;ZZ)Z",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static BiFunction aqtweaks$skipDeadRecipeJson(
            BiFunction processor,
            ModContainer mod,
            String base,
            Function preprocessor,
            BiFunction processorAgain,
            boolean defaultUnfoundRoot,
            boolean visitAllFiles) {
        if (processor == null || base == null || !base.contains("/recipes")) {
            return processor;
        }
        return (root, file) -> {
            if (file instanceof Path && RecipeJsonSkip.shouldSkip((Path) file)) {
                return Boolean.TRUE;
            }
            return processor.apply(root, file);
        };
    }
}
