package com.apocollis.aqtweaks.qualitytools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig.QualityToolsGeneral;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import teamroots.embers.recipe.DawnstoneAnvilRecipe;
import teamroots.embers.recipe.RecipeRegistry;
import teamroots.embers.tileentity.TileEntityDawnstoneAnvil;

public class QualityRuneAnvilRecipe extends DawnstoneAnvilRecipe {
    private static final Logger LOGGER = LogManager.getLogger("aqtweaks");
    private static final List<QualityRuneAnvilRecipe> RECIPES = new ArrayList<>();
    private static final Map<UUID, Long> MISMATCH_AT = new ConcurrentHashMap<>();

    private enum Rung {
        COMMON, UNCOMMON, RARE, LEGENDARY
    }

    private final String runeId;
    private final Rung rung;

    private QualityRuneAnvilRecipe(Item rune, String runeId, Rung rung) {
        super(Ingredient.fromItem(Items.IRON_SWORD), Ingredient.fromItem(rune), new ItemStack[] {new ItemStack(Items.IRON_SWORD)});
        this.runeId = runeId;
        this.rung = rung;
        this.bottom = Ingredient.EMPTY;
        this.top = Ingredient.fromItem(rune);
        this.result = Collections.emptyList();
    }

    public static void registerAll() {
        RECIPES.clear();
        QualityToolsGeneral cfg = ArcanaQuestTweaksConfig.QualityToolsModuleConfig.general;
        add(cfg.commonRune, Rung.COMMON);
        add(cfg.uncommonRune, Rung.UNCOMMON);
        add(cfg.rareRune, Rung.RARE);
        add(cfg.legendaryRune, Rung.LEGENDARY);
        for (int i = RECIPES.size() - 1; i >= 0; i--) {
            RecipeRegistry.dawnstoneAnvilRecipes.add(0, RECIPES.get(i));
        }
        if (RECIPES.isEmpty()) {
            LOGGER.warn("Quality Tools Module: no Dawnstone rune recipes (check Common/Uncommon/Rare/Legendary Rune ids)");
        } else {
            LOGGER.info("Quality Tools Module: registered {} Dawnstone rune recipes", RECIPES.size());
        }
    }

    private static void add(String id, Rung rung) {
        Item item = resolveRuneItem(id);
        if (item == null) {
            if (id != null && !id.isEmpty()) {
                LOGGER.warn("Quality Tools Module: Dawnstone rune item not found: {}", id);
            }
            return;
        }
        ResourceLocation name = item.getRegistryName();
        String runeId = name != null ? name.toString() : id;
        RECIPES.add(new QualityRuneAnvilRecipe(item, runeId, rung));
    }

    private static Item resolveRuneItem(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        ResourceLocation rl = new ResourceLocation(id);
        Item item = ForgeRegistries.ITEMS.getValue(rl);
        if (isPresent(item)) {
            return item;
        }
        String altPath = aliasPath(rl.getPath());
        if (altPath == null) {
            return null;
        }
        item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(rl.getNamespace(), altPath));
        return isPresent(item) ? item : null;
    }

    private static boolean isPresent(Item item) {
        return item != null && item != Items.AIR;
    }

    private static String aliasPath(String path) {
        return switch (path) {
            case "common_mat" -> "itemcommonmat";
            case "itemcommonmat" -> "common_mat";
            case "uncommon_mat" -> "itemuncommonmat";
            case "itemuncommonmat" -> "uncommon_mat";
            case "rare_mat" -> "itemraremat";
            case "itemraremat" -> "rare_mat";
            case "legendary_mat" -> "itemlegendarymat";
            case "itemlegendarymat" -> "legendary_mat";
            default -> null;
        };
    }

    public static boolean isConfiguredRune(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        Item item = stack.getItem();
        QualityToolsGeneral cfg = ArcanaQuestTweaksConfig.QualityToolsModuleConfig.general;
        return item == resolveRuneItem(cfg.commonRune)
                || item == resolveRuneItem(cfg.uncommonRune)
                || item == resolveRuneItem(cfg.rareRune)
                || item == resolveRuneItem(cfg.legendaryRune);
    }

    public static boolean anyMatches(ItemStack bottom, ItemStack top) {
        for (QualityRuneAnvilRecipe recipe : RECIPES) {
            if (recipe.matches(bottom, top)) {
                return true;
            }
        }
        return false;
    }

    public static void onFailedHammer(TileEntity tile) {
        if (!ArcanaQuestTweaksConfig.QualityToolsModuleConfig.general.enable) {
            return;
        }
        if (!(tile instanceof TileEntityDawnstoneAnvil anvil)) {
            return;
        }
        ItemStack bottom = anvil.inventory.getStackInSlot(0);
        ItemStack top = anvil.inventory.getStackInSlot(1);
        if (!QualityNbt.isQualityItem(bottom) || !isConfiguredRune(top)) {
            return;
        }
        if (anyMatches(bottom, top)) {
            return;
        }
        notifyMismatch(tile.getWorld(), tile.getPos());
    }

    public static void notifyMismatch(World world, BlockPos pos) {
        if (world == null || world.isRemote || pos == null) {
            return;
        }
        long now = world.getTotalWorldTime();
        AxisAlignedBB box = new AxisAlignedBB(pos).grow(6.0);
        for (EntityPlayer player : world.getEntitiesWithinAABB(EntityPlayer.class, box)) {
            Long last = MISMATCH_AT.get(player.getUniqueID());
            if (last != null && now - last < 40) {
                continue;
            }
            MISMATCH_AT.put(player.getUniqueID(), now);
            player.sendStatusMessage(new TextComponentTranslation("chat.aqtweaks.quality.rune_mismatch"), true);
        }
    }

    @Override
    public boolean matches(ItemStack bottom, ItemStack top) {
        if (!ArcanaQuestTweaksConfig.QualityToolsModuleConfig.general.enable) {
            return false;
        }
        if (bottom == null || top == null || bottom.isEmpty() || top.isEmpty()) {
            return false;
        }
        ResourceLocation name = top.getItem().getRegistryName();
        if (name == null || !runeId.equals(name.toString())) {
            return false;
        }
        if (!QualityNbt.isQualityItem(bottom)) {
            return false;
        }
        String live = QualityNbt.liveColor(bottom);
        if (QualityNbt.COLOR_DARK_GRAY.equals(live)) {
            return false;
        }
        String kept = QualityNbt.keptColor(bottom);
        if (QualityNbt.COLOR_GRAY.equals(live) && !QualityNbt.COLOR_RED.equals(kept)) {
            return false;
        }
        return switch (rung) {
            case COMMON -> QualityNbt.COLOR_RED.equals(kept)
                    || (isNormalOrYellow(kept) && QualityNbt.applyColorWouldSucceed(bottom, QualityNbt.COLOR_YELLOW));
            case UNCOMMON -> isUpgradeOrReroll(kept, QualityNbt.COLOR_YELLOW, QualityNbt.COLOR_GREEN)
                    && QualityNbt.applyColorWouldSucceed(bottom, QualityNbt.COLOR_GREEN);
            case RARE -> isUpgradeOrReroll(kept, QualityNbt.COLOR_GREEN, QualityNbt.COLOR_BLUE)
                    && QualityNbt.applyColorWouldSucceed(bottom, QualityNbt.COLOR_BLUE);
            case LEGENDARY -> isUpgradeOrReroll(kept, QualityNbt.COLOR_BLUE, QualityNbt.COLOR_GOLD)
                    && QualityNbt.applyColorWouldSucceed(bottom, QualityNbt.COLOR_GOLD);
        };
    }

    private static boolean isNormalOrYellow(String kept) {
        return QualityNbt.COLOR_NORMAL.equals(kept) || QualityNbt.COLOR_YELLOW.equals(kept);
    }

    private static boolean isUpgradeOrReroll(String kept, String from, String same) {
        return from.equals(kept) || same.equals(kept);
    }

    @Override
    public List<ItemStack> getResult(TileEntity tile, ItemStack bottom, ItemStack top) {
        ItemStack out = bottom.copy();
        String kept = QualityNbt.keptColor(out);
        if (rung == Rung.COMMON && QualityNbt.COLOR_RED.equals(kept)) {
            QualityNbt.stripLiveQuality(out);
            QualityNbt.clearQualityBase(out);
            QualityNbt.setWearFlag(out, false);
        } else {
            String to = switch (rung) {
                case COMMON -> QualityNbt.COLOR_YELLOW;
                case UNCOMMON -> QualityNbt.COLOR_GREEN;
                case RARE -> QualityNbt.COLOR_BLUE;
                case LEGENDARY -> QualityNbt.COLOR_GOLD;
            };
            QualityNbt.applyColor(out, to);
            QualityNbt.setQualityBaseFromLive(out);
            QualityNbt.setWearFlag(out, false);
        }
        if (top.getCount() > 1 && tile instanceof TileEntityDawnstoneAnvil anvil) {
            ItemStack leftover = top.copy();
            leftover.shrink(1);
            anvil.inventory.setStackInSlot(1, leftover);
        }
        return Collections.singletonList(out);
    }

    @Override
    public List<ItemStack> getBottomInputs() {
        List<ItemStack> list = new ArrayList<>();
        list.add(new ItemStack(Items.IRON_SWORD));
        list.add(new ItemStack(Items.IRON_PICKAXE));
        list.add(new ItemStack(Items.IRON_CHESTPLATE));
        return list;
    }

    @Override
    public List<ItemStack> getTopInputs() {
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(runeId));
        if (item == null) {
            return Collections.emptyList();
        }
        return Collections.singletonList(new ItemStack(item));
    }

    @Override
    public List<ItemStack> getOutputs() {
        return getBottomInputs();
    }
}
