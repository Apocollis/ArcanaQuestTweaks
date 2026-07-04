package com.apocollis.aqtweaks;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumHand;

public class Reflect {
    private static Method isSprintingMethod;
    private static Method setSprintingMethod;
    private static Field capabilitiesField;
    private static Field isCreativeModeField;
    private static Field motionYField;
    private static Method resetActiveHandMethod;
    private static Method isOnLadderMethod;
    private static Method swingArmMethod;

    // Open Glider reflection
    private static boolean isGliderLoaded = false;
    private static Method getIsPlayerGlidingMethod;
    private static Method setIsGliderDeployedMethod;
    private static Method setIsPlayerGlidingMethod;

    // Grappling Hook reflection
    private static boolean isGrappleLoaded = false;
    private static Field grappleControllersField;
    private static Field grappleAttachedField;
    private static Method grappleUnattachMethod;

    // Elenai Dodge 2 Absorption capability reflection
    private static net.minecraftforge.common.capabilities.Capability<?> absorptionCap;
    private static Method getAbsorptionMethod;

    static {
        // isSprinting
        try {
            isSprintingMethod = Entity.class.getMethod("func_70051_ag");
        } catch (NoSuchMethodException e) {
            try {
                isSprintingMethod = Entity.class.getMethod("isSprinting");
            } catch (NoSuchMethodException ex) {
                ex.printStackTrace();
            }
        }

        // setSprinting
        try {
            setSprintingMethod = Entity.class.getMethod("func_70031_b", boolean.class);
        } catch (NoSuchMethodException e) {
            try {
                setSprintingMethod = Entity.class.getMethod("setSprinting", boolean.class);
            } catch (NoSuchMethodException ex) {
                ex.printStackTrace();
            }
        }

        // capabilities
        try {
            capabilitiesField = EntityPlayer.class.getField("field_71075_bZ");
        } catch (NoSuchFieldException e) {
            try {
                capabilitiesField = EntityPlayer.class.getField("capabilities");
            } catch (NoSuchFieldException ex) {
                ex.printStackTrace();
            }
        }

        // isCreativeMode
        if (capabilitiesField != null) {
            try {
                isCreativeModeField = capabilitiesField.getType().getField("field_75098_d");
            } catch (NoSuchFieldException e) {
                try {
                    isCreativeModeField = capabilitiesField.getType().getField("isCreativeMode");
                } catch (NoSuchFieldException ex) {
                    ex.printStackTrace();
                }
            }
        }

        // motionY
        try {
            motionYField = Entity.class.getField("field_70181_x");
        } catch (NoSuchFieldException e) {
            try {
                motionYField = Entity.class.getField("motionY");
            } catch (NoSuchFieldException ex) {
                ex.printStackTrace();
            }
        }

        // resetActiveHand
        try {
            resetActiveHandMethod = EntityLivingBase.class.getMethod("func_184602_cy");
        } catch (NoSuchMethodException e) {
            try {
                resetActiveHandMethod = EntityLivingBase.class.getMethod("resetActiveHand");
            } catch (NoSuchMethodException ex) {
                ex.printStackTrace();
            }
        }

        // isOnLadder
        try {
            isOnLadderMethod = EntityLivingBase.class.getMethod("func_70617_f_");
        } catch (NoSuchMethodException e) {
            try {
                isOnLadderMethod = EntityLivingBase.class.getMethod("isOnLadder");
            } catch (NoSuchMethodException ex) {
                ex.printStackTrace();
            }
        }

        // swingArm
        try {
            swingArmMethod = EntityLivingBase.class.getMethod("func_184609_a", EnumHand.class);
        } catch (NoSuchMethodException e) {
            try {
                swingArmMethod = EntityLivingBase.class.getMethod("swingArm", EnumHand.class);
            } catch (NoSuchMethodException ex) {
                ex.printStackTrace();
            }
        }

        // Open Glider API
        try {
            Class<?> gliderHelperClass = Class.forName("gr8pefish.openglider.api.helper.GliderHelper");
            getIsPlayerGlidingMethod = gliderHelperClass.getMethod("getIsPlayerGliding", EntityPlayer.class);
            setIsGliderDeployedMethod = gliderHelperClass.getMethod("setIsGliderDeployed", EntityPlayer.class, boolean.class);
            setIsPlayerGlidingMethod = gliderHelperClass.getMethod("setIsPlayerGliding", EntityPlayer.class, boolean.class);
            isGliderLoaded = true;
        } catch (Exception e) {
            // Open Glider not loaded
        }

        // Grappling Hook Mod API
        try {
            Class<?> grapplemodClass = Class.forName("com.yyon.grapplinghook.grapplemod");
            grappleControllersField = grapplemodClass.getField("controllers");
            
            Class<?> grappleControllerClass = Class.forName("com.yyon.grapplinghook.controllers.grappleController");
            grappleAttachedField = grappleControllerClass.getField("attached");
            grappleUnattachMethod = grappleControllerClass.getMethod("unattach");
            isGrappleLoaded = true;
        } catch (Exception e) {
            // Grappling Hook Mod not loaded
        }

        // Elenai Dodge 2 Absorption Cap
        try {
            Class<?> providerClass = Class.forName("com.elenai.elenaidodge2.capability.absorption.AbsorptionProvider");
            Field capField = providerClass.getField("ABSORPTION_CAP");
            absorptionCap = (net.minecraftforge.common.capabilities.Capability<?>) capField.get(null);
            
            Class<?> iAbsClass = Class.forName("com.elenai.elenaidodge2.capability.absorption.IAbsorption");
            getAbsorptionMethod = iAbsClass.getMethod("getAbsorption");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean isSprinting(Entity player) {
        if (isSprintingMethod != null) {
            try {
                return (Boolean) isSprintingMethod.invoke(player);
            } catch (Exception e) {
                // fall back
            }
        }
        return player.isSprinting();
    }

    public static void setSprinting(Entity player, boolean sprinting) {
        if (setSprintingMethod != null) {
            try {
                setSprintingMethod.invoke(player, sprinting);
                return;
            } catch (Exception e) {
                // fall back
            }
        }
        player.setSprinting(sprinting);
    }

    public static boolean isCreative(EntityPlayer player) {
        if (capabilitiesField != null && isCreativeModeField != null) {
            try {
                Object caps = capabilitiesField.get(player);
                return (Boolean) isCreativeModeField.get(caps);
            } catch (Exception e) {
                // fall back
            }
        }
        return player.capabilities.isCreativeMode;
    }

    public static void setMotionY(Entity player, double motionY) {
        if (motionYField != null) {
            try {
                motionYField.setDouble(player, motionY);
                return;
            } catch (Exception e) {
                // fall back
            }
        }
        player.motionY = motionY;
    }

    public static double getMotionY(Entity player) {
        if (motionYField != null) {
            try {
                return motionYField.getDouble(player);
            } catch (Exception e) {
                // fall back
            }
        }
        return player.motionY;
    }

    public static void resetActiveHand(EntityLivingBase entity) {
        if (resetActiveHandMethod != null) {
            try {
                resetActiveHandMethod.invoke(entity);
                return;
            } catch (Exception e) {
                // fall back
            }
        }
        entity.resetActiveHand();
    }

    public static boolean isOnLadder(EntityLivingBase entity) {
        if (isOnLadderMethod != null) {
            try {
                return (Boolean) isOnLadderMethod.invoke(entity);
            } catch (Exception e) {
                // fall back
            }
        }
        return entity.isOnLadder();
    }

    public static void swingArm(EntityLivingBase entity, EnumHand hand) {
        if (swingArmMethod != null) {
            try {
                swingArmMethod.invoke(entity, hand);
                return;
            } catch (Exception e) {
                // fall back
            }
        }
        entity.swingArm(hand);
    }

    // Mod Integration helper methods
    public static boolean isGliderLoaded() {
        return isGliderLoaded;
    }

    public static boolean isGliding(EntityPlayer player) {
        if (!isGliderLoaded) return false;
        try {
            return (Boolean) getIsPlayerGlidingMethod.invoke(null, player);
        } catch (Exception e) {
            return false;
        }
    }

    public static void undeployGlider(EntityPlayer player) {
        if (!isGliderLoaded) return;
        try {
            setIsGliderDeployedMethod.invoke(null, player, false);
            setIsPlayerGlidingMethod.invoke(null, player, false);
        } catch (Exception e) {
            // ignore
        }
    }

    public static boolean isGrappleLoaded() {
        return isGrappleLoaded;
    }

    public static boolean isGrappling(EntityPlayer player) {
        if (!isGrappleLoaded) return false;
        try {
            java.util.Map<?, ?> controllers = (java.util.Map<?, ?>) grappleControllersField.get(null);
            if (controllers == null) return false;
            Object controller = controllers.get(player.getEntityId());
            if (controller == null) return false;
            return grappleAttachedField.getBoolean(controller);
        } catch (Exception e) {
            return false;
        }
    }

    public static void detachGrapple(EntityPlayer player) {
        if (!isGrappleLoaded) return;
        try {
            java.util.Map<?, ?> controllers = (java.util.Map<?, ?>) grappleControllersField.get(null);
            if (controllers == null) return;
            Object controller = controllers.get(player.getEntityId());
            if (controller == null) return;
            grappleUnattachMethod.invoke(controller);
        } catch (Exception e) {
            // ignore
        }
    }

    public static boolean isRopeBlock(Block block) {
        if (block == null || block.getRegistryName() == null) return false;
        String name = block.getRegistryName().toString().toLowerCase();
        return name.contains("rope");
    }

    public static int getAbsorptionFeathers(EntityPlayer player) {
        if (player.world.isRemote) {
            return com.elenai.elenaidodge2.util.ClientStorage.absorption;
        }
        if (absorptionCap != null && getAbsorptionMethod != null) {
            try {
                Object capObj = player.getCapability(absorptionCap, null);
                if (capObj != null) {
                    return (Integer) getAbsorptionMethod.invoke(capObj);
                }
            } catch (Exception e) {
                // ignore
            }
        }
        return 0;
    }

    public static int getWeight(EntityPlayer player) {
        if (player.world.isRemote) {
            return com.elenai.elenaidodge2.util.ClientStorage.weight;
        } else if (player instanceof net.minecraft.entity.player.EntityPlayerMP) {
            return com.elenai.elenaidodge2.api.FeathersHelper.getWeight((net.minecraft.entity.player.EntityPlayerMP) player);
        }
        return 0;
    }

    public static boolean hasEnoughStamina(EntityPlayer player, int cost) {
        if (isCreative(player)) return true;
        int dodges = 0;
        if (player.world.isRemote) {
            dodges = com.elenai.elenaidodge2.util.ClientStorage.dodges;
        } else if (player instanceof net.minecraft.entity.player.EntityPlayerMP) {
            dodges = com.elenai.elenaidodge2.api.FeathersHelper.getFeatherLevel((net.minecraft.entity.player.EntityPlayerMP) player);
        }
        int absorption = getAbsorptionFeathers(player);
        int weight = getWeight(player);

        // Check 1: total feathers check
        if (dodges + absorption < cost) {
            return false;
        }

        // Check 2: weight check (iron feathers check)
        if (weight > 0) {
            if (dodges - cost < weight && absorption - cost < 0) {
                return false;
            }
        }

        return true;
    }
}
