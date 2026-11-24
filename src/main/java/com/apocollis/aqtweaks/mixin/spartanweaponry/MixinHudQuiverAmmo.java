package com.apocollis.aqtweaks.mixin.spartanweaponry;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig.ClientModuleConfig;
import com.apocollis.aqtweaks.client.HotbarSideLayout;
import com.oblivioussp.spartanweaponry.client.gui.AlignmentHelper;
import com.oblivioussp.spartanweaponry.client.gui.HudQuiverAmmo;
import net.minecraft.client.gui.ScaledResolution;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * With {@code Offhand Slot Side = right} the quiver slot sits right of the hotbar, behind the offhand slot (which
 * only takes room while it holds an item), level with the hotbar. The quiver HUD's own alignment and offsets are
 * ignored then and left unchanged in its config.
 */
@Mixin(value = HudQuiverAmmo.class, remap = false)
public abstract class MixinHudQuiverAmmo {

    private static final String ALIGNED = "Lcom/oblivioussp/spartanweaponry/client/gui/HudQuiverAmmo;getAligned";
    private static final String ARGS =
            "(Lcom/oblivioussp/spartanweaponry/client/gui/AlignmentHelper$Alignment;ILnet/minecraft/client/gui/ScaledResolution;)I";

    @Redirect(method = "render", remap = false, require = 0,
            at = @At(value = "INVOKE", target = ALIGNED + "X" + ARGS, remap = false))
    private int aqtweaks$quiverX(HudQuiverAmmo self, AlignmentHelper.Alignment alignment, int offset,
            ScaledResolution res) {
        if (!HotbarSideLayout.rightSide()) {
            return self.getAlignedX(alignment, offset, res);
        }
        return res.getScaledWidth() / 2 + HotbarSideLayout.HOTBAR_HALF_WIDTH + HotbarSideLayout.offhandReserve();
    }

    @Redirect(method = "render", remap = false, require = 0,
            at = @At(value = "INVOKE", target = ALIGNED + "Y" + ARGS, remap = false))
    private int aqtweaks$quiverY(HudQuiverAmmo self, AlignmentHelper.Alignment alignment, int offset,
            ScaledResolution res) {
        if (!HotbarSideLayout.rightSide()) {
            return self.getAlignedY(alignment, offset, res);
        }
        return res.getScaledHeight() - ClientModuleConfig.hotbarSide.quiverBottomOffset;
    }
}
