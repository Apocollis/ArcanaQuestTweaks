package com.apocollis.aqtweaks.simpledifficulty;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.util.Reflect;
import com.charles445.simpledifficulty.api.SDItems;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Water Collector glass-bottle fill. Registered only when Simple Difficulty is loaded.
 */
public class SimpleDifficultyModule {

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        EntityPlayer player = event.getEntityPlayer();
        if (player == null || player instanceof FakePlayer) return;
        World world = event.getWorld();
        if (world == null) return;
        ItemStack held = event.getItemStack();
        if (held.isEmpty() || held.getItem() != Items.GLASS_BOTTLE) return;
        if (!ArcanaQuestTweaksConfig.ReskillableModuleConfig.perks.waterCollector.enable) return;
        if (!Reflect.hasUnlockable(player, "aqtweaks:water_collector")) return;
        RayTraceResult hit = rayWater(world, player);
        if (hit == null || hit.typeOfHit != RayTraceResult.Type.BLOCK) return;
        BlockPos pos = hit.getBlockPos();
        if (world.getBlockState(pos).getMaterial() != Material.WATER) return;
        event.setCanceled(true);
        event.setCancellationResult(EnumActionResult.SUCCESS);
        if (world.isRemote) return;
        world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.ITEM_BOTTLE_FILL,
                SoundCategory.NEUTRAL, 1.0f, 1.0f);
        if (!player.capabilities.isCreativeMode) {
            held.shrink(1);
        }
        ItemStack purified = new ItemStack(SDItems.purifiedWaterBottle);
        if (held.isEmpty()) {
            player.setHeldItem(event.getHand(), purified);
        } else if (!player.inventory.addItemStackToInventory(purified)) {
            player.dropItem(purified, false);
        }
        EnumHand hand = event.getHand();
        if (hand != null) player.swingArm(hand);
    }

    private static RayTraceResult rayWater(World world, EntityPlayer player) {
        float reach = (float) player.getEntityAttribute(EntityPlayer.REACH_DISTANCE).getAttributeValue();
        Vec3d start = player.getPositionEyes(1.0f);
        Vec3d look = player.getLook(1.0f);
        Vec3d end = start.add(look.x * reach, look.y * reach, look.z * reach);
        return world.rayTraceBlocks(start, end, true, false, false);
    }
}
