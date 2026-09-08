package com.apocollis.aqtweaks.spawning;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig.SpawningModuleConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Pack JSON {@code config/arcanaquest/mob_spawnparties.json}. Natural ticking spawns only.
 */
public final class SpawnParties {

    private static final Logger LOGGER = LogManager.getLogger("AQTweaks-Spawning");
    private static final Gson GSON = new GsonBuilder().create();

    private static File configDir;
    private static volatile List<Party> parties = List.of();
    private static final Set<String> LOGGED_UNKNOWN = ConcurrentHashMap.newKeySet();
    private static final Set<String> LOGGED_BAD = ConcurrentHashMap.newKeySet();

    private SpawnParties() {}

    public static void load(File configDirectory) {
        configDir = configDirectory;
        reload();
    }

    public static void reload() {
        LOGGED_UNKNOWN.clear();
        LOGGED_BAD.clear();
        File dir = configDir;
        if (dir == null) {
            parties = List.of();
            return;
        }
        String relative = SpawningModuleConfig.general.spawnPartiesFile;
        if (relative == null || relative.isBlank()) {
            LOGGER.warn("[AQTweaks] Spawn parties file path is empty; mixed groups off.");
            parties = List.of();
            return;
        }
        File file = new File(dir, relative.replace('/', File.separatorChar));
        if (!file.isFile()) {
            LOGGER.warn("[AQTweaks] Missing {}; mixed groups off.", file.getAbsolutePath());
            parties = List.of();
            return;
        }
        try (FileReader reader = new FileReader(file)) {
            FileDto parsed = GSON.fromJson(reader, FileDto.class);
            if (parsed == null || parsed.parties == null) {
                LOGGER.warn("[AQTweaks] {} has no parties array; mixed groups off.", file.getAbsolutePath());
                parties = List.of();
                return;
            }
            List<Party> loaded = new ArrayList<>();
            for (PartyDto dto : parsed.parties) {
                Party party = parseParty(dto);
                if (party != null) {
                    loaded.add(party);
                }
            }
            parties = List.copyOf(loaded);
            LOGGER.info("[AQTweaks] Loaded {} spawn parties from {}.", parties.size(), file.getAbsolutePath());
        } catch (Exception e) {
            LOGGER.warn("[AQTweaks] Failed to parse {}; mixed groups off.", file.getAbsolutePath(), e);
            parties = List.of();
        }
    }

    public static void tryAfterLeader(EntityLiving leader) {
        if (leader == null || leader.world == null || leader.world.isRemote) {
            return;
        }
        var general = SpawningModuleConfig.general;
        if (!general.enable || !general.enableMixedGroups) {
            return;
        }
        List<Party> list = parties;
        if (list.isEmpty()) {
            return;
        }
        String id = idOf(leader);
        if (id == null) {
            return;
        }
        World world = leader.world;
        BlockPos pos = new BlockPos(MathHelper.floor(leader.posX), MathHelper.floor(leader.posY),
                MathHelper.floor(leader.posZ));
        for (Party party : list) {
            if (!party.leaders.contains(id)) {
                continue;
            }
            if (!whenMatches(party.when, world, pos)) {
                continue;
            }
            if (party.chance < 1.0 && world.rand.nextDouble() >= party.chance) {
                return;
            }
            spawnCompanions(world, leader, party);
            return;
        }
    }

    private static void spawnCompanions(World world, EntityLiving leader, Party party) {
        for (Companion companion : party.companions) {
            int count = roll(world, companion.min, companion.max);
            if (count <= 0) {
                continue;
            }
            ResourceLocation key = new ResourceLocation(companion.mob);
            Class<? extends Entity> spawnClass = EntityList.getClass(key);
            if (spawnClass == null || !EntityLiving.class.isAssignableFrom(spawnClass)) {
                if (LOGGED_UNKNOWN.add(companion.mob)) {
                    LOGGER.warn("[AQTweaks] Unknown or non-living spawn party companion {}.", companion.mob);
                }
                continue;
            }
            SpawnPackFiller.placeNearby(world, leader, count, spawnClass, w -> {
                Entity created = EntityList.createEntityByIDFromName(key, w);
                if (!(created instanceof EntityLiving living)) {
                    if (created != null) {
                        created.setDead();
                    }
                    return null;
                }
                return living;
            }, true);
        }
    }

    private static boolean whenMatches(When when, World world, BlockPos pos) {
        if (when.dimensions != null && !when.dimensions.contains(world.provider.getDimension())) {
            return false;
        }
        if (when.layer == Layer.SURFACE && SpawnLayerFilter.isCavePick(world, pos)) {
            return false;
        }
        if (when.layer == Layer.CAVE && !SpawnLayerFilter.isCavePick(world, pos)) {
            return false;
        }
        boolean day = world.isDaytime();
        if (when.time == Time.DAY && !day) {
            return false;
        }
        if (when.time == Time.NIGHT && day) {
            return false;
        }
        if (when.biomes != null) {
            Biome biome = world.getBiome(pos);
            ResourceLocation name = biome != null ? biome.getRegistryName() : null;
            if (name == null || !when.biomes.contains(name.toString())) {
                return false;
            }
        }
        int y = pos.getY();
        if (when.minY != null && y < when.minY) {
            return false;
        }
        if (when.maxY != null && y > when.maxY) {
            return false;
        }
        return true;
    }

    private static int roll(World world, int min, int max) {
        int span = max - min + 1;
        if (span <= 1) {
            return min;
        }
        return min + world.rand.nextInt(span);
    }

    private static String idOf(EntityLiving living) {
        ResourceLocation key = EntityList.getKey(living);
        return key != null ? key.toString() : null;
    }

    private static Party parseParty(PartyDto dto) {
        if (dto == null) {
            return null;
        }
        String id = dto.id != null && !dto.id.isBlank() ? dto.id.trim() : "unnamed";
        Set<String> leaders = toIdSet(dto.leaders);
        if (leaders.isEmpty()) {
            bad(id, "no leaders");
            return null;
        }
        List<Companion> companions = new ArrayList<>();
        if (dto.companions != null) {
            for (CompanionDto c : dto.companions) {
                if (c == null || c.mob == null || c.mob.isBlank()) {
                    continue;
                }
                int min = c.min;
                int max = c.max;
                if (max < 0) {
                    continue;
                }
                if (min < 0) {
                    min = 0;
                }
                if (max < min) {
                    bad(id, "companion " + c.mob + " max < min");
                    continue;
                }
                companions.add(new Companion(c.mob.trim(), min, max));
            }
        }
        if (companions.isEmpty()) {
            bad(id, "no companions");
            return null;
        }
        double chance = dto.chance == null ? 1.0 : dto.chance;
        if (chance < 0.0 || chance > 1.0) {
            bad(id, "chance must be 0..1");
            return null;
        }
        When when = parseWhen(dto.when, id);
        if (when == null) {
            return null;
        }
        return new Party(id, leaders, chance, when, List.copyOf(companions));
    }

    private static When parseWhen(WhenDto dto, String partyId) {
        if (dto == null) {
            return new When(null, Layer.ANY, Time.BOTH, null, null, null);
        }
        Set<Integer> dims = null;
        if (dto.dimension != null) {
            dims = new HashSet<>(dto.dimension);
        }
        Layer layer = Layer.ANY;
        if (dto.layer != null && !dto.layer.isBlank()) {
            switch (dto.layer.trim().toLowerCase()) {
                case "any" -> layer = Layer.ANY;
                case "surface" -> layer = Layer.SURFACE;
                case "cave" -> layer = Layer.CAVE;
                default -> {
                    bad(partyId, "unknown layer " + dto.layer);
                    return null;
                }
            }
        }
        Time time = Time.BOTH;
        if (dto.time != null && !dto.time.isBlank()) {
            switch (dto.time.trim().toLowerCase()) {
                case "both" -> time = Time.BOTH;
                case "day" -> time = Time.DAY;
                case "night" -> time = Time.NIGHT;
                default -> {
                    bad(partyId, "unknown time " + dto.time);
                    return null;
                }
            }
        }
        Set<String> biomes = dto.biomes != null ? toIdSet(dto.biomes) : null;
        if (biomes != null && biomes.isEmpty()) {
            biomes = Set.of();
        }
        return new When(dims, layer, time, biomes, dto.minheight, dto.maxheight);
    }

    private static void bad(String partyId, String reason) {
        String key = partyId + ":" + reason;
        if (LOGGED_BAD.add(key)) {
            LOGGER.warn("[AQTweaks] Skipping spawn party {}: {}.", partyId, reason);
        }
    }

    private static Set<String> toIdSet(List<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> ids = new HashSet<>();
        for (String entry : raw) {
            if (entry == null) {
                continue;
            }
            String id = entry.trim();
            if (!id.isEmpty()) {
                ids.add(id);
            }
        }
        return ids;
    }

    private enum Layer { ANY, SURFACE, CAVE }

    private enum Time { BOTH, DAY, NIGHT }

    private record Party(String id, Set<String> leaders, double chance, When when, List<Companion> companions) {}

    private record When(Set<Integer> dimensions, Layer layer, Time time, Set<String> biomes, Integer minY, Integer maxY) {}

    private record Companion(String mob, int min, int max) {}

    @SuppressWarnings("unused")
    private static final class FileDto {
        @SerializedName("parties")
        List<PartyDto> parties;
    }

    @SuppressWarnings("unused")
    private static final class PartyDto {
        @SerializedName("id")
        String id;
        @SerializedName("leaders")
        List<String> leaders;
        @SerializedName("chance")
        Double chance;
        @SerializedName("when")
        WhenDto when;
        @SerializedName("companions")
        List<CompanionDto> companions;
    }

    @SuppressWarnings("unused")
    private static final class WhenDto {
        @SerializedName("dimension")
        List<Integer> dimension;
        @SerializedName("layer")
        String layer;
        @SerializedName("time")
        String time;
        @SerializedName("biomes")
        List<String> biomes;
        @SerializedName("minheight")
        Integer minheight;
        @SerializedName("maxheight")
        Integer maxheight;
    }

    @SuppressWarnings("unused")
    private static final class CompanionDto {
        @SerializedName("mob")
        String mob;
        @SerializedName("min")
        int min;
        @SerializedName("max")
        int max;
    }
}
