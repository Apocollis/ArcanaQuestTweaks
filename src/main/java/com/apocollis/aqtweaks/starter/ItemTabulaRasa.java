package com.apocollis.aqtweaks.starter;

import com.apocollis.aqtweaks.ArcanaQuestTweaks;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Tabula Rasa: a draught that wipes Reskillable progress in place and reopens the starter class menu. No Reskillable
 * type is referenced here; the work is in {@link StarterResetService}.
 */
public final class ItemTabulaRasa extends Item {

    public static final String ID = "tabula_rasa";

    public ItemTabulaRasa() {
        setRegistryName(ArcanaQuestTweaks.MODID, ID);
        setTranslationKey(ArcanaQuestTweaks.MODID + "." + ID);
        setMaxStackSize(1);
        setCreativeTab(CreativeTabs.MISC);
    }

    @Override
    public EnumAction getItemUseAction(ItemStack stack) {
        return EnumAction.DRINK;
    }

    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        return 32;
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return true;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (!world.isRemote && !StarterResetService.canStartUse((EntityPlayerMP) player)) {
            return new ActionResult<>(EnumActionResult.FAIL, stack);
        }
        player.setActiveHand(hand);
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    @Override
    public ItemStack onItemUseFinish(ItemStack stack, World world, EntityLivingBase entity) {
        if (world.isRemote || !(entity instanceof EntityPlayerMP player)) return stack;
        if (StarterResetService.applyOnDrinkFinish(player) && !player.capabilities.isCreativeMode) {
            stack.shrink(1);
        }
        return stack;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        tooltip.add(I18n.format("item.aqtweaks.tabula_rasa.lore"));
        tooltip.add(I18n.format("item.aqtweaks.tabula_rasa.lore2"));
    }
}
