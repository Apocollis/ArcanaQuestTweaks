package com.apocollis.aqtweaks.mixin.comfort;

import net.minecraft.client.gui.inventory.GuiContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Client-only access to the inventory origin. */
@Mixin(GuiContainer.class)
public interface AccessorGuiContainer {
    @Accessor("guiLeft")
    int aqtweaks$getGuiLeft();

    @Accessor("guiTop")
    int aqtweaks$getGuiTop();
}
