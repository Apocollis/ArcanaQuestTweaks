package com.apocollis.aqtweaks.portal;

import com.apocollis.aqtweaks.ArcanaQuestTweaks;
import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig.PortalModuleConfig;

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
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
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
        if (!PortalModuleConfig.general.enable) {
            return EnumActionResult.PASS;
        }
        ItemStack stack = player.getHeldItem(hand);
        if (player.isSneaking()) {
            if (!isBound(stack)) {
                return EnumActionResult.PASS;
            }
            if (!world.isRemote) {
                unbind(stack, player);
            }
            return EnumActionResult.SUCCESS;
        }
        if (world.isRemote) {
            return EnumActionResult.SUCCESS;
        }
        if (!isBound(stack)) {
            bind(stack, player, pos.offset(facing), world.provider.getDimension());
            return EnumActionResult.SUCCESS;
        }
        return tryOpen(player, world, stack) ? EnumActionResult.SUCCESS : EnumActionResult.FAIL;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (!PortalModuleConfig.general.enable) {
            return new ActionResult<>(EnumActionResult.FAIL, stack);
        }
        if (player.isSneaking()) {
            if (!world.isRemote && isBound(stack)) {
                unbind(stack, player);
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
        EnumActionResult result = tryOpen(player, world, stack) ? EnumActionResult.SUCCESS : EnumActionResult.FAIL;
        return new ActionResult<>(result, stack);
    }

    private boolean tryOpen(EntityPlayer player, World world, ItemStack stack) {
        int dim = stack.getTagCompound().getInteger(TAG_DIM);
        World destWorld = player.getServer() != null ? player.getServer().getWorld(dim) : null;
        if (destWorld == null) {
            player.sendStatusMessage(new TextComponentString(
                    PortalLang.format("item.aqtweaks.spatial_rift_tear.missing_dim")), true);
            return false;
        }
        BlockPos dest = new BlockPos(
                stack.getTagCompound().getInteger(TAG_X),
                stack.getTagCompound().getInteger(TAG_Y),
                stack.getTagCompound().getInteger(TAG_Z));
        if (!PortalModule.spawnLinkedRifts(world, destWorld, player, dest, false)) {
            player.sendStatusMessage(new TextComponentString(
                    PortalLang.format("item.aqtweaks.spatial_rift_tear.failed")), true);
            return false;
        }
        if (!player.capabilities.isCreativeMode) {
            stack.shrink(1);
        }
        return true;
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        if (isBound(stack)) {
            return PortalLang.format(PortalLang.TEAR_NAME_LINKED);
        }
        return PortalLang.format(PortalLang.TEAR_NAME);
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return isBound(stack);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, World world, List<String> tooltip, ITooltipFlag flag) {
        tooltip.add(PortalLang.format(PortalLang.TEAR_LORE));
        if (!isBound(stack)) {
            tooltip.add(PortalLang.format(PortalLang.TEAR_UNBOUND));
        } else {
            NBTTagCompound tag = stack.getTagCompound();
            tooltip.add(TextFormatting.LIGHT_PURPLE + PortalLang.format(
                    PortalLang.TEAR_BOUND,
                    tag.getInteger(TAG_X), tag.getInteger(TAG_Y), tag.getInteger(TAG_Z), tag.getInteger(TAG_DIM)));
        }
        tooltip.add(PortalLang.format(PortalLang.TEAR_SHIFT));
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
        player.sendStatusMessage(new TextComponentString(
                PortalLang.format("item.aqtweaks.spatial_rift_tear.attuned", pos.getX(), pos.getY(), pos.getZ())), true);
    }

    static void unbind(ItemStack stack, EntityPlayer player) {
        if (!isBound(stack)) {
            return;
        }
        NBTTagCompound tag = stack.getTagCompound();
        tag.setBoolean(TAG_BOUND, false);
        tag.removeTag(TAG_X);
        tag.removeTag(TAG_Y);
        tag.removeTag(TAG_Z);
        tag.removeTag(TAG_DIM);
        player.world.playSound(null, player.getPosition(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                SoundCategory.PLAYERS, 0.7F, 0.8F);
        player.sendStatusMessage(new TextComponentString(
                PortalLang.format("item.aqtweaks.spatial_rift_tear.cleared")), true);
    }
}
