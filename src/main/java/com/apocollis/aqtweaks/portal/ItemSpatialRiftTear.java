package com.apocollis.aqtweaks.portal;

import com.apocollis.aqtweaks.ArcanaQuestTweaks;
import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig.PortalModuleConfig;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

public class ItemSpatialRiftTear extends Item {

    public static final String TAG_X = "BoundX";
    public static final String TAG_Y = "BoundY";
    public static final String TAG_Z = "BoundZ";
    public static final String TAG_DIM = "BoundDim";
    public static final String TAG_BOUND = "Bound";

    public ItemSpatialRiftTear() {
        setRegistryName(ArcanaQuestTweaks.MODID, "spatial_rift_tear");
        setTranslationKey("aqtweaks.spatial_rift_tear");
        setMaxStackSize(1);
        setCreativeTab(CreativeTabs.MISC);
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand,
            EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (!player.isSneaking() || !PortalModuleConfig.general.enable) {
            return EnumActionResult.PASS;
        }
        if (!world.isRemote) {
            bind(player.getHeldItem(hand), player, pos.up(), world.provider.getDimension());
        }
        return EnumActionResult.SUCCESS;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (!PortalModuleConfig.general.enable) {
            return new ActionResult<>(EnumActionResult.FAIL, stack);
        }
        if (player.isSneaking()) {
            if (!world.isRemote) {
                BlockPos feet = new BlockPos(player.posX, Math.floor(player.posY), player.posZ);
                bind(stack, player, feet, world.provider.getDimension());
            }
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }
        RayTraceResult hit = rayTrace(world, player, false);
        if (hit != null && hit.typeOfHit == RayTraceResult.Type.BLOCK) {
            return new ActionResult<>(EnumActionResult.PASS, stack);
        }
        if (world.isRemote) {
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }
        if (!isBound(stack)) {
            BlockPos feet = new BlockPos(player.posX, Math.floor(player.posY), player.posZ);
            bind(stack, player, feet, world.provider.getDimension());
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }
        int dim = stack.getTagCompound().getInteger(TAG_DIM);
        World destWorld = player.getServer() != null ? player.getServer().getWorld(dim) : null;
        if (destWorld == null) {
            player.sendStatusMessage(new TextComponentTranslation("item.aqtweaks.spatial_rift_tear.missing_dim"), true);
            return new ActionResult<>(EnumActionResult.FAIL, stack);
        }
        BlockPos dest = new BlockPos(
                stack.getTagCompound().getInteger(TAG_X),
                stack.getTagCompound().getInteger(TAG_Y),
                stack.getTagCompound().getInteger(TAG_Z));
        if (!PortalModule.spawnLinkedRifts(world, destWorld, player, dest, false)) {
            player.sendStatusMessage(new TextComponentTranslation("item.aqtweaks.spatial_rift_tear.failed"), true);
            return new ActionResult<>(EnumActionResult.FAIL, stack);
        }
        if (!player.capabilities.isCreativeMode) {
            stack.shrink(1);
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        if (isBound(stack)) {
            return net.minecraft.util.text.translation.I18n.translateToLocal(
                    "item.aqtweaks.spatial_rift_tear.name_linked");
        }
        return super.getItemStackDisplayName(stack);
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return isBound(stack);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, World world, List<String> tooltip, ITooltipFlag flag) {
        tooltip.add(I18n.format("item.aqtweaks.spatial_rift_tear.lore"));
        if (!isBound(stack)) {
            tooltip.add(I18n.format("item.aqtweaks.spatial_rift_tear.unbound"));
            return;
        }
        NBTTagCompound tag = stack.getTagCompound();
        tooltip.add(net.minecraft.util.text.TextFormatting.LIGHT_PURPLE + I18n.format(
                "item.aqtweaks.spatial_rift_tear.bound",
                tag.getInteger(TAG_X), tag.getInteger(TAG_Y), tag.getInteger(TAG_Z), tag.getInteger(TAG_DIM)));
        if (GuiScreen.isShiftKeyDown()) {
            tooltip.add(I18n.format("item.aqtweaks.spatial_rift_tear.shift"));
        } else {
            tooltip.add(I18n.format("item.aqtweaks.spatial_rift_tear.shift_hint"));
        }
    }

    public static boolean isBound(ItemStack stack) {
        return stack.hasTagCompound() && stack.getTagCompound().getBoolean(TAG_BOUND);
    }

    static void bind(ItemStack stack, EntityPlayer player, BlockPos pos, int dim) {
        NBTTagCompound tag = stack.hasTagCompound() ? stack.getTagCompound() : new NBTTagCompound();
        tag.setBoolean(TAG_BOUND, true);
        tag.setInteger(TAG_X, pos.getX());
        tag.setInteger(TAG_Y, pos.getY());
        tag.setInteger(TAG_Z, pos.getZ());
        tag.setInteger(TAG_DIM, dim);
        stack.setTagCompound(tag);
        player.world.playSound(null, player.getPosition(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                SoundCategory.PLAYERS, 0.7F, 1.2F);
        player.sendMessage(new TextComponentTranslation(
                "item.aqtweaks.spatial_rift_tear.attuned", pos.getX(), pos.getY(), pos.getZ()));
    }
}
