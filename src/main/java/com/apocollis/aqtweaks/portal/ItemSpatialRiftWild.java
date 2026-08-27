package com.apocollis.aqtweaks.portal;

import com.apocollis.aqtweaks.ArcanaQuestTweaks;
import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig.PortalModuleConfig;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

public class ItemSpatialRiftWild extends Item {

    public ItemSpatialRiftWild() {
        setRegistryName(ArcanaQuestTweaks.MODID, "spatial_rift_wild");
        setTranslationKey("aqtweaks.spatial_rift_wild");
        setMaxStackSize(1);
        setCreativeTab(CreativeTabs.MISC);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (!PortalModuleConfig.general.enable) {
            return new ActionResult<>(EnumActionResult.FAIL, stack);
        }
        RayTraceResult hit = rayTrace(world, player, false);
        if (hit != null && hit.typeOfHit == RayTraceResult.Type.BLOCK) {
            return new ActionResult<>(EnumActionResult.PASS, stack);
        }
        if (world.isRemote) {
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }
        BlockPos dest = PortalModule.findRandomLand(world, player);
        if (dest == null) {
            player.sendStatusMessage(new TextComponentTranslation("item.aqtweaks.spatial_rift_wild.no_land"), true);
            return new ActionResult<>(EnumActionResult.FAIL, stack);
        }
        if (!PortalModule.spawnLinkedRifts(world, player, dest, true)) {
            player.sendStatusMessage(new TextComponentTranslation("item.aqtweaks.spatial_rift_tear.failed"), true);
            return new ActionResult<>(EnumActionResult.FAIL, stack);
        }
        if (!player.capabilities.isCreativeMode) {
            stack.shrink(1);
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, World world, List<String> tooltip, ITooltipFlag flag) {
        tooltip.add(I18n.format("item.aqtweaks.spatial_rift_wild.lore"));
        if (GuiScreen.isShiftKeyDown()) {
            tooltip.add(I18n.format("item.aqtweaks.spatial_rift_wild.shift"));
        } else {
            tooltip.add(I18n.format("item.aqtweaks.spatial_rift_wild.shift_hint"));
        }
    }
}
