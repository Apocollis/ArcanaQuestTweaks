package com.apocollis.aqtweaks.qualitytools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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
    private static final List<QualityRuneAnvilRecipe> RECIPES = new ArrayList<>();
    private static final Map<UUID, Long> MISMATCH_AT = new ConcurrentHashMap<>();

    private final String runeId;
    private final boolean common;

    private QualityRuneAnvilRecipe(Item rune, String runeId, boolean common) {
        super(Ingredient.fromItem(Items.IRON_SWORD), Ingredient.fromItem(rune), new ItemStack[] {new ItemStack(Items.IRON_SWORD)});
        this.runeId = runeId;
        this.common = common;
        this.bottom = Ingredient.EMPTY;
        this.top = Ingredient.fromItem(rune);
        this.result = Collections.emptyList();
    }

    public static void registerAll() {
        RECIPES.clear();
        QualityToolsGeneral cfg = ArcanaQuestTweaksConfig.QualityToolsModuleConfig.general;
        add(cfg.commonRune, true);
        add(cfg.uncommonRune, false);
        add(cfg.rareRune, false);
        add(cfg.legendaryRune, false);
        for (int i = RECIPES.size() - 1; i >= 0; i--) {
            RecipeRegistry.dawnstoneAnvilRecipes.add(0, RECIPES.get(i));
        }
    }

    private static void add(String id, boolean common) {
        if (id == null || id.isEmpty()) {
            return;
        }
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
        if (item == null || item == Items.AIR) {
            return;
        }
        RECIPES.add(new QualityRuneAnvilRecipe(item, id, common));
    }

    public static boolean isConfiguredRune(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        ResourceLocation name = stack.getItem().getRegistryName();
        if (name == null) {
            return false;
        }
        String id = name.toString();
        QualityToolsGeneral cfg = ArcanaQuestTweaksConfig.QualityToolsModuleConfig.general;
        return id.equals(cfg.commonRune) || id.equals(cfg.uncommonRune)
                || id.equals(cfg.rareRune) || id.equals(cfg.legendaryRune);
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
        if (common) {
            if (QualityNbt.COLOR_RED.equals(kept)) {
                return true;
            }
            return QualityNbt.COLOR_NORMAL.equals(kept)
                    && QualityNbt.applyColorWouldSucceed(bottom, QualityNbt.COLOR_YELLOW);
        }
        QualityToolsGeneral cfg = ArcanaQuestTweaksConfig.QualityToolsModuleConfig.general;
        if (runeId.equals(cfg.uncommonRune)) {
            return QualityNbt.COLOR_YELLOW.equals(kept)
                    && QualityNbt.applyColorWouldSucceed(bottom, QualityNbt.COLOR_GREEN);
        }
        if (runeId.equals(cfg.rareRune)) {
            return QualityNbt.COLOR_GREEN.equals(kept)
                    && QualityNbt.applyColorWouldSucceed(bottom, QualityNbt.COLOR_BLUE);
        }
        if (runeId.equals(cfg.legendaryRune)) {
            return QualityNbt.COLOR_BLUE.equals(kept)
                    && QualityNbt.applyColorWouldSucceed(bottom, QualityNbt.COLOR_GOLD);
        }
        return false;
    }

    @Override
    public List<ItemStack> getResult(TileEntity tile, ItemStack bottom, ItemStack top) {
        ItemStack out = bottom.copy();
        String kept = QualityNbt.keptColor(out);
        if (common && QualityNbt.COLOR_RED.equals(kept)) {
            QualityNbt.stripLiveQuality(out);
            QualityNbt.clearQualityBase(out);
            QualityNbt.setWearFlag(out, false);
        } else if (common) {
            QualityNbt.applyColor(out, QualityNbt.COLOR_YELLOW);
            QualityNbt.setQualityBaseFromLive(out);
            QualityNbt.setWearFlag(out, false);
        } else {
            QualityToolsGeneral cfg = ArcanaQuestTweaksConfig.QualityToolsModuleConfig.general;
            String to = QualityNbt.COLOR_GREEN;
            if (runeId.equals(cfg.rareRune)) {
                to = QualityNbt.COLOR_BLUE;
            } else if (runeId.equals(cfg.legendaryRune)) {
                to = QualityNbt.COLOR_GOLD;
            }
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
