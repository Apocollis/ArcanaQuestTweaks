package com.apocollis.aqtweaks.comfort;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

/**
 * OP {@code /aqcomfort}: print the current Homestead scan and apply it immediately.
 */
public final class CommandAqComfort extends CommandBase {

    @Override
    public String getName() {
        return "aqcomfort";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/aqcomfort";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length != 0) {
            throw new CommandException(getUsage(sender));
        }
        if (!(sender instanceof EntityPlayerMP player)) {
            throw new CommandException("Only a player can run /aqcomfort");
        }
        World world = player.world;
        if (world == null || world.isRemote) {
            throw new CommandException("No server world");
        }

        ComfortSystemHandler.ComfortEval eval = ComfortSystemHandler.evaluate(player);
        ComfortSystemHandler.applyEvaluation(player, eval);
        ComfortSystemHandler.ComfortEval after = ComfortSystemHandler.evaluate(player);

        send(player, TextFormatting.GOLD, "Comfort scan");
        send(player, TextFormatting.GRAY, String.format(
                "resting=%s pose=%s furniture=%s scoreBand=%d granted=%d",
                yn(after.currentlyResting),
                yn(after.restPose),
                yn(after.furniture),
                after.scoreBand,
                after.grantedBand));
        send(player, TextFormatting.GRAY, String.format(
                "hurtRemain=%d attackRemain=%d promoteIn=%s",
                after.hurtRemain,
                after.attackRemain,
                after.promoteInTicks < 0L ? "-" : Long.toString(after.promoteInTicks)));
        send(player, TextFormatting.GRAY, String.format(
                "cozy=%.1f bonus=%.1f penalty=%.1f effective=%.1f",
                after.cozy, after.bonuses, after.penalties, after.effective));

        List<String> categories = new ArrayList<>(after.cappedByCategory.keySet());
        Collections.sort(categories);
        if (categories.isEmpty()) {
            send(player, TextFormatting.DARK_GRAY, "cozy by category: (none)");
        } else {
            StringBuilder byCategory = new StringBuilder("cozy by category:");
            for (String category : categories) {
                Float amount = after.cappedByCategory.get(category);
                byCategory.append(' ').append(category).append('=').append(fmt(amount != null ? amount : 0.0f));
            }
            send(player, TextFormatting.GRAY, byCategory.toString());
        }

        send(player, TextFormatting.GRAY, "bonuses: " + formatModifiers(after.activeBonuses));
        send(player, TextFormatting.GRAY, String.format(
                "penalties: temp=%.1f thirst=%.1f hunger=%.1f health=%.1f effects=%s",
                after.tempPenalty,
                after.thirstPenalty,
                after.hungerPenalty,
                after.healthPenalty,
                formatModifiers(after.activePenaltyEffects)));
    }

    private static String formatModifiers(List<ComfortSettings.PotionModifier> modifiers) {
        if (modifiers == null || modifiers.isEmpty()) return "(none)";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < modifiers.size(); i++) {
            ComfortSettings.PotionModifier m = modifiers.get(i);
            if (i > 0) sb.append(' ');
            sb.append(m.potion).append('=').append(fmt(m.amount));
        }
        return sb.toString();
    }

    private static String fmt(float value) {
        return String.format("%.1f", value);
    }

    private static String yn(boolean value) {
        return value ? "yes" : "no";
    }

    private static void send(EntityPlayerMP player, TextFormatting color, String text) {
        TextComponentString message = new TextComponentString(text);
        message.getStyle().setColor(color);
        player.sendMessage(message);
    }
}
