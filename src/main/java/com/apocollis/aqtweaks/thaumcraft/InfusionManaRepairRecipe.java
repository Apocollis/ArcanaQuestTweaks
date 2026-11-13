package com.apocollis.aqtweaks.thaumcraft;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemShears;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.util.RecipeMatcher;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.OreIngredient;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.capabilities.IPlayerKnowledge;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.api.crafting.InfusionRecipe;
import thaumcraft.api.items.ItemsTC;
import vazkii.botania.api.mana.IManaUsingItem;
import vazkii.botania.common.item.ItemManaTablet;

/**
 * Mana-Weave: infuses any damageable item with Botania's Manasteel self-repair. The pedestal items depend on the central
 * item (its Manasteel cousin, or a Manasteel block), so {@link #matches} builds the ingredient list per central item.
 */
public class InfusionManaRepairRecipe extends InfusionRecipe {
    public static final String RESEARCH = "MANA_REPAIR";

    public InfusionManaRepairRecipe() {
        super(RESEARCH, displayOutput(), ManaRepairLogic.INSTABILITY, essentia(), new ItemStack(Items.IRON_CHESTPLATE),
                displayComponents());
    }

    private static AspectList essentia() {
        return new AspectList()
                .add(Aspect.TOOL, ManaRepairLogic.ESSENTIA_INSTRUMENTUM)
                .add(Aspect.PROTECT, ManaRepairLogic.ESSENTIA_PRAEMUNIO)
                .add(Aspect.ENERGY, ManaRepairLogic.ESSENTIA_POTENTIA)
                .add(Aspect.EXCHANGE, ManaRepairLogic.ESSENTIA_PERMUTATIO);
    }

    private static ItemStack displayOutput() {
        ItemStack out = new ItemStack(Items.IRON_CHESTPLATE);
        ManaRepairTag.set(out);
        return out;
    }

    /** What the Thaumonomicon page shows: the chestplate case. */
    private static Object[] displayComponents() {
        Item chest = Item.REGISTRY.getObject(new ResourceLocation("botania", "manasteelchest"));
        return new Object[] {
                chest == null ? "blockManasteel" : new ItemStack(chest),
                tabletStack(),
                "runeManaB",
                "manaPearl",
                "manaDiamond",
                new ItemStack(ItemsTC.salisMundus),
                "ingotThaumium",
                "ingotThaumium" };
    }

    private static ItemStack tabletStack() {
        Item item = Item.REGISTRY.getObject(new ResourceLocation("botania", "manatablet"));
        ItemStack tablet = new ItemStack(item == null ? Items.AIR : item);
        if (item != null) ItemManaTablet.setMana(tablet, ManaRepairLogic.REQUIRED_TABLET_MANA);
        return tablet;
    }

    /** Any damageable item that is not already infused, not a Botania mana item, and not already Manasteel. */
    public static boolean canInfuse(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.isItemStackDamageable()) return false;
        if (ManaRepairTag.has(stack)) return false;
        Item item = stack.getItem();
        if (item instanceof IManaUsingItem && ((IManaUsingItem) item).usesMana(stack)) return false;
        ResourceLocation name = item.getRegistryName();
        return name == null || !ManaRepairLogic.isManasteelItem(name.toString());
    }

    static ManaRepairLogic.Kind kindOf(ItemStack stack) {
        Item item = stack.getItem();
        if (item instanceof ItemArmor) {
            EntityEquipmentSlot slot = ((ItemArmor) item).armorType;
            switch (slot) {
                case HEAD: return ManaRepairLogic.Kind.HELMET;
                case CHEST: return ManaRepairLogic.Kind.CHEST;
                case LEGS: return ManaRepairLogic.Kind.LEGS;
                case FEET: return ManaRepairLogic.Kind.BOOTS;
                default: return ManaRepairLogic.Kind.OTHER;
            }
        }
        if (item instanceof ItemShears) return ManaRepairLogic.Kind.SHEARS;
        if (item instanceof ItemSword) return ManaRepairLogic.Kind.SWORD;
        java.util.Set<String> classes = item.getToolClasses(stack);
        if (classes.contains("pickaxe")) return ManaRepairLogic.Kind.PICKAXE;
        if (classes.contains("axe") || item instanceof ItemAxe) return ManaRepairLogic.Kind.AXE;
        if (classes.contains("shovel")) return ManaRepairLogic.Kind.SHOVEL;
        return ManaRepairLogic.Kind.OTHER;
    }

    private static Ingredient cousinIngredient(ItemStack central) {
        ResourceLocation name = central.getItem().getRegistryName();
        List<String> names = ManaRepairLogic.cousins(name == null ? null : name.toString(), kindOf(central));
        List<ItemStack> stacks = new ArrayList<>();
        for (String cousin : names) {
            Item item = Item.REGISTRY.getObject(new ResourceLocation(cousin));
            if (item != null) stacks.add(new ItemStack(item, 1, OreDictionary.WILDCARD_VALUE));
        }
        if (stacks.isEmpty()) return new OreIngredient("blockManasteel");
        return Ingredient.fromStacks(stacks.toArray(new ItemStack[0]));
    }

    private static final Ingredient TABLET = new Ingredient(new ItemStack[] { tabletStack() }) {
        @Override
        public boolean apply(ItemStack stack) {
            if (stack == null || !(stack.getItem() instanceof ItemManaTablet)) return false;
            ItemManaTablet item = (ItemManaTablet) stack.getItem();
            return ManaRepairLogic.tabletQualifies(item.getMana(stack), ItemManaTablet.isStackCreative(stack));
        }
    };

    private static List<Ingredient> required(ItemStack central) {
        List<Ingredient> list = new ArrayList<>(8);
        list.add(cousinIngredient(central));
        list.add(TABLET);
        list.add(new OreIngredient("runeManaB"));
        list.add(new OreIngredient("manaPearl"));
        list.add(new OreIngredient("manaDiamond"));
        list.add(Ingredient.fromItems(ItemsTC.salisMundus));
        list.add(new OreIngredient("ingotThaumium"));
        list.add(new OreIngredient("ingotThaumium"));
        return list;
    }

    @Override
    public boolean matches(List<ItemStack> input, ItemStack central, World world, EntityPlayer player) {
        if (player == null || !canInfuse(central)) return false;
        IPlayerKnowledge knowledge = ThaumcraftCapabilities.getKnowledge(player);
        if (knowledge == null || !knowledge.isResearchKnown(this.research)) return false;
        return RecipeMatcher.findMatches(input, required(central)) != null;
    }

    @Override
    public Object getRecipeOutput(EntityPlayer player, ItemStack central, List<ItemStack> components) {
        ItemStack out = central.copy();
        ManaRepairTag.set(out);
        return out;
    }
}
