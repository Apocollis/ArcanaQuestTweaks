package com.apocollis.aqtweaks.reskillable;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import codersafterdark.reskillable.api.ReskillableRegistries;
import codersafterdark.reskillable.api.data.PlayerData;
import codersafterdark.reskillable.api.data.PlayerDataHandler;
import codersafterdark.reskillable.api.data.PlayerSkillInfo;
import codersafterdark.reskillable.api.skill.Skill;
import net.minecraft.block.Block;
import net.minecraft.block.BlockCocoa;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.BlockFlower;
import net.minecraft.block.BlockGravel;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockLog;
import net.minecraft.block.BlockMelon;
import net.minecraft.block.BlockMushroom;
import net.minecraft.block.BlockNetherWart;
import net.minecraft.block.BlockOre;
import net.minecraft.block.BlockPumpkin;
import net.minecraft.block.BlockRedstoneOre;
import net.minecraft.block.BlockTallGrass;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityPotion;
import net.minecraft.init.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.IShearable;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.fml.common.FMLCommonHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-level math and classify helpers. Reskillable API types stay in this package.
 */
public final class ReskillableBonuses {

    static final Logger LOGGER = LogManager.getLogger("AQTweaks-Reskillable");

    static final UUID UUID_ATTACK = UUID.fromString("8c1e6b10-4a3d-4c6f-9e2a-0b7d2f11a001");
    static final UUID UUID_ARMOR = UUID.fromString("8c1e6b10-4a3d-4c6f-9e2a-0b7d2f11a002");
    static final UUID UUID_SPEED = UUID.fromString("8c1e6b10-4a3d-4c6f-9e2a-0b7d2f11a003");

    static final String MOD_ATTACK = "aqtweaks.reskillable.attack";
    static final String MOD_ARMOR = "aqtweaks.reskillable.armor";
    static final String MOD_SPEED = "aqtweaks.reskillable.speed";

    private static final Set<String> MAGIC_LOG_SEEN = new HashSet<>();
    private static final int MAGIC_LOG_CAP = 48;

    private static final String[] SKILL_PATHS = {
            "attack", "defense", "agility", "building", "mining", "gathering", "farming", "magic"
    };
    private static final ResourceLocation[] SKILL_IDS = new ResourceLocation[SKILL_PATHS.length];

    /**
     * EntityPlayer.equals/hashCode compare only the entity id, so one player-keyed map would let a
     * client and a server player share a slot. The two logical sides get separate maps instead.
     */
    private static final Map<UUID, int[]> SERVER_LEVELS = new ConcurrentHashMap<>();
    private static final Map<UUID, int[]> CLIENT_LEVELS = new ConcurrentHashMap<>();

    static {
        for (int i = 0; i < SKILL_PATHS.length; i++) {
            SKILL_IDS[i] = new ResourceLocation("reskillable", SKILL_PATHS[i]);
        }
    }

    private ReskillableBonuses() {}

    public static boolean enabled() {
        return ArcanaQuestTweaksConfig.ReskillableModuleConfig.general.enable;
    }

    public static int skillLevel(EntityPlayer player, String path) {
        if (player == null) return 0;
        int index = skillIndex(path);
        if (index < 0) return resolveLevel(player, new ResourceLocation("reskillable", path));
        int[] row = levelCache(player).computeIfAbsent(player.getUniqueID(), key -> newLevelRow());
        int level = row[index];
        if (level < 0) {
            level = resolveLevel(player, SKILL_IDS[index]);
            row[index] = level;
        }
        return level;
    }

    static void invalidateLevels(EntityPlayer player) {
        if (player == null) return;
        levelCache(player).remove(player.getUniqueID());
    }

    static void invalidateLevels(boolean client) {
        (client ? CLIENT_LEVELS : SERVER_LEVELS).clear();
    }

    static void invalidateAllLevels() {
        SERVER_LEVELS.clear();
        CLIENT_LEVELS.clear();
    }

    private static Map<UUID, int[]> levelCache(EntityPlayer player) {
        World world = player.getEntityWorld();
        return world != null && world.isRemote ? CLIENT_LEVELS : SERVER_LEVELS;
    }

    private static int[] newLevelRow() {
        int[] row = new int[SKILL_PATHS.length];
        java.util.Arrays.fill(row, -1);
        return row;
    }

    private static int skillIndex(String path) {
        for (int i = 0; i < SKILL_PATHS.length; i++) {
            if (SKILL_PATHS[i].equals(path)) return i;
        }
        return -1;
    }

    private static int resolveLevel(EntityPlayer player, ResourceLocation skillId) {
        PlayerData data = PlayerDataHandler.get(player);
        if (data == null) return 0;
        Skill skill = ReskillableRegistries.SKILLS.getValue(skillId);
        if (skill == null) return 0;
        PlayerSkillInfo info = data.getSkillInfo(skill);
        if (info == null) return 0;
        return Math.max(0, info.getLevel());
    }

    public static boolean roll(World world, EntityPlayer player, String skillPath, double k) {
        if (world == null || k <= 0.0) return false;
        int level = skillLevel(player, skillPath);
        if (level <= 0) return false;
        double chance = Math.min(1.0, level * k);
        return world.rand.nextDouble() < chance;
    }

    public static int addBuildingPlaceReach(EntityPlayer player, int base) {
        if (!enabled() || player == null || player.isCreative()) return base;
        double k = ArcanaQuestTweaksConfig.ReskillableModuleConfig.building.placeReachPerLevel;
        if (k <= 0.0) return base;
        int extra = (int) Math.floor(skillLevel(player, "building") * k);
        if (extra <= 0) return base;
        return base + extra;
    }

    public static int addBuildingMaxBlocks(EntityPlayer player, int base) {
        if (!enabled() || player == null || player.isCreative()) return base;
        double k = ArcanaQuestTweaksConfig.ReskillableModuleConfig.building.maxBlocksPerLevel;
        if (k <= 0.0) return base;
        int extra = (int) Math.floor(skillLevel(player, "building") * k);
        if (extra <= 0) return base;
        return base + extra;
    }

    public static boolean isMatureCrop(IBlockState state) {
        if (state == null) return false;
        Block block = state.getBlock();
        if (block instanceof BlockCrops) {
            return ((BlockCrops) block).isMaxAge(state);
        }
        if (block instanceof BlockNetherWart) {
            return state.getValue(BlockNetherWart.AGE) >= 3;
        }
        if (block instanceof BlockCocoa) {
            return state.getValue(BlockCocoa.AGE) >= 2;
        }
        return block instanceof BlockPumpkin || block instanceof BlockMelon;
    }

    public static boolean isMelonOrPumpkin(IBlockState state) {
        if (state == null) return false;
        Block block = state.getBlock();
        return block instanceof BlockPumpkin || block instanceof BlockMelon;
    }

    public static boolean isHerbalistBlock(IBlockState state) {
        if (state == null) return false;
        ResourceLocation key = Block.REGISTRY.getNameForObject(state.getBlock());
        if (key == null) return false;
        String ns = key.getNamespace();
        String[] list = ArcanaQuestTweaksConfig.ReskillableModuleConfig.gathering.herbalistNamespaces;
        if (list == null) return false;
        for (String id : list) {
            if (id != null && id.equalsIgnoreCase(ns)) return true;
        }
        return false;
    }

    public static boolean isBountifulCrop(IBlockState state) {
        if (state == null || isMelonOrPumpkin(state) || isHerbalistBlock(state)) return false;
        Block block = state.getBlock();
        if (block instanceof BlockCrops) {
            return ((BlockCrops) block).isMaxAge(state);
        }
        if (block instanceof BlockNetherWart) {
            return state.getValue(BlockNetherWart.AGE) >= 3;
        }
        if (block instanceof BlockCocoa) {
            return state.getValue(BlockCocoa.AGE) >= 2;
        }
        return false;
    }

    public static boolean isOreBlock(World world, IBlockState state) {
        if (state == null) return false;
        Block block = state.getBlock();
        if (block instanceof BlockOre || block instanceof BlockRedstoneOre) return true;
        ItemStack probe = itemFromBlock(world, state);
        if (probe.isEmpty()) return false;
        try {
            int[] ids = OreDictionary.getOreIDs(probe);
            for (int id : ids) {
                String name = OreDictionary.getOreName(id);
                if (name != null && name.startsWith("ore")) return true;
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    public static boolean isForageBlock(World world, IBlockState state) {
        if (state == null || isMatureCrop(state) || isOreBlock(world, state)) return false;
        Block block = state.getBlock();
        if (block instanceof BlockLog || block instanceof BlockLeaves || block instanceof BlockGravel) return true;
        if (block instanceof BlockFlower || block instanceof BlockMushroom || block instanceof BlockTallGrass) {
            return true;
        }
        ItemStack probe = itemFromBlock(world, state);
        if (probe.isEmpty()) return false;
        try {
            int[] ids = OreDictionary.getOreIDs(probe);
            for (int id : ids) {
                String name = OreDictionary.getOreName(id);
                if ("logWood".equals(name) || "treeLeaves".equals(name)) return true;
            }
        } catch (Exception ignored) {
        }
        ResourceLocation key = Block.REGISTRY.getNameForObject(block);
        if (key == null) return false;
        String path = key.getPath().toLowerCase(Locale.ROOT);
        return path.contains("mushroom") || path.contains("flower");
    }

    public static boolean hasSilkTouch(EntityPlayer player) {
        if (player == null) return false;
        ItemStack held = player.getHeldItemMainhand();
        if (held.isEmpty()) return false;
        return net.minecraft.enchantment.EnchantmentHelper.getEnchantmentLevel(Enchantments.SILK_TOUCH, held) > 0;
    }

    public static boolean isSpellLike(DamageSource source) {
        if (source == null) return false;
        String type = source.getDamageType();
        if (type == null) type = "";
        String lower = type.toLowerCase(Locale.ROOT);
        for (String deny : ArcanaQuestTweaksConfig.ReskillableModuleConfig.magic.denyTypes) {
            if (deny != null && deny.equalsIgnoreCase(type)) return false;
        }
        if ("thorns".equals(lower)) return false;
        if (source == DamageSource.MAGIC) return false;
        Entity immediate = source.getImmediateSource();
        if (immediate instanceof EntityPotion || immediate instanceof net.minecraft.entity.EntityAreaEffectCloud) {
            return false;
        }
        boolean magic = source.isMagicDamage();
        if (!magic) {
            for (String prefix : ArcanaQuestTweaksConfig.ReskillableModuleConfig.magic.allowTypePrefixes) {
                if (prefix != null && !prefix.isEmpty() && lower.startsWith(prefix.toLowerCase(Locale.ROOT))) {
                    magic = true;
                    break;
                }
            }
        }
        return magic;
    }

    public static float magicMultiplier(EntityPlayer player, boolean outgoing) {
        if (!enabled() || player == null) return 1.0f;
        double k = ArcanaQuestTweaksConfig.ReskillableModuleConfig.magic.perLevel;
        if (k <= 0.0) return 1.0f;
        double bonus = Math.min(0.4, skillLevel(player, "magic") * k);
        if (bonus <= 0.0) return 1.0f;
        return outgoing ? (float) (1.0 + bonus) : (float) (1.0 - bonus);
    }

    public static void maybeLogMagic(DamageSource source, Entity trueSource, Entity victim, boolean classified) {
        var cfg = ArcanaQuestTweaksConfig.ReskillableModuleConfig.magic;
        if (!cfg.logClassify) return;
        boolean playerInvolved = trueSource instanceof EntityPlayer || victim instanceof EntityPlayer;
        if (!playerInvolved || source == null) return;
        String type = source.getDamageType();
        Entity immediate = source.getImmediateSource();
        String key = String.valueOf(type) + '|' + source.isMagicDamage() + '|'
                + className(immediate) + '|' + className(trueSource) + '|' + classified;
        synchronized (MAGIC_LOG_SEEN) {
            if (MAGIC_LOG_SEEN.size() >= MAGIC_LOG_CAP) return;
            if (!MAGIC_LOG_SEEN.add(key)) return;
        }
        LOGGER.info("magic classify type={} isMagic={} immediate={} trueSource={} victim={} classified={}",
                type, source.isMagicDamage(), className(immediate), className(trueSource), className(victim), classified);
    }

    public static void restampOnlinePlayers() {
        invalidateAllLevels();
        var server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null) return;
        for (EntityPlayer player : server.getPlayerList().getPlayers()) {
            ReskillableModule.applyAttributes(player);
        }
    }

    static boolean isShearableLiving(Entity entity) {
        return entity instanceof IShearable;
    }

    private static ItemStack itemFromBlock(World world, IBlockState state) {
        try {
            Block block = state.getBlock();
            int meta = block.damageDropped(state);
            return new ItemStack(block, 1, meta);
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }

    private static String className(Entity entity) {
        return entity == null ? "null" : entity.getClass().getName();
    }
}
