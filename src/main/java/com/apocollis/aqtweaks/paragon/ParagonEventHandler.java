package com.apocollis.aqtweaks.paragon;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.SPacketEntityVelocity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.UseHoeEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.ItemCraftedEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Hands each game event to the trait of the stack involved. Every hook returns at once for an item that is not one of the
 * nine Tartarite items, and reads the stack's tag only once for one that is.
 */
public class ParagonEventHandler {
    private static final EntityEquipmentSlot[] ARMOR = {EntityEquipmentSlot.HEAD, EntityEquipmentSlot.CHEST,
            EntityEquipmentSlot.LEGS, EntityEquipmentSlot.FEET};
    private static final int SYNC_INTERVAL = 20;

    /** Launches asked for during a hit, applied after the hit's own knockback has run. */
    private static final List<Object[]> LAUNCHES = new ArrayList<>();

    /** Queue an upward push for after the current hit's knockback. Server thread only. */
    public static void queueLaunch(EntityLivingBase target, double speed) {
        LAUNCHES.add(new Object[] {target, speed});
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        EntityPlayer player = event.getPlayer();
        if (player == null || player.world.isRemote) return;
        ItemStack stack = player.getHeldItemMainhand();
        ParagonTrait trait = ParagonRegistry.traitOf(stack);
        if (trait != null) trait.onBlockBreak(event, player, stack, ParagonRegistry.kindOf(stack));
    }

    @SubscribeEvent
    public void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        EntityPlayer player = event.getEntityPlayer();
        ItemStack stack = player.getHeldItemMainhand();
        ParagonTrait trait = ParagonRegistry.traitOf(stack);
        if (trait != null) trait.onBreakSpeed(event, player, stack, ParagonRegistry.kindOf(stack));
    }

    @SubscribeEvent
    public void onHurt(LivingHurtEvent event) {
        DamageSource source = event.getSource();
        if (!(source.getTrueSource() instanceof EntityPlayer) || source.getImmediateSource() != source.getTrueSource()
                || !"player".equals(source.getDamageType())) {
            return;
        }
        EntityPlayer player = (EntityPlayer) source.getTrueSource();
        if (player.world.isRemote) return;
        ItemStack stack = player.getHeldItemMainhand();
        ParagonTrait trait = ParagonRegistry.traitOf(stack);
        if (trait == null) return;
        TartariteKind kind = ParagonRegistry.kindOf(stack);
        if (kind.isHandheld()) trait.onMeleeHit(event, player, event.getEntityLiving(), stack, kind);
    }

    @SubscribeEvent
    public void onDamaged(LivingDamageEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer) || event.getEntityLiving().world.isRemote) return;
        EntityPlayer wearer = (EntityPlayer) event.getEntityLiving();
        for (EntityEquipmentSlot slot : ARMOR) {
            ItemStack stack = wearer.getItemStackFromSlot(slot);
            ParagonTrait trait = ParagonRegistry.traitOf(stack);
            if (trait == null) continue;
            TartariteKind kind = ParagonRegistry.kindOf(stack);
            if (kind.isArmor()) trait.onArmorDamaged(event, wearer, stack, kind);
        }
    }

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != EnumHand.MAIN_HAND || event.getWorld().isRemote) return;
        EntityPlayer player = event.getEntityPlayer();
        ItemStack stack = player.getHeldItemMainhand();
        ParagonTrait trait = ParagonRegistry.traitOf(stack);
        if (trait != null) trait.onRightClickBlock(event, player, stack, ParagonRegistry.kindOf(stack));
    }

    @SubscribeEvent
    public void onUseHoe(UseHoeEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        if (player.world.isRemote) return;
        ItemStack stack = event.getCurrent();
        ParagonTrait trait = ParagonRegistry.traitOf(stack);
        if (trait != null) trait.onUseHoe(event, player, stack);
    }

    @SubscribeEvent
    public void onJump(LivingEvent.LivingJumpEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        ParagonTrait trait = ParagonRegistry.fullSetTrait(player);
        if (trait != null) trait.onSetJump(event, player);
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.world.isRemote
                || event.player.ticksExisted % SYNC_INTERVAL != 0) {
            return;
        }
        List<ParagonTrait> traits = ParagonRegistry.wearerTraits();
        for (int i = 0; i < traits.size(); i++) {
            traits.get(i).syncWearer(event.player);
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || LAUNCHES.isEmpty()) return;
        for (Object[] launch : LAUNCHES) {
            EntityLivingBase target = (EntityLivingBase) launch[0];
            if (target.isDead) continue;
            target.motionY += (Double) launch[1];
            target.velocityChanged = true;
            if (target instanceof EntityPlayerMP) {
                ((EntityPlayerMP) target).connection.sendPacket(new SPacketEntityVelocity(target));
            }
        }
        LAUNCHES.clear();
    }

    /** Metallurgy's own Paragon wins when both recipes could have matched; it takes our trait off the result. */
    @SubscribeEvent
    public void onCrafted(ItemCraftedEvent event) {
        ItemStack out = event.crafting;
        if (ParagonRegistry.kindOf(out) == null || !ParagonRegistry.hasMetallurgyParagon(out)) return;
        String id = ParagonRegistry.storedId(out);
        if (id == null) return;
        ParagonTrait trait = ParagonRegistry.get(id);
        if (trait != null) trait.onRemoved(out);
        out.getTagCompound().removeTag(ParagonRegistry.NBT_KEY);
    }
}
