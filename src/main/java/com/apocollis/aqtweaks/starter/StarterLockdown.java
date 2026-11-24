package com.apocollis.aqtweaks.starter;

import java.util.UUID;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingSetAttackTargetEvent;
import net.minecraftforge.event.entity.living.PotionEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Server-side session lockdown for a player who has not yet chosen a starter class (plan 12.3). Not saved, so a
 * crash cannot leave it on. {@link #isLocked(EntityPlayer)} is the one predicate the tick handlers of other modules
 * call; it names no parent mod types so always-on classes may use it.
 */
public final class StarterLockdown {

    private static final LockdownSet SET = new LockdownSet();

    private StarterLockdown() {}

    public static boolean isLocked(EntityPlayer player) {
        return player != null && !player.world.isRemote && SET.contains(player.getUniqueID());
    }

    public static boolean isLocked(UUID id) {
        return SET.contains(id);
    }

    public static void lock(EntityPlayer player) {
        lockAt(player.getUniqueID(), player.posX, player.posY, player.posZ);
    }

    /** Locks by id with the position the player is held at (also what the tests use). */
    public static void lockAt(UUID id, double x, double y, double z) {
        SET.lock(id, x, y, z);
    }

    public static void unlock(UUID id) {
        SET.unlock(id);
    }

    /** Test and shutdown helper. */
    public static void clear() {
        SET.clear();
    }

    public static int size() {
        return SET.size();
    }

    private static boolean locked(EntityLivingBase entity) {
        return entity instanceof EntityPlayer && isLocked((EntityPlayer) entity);
    }

    /** Event handlers; registered on the Forge bus by {@link StarterModule}. */
    public static final class Events {

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public void onAttacked(LivingAttackEvent event) {
            if (locked(event.getEntityLiving())) event.setCanceled(true);
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public void onHurt(LivingHurtEvent event) {
            if (locked(event.getEntityLiving())) event.setCanceled(true);
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public void onDamage(LivingDamageEvent event) {
            if (locked(event.getEntityLiving())) event.setCanceled(true);
        }

        @SubscribeEvent
        public void onTarget(LivingSetAttackTargetEvent event) {
            EntityLivingBase target = event.getTarget();
            if (target != null && locked(target) && event.getEntityLiving() instanceof EntityLiving) {
                ((EntityLiving) event.getEntityLiving()).setAttackTarget(null);
            }
        }

        @SubscribeEvent
        public void onPotion(PotionEvent.PotionApplicableEvent event) {
            if (locked(event.getEntityLiving())) event.setResult(Event.Result.DENY);
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public void onAttackEntity(AttackEntityEvent event) {
            if (isLocked(event.getEntityPlayer())) event.setCanceled(true);
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public void onInteract(PlayerInteractEvent event) {
            if (event.isCancelable() && isLocked(event.getEntityPlayer())) event.setCanceled(true);
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public void onBreak(BlockEvent.BreakEvent event) {
            if (isLocked(event.getPlayer())) event.setCanceled(true);
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public void onPlace(BlockEvent.PlaceEvent event) {
            if (isLocked(event.getPlayer())) event.setCanceled(true);
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public void onPickup(EntityItemPickupEvent event) {
            if (isLocked(event.getEntityPlayer())) event.setCanceled(true);
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public void onToss(ItemTossEvent event) {
            if (isLocked(event.getPlayer())) {
                // A cancelled toss would lose the stack: put it back first.
                event.getPlayer().inventory.addItemStackToInventory(event.getEntityItem().getItem());
                event.setCanceled(true);
            }
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public void onUseStart(LivingEntityUseItemEvent.Start event) {
            if (locked(event.getEntityLiving())) event.setCanceled(true);
        }

        @SubscribeEvent
        public void onTick(TickEvent.PlayerTickEvent event) {
            if (event.phase != TickEvent.Phase.START || !isLocked(event.player)) return;
            EntityPlayer p = event.player;
            p.setHealth(p.getMaxHealth());
            p.setAir(300);
            p.fallDistance = 0.0F;
            p.extinguish();
            p.getFoodStats().addStats(20, 1.0F);
            if (!p.getActivePotionEffects().isEmpty()) {
                p.clearActivePotions();
            }
            if (p instanceof EntityPlayerMP) {
                EntityPlayerMP mp = (EntityPlayerMP) p;
                if (mp.openContainer != mp.inventoryContainer) {
                    mp.closeContainer();
                }
                double[] a = SET.anchor(mp.getUniqueID());
                if (a != null && mp.connection != null) {
                    double dx = mp.posX - a[0];
                    double dy = mp.posY - a[1];
                    double dz = mp.posZ - a[2];
                    if (dx * dx + dy * dy + dz * dz > 0.04D) {
                        mp.connection.setPlayerLocation(a[0], a[1], a[2], mp.rotationYaw, mp.rotationPitch);
                    }
                }
                mp.motionX = 0.0D;
                mp.motionY = 0.0D;
                mp.motionZ = 0.0D;
            }
        }
    }
}
