package com.apocollis.aqtweaks.paragon;

import java.util.EnumSet;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.gildedgames.the_aether.entities.block.EntityFloatingBlock;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

/**
 * Gravitite, after the Aether's pieces: the sword throws its target upward, the pickaxe, axe and shovel lift the block
 * they are used on, and a full set of four armor pieces gives an extra high jump.
 */
public class GravititeParagonTrait extends ParagonTrait {
    public GravititeParagonTrait() {
        super("gravitite", "blockEnchantedGravitite", "aether_legacy", EnumSet.of(TartariteKind.SWORD,
                TartariteKind.PICKAXE, TartariteKind.AXE, TartariteKind.SHOVEL, TartariteKind.HELMET,
                TartariteKind.CHESTPLATE, TartariteKind.LEGGINGS, TartariteKind.BOOTS));
    }

    @Override
    public void onMeleeHit(LivingHurtEvent event, EntityPlayer player, EntityLivingBase target, ItemStack stack,
                           TartariteKind kind) {
        if (kind != TartariteKind.SWORD) return;
        double speed = ArcanaQuestTweaksConfig.ParagonModuleConfig.traits.gravititeLaunchSpeed;
        if (speed > 0.0D) ParagonEventHandler.queueLaunch(target, speed);
    }

    @Override
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event, EntityPlayer player, ItemStack stack,
                                  TartariteKind kind) {
        if (kind != TartariteKind.PICKAXE && kind != TartariteKind.AXE && kind != TartariteKind.SHOVEL) return;
        World world = event.getWorld();
        BlockPos pos = event.getPos();
        if (!ForgeHooks.isToolEffective(world, pos, stack) || !world.isAirBlock(pos.up())) return;
        IBlockState state = world.getBlockState(pos);
        if (world.getTileEntity(pos) != null || state.getBlock().getBlockHardness(state, world, pos) == -1.0F
                || !world.isBlockModifiable(player, pos) || !player.canPlayerEdit(pos, event.getFace(), stack)) {
            event.setCanceled(true);
            event.setCancellationResult(EnumActionResult.FAIL);
            return;
        }
        world.spawnEntity(new EntityFloatingBlock(world, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, state));
        stack.damageItem(ArcanaQuestTweaksConfig.ParagonModuleConfig.traits.gravititeFloatDurability, player);
        event.setCanceled(true);
        event.setCancellationResult(EnumActionResult.SUCCESS);
    }

    @Override
    public void onSetJump(LivingEvent.LivingJumpEvent event, EntityPlayer player) {
        double speed = ArcanaQuestTweaksConfig.ParagonModuleConfig.traits.gravititeSetJumpSpeed;
        if (speed > player.motionY) player.motionY = speed;
    }
}
