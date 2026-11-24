package com.apocollis.aqtweaks.potion;

import java.util.UUID;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.monster.EntityWitch;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.entity.projectile.EntityLlamaSpit;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.util.DamageSource;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * What Stun blocks: item use, direct (melee) attacks, launching projectiles, and a witch drinking. Projectiles
 * already in flight are untouched. Registered on both sides so a stunned player's client agrees with the server.
 */
public class StunHandler {
    /** Vanilla's witch drinking speed penalty, removed when we cut a drink short. */
    private static final UUID WITCH_DRINK_MODIFIER = UUID.fromString("5CD17E52-A79A-43D3-A529-90FDE04B181E");

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onUseStart(LivingEntityUseItemEvent.Start event) {
        if (Stun.isStunned(event.getEntityLiving())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (Stun.isStunned(event.getEntityPlayer())) {
            event.setCanceled(true);
        }
    }

    /** Doors, chests and the like still work; the held item does not. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (Stun.isStunned(event.getEntityPlayer())) {
            event.setUseItem(Event.Result.DENY);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!event.getItemStack().isEmpty() && Stun.isStunned(event.getEntityPlayer())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPlayerAttack(AttackEntityEvent event) {
        if (Stun.isStunned(event.getEntityPlayer())) {
            event.setCanceled(true);
        }
    }

    /** A direct hit by the stunned entity (melee). Thorns and indirect damage are not its action. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onAttack(LivingAttackEvent event) {
        DamageSource source = event.getSource();
        Entity attacker = source.getTrueSource();
        if (!(attacker instanceof EntityLivingBase) || source.getImmediateSource() != attacker) return;
        if ("thorns".equals(source.getDamageType())) return;
        if (Stun.isStunned((EntityLivingBase) attacker)) {
            event.setCanceled(true);
        }
    }

    /** A new projectile whose owner is stunned never spawns. Projectiles already flying are not recalled. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onProjectileJoin(EntityJoinWorldEvent event) {
        Entity e = event.getEntity();
        if (e.world.isRemote || e.ticksExisted != 0) return;
        Entity owner = ownerOf(e);
        if (owner instanceof EntityLivingBase && Stun.isStunned((EntityLivingBase) owner)) {
            event.setCanceled(true);
        }
    }

    private static Entity ownerOf(Entity e) {
        if (e instanceof EntityArrow) return ((EntityArrow) e).shootingEntity;
        if (e instanceof EntityThrowable) return ((EntityThrowable) e).getThrower();
        if (e instanceof EntityFireball) return ((EntityFireball) e).shootingEntity;
        if (e instanceof EntityLlamaSpit) return ((EntityLlamaSpit) e).owner;
        return null;
    }

    /**
     * A witch only applies its potion if it is still drinking next tick, so while stunned the drink is cut off
     * every tick (and the speed penalty it adds for drinking is lifted).
     */
    @SubscribeEvent
    public void onWitchUpdate(LivingEvent.LivingUpdateEvent event) {
        EntityLivingBase living = event.getEntityLiving();
        if (!(living instanceof EntityWitch) || living.world.isRemote) return;
        EntityWitch witch = (EntityWitch) living;
        if (!witch.isDrinkingPotion() || !Stun.isStunned(witch)) return;
        witch.setDrinkingPotion(false);
        ItemStack held = witch.getHeldItemMainhand();
        if (held.getItem() == Items.POTIONITEM) {
            witch.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, ItemStack.EMPTY);
        }
        IAttributeInstance speed = witch.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(WITCH_DRINK_MODIFIER);
        }
    }
}
