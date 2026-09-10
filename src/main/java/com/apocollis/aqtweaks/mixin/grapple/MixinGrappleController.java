package com.apocollis.aqtweaks.mixin.grapple;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;

import com.apocollis.aqtweaks.stamina.EmberMotorHelper;

import com.apocollis.aqtweaks.util.Reflect;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.lang.reflect.Field;

@Mixin(targets = "com.yyon.grapplinghook.controllers.grappleController", remap = false)
public abstract class MixinGrappleController {

    private static final Logger AQTWEAKS$LOGGER = LogManager.getLogger("AQTweaks-Stamina");

    // This Redirect runs on every client movement tick, so the field handles are resolved once.
    // A lookup that fails is never retried, which is why the miss has to be logged.
    @Unique
    private static volatile Field aqtweaks$motorField;
    @Unique
    private static volatile boolean aqtweaks$motorFieldResolved;
    @Unique
    private static volatile Field aqtweaks$entityField;
    @Unique
    private static volatile boolean aqtweaks$entityFieldResolved;

    @Redirect(
            method = "updatePlayerPos",
            at = @At(
                    value = "FIELD",
                    target = "Lcom/yyon/grapplinghook/GrappleCustomization;motor:Z",
                    opcode = Opcodes.GETFIELD
            )
    )
    private boolean aqtweaks$motorRequiresEmber(@Coerce Object custom) {
        boolean motor = false;
        Field motorField = aqtweaks$motorField(custom);
        if (motorField != null) {
            try {
                motor = motorField.getBoolean(custom);
            } catch (Exception ignored) {}
        }
        if (!motor) return false;
        if (!ArcanaQuestTweaksConfig.StaminaModuleConfig.grapple.motorRequiresEmber) return true;

        Entity rider = null;
        Field entityField = aqtweaks$entityField();
        if (entityField != null) {
            try {
                Object value = entityField.get(this);
                if (value instanceof Entity) rider = (Entity) value;
            } catch (Exception ignored) {}
        }
        if (!(rider instanceof EntityPlayer)) {
            rider = Reflect.getClientPlayer();
        }
        if (!(rider instanceof EntityPlayer)) return true;
        return EmberMotorHelper.hasEmber((EntityPlayer) rider, ArcanaQuestTweaksConfig.StaminaModuleConfig.grapple.motorEmberCost);
    }

    @Unique
    private static Field aqtweaks$motorField(Object custom) {
        if (custom == null) return null;
        if (!aqtweaks$motorFieldResolved) {
            aqtweaks$motorFieldResolved = true;
            try {
                aqtweaks$motorField = custom.getClass().getField("motor");
            } catch (Throwable t) {
                AQTWEAKS$LOGGER.warn("Could not resolve GrappleCustomization.motor; motor Ember gating is inactive", t);
            }
        }
        return aqtweaks$motorField;
    }

    @Unique
    private Field aqtweaks$entityField() {
        if (!aqtweaks$entityFieldResolved) {
            aqtweaks$entityFieldResolved = true;
            try {
                aqtweaks$entityField = this.getClass().getField("entity");
            } catch (Throwable t) {
                AQTWEAKS$LOGGER.warn("Could not resolve grappleController.entity; falling back to the client player", t);
            }
        }
        return aqtweaks$entityField;
    }
}
