package com.apocollis.aqtweaks.mixin.comfort;

import java.util.List;
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GuiScreen.class)
public interface InvokerGuiScreen {
    @Invoker("drawHoveringText")
    void aqtweaks$drawHoveringText(List<String> textLines, int x, int y);
}
