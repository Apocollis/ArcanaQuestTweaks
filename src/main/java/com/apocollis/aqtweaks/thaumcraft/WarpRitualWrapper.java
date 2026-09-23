package com.apocollis.aqtweaks.thaumcraft;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;

import com.apocollis.aqtweaks.util.Reflect;

import com.bewitchment.api.registry.Ritual;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.ItemStackHandler;
import java.util.List;
import java.util.function.Predicate;

public class WarpRitualWrapper extends Ritual {

    private final Ritual parent;
    private final int warpNormal;
    private final int warpTemp;
    private final int warpPerm;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public WarpRitualWrapper(Ritual parent, int warpNormal, int warpTemp, int warpPerm) {
        super(parent.getRegistryName(), 
              (List<Ingredient>) parent.input, 
              (Predicate) parent.sacrificePredicate, 
              (List<ItemStack>) parent.output, 
              parent.canBePerformedRemotely, 
              parent.startingPower, 
              parent.runningPower, 
              parent.circles[0], 
              parent.circles[1], 
              parent.circles[2], 
              parent.time);
        this.parent = parent;
        this.warpNormal = warpNormal;
        this.warpTemp = warpTemp;
        this.warpPerm = warpPerm;
        this.setRegistryName(parent.getRegistryName());
    }

    @Override
    public boolean isValid(World world, BlockPos pos, EntityPlayer player, ItemStackHandler inventory) {
        return parent.isValid(world, pos, player, inventory);
    }

    @Override
    public void onStarted(World world, BlockPos pos, EntityPlayer player, ItemStackHandler inventory) {
        parent.onStarted(world, pos, player, inventory);
    }

    @Override
    public void onFinished(World world, BlockPos altarPos, BlockPos glyphPos, EntityPlayer player, ItemStackHandler inventory) {
        parent.onFinished(world, altarPos, glyphPos, player, inventory);

        if (player != null && !Reflect.isRemote(world)) {
            boolean added = false;

            if (warpNormal > 0) {
                int n = warpNormal;
                if (Reflect.hasUnlockable(player, "aqtweaks:cold_iron_mind")
                        && ArcanaQuestTweaksConfig.ReskillableModuleConfig.perks.coldIronMind.enable) {
                    n = n / 2;
                }
                if (n > 0) {
                    ThaumcraftHelper.addWarp(player, 0, n);
                    added = true;
                }
            }

            if (warpTemp > 0) {
                int n = warpTemp;
                if (Reflect.hasUnlockable(player, "aqtweaks:cold_iron_mind")
                        && ArcanaQuestTweaksConfig.ReskillableModuleConfig.perks.coldIronMind.enable) {
                    n = n / 2;
                }
                if (n > 0) {
                    ThaumcraftHelper.addWarp(player, 1, n);
                    added = true;
                }
            }

            if (warpPerm > 0) {
                int n = warpPerm;
                if (Reflect.hasUnlockable(player, "aqtweaks:cold_iron_mind")
                        && ArcanaQuestTweaksConfig.ReskillableModuleConfig.perks.coldIronMind.enable) {
                    n = n / 2;
                }
                if (n > 0) {
                    ThaumcraftHelper.addWarp(player, 2, n);
                    added = true;
                }
            }

            if (added) {
                ThaumcraftHelper.syncWarp(player);
            }
        }
    }

    @Override
    public void onHalted(World world, BlockPos altarPos, BlockPos glyphPos, EntityPlayer player, ItemStackHandler inventory) {
        parent.onHalted(world, altarPos, glyphPos, player, inventory);
    }

    @Override
    public void onUpdate(World world, BlockPos altarPos, BlockPos glyphPos, EntityPlayer player, ItemStackHandler inventory) {
        parent.onUpdate(world, altarPos, glyphPos, player, inventory);
    }
}
