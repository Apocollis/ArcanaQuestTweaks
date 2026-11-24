package com.apocollis.aqtweaks.thaumcraft.battlemage;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import thaumcraft.api.casters.FocusEngine;
import thaumcraft.api.casters.FocusPackage;
import thaumcraft.common.items.casters.ItemFocus;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads {@code config/arcanaquesttweaks/cultist_battlemages.json} (a default copy is written on first run), parses it
 * with {@link LibraryParser} and resolves every castable focus into a {@link BattlemageFocus}. A focus whose item or
 * effect keys are not registered is logged once and dropped; the mob keeps working without it.
 */
public final class FocusLibrary {

    private static final Logger LOG = LogManager.getLogger("AQTweaks-Battlemage");
    private static final String RESOURCE = "/assets/aqtweaks/battlemage/cultist_battlemages.json";

    private static LibraryData data;
    private static final Map<String, BattlemageFocus> RESOLVED = new HashMap<>();
    private static boolean loaded;

    private FocusLibrary() {}

    public static synchronized LibraryData data() {
        ensureLoaded();
        return data;
    }

    /** The resolved focus for an id, or null if it was dropped or is AI-performed. */
    public static synchronized BattlemageFocus resolved(String id) {
        ensureLoaded();
        return RESOLVED.get(id);
    }

    public static synchronized void reload() {
        loaded = false;
        ensureLoaded();
    }

    private static void ensureLoaded() {
        if (loaded) {
            return;
        }
        loaded = true;
        RESOLVED.clear();
        String json = readConfigOrDefault();
        data = LibraryParser.parse(json);
        for (String p : data.problems) {
            LOG.warn("[battlemage library] {}", p);
        }
        for (FocusDef def : data.foci.values()) {
            if (def.performed) {
                RESOLVED.put(def.id, new BattlemageFocus(def, null, null, 0xFFFFFF));
                continue;
            }
            BattlemageFocus f = resolve(def);
            if (f != null) {
                RESOLVED.put(def.id, f);
            }
        }
        for (VariantDef v : data.variants.values()) {
            if (v.fixedLoadout == null) {
                continue;
            }
            for (String id : v.fixedLoadout) {
                if (!RESOLVED.containsKey(id)) {
                    LOG.warn("[battlemage library] variant {} lists focus {} but it did not resolve; the mage will not carry it", v.id, id);
                }
            }
        }
        LOG.info("[battlemage library] {} foci ready, {} tiers, {} variants", RESOLVED.size(), data.tiers.size(), data.variants.size());
    }

    private static String readConfigOrDefault() {
        File dir = new File(Loader.instance().getConfigDir(), "arcanaquesttweaks");
        File file = new File(dir, "cultist_battlemages.json");
        String bundled = readResource();
        if (!file.isFile()) {
            try {
                Files.createDirectories(dir.toPath());
                Files.write(file.toPath(), bundled.getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                LOG.warn("Could not write {}: {}", file, e.getMessage());
            }
            return bundled;
        }
        try {
            String existing = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            return upgradeIfOld(dir, file, existing, bundled);
        } catch (IOException e) {
            LOG.warn("Could not read {}, using the bundled default: {}", file, e.getMessage());
            return bundled;
        }
    }

    /**
     * A config copy older than {@link LibraryMigration#CURRENT} (stage 1 wrote none) is backed up next to itself and
     * replaced by the bundled default; a current or newer copy is left alone (plan 4.1, versioned migration).
     */
    private static String upgradeIfOld(File dir, File file, String existing, String bundled) {
        Integer found = LibraryMigration.readVersion(existing);
        if (!LibraryMigration.needsUpgrade(found)) {
            return existing;
        }
        java.util.Set<String> names = new java.util.HashSet<>();
        String[] listed = dir.list();
        if (listed != null) {
            names.addAll(java.util.Arrays.asList(listed));
        }
        String backup = LibraryMigration.backupName(file.getName(), names);
        try {
            Files.copy(file.toPath(), new File(dir, backup).toPath());
            Files.write(file.toPath(), bundled.getBytes(StandardCharsets.UTF_8));
            LOG.warn("[battlemage library] {} had schemaVersion {} (current {}); the old copy was saved as {} and replaced by the bundled default. Port any edits you made from the backup.",
                    file.getName(), found, LibraryMigration.CURRENT, backup);
            return bundled;
        } catch (IOException e) {
            LOG.warn("[battlemage library] could not upgrade {} ({}); using the bundled default for this run", file, e.getMessage());
            return bundled;
        }
    }

    private static String readResource() {
        try (InputStream in = FocusLibrary.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                return "{}";
            }
            return new String(readAll(in), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "{}";
        }
    }

    private static byte[] readAll(InputStream in) throws IOException {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = in.read(buf)) > 0) {
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }

    private static BattlemageFocus resolve(FocusDef def) {
        CtFocusLine line = CtFocusLine.parse(def.nbt);
        if (line == null) {
            LOG.warn("Battlemage focus {} dropped: unreadable nbt line", def.id);
            return null;
        }
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(line.itemId));
        if (item == null) {
            LOG.warn("Battlemage focus {} dropped: item {} is not registered", def.id, line.itemId);
            return null;
        }
        if (line.nbt == null) {
            LOG.warn("Battlemage focus {} dropped: no tag", def.id);
            return null;
        }
        NBTTagCompound tag;
        try {
            tag = JsonToNBT.getTagFromJson(line.nbt);
        } catch (NBTException e) {
            LOG.warn("Battlemage focus {} dropped: bad nbt ({})", def.id, e.getMessage());
            return null;
        }
        String missing = firstMissingElement(tag);
        if (missing != null) {
            LOG.warn("Battlemage focus {} dropped: focus element {} is not registered", def.id, missing);
            return null;
        }
        ItemStack stack = new ItemStack(item);
        stack.setTagCompound(tag);
        FocusPackage pkg = ItemFocus.getPackage(stack);
        if (pkg == null) {
            LOG.warn("Battlemage focus {} dropped: no package in the tag", def.id);
            return null;
        }
        int color = tag.hasKey("color") ? tag.getInteger("color") : 0xFFFFFF;
        return new BattlemageFocus(def, pkg, item, color);
    }

    /** Walks the tag for node compounds ({@code type} plus {@code key}) and returns the first unregistered key. */
    static String firstMissingElement(NBTBase base) {
        if (base instanceof NBTTagCompound) {
            NBTTagCompound c = (NBTTagCompound) base;
            if (c.hasKey("type", 8) && c.hasKey("key", 8)) {
                String type = c.getString("type");
                String key = c.getString("key");
                if (("MEDIUM".equals(type) || "MOD".equals(type) || "EFFECT".equals(type))
                        && FocusEngine.getElement(key) == null) {
                    return key;
                }
            }
            for (String k : c.getKeySet()) {
                String m = firstMissingElement(c.getTag(k));
                if (m != null) {
                    return m;
                }
            }
        } else if (base instanceof NBTTagList) {
            NBTTagList l = (NBTTagList) base;
            for (int i = 0; i < l.tagCount(); i++) {
                String m = firstMissingElement(l.get(i));
                if (m != null) {
                    return m;
                }
            }
        }
        return null;
    }
}
