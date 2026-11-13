package com.apocollis.aqtweaks.paragon;

import java.util.EnumSet;
import java.util.List;

import com.shiroroku.theaurorian.Util.AurorianSteelHelper;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.UseHoeEvent;
import net.minecraftforge.event.world.BlockEvent;

/**
 * Aurorian Steel: the item levels up with use and gains enchantment levels, through The Aurorian's own helper, which only
 * reads and writes the stack's {@code currentupgradelevel} and {@code upgrademultiplier} tags and its enchantments.
 */
public class AurorianSteelParagonTrait extends ParagonTrait {
    public AurorianSteelParagonTrait() {
        super("aurorian_steel", "blockAurorianSteel", "theaurorian", EnumSet.allOf(TartariteKind.class));
    }

    @Override
    public void onAbsorbed(ItemStack stack, TartariteKind kind) {
        AurorianSteelHelper.checkNbt(stack);
    }

    @Override
    public void onRemoved(ItemStack stack) {
        if (stack.hasTagCompound()) {
            stack.getTagCompound().removeTag("currentupgradelevel");
            stack.getTagCompound().removeTag("upgrademultiplier");
        }
    }

    private static void level(ItemStack stack, EntityLivingBase user) {
        AurorianSteelHelper.handleAurorianSteelDurability(stack, user.world, user);
    }

    @Override
    public void onBlockBreak(BlockEvent.BreakEvent event, EntityPlayer player, ItemStack stack, TartariteKind kind) {
        if (kind.isTool()) level(stack, player);
    }

    @Override
    public void onMeleeHit(LivingHurtEvent event, EntityPlayer player, EntityLivingBase target, ItemStack stack,
                           TartariteKind kind) {
        if (kind == TartariteKind.SWORD || kind == TartariteKind.AXE) level(stack, player);
    }

    @Override
    public void onArmorDamaged(LivingDamageEvent event, EntityLivingBase wearer, ItemStack stack, TartariteKind kind) {
        level(stack, wearer);
    }

    @Override
    public void onUseHoe(UseHoeEvent event, EntityPlayer player, ItemStack stack) {
        level(stack, player);
    }

    @Override
    public void addTooltip(ItemStack stack, TartariteKind kind, World world, List<String> tooltip, ITooltipFlag flag) {
        AurorianSteelHelper.getAurorianSteelInfo(stack, world, tooltip, flag);
    }
}
