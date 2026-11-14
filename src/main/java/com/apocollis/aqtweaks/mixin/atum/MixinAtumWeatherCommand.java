package com.apocollis.aqtweaks.mixin.atum;

import com.apocollis.aqtweaks.atum.AtumWeatherCommands;
import com.teammetallurgy.atum.commands.AtumWeather;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Runs {@code /atumweather} through {@link AtumWeatherCommands} so a forced sandstorm gets its warning like a natural
 * one. The logic lives outside the mixin (a mixin class cannot be called from other code). Both the development and
 * the obfuscated method names are listed.
 */
@Mixin(value = AtumWeather.class, remap = false)
public abstract class MixinAtumWeatherCommand {

    @Inject(method = {"execute", "func_184881_a"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void aqtweaks$execute(MinecraftServer server, ICommandSender sender, String[] args, CallbackInfo ci)
            throws CommandException {
        AtumWeatherCommands.execute(server, sender, args);
        ci.cancel();
    }

    @Inject(method = {"getTabCompletions", "func_184883_a"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void aqtweaks$tabs(MinecraftServer server, ICommandSender sender, String[] args, BlockPos targetPos,
                               CallbackInfoReturnable<List<String>> cir) {
        cir.setReturnValue(AtumWeatherCommands.tabCompletions(args));
    }

    @Inject(method = {"getUsage", "func_71518_a"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void aqtweaks$usage(ICommandSender sender, CallbackInfoReturnable<String> cir) {
        cir.setReturnValue(AtumWeatherCommands.USAGE);
    }
}
