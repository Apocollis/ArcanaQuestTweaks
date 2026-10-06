package com.apocollis.aqtweaks.mixin.sereneseasons;

import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import sereneseasons.api.season.Season;
import sereneseasons.command.SSCommand;

/**
 * Serene Seasons' {@code /ss} ({@code /sereneseasons}) only completes its first argument and returns
 * {@code null} for every later position. {@code MinecraftServer.getTabCompletions} calls {@code isEmpty()} on
 * the result, so {@code /ss setseason <TAB>} threw a NullPointerException (soft crash, "Error executing task"
 * on the server thread). For {@code setseason <name>} offer the sub-season names the command accepts (the
 * lowercase {@code Season.SubSeason} names, filtered by what is typed); any other position gets an empty list.
 */
@Mixin(value = SSCommand.class, remap = false)
public abstract class MixinSSCommand {

    @Inject(method = "func_184883_a", at = @At("RETURN"), cancellable = true)
    private void aqtweaks$completeSeasons(MinecraftServer server, ICommandSender sender, String[] args,
            @Nullable BlockPos pos, CallbackInfoReturnable<List<String>> cir) {
        if (cir.getReturnValue() != null) {
            return;
        }
        if (args.length == 2 && "setseason".equals(args[0])) {
            Season.SubSeason[] values = Season.SubSeason.VALUES;
            String[] names = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                names[i] = values[i].toString().toLowerCase(java.util.Locale.ROOT);
            }
            cir.setReturnValue(CommandBase.getListOfStringsMatchingLastWord(args, names));
        } else {
            cir.setReturnValue(Collections.emptyList());
        }
    }
}
