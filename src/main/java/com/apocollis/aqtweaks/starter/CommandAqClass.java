package com.apocollis.aqtweaks.starter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

/** OP {@code /aqclass reload|skip|reset|regrant|info}: run and recover the starter class menu. */
public final class CommandAqClass extends CommandBase {

    private static final List<String> SUBS = Arrays.asList("reload", "skip", "reset", "regrant", "info");

    @Override
    public String getName() {
        return "aqclass";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/aqclass <reload | skip <player> | reset <player> confirm | regrant <player> | info [player]>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos pos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, SUBS);
        }
        if (args.length == 2 && !"reload".equalsIgnoreCase(args[0])) {
            return getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames());
        }
        if (args.length == 3 && "reset".equalsIgnoreCase(args[0])) {
            return getListOfStringsMatchingLastWord(args, Collections.singletonList("confirm"));
        }
        return Collections.emptyList();
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 1) {
            throw new CommandException(getUsage(sender));
        }
        StarterClassRegistry.ensureLoaded();
        String sub = args[0].toLowerCase();
        switch (sub) {
            case "reload":
                reload(sender);
                return;
            case "info":
                info(server, sender, args);
                return;
            case "skip": {
                EntityPlayerMP target = target(server, sender, args);
                StarterModule.skip(target);
                send(sender, TextFormatting.GOLD, target.getName() + " is released and the pending flag is cleared.");
                return;
            }
            case "reset": {
                EntityPlayerMP target = target(server, sender, args);
                if (args.length < 3 || !"confirm".equalsIgnoreCase(args[2])) {
                    throw new CommandException("Add 'confirm': /aqclass reset " + target.getName() + " confirm");
                }
                if (StarterModule.reset(target)) {
                    send(sender, TextFormatting.GOLD, target.getName() + " is a new character again; the menu opens shortly.");
                } else {
                    send(sender, TextFormatting.RED, "Flags reset, but there is no valid class, so no menu. See /aqclass info.");
                }
                return;
            }
            case "regrant": {
                EntityPlayerMP target = target(server, sender, args);
                boolean granted = StarterReward.regrantIfMissing(target);
                send(sender, TextFormatting.GOLD, granted ? "Class perk granted again to " + target.getName() + "."
                        : "Nothing to grant for " + target.getName()
                                + " (no chosen class, no perk, or the perk is already unlocked).");
                return;
            }
            default:
                throw new CommandException(getUsage(sender));
        }
    }

    private static EntityPlayerMP target(MinecraftServer server, ICommandSender sender, String[] args)
            throws CommandException {
        if (args.length < 2) {
            throw new CommandException("Name a player.");
        }
        return getPlayer(server, sender, args[1]);
    }

    private static void reload(ICommandSender sender) {
        List<String> problems = StarterClassRegistry.reload();
        send(sender, TextFormatting.GOLD, "Starter classes reloaded: " + StarterClassRegistry.valid().size() + " valid.");
        for (String line : problems) {
            send(sender, TextFormatting.RED, line);
        }
        if (!StarterClassRegistry.hasValid()) {
            send(sender, TextFormatting.RED, "No valid class: the menu and the lockdown are off.");
        }
    }

    private static void info(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        StringBuilder ids = new StringBuilder();
        for (StarterClassDef d : StarterClassRegistry.valid()) {
            if (ids.length() > 0) ids.append(", ");
            ids.append(d.id);
        }
        send(sender, TextFormatting.GOLD, "Valid classes (" + StarterClassRegistry.valid().size() + "): " + ids);
        for (String line : StarterClassRegistry.errors()) {
            send(sender, TextFormatting.RED, line);
        }
        if (args.length >= 2) {
            EntityPlayerMP p = getPlayer(server, sender, args[1]);
            send(sender, TextFormatting.GRAY, p.getName() + ": pending=" + StarterState.isPending(p)
                    + " chosen=" + StarterState.isChosen(p) + " noKit=" + StarterState.isNoKit(p) + " class='" + StarterState.classId(p)
                    + "' csgReceived=" + StarterState.csgReceived(p)
                    + " choosing=" + StarterModule.isChoosing(p.getUniqueID())
                    + " locked=" + StarterLockdown.isLocked(p));
        }
    }

    private static void send(ICommandSender sender, TextFormatting color, String text) {
        TextComponentString message = new TextComponentString(text);
        message.getStyle().setColor(color);
        sender.sendMessage(message);
    }
}
