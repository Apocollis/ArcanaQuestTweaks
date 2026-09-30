package com.apocollis.aqtweaks.simpletomb;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.items.ItemHandlerHelper;

import javax.annotation.Nullable;
import java.text.SimpleDateFormat;
import java.util.*;

public final class CommandAqTomb extends CommandBase {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Override
    public String getName() {
        return "aqtomb";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/aqtomb <list|recover> <player> [backupIndex] [targetPlayer]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public List<String> getAliases() {
        return Collections.singletonList("aqtgrave");
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 2) {
            throw new WrongUsageException(getUsage(sender));
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        String playerName = args[1];

        EntityPlayerMP sourcePlayer = server.getPlayerList().getPlayerByUsername(playerName);
        UUID targetUuid = null;
        if (sourcePlayer != null) {
            targetUuid = sourcePlayer.getUniqueID();
        } else {
            com.mojang.authlib.GameProfile profile = server.getPlayerProfileCache().getGameProfileForUsername(playerName);
            if (profile != null) {
                targetUuid = profile.getId();
            }
        }

        if (targetUuid == null) {
            throw new CommandException("Player '" + playerName + "' not found.");
        }

        TombBackupSaveData saveData = TombBackupSaveData.get(server);
        List<TombBackupSaveData.DeathBackupRecord> backups = saveData.getBackups(targetUuid);

        if ("list".equals(sub)) {
            if (backups.isEmpty()) {
                sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "No death backups found for " + playerName + "."));
                return;
            }

            sender.sendMessage(new TextComponentString(TextFormatting.GOLD + "=== Death backups for " + playerName + " (" + backups.size() + ") ==="));
            for (int i = 0; i < backups.size(); i++) {
                TombBackupSaveData.DeathBackupRecord rec = backups.get(i);
                String dateStr = DATE_FORMAT.format(new Date(rec.timestamp));
                String posStr = String.format("Dim %d (%.0f, %.0f, %.0f)", rec.deathDim, rec.deathX, rec.deathY, rec.deathZ);
                String graveStr = rec.gravePlaced
                        ? (TextFormatting.GREEN + "Grave @ Dim " + rec.graveDim + " " + rec.gravePos)
                        : (TextFormatting.RED + "No Grave Placed");
                sender.sendMessage(new TextComponentString(TextFormatting.AQUA + "[" + i + "] " +
                        TextFormatting.WHITE + dateStr + " | " + posStr + " | " + graveStr +
                        TextFormatting.YELLOW + " (" + rec.entries.size() + " stacks)"));
            }
        } else if ("recover".equals(sub)) {
            if (backups.isEmpty()) {
                throw new CommandException("No death backups found for " + playerName + ".");
            }

            int backupIndex = 0;
            if (args.length >= 3) {
                backupIndex = parseInt(args[2], 0, backups.size() - 1);
            }

            EntityPlayerMP recipient = sourcePlayer;
            if (args.length >= 4) {
                recipient = getPlayer(server, sender, args[3]);
            }

            if (recipient == null) {
                throw new CommandException("Target recipient player is offline or not found.");
            }

            TombBackupSaveData.DeathBackupRecord rec = saveData.removeBackup(targetUuid, backupIndex);
            if (rec == null) {
                throw new CommandException("Failed to retrieve backup at index " + backupIndex + ".");
            }

            int restoredCount = 0;
            for (TombBackupSaveData.BackupEntry entry : rec.entries) {
                ItemStack stack = entry.stack.copy();
                if (stack.isEmpty()) {
                    continue;
                }
                if (entry.invType >= 0 && entry.targetSlot >= 0) {
                    TombSlotMaps.restoreItemToSlot(recipient, stack, entry.invType, entry.targetSlot);
                } else {
                    ItemHandlerHelper.giveItemToPlayer(recipient, stack);
                }
                restoredCount++;
            }

            recipient.inventoryContainer.detectAndSendChanges();

            sender.sendMessage(new TextComponentString(TextFormatting.GREEN + "Successfully restored " + restoredCount +
                    " items from backup [" + backupIndex + "] to " + recipient.getName() + "."));
            if (rec.gravePlaced) {
                sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "Note: The physical grave block at Dim " +
                        rec.graveDim + " " + rec.gravePos + " was NOT emptied or removed."));
            }

            if (recipient != sender) {
                recipient.sendMessage(new TextComponentString(TextFormatting.GREEN + "An operator restored " +
                        restoredCount + " items from your death backup."));
            }
        } else {
            throw new WrongUsageException(getUsage(sender));
        }
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "list", "recover");
        }
        if (args.length == 2 || args.length == 4) {
            return getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames());
        }
        if (args.length == 3 && "recover".equalsIgnoreCase(args[0])) {
            return getListOfStringsMatchingLastWord(args, "0", "1", "2");
        }
        return Collections.emptyList();
    }
}
