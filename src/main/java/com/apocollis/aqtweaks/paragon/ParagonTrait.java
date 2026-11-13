package com.apocollis.aqtweaks.paragon;

import java.util.EnumSet;
import java.util.List;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.UseHoeEvent;
import net.minecraftforge.event.world.BlockEvent;

/**
 * A property a Tartarite item takes from another metal. A trait overrides only the hooks it needs; the dispatcher
 * ({@link ParagonEventHandler}) reads the stack once and calls the trait of the stack, so these run only for items that
 * carry it.
 */
public abstract class ParagonTrait {
    private final String id;
    private final String oreName;
    private final String modId;
    private final EnumSet<TartariteKind> kinds;

    protected ParagonTrait(String id, String oreName, String modId, EnumSet<TartariteKind> kinds) {
        this.id = id;
        this.oreName = oreName;
        this.modId = modId;
        this.kinds = kinds;
    }

    /** Stored in the stack's {@code arcana_paragon} tag and used in the config and the language keys. */
    public final String getId() {
        return id;
    }

    /** Ore dictionary name of the metal block the four-block ring uses. */
    public final String getOreName() {
        return oreName;
    }

    /** The mod this metal comes from; the trait registers, and its recipes load, only when it is present. */
    public final String getModId() {
        return modId;
    }

    public final boolean appliesTo(TartariteKind kind) {
        return kinds.contains(kind);
    }

    public final EnumSet<TartariteKind> getKinds() {
        return kinds;
    }

    /** True when the trait needs a periodic check of what the player wears (it adds an attribute, for example). */
    public boolean tracksWearer() {
        return false;
    }

    public void onAbsorbed(ItemStack stack, TartariteKind kind) {}

    public void onRemoved(ItemStack stack) {}

    /** Server: the holder broke a block with the stack in hand. */
    public void onBlockBreak(BlockEvent.BreakEvent event, EntityPlayer player, ItemStack stack, TartariteKind kind) {}

    /** Both sides: mining speed against a block. */
    public void onBreakSpeed(PlayerEvent.BreakSpeed event, EntityPlayer player, ItemStack stack, TartariteKind kind) {}

    /** Server: the holder is about to hurt {@code target} with a melee hit of the stack. */
    public void onMeleeHit(LivingHurtEvent event, EntityPlayer player, EntityLivingBase target, ItemStack stack,
                           TartariteKind kind) {}

    /** Server: the wearer took damage while wearing the stack. */
    public void onArmorDamaged(LivingDamageEvent event, EntityLivingBase wearer, ItemStack stack, TartariteKind kind) {}

    /** Server, from time to time: bring the trait's effect on the wearer up to date. */
    public void syncWearer(EntityPlayer player) {}

    /** Server: a right click on a block with the stack in the main hand. */
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event, EntityPlayer player, ItemStack stack,
                                  TartariteKind kind) {}

    public void onUseHoe(UseHoeEvent event, EntityPlayer player, ItemStack stack) {}

    /** The player wears four pieces that all carry this trait and jumped. */
    public void onSetJump(LivingEvent.LivingJumpEvent event, EntityPlayer player) {}

    /** Client: trait lines for the tooltip, after the standard header. */
    public void addTooltip(ItemStack stack, TartariteKind kind, World world, List<String> tooltip, ITooltipFlag flag) {}
}
