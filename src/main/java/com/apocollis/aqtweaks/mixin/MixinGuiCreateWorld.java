package com.apocollis.aqtweaks.mixin;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.world.WorldType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiCreateWorld.class)
public abstract class MixinGuiCreateWorld {

    @Shadow
    private int selectedIndex;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void aqtweaks$setDefaultWorldType(CallbackInfo ci) {
        String defaultType = ArcanaQuestTweaksConfig.ClientModuleConfig.getDefaultWorldType();
        if (defaultType == null || defaultType.trim().isEmpty()) {
            return;
        }
        String target = defaultType.trim();
        for (int i = 0; i < WorldType.WORLD_TYPES.length; i++) {
            WorldType wt = WorldType.WORLD_TYPES[i];
            if (wt != null && wt.canBeCreated() && target.equalsIgnoreCase(wt.getName())) {
                this.selectedIndex = i;
                break;
            }
        }
    }
}
