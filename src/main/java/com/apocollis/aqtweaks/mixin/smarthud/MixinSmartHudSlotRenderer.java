package com.apocollis.aqtweaks.mixin.smarthud;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig.ClientModuleConfig;
import com.apocollis.aqtweaks.client.HotbarSideLayout;
import com.apocollis.aqtweaks.client.SmartHudRows;
import dev.sapphic.smarthud.config.SlotsConfig;
import dev.sapphic.smarthud.render.SlotRenderer;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * With {@code Offhand Slot Side = right}, Smart HUD's item slots sit on the right whatever the main hand, and move
 * out by the room the offhand slot and the quiver are using (nothing when neither is on screen). Its own
 * attack indicator, drawn beyond the slots, moves with them. Smart HUD's config is untouched.
 * Targets use SRG names ({@code func_184591_cq} is {@code getPrimaryHand}) in Smart HUD's own bytecode.
 */
@Mixin(value = SlotRenderer.class, remap = false)
public abstract class MixinSmartHudSlotRenderer {

    private static final String PRE = "draw(Lnet/minecraftforge/client/event/RenderGameOverlayEvent$Pre;)V";
    private static final String POST = "draw(Lnet/minecraftforge/client/event/RenderGameOverlayEvent$Post;)V";

    @Redirect(method = PRE, remap = false, require = 0,
            at = @At(value = "INVOKE", remap = false,
                    target = "Lnet/minecraft/client/entity/EntityPlayerSP;func_184591_cq()Lnet/minecraft/util/EnumHandSide;"))
    private static EnumHandSide aqtweaks$slotsRight(net.minecraft.client.entity.EntityPlayerSP player) {
        return HotbarSideLayout.rightSide() ? EnumHandSide.RIGHT : player.getPrimaryHand();
    }

    @Redirect(method = POST, remap = false, require = 0,
            at = @At(value = "INVOKE", remap = false,
                    target = "Lnet/minecraft/entity/player/EntityPlayer;func_184591_cq()Lnet/minecraft/util/EnumHandSide;"))
    private static EnumHandSide aqtweaks$indicatorRight(net.minecraft.entity.player.EntityPlayer player) {
        return HotbarSideLayout.rightSide() ? EnumHandSide.RIGHT : player.getPrimaryHand();
    }

    @Shadow
    @Final
    private static ResourceLocation SLOTS_TEXTURE_SHEET;

    /** Too little room right of the shield and quiver for every slot: draw a second row (see SmartHudRows). */
    @Inject(method = PRE, remap = false, require = 0, at = @At("HEAD"), cancellable = true)
    private static void aqtweaks$wrapRows(RenderGameOverlayEvent.Pre event, CallbackInfo ci) {
        if (HotbarSideLayout.rightSide() && ClientModuleConfig.hotbarSide.smartHudWrap && SlotsConfig.enabled
                && event.getType() == RenderGameOverlayEvent.ElementType.HOTBAR
                && SmartHudRows.tryDraw(event.getResolution(), SLOTS_TEXTURE_SHEET)) {
            ci.cancel();
        }
    }

    /** Local 7 is the screen centre; every slot, count and indicator x is derived from it. */
    @ModifyVariable(method = PRE, remap = false, require = 0, at = @At("STORE"), index = 7)
    private static int aqtweaks$shiftSlots(int centre) {
        return centre + HotbarSideLayout.smartHudShift();
    }

    @ModifyVariable(method = POST, remap = false, require = 0, at = @At("STORE"), index = 7)
    private static int aqtweaks$shiftIndicator(int centre) {
        return centre + HotbarSideLayout.smartHudShift();
    }
}
