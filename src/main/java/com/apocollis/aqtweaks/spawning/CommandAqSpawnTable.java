package com.apocollis.aqtweaks.spawning;

import com.apocollis.aqtweaks.spawning.table.LenientJson;
import com.apocollis.aqtweaks.spawning.table.PotentialSpawnVerifier;
import com.apocollis.aqtweaks.spawning.table.SpawnRuleSpec;
import com.apocollis.aqtweaks.spawning.table.SpawnTableBuilder;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import mcjty.incontrol.rules.RulesManager;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * {@code /aqspawntable}: state of the potential spawn table; {@code reload} reads the pack files again;
 * {@code verify} compares the table with In Control's {@code potentialspawn.json} for every dimension, biome and
 * creature type and writes the full report to {@code config/arcanaquesttweaks/potentialspawn_verify.txt};
 * {@code dump} writes the table as a {@code potentialspawn.json} to {@code potentialspawn_from_table.json} in the same
 * folder, to diff against the generated file.
 */
public final class CommandAqSpawnTable extends CommandBase {
    private static final Logger LOGGER = LogManager.getLogger("AQTweaks-Spawning");
    private static final int CHAT_LINES = 6;

    @Override
    public String getName() {
        return "aqspawntable";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/aqspawntable | /aqspawntable reload | /aqspawntable verify | /aqspawntable dump";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        String sub = args.length == 0 ? "info" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "info":
                say(sender, PotentialSpawnTable.describe() + "; In Control has "
                        + RulesManager.potentialSpawnRules.size() + " potential-spawn rules loaded.");
                break;
            case "reload":
                say(sender, "Potential spawn table: " + PotentialSpawnTable.reload());
                break;
            case "verify":
                verify(sender);
                break;
            case "dump":
                dump(sender);
                break;
            default:
                throw new CommandException(getUsage(sender));
        }
    }

    private static void verify(ICommandSender sender) {
        List<SpawnRuleSpec> table = PotentialSpawnTable.specs();
        File config = PotentialSpawnTable.configDirectory();
        if (table == null || config == null) {
            say(sender, "The table is not built; see the log for why. Try /aqspawntable reload.");
            return;
        }
        File rulesFile = new File(config, "incontrol/potentialspawn.json");
        if (!rulesFile.isFile()) {
            say(sender, "No " + rulesFile.getAbsolutePath() + " to compare with.");
            return;
        }
        PotentialSpawnVerifier.Report report;
        try {
            JsonElement parsed = LenientJson.parse(new String(Files.readAllBytes(rulesFile.toPath()),
                    StandardCharsets.UTF_8));
            if (!parsed.isJsonArray()) {
                say(sender, rulesFile.getName() + " is not a JSON array.");
                return;
            }
            List<String> biomes = new ArrayList<String>();
            for (ResourceLocation id : ForgeRegistries.BIOMES.getKeys()) {
                biomes.add(id.toString());
            }
            List<String> types = new ArrayList<String>();
            for (EnumCreatureType type : EnumCreatureType.values()) {
                types.add(type.name());
            }
            report = PotentialSpawnVerifier.compare(table, parsed.getAsJsonArray(), biomes, types);
        } catch (IOException | RuntimeException failure) {
            LOGGER.warn("[AQTweaks] Potential spawn table verify failed.", failure);
            say(sender, "Verify failed: " + failure);
            return;
        }

        List<String> lines = new ArrayList<String>();
        lines.add("Table: " + report.tableRules + " rows. potentialspawn.json: " + report.fileRules
                + " rules (In Control has " + RulesManager.potentialSpawnRules.size() + " loaded). "
                + report.queriesChecked + " queries checked.");
        for (String field : report.unsupportedFields) {
            lines.add("The file uses a field the table does not implement: " + field);
        }
        lines.addAll(report.differences);
        lines.add(report.identical() ? "IDENTICAL: every query gets the same entries."
                : "DIFFERENT: the table would not spawn what potentialspawn.json spawns.");
        File out = new File(config, "arcanaquesttweaks/potentialspawn_verify.txt");
        try {
            Files.createDirectories(out.getParentFile().toPath());
            Files.write(out.toPath(), lines, StandardCharsets.UTF_8);
        } catch (IOException failure) {
            LOGGER.warn("[AQTweaks] Could not write {}.", out.getAbsolutePath(), failure);
        }
        for (String line : lines) {
            LOGGER.info("[AQTweaks] spawn table verify: {}", line);
        }
        int shown = Math.min(lines.size() - 1, CHAT_LINES);
        for (int i = 0; i < shown; i++) {
            say(sender, lines.get(i));
        }
        if (lines.size() - 1 > shown) {
            say(sender, "(" + (lines.size() - 1 - shown) + " more lines in " + out.getAbsolutePath() + ")");
        }
        say(sender, lines.get(lines.size() - 1));
    }

    private static void dump(ICommandSender sender) {
        List<SpawnRuleSpec> table = PotentialSpawnTable.specs();
        File config = PotentialSpawnTable.configDirectory();
        if (table == null || config == null) {
            say(sender, "The table is not built; see the log for why. Try /aqspawntable reload.");
            return;
        }
        File out = new File(config, "arcanaquesttweaks/potentialspawn_from_table.json");
        try {
            Files.createDirectories(out.getParentFile().toPath());
            String json = new GsonBuilder().setPrettyPrinting().create().toJson(SpawnTableBuilder.toJson(table));
            Files.write(out.toPath(), json.getBytes(StandardCharsets.UTF_8));
            say(sender, "Wrote " + table.size() + " rows to " + out.getAbsolutePath());
        } catch (IOException failure) {
            LOGGER.warn("[AQTweaks] Could not write {}.", out.getAbsolutePath(), failure);
            say(sender, "Could not write " + out.getAbsolutePath() + ": " + failure.getMessage());
        }
    }

    private static void say(ICommandSender sender, String text) {
        sender.sendMessage(new TextComponentString(text));
    }
}
