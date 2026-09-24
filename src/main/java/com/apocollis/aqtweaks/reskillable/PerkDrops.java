package com.apocollis.aqtweaks.reskillable;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.mixin.MixinBlockCropsSeed;
import net.minecraft.block.Block;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.BlockGravel;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockLog;
import net.minecraft.block.BlockCocoa;
import net.minecraft.block.BlockMelon;
import net.minecraft.block.BlockNetherWart;
import net.minecraft.block.BlockPumpkin;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemShears;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.oredict.OreDictionary;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.api.blocks.BlocksTC;

import java.util.List;

/** Extra harvest rolls for the wishlist perks. The forage and farming drips still run on their own. */
public final class PerkDrops {

    private static final double BONUS = 0.02;

    private PerkDrops() {}

    public static boolean isTomatoVine(IBlockState state) {
        if (state == null) return false;
        String name = state.getBlock().getClass().getName();
        return name.endsWith("BlockTomatoVine");
    }

    public static void afterCoreRolls(BlockEvent.HarvestDropsEvent event) {
        EntityPlayer player = event.getHarvester();
        IBlockState state = event.getState();
        List<ItemStack> drops = event.getDrops();
        if (player == null || state == null || drops == null) return;
        var perks = ArcanaQuestTweaksConfig.ReskillableModuleConfig.perks;
        boolean silk = ReskillableBonuses.hasSilkTouch(player);
        boolean shears = !player.getHeldItemMainhand().isEmpty()
                && player.getHeldItemMainhand().getItem() instanceof ItemShears;

        if (!silk && ReskillableBonuses.isOreBlock(event.getWorld(), state)) {
            motherlode(event.getWorld(), player, perks, drops);
            if (PerkAccess.on(player, "aqtweaks:lithomancy", perks.lithomancy.enable)) {
                lithomancy(event.getWorld(), player, state, drops);
            }
        }

        if (silk || shears) {
            return;
        }

        if (PerkAccess.on(player, "aqtweaks:lumberjack", perks.lumberjack.enable) && isLog(state, drops)) {
            if (ReskillableBonuses.roll(event.getWorld(), player, "gathering", BONUS)) {
                addOneOreName(drops, "logWood");
            }
        }
        if (PerkAccess.on(player, "aqtweaks:reforester", perks.reforester.enable) && isLeaves(state)) {
            reforest(event.getWorld(), state, drops);
        }
        if (PerkAccess.on(player, "aqtweaks:sifter", perks.sifter.enable)) {
            sift(event.getWorld(), player, state, drops);
        }
        if (PerkAccess.on(player, "aqtweaks:orchard", perks.orchard.enable)
                && ReskillableBonuses.isMelonOrPumpkin(state)
                && ReskillableBonuses.roll(event.getWorld(), player, "farming", BONUS)) {
            addOne(drops);
        }
        if (PerkAccess.on(player, "aqtweaks:seed_harvester", perks.seedHarvester.enable)
                && ReskillableBonuses.isMatureCrop(state)
                && ReskillableBonuses.roll(event.getWorld(), player, "farming", BONUS)) {
            addSeed(state, drops);
        }
    }

    public static boolean ownsLithomancy(BlockEvent.HarvestDropsEvent event) {
        EntityPlayer player = event.getHarvester();
        if (player == null || event.getState() == null) return false;
        if (ReskillableBonuses.hasSilkTouch(player)) return false;
        if (!ReskillableBonuses.isOreBlock(event.getWorld(), event.getState())) return false;
        return PerkAccess.on(player, "aqtweaks:lithomancy",
                ArcanaQuestTweaksConfig.ReskillableModuleConfig.perks.lithomancy.enable);
    }

    private static void motherlode(World world, EntityPlayer player,
                                    ArcanaQuestTweaksConfig.ReskillablePerks perks, List<ItemStack> drops) {
        if (!PerkAccess.on(player, "aqtweaks:motherlode", perks.motherlode.enable)) return;
        if (!ReskillableBonuses.roll(world, player, "mining", BONUS)) return;
        for (ItemStack stack : drops) {
            if (stack.isEmpty()) continue;
            if (!isOreStack(stack)) continue;
            if (stack.getCount() < stack.getMaxStackSize()) {
                stack.grow(1);
                return;
            }
        }
    }

    private static void lithomancy(World world, EntityPlayer player, IBlockState state, List<ItemStack> drops) {
        int mining = ReskillableBonuses.skillLevel(player, "mining");
        double chance = rareEarthBase(state) * (1.0 + mining * 0.0625);
        if (world.rand.nextDouble() >= chance) return;
        if (ItemsTC.nuggets == null) return;
        drops.add(new ItemStack(ItemsTC.nuggets, 1, 10));
    }

    static double rareEarthBase(IBlockState state) {
        Block block = state.getBlock();
        if (block == Blocks.DIAMOND_ORE) return 0.05;
        if (block == Blocks.EMERALD_ORE) return 0.075;
        if (block == Blocks.LAPIS_ORE) return 0.01;
        if (block == Blocks.COAL_ORE) return 0.001;
        if (block == Blocks.REDSTONE_ORE || block == Blocks.LIT_REDSTONE_ORE) return 0.01;
        if (block == Blocks.QUARTZ_ORE) return 0.01;
        if (block == BlocksTC.oreAmber || block == BlocksTC.oreQuartz) return 0.05;
        return 0.01;
    }

    private static void reforest(World world, IBlockState state, List<ItemStack> drops) {
        for (ItemStack stack : drops) {
            if (isSapling(stack)) return;
        }
        Block block = state.getBlock();
        if (!(block instanceof BlockLeaves leaves)) return;
        Item item = leaves.getItemDropped(state, world.rand, 0);
        if (item == null || item == Items.AIR) return;
        ItemStack one = new ItemStack(item, 1, leaves.damageDropped(state));
        if (!isSapling(one)) return;
        drops.add(one);
    }

    private static void sift(World world, EntityPlayer player, IBlockState state, List<ItemStack> drops) {
        boolean roll = ReskillableBonuses.roll(world, player, "gathering", BONUS);
        if (state.getBlock() instanceof BlockGravel) {
            if (!containsItem(drops, Item.getItemFromBlock(Blocks.GRAVEL))) {
                drops.add(new ItemStack(Blocks.GRAVEL));
            }
            if (roll) {
                drops.add(new ItemStack(Items.FLINT));
            }
        } else if (state.getBlock() == Blocks.CLAY && roll) {
            drops.add(new ItemStack(Items.CLAY_BALL));
        }
    }

    private static void addSeed(IBlockState state, List<ItemStack> drops) {
        ItemStack seed = seedOf(state);
        if (seed.isEmpty()) return;
        drops.add(seed);
    }

    private static ItemStack seedOf(IBlockState state) {
        Block block = state.getBlock();
        if (block instanceof BlockCrops crops) {
            Item seed = ((MixinBlockCropsSeed) (Object) crops).aqtweaks$getSeed();
            if (seed == null) return ItemStack.EMPTY;
            return new ItemStack(seed);
        }
        if (block instanceof BlockNetherWart) return new ItemStack(Items.NETHER_WART);
        if (block instanceof BlockCocoa) return new ItemStack(Items.DYE, 1, 3);
        if (block instanceof BlockMelon) return new ItemStack(Items.MELON_SEEDS);
        if (block instanceof BlockPumpkin) return new ItemStack(Items.PUMPKIN_SEEDS);
        return ItemStack.EMPTY;
    }

    private static boolean isLog(IBlockState state, List<ItemStack> drops) {
        if (state.getBlock() instanceof BlockLog) return true;
        for (ItemStack stack : drops) {
            if (hasOre(stack, "logWood")) return true;
        }
        return hasOre(new ItemStack(state.getBlock(), 1, state.getBlock().damageDropped(state)), "logWood");
    }

    private static boolean isLeaves(IBlockState state) {
        if (state.getBlock() instanceof BlockLeaves) return true;
        return hasOre(new ItemStack(state.getBlock(), 1, state.getBlock().damageDropped(state)), "treeLeaves");
    }

    private static boolean isSapling(ItemStack stack) {
        return hasOre(stack, "treeSapling");
    }

    private static boolean isOreStack(ItemStack stack) {
        if (stack.getItem() == Item.getItemFromBlock(Blocks.DIAMOND_ORE)
                || stack.getItem() == Item.getItemFromBlock(Blocks.EMERALD_ORE)
                || stack.getItem() == Item.getItemFromBlock(Blocks.COAL_ORE)
                || stack.getItem() == Item.getItemFromBlock(Blocks.IRON_ORE)
                || stack.getItem() == Item.getItemFromBlock(Blocks.GOLD_ORE)
                || stack.getItem() == Item.getItemFromBlock(Blocks.LAPIS_ORE)
                || stack.getItem() == Item.getItemFromBlock(Blocks.REDSTONE_ORE)
                || stack.getItem() == Item.getItemFromBlock(Blocks.QUARTZ_ORE)) {
            return true;
        }
        int[] ids = OreDictionary.getOreIDs(stack);
        for (int id : ids) {
            String name = OreDictionary.getOreName(id);
            if (name != null && name.startsWith("ore")) return true;
        }
        return false;
    }

    private static void addOne(List<ItemStack> drops) {
        for (ItemStack stack : drops) {
            if (stack.isEmpty()) continue;
            if (stack.getCount() < stack.getMaxStackSize()) {
                stack.grow(1);
                return;
            }
        }
    }

    private static void addOneOreName(List<ItemStack> drops, String ore) {
        for (ItemStack stack : drops) {
            if (!hasOre(stack, ore)) continue;
            if (stack.getCount() < stack.getMaxStackSize()) {
                stack.grow(1);
                return;
            }
        }
    }

    private static boolean containsItem(List<ItemStack> drops, Item item) {
        for (ItemStack stack : drops) {
            if (stack.getItem() == item) return true;
        }
        return false;
    }

    private static boolean hasOre(ItemStack stack, String ore) {
        if (stack == null || stack.isEmpty()) return false;
        int[] ids = OreDictionary.getOreIDs(stack);
        for (int id : ids) {
            if (ore.equals(OreDictionary.getOreName(id))) return true;
        }
        return false;
    }

    public static void markFreeBreak(EntityPlayer player, IBlockState state) {
        PerkDurability.mark(player, state);
    }

    public static boolean skipDurability(EntityPlayer player, ItemStack stack, IBlockState broken) {
        return PerkDurability.skip(player, stack, broken);
    }

    /** Kept so orchard right-click can share the farming roll. */
    public static boolean orchardRoll(World world, EntityPlayer player) {
        return PerkAccess.on(player, "aqtweaks:orchard",
                ArcanaQuestTweaksConfig.ReskillableModuleConfig.perks.orchard.enable)
                && ReskillableBonuses.roll(world, player, "farming", BONUS);
    }

    public static BlockPos feet(EntityPlayer player) {
        return new BlockPos(player.posX, player.posY, player.posZ);
    }
}
