package com.apocollis.aqtweaks.atum;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.teammetallurgy.atum.network.NetworkHandler;
import com.teammetallurgy.atum.network.packet.PacketStormStrength;
import com.teammetallurgy.atum.network.packet.PacketWeather;
import com.teammetallurgy.atum.utils.AtumConfig;
import com.teammetallurgy.atum.world.WorldProviderAtum;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.WorldServer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * {@code /atumweather <sandstorm|clear|status> [seconds] [skipWarning]}, run in place of Atum's own command (see
 * {@code MixinAtumWeatherCommand}) so a forced sandstorm warns like a natural one: the configured
 * {@code Storm Warning Seconds} of building dust first, then the storm. Atum's {@code storm} stays an alias.
 */
public final class AtumWeatherCommands {
    private AtumWeatherCommands() {}

    public static final String USAGE = "/atumweather <sandstorm|clear|status> [seconds] [skipWarning]";
    private static final List<String> WORDS = Arrays.asList("sandstorm", "clear", "status");

    public static void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 1) throw new CommandException(USAGE);
        String sub = args[0].toLowerCase(Locale.ROOT);
        if ("storm".equals(sub)) sub = "sandstorm";
        if (!WORDS.contains(sub)) throw new CommandException(USAGE);

        WorldServer world = server.getWorld(AtumConfig.DIMENSION_ID);
        if (world == null || !(world.provider instanceof WorldProviderAtum)) {
            throw new CommandException("The Atum dimension is not loaded.");
        }
        WorldProviderAtum provider = (WorldProviderAtum) world.provider;
        PendingStorm pending = (PendingStorm) provider;
        int dim = provider.getDimension();
        boolean looming = ArcanaQuestTweaksConfig.AtumModuleConfig.enableLoomingSandstorms;

        switch (sub) {
            case "status": {
                int warningTicks = warningTicks();
                String phase = provider.isStorming ? "storming"
                        : AtumWarningRamp.level(false, provider.stormTime, warningTicks) > 0.0F ? "warning" : "clear";
                reply(sender, TextFormatting.GOLD, String.format(Locale.ROOT,
                        "Atum weather: %s, storm strength %.2f, %d s until it %s", phase, provider.stormStrength,
                        provider.stormTime / 20, provider.isStorming ? "ends" : "changes"));
                return;
            }
            case "clear": {
                AtumWeatherCommandHelper.Parsed p = AtumWeatherCommandHelper.parse(args, 1, false);
                if (p.error != null) throw new CommandException(p.error);
                pending.aqtweaks$setPendingStormTicks(0);
                world.getWorldInfo().setCleanWeatherTime(0);
                provider.isStorming = false;
                provider.stormTime = AtumWeatherCommandHelper.clearTicks(p.seconds, looming,
                        ArcanaQuestTweaksConfig.AtumModuleConfig.stormIntervalMinSeconds,
                        ArcanaQuestTweaksConfig.AtumModuleConfig.stormIntervalMaxSeconds, world.rand::nextInt);
                NetworkHandler.WRAPPER.sendToDimension(new PacketWeather(false, provider.stormTime), dim);
                reply(sender, TextFormatting.AQUA, "Atum sandstorm is ending; clear for " + provider.stormTime / 20 + " s.");
                return;
            }
            default: {
                if (!AtumConfig.SANDSTORM_ENABLED) {
                    throw new CommandException("Sandstorms are disabled in Atum's own config, so nothing would happen.");
                }
                AtumWeatherCommandHelper.Parsed p = AtumWeatherCommandHelper.parse(args, 1, true);
                if (p.error != null) throw new CommandException(p.error);
                world.getWorldInfo().setCleanWeatherTime(0);
                int warningTicks = warningTicks();
                if (AtumWeatherCommandHelper.warns(p.skipWarning, warningTicks)) {
                    boolean snapped = provider.isStorming || provider.stormStrength > 0.05F;
                    provider.isStorming = false;
                    provider.stormTime = warningTicks;
                    provider.stormStrength = 0.0F;
                    provider.prevStormStrength = 0.0F;
                    pending.aqtweaks$setPendingStormTicks(AtumWeatherCommandHelper.pendingTicks(p.seconds));
                    NetworkHandler.WRAPPER.sendToDimension(new PacketWeather(false, warningTicks), dim);
                    NetworkHandler.WRAPPER.sendToDimension(new PacketStormStrength(0.0F), dim);
                    reply(sender, TextFormatting.AQUA, "Atum sandstorm in " + warningTicks / 20 + " s (the warning builds first)"
                            + (p.seconds > 0 ? ", lasting " + p.seconds + " s" : "") + "."
                            + (snapped ? " An active storm was cleared to show the warning." : ""));
                } else {
                    pending.aqtweaks$setPendingStormTicks(0);
                    int ticks = AtumWeatherCommandHelper.stormTicks(p.seconds, looming,
                            ArcanaQuestTweaksConfig.AtumModuleConfig.stormDurationMinSeconds,
                            ArcanaQuestTweaksConfig.AtumModuleConfig.stormDurationMaxSeconds, world.rand::nextInt);
                    provider.isStorming = true;
                    provider.stormTime = ticks;
                    NetworkHandler.WRAPPER.sendToDimension(new PacketWeather(true, ticks), dim);
                    reply(sender, TextFormatting.AQUA, "Atum sandstorm started (no warning), lasting " + ticks / 20 + " s.");
                }
            }
        }
    }

    private static int warningTicks() {
        ArcanaQuestTweaksConfig.AtumFog fog = ArcanaQuestTweaksConfig.ClientModuleConfig.atumFog;
        return fog.enableStormWarning ? fog.stormWarningSeconds * 20 : 0;
    }

    public static List<String> tabCompletions(String[] args) {
        if (args.length == 1) return filter(WORDS, args[0]);
        if (args.length >= 2 && args.length <= 3) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if ("sandstorm".equals(sub) || "storm".equals(sub)) return filter(Collections.singletonList("skipWarning"), args[args.length - 1]);
        }
        return Collections.emptyList();
    }

    private static List<String> filter(List<String> options, String prefix) {
        List<String> out = new ArrayList<>();
        for (String o : options) {
            if (o.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT))) out.add(o);
        }
        return out;
    }

    private static void reply(ICommandSender sender, TextFormatting color, String text) {
        TextComponentString message = new TextComponentString(text);
        message.getStyle().setColor(color);
        sender.sendMessage(message);
    }
}
