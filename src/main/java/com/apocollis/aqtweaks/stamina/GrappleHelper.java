package com.apocollis.aqtweaks.stamina;

import com.yyon.grapplinghook.GrappleCustomization;
import com.yyon.grapplinghook.controllers.grappleController;
import com.yyon.grapplinghook.entities.grappleArrow;
import com.yyon.grapplinghook.grapplemod;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Loader;

import java.util.HashSet;

/**
 * Compile-hard Grappling Hook access (formerly reflection in {@code Reflect}). Every method is a
 * no-op when the mod is absent; callers on always-on classes (StaminaModule) go through here and the
 * guard keeps the Grappling Hook classes from being touched, so the pack does not need the mod to boot.
 */
public final class GrappleHelper {

    private static final boolean LOADED = Loader.isModLoaded("grapplemod");

    private GrappleHelper() {}

    public static boolean isLoaded() {
        return LOADED;
    }

    /** The player's live controller, or null. */
    public static grappleController controller(EntityPlayer player) {
        if (!LOADED || player == null) return null;
        return grapplemod.controllers.get(player.getEntityId());
    }

    public static GrappleCustomization customization(EntityPlayer player) {
        grappleController controller = controller(player);
        return controller != null ? controller.custom : null;
    }

    public static boolean isGrappling(EntityPlayer player) {
        if (!LOADED || player == null) return false;
        int id = player.getEntityId();
        if (grapplemod.attached.contains(id)) return true;
        grappleController controller = grapplemod.controllers.get(id);
        return controller != null && controller.attached;
    }

    /** Release the player's hook: controller first, else the server-side end-of-grapple message. */
    public static void detach(EntityPlayer player) {
        if (!LOADED || player == null) return;
        grappleController controller = grapplemod.controllers.get(player.getEntityId());
        if (controller != null) {
            controller.unattach();
            return;
        }
        World world = player.world;
        if (world == null) return;
        HashSet<Integer> arrowIds = new HashSet<>();
        HashSet<grappleArrow> arrows = grapplemod.allarrows.get(player.getEntityId());
        if (arrows != null) {
            for (Entity arrow : arrows) {
                arrowIds.add(arrow.getEntityId());
            }
        }
        grapplemod.receiveGrappleEnd(player.getEntityId(), world, arrowIds);
    }
}
