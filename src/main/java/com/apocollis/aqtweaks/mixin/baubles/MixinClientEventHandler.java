package com.apocollis.aqtweaks.mixin.baubles;

import com.apocollis.aqtweaks.client.BaublesMenuClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Marks a Baubles key press that already queued {@code PacketOpen}, so the tick
 * poll does not send a second one. Does not call {@code Keyboard.isKeyDown}.
 */
@Mixin(targets = "baubles.client.ClientEventHandler", remap = false)
public class MixinClientEventHandler {

    @Inject(
            method = "onKeyInput",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraftforge/fml/common/network/simpleimpl/SimpleNetworkWrapper;sendToServer(Lnet/minecraftforge/fml/common/network/simpleimpl/IMessage;)V"
            )
    )
    private static void aqtweaks$noteBaublesPacket(CallbackInfo ci) {
        BaublesMenuClient.noteHardwareOpen();
    }
}
