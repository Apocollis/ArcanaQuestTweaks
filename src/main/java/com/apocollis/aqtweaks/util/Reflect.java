package com.apocollis.aqtweaks.util;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;

import com.apocollis.aqtweaks.stamina.StaminaModule;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.storage.MapStorage;

public class Reflect {
    private static Method isSprintingMethod;
    private static Method setSprintingMethod;
    private static Field capabilitiesField;
    private static Field isCreativeModeField;
    private static Field motionYField;
    private static Method resetActiveHandMethod;
    private static Method isOnLadderMethod;
    private static Method swingArmMethod;
    private static Method getMinecraftMethod;
    private static Method isSpectatorMethod;
    private static Method isSneakingMethod;
    private static Method isInWaterMethod;
    private static Method getEntityBoundingBoxMethod;
    private static Field boundingBoxField;
    private static Field aabbMinXField;
    private static Field aabbMinYField;
    private static Field aabbMinZField;
    private static Field aabbMaxXField;
    private static Field aabbMaxYField;
    private static Field aabbMaxZField;
    private static Method aabbGrowMethod;
    private static Method aabbGrowXYZMethod;
    private static Field vec3iXField;
    private static Field vec3iYField;
    private static Field vec3iZField;
    private static Method vec3iGetXMethod;
    private static Method vec3iGetYMethod;
    private static Method vec3iGetZMethod;
    private static Method blockStateGetBlockMethod;
    private static Method blockStateGetCollisionBoundingBoxMethod;
    private static Method itemStackIsEmptyMethod;
    private static Method worldGetBiomeMethod;
    private static Method worldGetSeedMethod;
    private static Method worldGetMapStorageMethod;
    private static Method blockIsAirMethod;
    private static Method blockGetDefaultStateMethod;
    private static Method blockGetSoundTypeMethod;
    private static Field blockSoundTypeField;
    private static Field soundEventRegistryField;
    private static Method registryGetObjectMethod;
    private static Method blockPosUpMethod;
    private static Method blockPosUpIntMethod;
    private static Method blockPosDownMethod;
    private static Method blockPosDownIntMethod;
    private static Method blockPosAddMethod;
    private static Method mapStorageGetOrLoadDataMethod;
    private static Method mapStorageSetDataMethod;
    private static Method worldSavedDataMarkDirtyMethod;
    private static Method getPositionMethod;
    private static Method getBlockStateMethod;
    private static Method chunkPrimerGetBlockStateMethod;
    private static Method chunkPrimerSetBlockStateMethod;
    private static Method chunkGetBlockStateMethod;
    private static Method chunkSetBlockStateMethod;
    private static Method blockStateGetMaterialMethod;
    private static Method mutableBlockPosSetPosMethod;
    private static Field materialAirField;
    private static Field materialWaterField;
    private static Field materialLavaField;
    private static Field materialRockField;
    private static Field materialGroundField;
    private static Field materialClayField;
    private static Field materialSandField;
    private static Field materialGrassField;
    private static Field materialIceField;
    private static Field materialPackedIceField;
    private static Field materialCraftedSnowField;
    private static Method isBlockLoadedMethod;
    private static Method getEntitiesWithinAABBMethod;
    private static Method addPotionEffectMethod;
    private static Method removePotionEffectMethod;
    private static Method getUniqueIDMethod;
    private static Method getServerMethod;
    private static Method sendMessageMethod;
    private static Method playSoundMethod;
    private static Method getTrueSourceMethod;
    private static Field damageSourceEntityField;
    private static Method getImmediateSourceMethod;
    private static Field damageSourceImmediateEntityField;
    private static Method isDamageAbsoluteMethod;
    private static Method isUnblockableMethod;
    private static Method getTextureManagerMethod;
    private static Field renderEngineField;
    private static Method bindTextureMethod;
    private static Method getBlockFromNameMethod;
    private static Field blockRegistryField;
    private static Method isMagicDamageMethod;
    private static Method isActiveItemStackBlockingMethod;
    private static Method getActiveItemStackMethod;
    private static Method isHandActiveMethod;
    private static Method getHeldItemMainhandMethod;
    private static Method attackEntityFromMethod;
    private static Method isPlayerSleepingMethod;
    private static Method isDaytimeMethod;
    private static Method getItemMethod;
    private static Method getMaxItemUseDurationMethod;
    private static Method getItemUseActionMethod;
    private static Method entityGetEntityDataMethod;
    private static Method nbtGetCompoundTagMethod;
    private static Method nbtHasKeyMethod;
    private static Method nbtGetIntegerMethod;
    private static Method nbtSetIntegerMethod;
    private static Method nbtGetBooleanMethod;
    private static Method nbtSetBooleanMethod;
    private static Method nbtGetDoubleMethod;
    private static Method nbtSetDoubleMethod;
    private static Method nbtGetStringMethod;
    private static Method nbtSetStringMethod;
    private static Method nbtGetIntArrayMethod;
    private static Method nbtSetIntArrayMethod;
    private static Method nbtGetTagListMethod;
    private static Method nbtRemoveTagMethod;
    private static Method nbtSetTagMethod;
    private static Method nbtTagCountMethod;
    private static Method nbtGetCompoundTagAtMethod;
    private static Method nbtAppendTagMethod;
    private static Method isInLavaMethod;
    private static Method isRidingMethod;
    private static Field playerField;
    private static Field movementInputField;
    private static Field jumpField;
    private static Field sneakField;
    private static Field moveForwardField;
    private static Field moveStrafeField;
    private static Field posXField;
    private static Field posYField;
    private static Field posZField;
    private static Field motionXField;
    private static Field motionZField;
    private static Field onGroundField;
    private static Field rotationYawField;
    private static Field isRemoteField;
    private static Field worldField;
    private static Field ticksExistedField;
    private static Field dimensionField;
    private static Field isDeadField;
    private static Field arrowShootingEntityField;
    private static Field fireballShootingEntityField;
    private static Field netHandlerPlayerField;

    // Open Glider reflection
    private static boolean isGliderLoaded = false;
    private static Method getIsPlayerGlidingMethod;
    private static Method getIsGliderDeployedMethod;
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

    // Reskillable reflection
    private static boolean isReskillableLoaded = false;
    private static Class<?> playerDataHandlerClass;
    private static Method playerDataGetMethod;
    private static Class<?> reskillableRegistriesClass;
    private static Object skillsRegistry;
    private static Object unlockablesRegistry;
    private static Method registryGetValueMethod;
    private static Class<?> playerDataClass;
    private static Method getSkillInfoMethod;
    private static Class<?> unlockableClass;
    private static Method getParentSkillMethod;
    private static Class<?> playerSkillInfoClass;
    private static Method isUnlockedMethod;

    // Simple Difficulty reflection
    private static boolean isSimpleDifficultyLoaded = false;
    private static Class<?> sdCapabilitiesClass;
    private static Method getThirstDataMethod;
    private static Class<?> thirstCapabilityClass;
    private static Method addThirstExhaustionMethod;

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

        // Minecraft.getMinecraft
        try {
            getMinecraftMethod = net.minecraft.client.Minecraft.class.getMethod("func_71410_x");
        } catch (Throwable e) {
            try {
                getMinecraftMethod = net.minecraft.client.Minecraft.class.getMethod("getMinecraft");
            } catch (Throwable ex) {
                // fall back to FMLClientHandler
            }
        }

        // isSpectator
        try {
            isSpectatorMethod = EntityPlayer.class.getMethod("func_175149_v");
        } catch (Throwable e) {
            try {
                isSpectatorMethod = EntityPlayer.class.getMethod("isSpectator");
            } catch (Throwable ex) {
                // ignore
            }
        }

        // isSneaking
        try {
            isSneakingMethod = Entity.class.getMethod("func_70093_af");
        } catch (Throwable e) {
            try {
                isSneakingMethod = Entity.class.getMethod("isSneaking");
            } catch (Throwable ex) {
                // ignore
            }
        }

        // isInWater
        try {
            isInWaterMethod = Entity.class.getMethod("func_70090_H");
        } catch (Throwable e) {
            try {
                isInWaterMethod = Entity.class.getMethod("isInWater");
            } catch (Throwable ex) {
                // ignore
            }
        }

        // isInLava
        try {
            isInLavaMethod = Entity.class.getMethod("func_180799_ab");
        } catch (Throwable e) {
            try {
                isInLavaMethod = Entity.class.getMethod("isInLava");
            } catch (Throwable ex) {
                // ignore
            }
        }

        // isRiding
        try {
            isRidingMethod = Entity.class.getMethod("func_184218_aH");
        } catch (Throwable e) {
            try {
                isRidingMethod = Entity.class.getMethod("isRiding");
            } catch (Throwable ex) {
                // ignore
            }
        }

        // player field on Minecraft
        try {
            playerField = net.minecraft.client.Minecraft.class.getField("field_71439_g");
        } catch (Throwable e) {
            try {
                playerField = net.minecraft.client.Minecraft.class.getField("player");
            } catch (Throwable ex) {}
        }

        // movementInput field on EntityPlayerSP
        try {
            Class<?> spClass = Class.forName("net.minecraft.client.entity.EntityPlayerSP");
            movementInputField = spClass.getField("field_71158_b");
        } catch (Throwable e) {
            try {
                Class<?> spClass = Class.forName("net.minecraft.client.entity.EntityPlayerSP");
                movementInputField = spClass.getField("movementInput");
            } catch (Throwable ex) {}
        }

        // MovementInput fields (jump, sneak, moveForward, moveStrafe)
        try {
            Class<?> inputClass = Class.forName("net.minecraft.util.MovementInput");
            try { jumpField = inputClass.getField("field_78901_c"); } catch (Throwable t) { try { jumpField = inputClass.getField("jump"); } catch (Throwable ignored) {} }
            try { sneakField = inputClass.getField("field_78899_d"); } catch (Throwable t) { try { sneakField = inputClass.getField("sneak"); } catch (Throwable ignored) {} }
            try { moveForwardField = inputClass.getField("field_192832_b"); } catch (Throwable t) { try { moveForwardField = inputClass.getField("moveForward"); } catch (Throwable ignored) {} }
            try { moveStrafeField = inputClass.getField("field_78902_a"); } catch (Throwable t) { try { moveStrafeField = inputClass.getField("moveStrafe"); } catch (Throwable ignored) {} }
        } catch (Throwable ignored) {}

        // Entity position, motion, and ground fields
        try { posXField = Entity.class.getField("field_70165_t"); } catch (Throwable t) { try { posXField = Entity.class.getField("posX"); } catch (Throwable ignored) {} }
        try { posYField = Entity.class.getField("field_70163_u"); } catch (Throwable t) { try { posYField = Entity.class.getField("posY"); } catch (Throwable ignored) {} }
        try { posZField = Entity.class.getField("field_70161_v"); } catch (Throwable t) { try { posZField = Entity.class.getField("posZ"); } catch (Throwable ignored) {} }
        try { motionXField = Entity.class.getField("field_70159_w"); } catch (Throwable t) { try { motionXField = Entity.class.getField("motionX"); } catch (Throwable ignored) {} }
        try { motionZField = Entity.class.getField("field_70179_y"); } catch (Throwable t) { try { motionZField = Entity.class.getField("motionZ"); } catch (Throwable ignored) {} }
        try { onGroundField = Entity.class.getField("field_70122_E"); } catch (Throwable t) { try { onGroundField = Entity.class.getField("onGround"); } catch (Throwable ignored) {} }
        try { rotationYawField = Entity.class.getField("field_70177_z"); } catch (Throwable t) { try { rotationYawField = Entity.class.getField("rotationYaw"); } catch (Throwable ignored) {} }

        // World.isRemote
        try { isRemoteField = net.minecraft.world.World.class.getField("field_72995_K"); } catch (Throwable t) { try { isRemoteField = net.minecraft.world.World.class.getField("isRemote"); } catch (Throwable ignored) {} }

        // Entity.world
        try { worldField = Entity.class.getField("field_70170_p"); } catch (Throwable t) { try { worldField = Entity.class.getField("world"); } catch (Throwable ignored) {} }

        // Entity.ticksExisted
        try { ticksExistedField = Entity.class.getField("field_70173_aa"); } catch (Throwable t) { try { ticksExistedField = Entity.class.getField("ticksExisted"); } catch (Throwable ignored) {} }

        // Entity.dimension
        try { dimensionField = Entity.class.getField("field_71093_bK"); } catch (Throwable t) { try { dimensionField = Entity.class.getField("dimension"); } catch (Throwable ignored) {} }

        // Entity.isDead
        try { isDeadField = Entity.class.getField("field_70128_L"); } catch (Throwable t) { try { isDeadField = Entity.class.getField("isDead"); } catch (Throwable ignored) {} }

        // EntityArrow.shootingEntity
        try {
            Class<?> arrowClass = Class.forName("net.minecraft.entity.projectile.EntityArrow");
            try { arrowShootingEntityField = arrowClass.getField("field_70192_c"); } catch (Throwable t) { try { arrowShootingEntityField = arrowClass.getField("shootingEntity"); } catch (Throwable ignored) {} }
        } catch (Throwable ignored) {}

        // EntityFireball.shootingEntity
        try {
            Class<?> fireballClass = Class.forName("net.minecraft.entity.projectile.EntityFireball");
            try { fireballShootingEntityField = fireballClass.getField("field_70235_a"); } catch (Throwable t) { try { fireballShootingEntityField = fireballClass.getField("shootingEntity"); } catch (Throwable ignored) {} }
        } catch (Throwable ignored) {}

        // NetHandlerPlayServer.player
        try {
            Class<?> nhClass = Class.forName("net.minecraft.network.NetHandlerPlayServer");
            try { netHandlerPlayerField = nhClass.getField("field_147369_b"); } catch (Throwable t) {
                try { netHandlerPlayerField = nhClass.getField("player"); } catch (Throwable ignored) {
                    try { netHandlerPlayerField = nhClass.getField("playerEntity"); } catch (Throwable ignored2) {}
                }
            }
        } catch (Throwable ignored) {}

        // DamageSource methods & fields
        try {
            Class<?> dsClass = DamageSource.class;
            try { getTrueSourceMethod = dsClass.getMethod("func_76346_g"); } catch (Throwable t) {
                try { getTrueSourceMethod = dsClass.getMethod("getTrueSource"); } catch (Throwable ignored) {}
            }
            try { damageSourceEntityField = dsClass.getField("field_76373_n"); } catch (Throwable t) {
                try { damageSourceEntityField = dsClass.getField("damageSourceEntity"); } catch (Throwable ignored) {}
            }
            try { getImmediateSourceMethod = dsClass.getMethod("func_76364_f"); } catch (Throwable t) {
                try { getImmediateSourceMethod = dsClass.getMethod("getImmediateSource"); } catch (Throwable ignored) {}
            }
            try { isDamageAbsoluteMethod = dsClass.getMethod("func_76363_m"); } catch (Throwable t) {
                try { isDamageAbsoluteMethod = dsClass.getMethod("isDamageAbsolute"); } catch (Throwable ignored) {}
            }
            try { isUnblockableMethod = dsClass.getMethod("func_76352_a"); } catch (Throwable t) {
                try { isUnblockableMethod = dsClass.getMethod("isUnblockable"); } catch (Throwable ignored) {}
            }
            try { isMagicDamageMethod = dsClass.getMethod("func_76347_k"); } catch (Throwable t) {
                try { isMagicDamageMethod = dsClass.getMethod("isMagicDamage"); } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}

        // EntityLivingBase & EntityPlayer methods
        try {
            Class<?> elbClass = EntityLivingBase.class;
            try { isActiveItemStackBlockingMethod = elbClass.getMethod("func_184585_cz"); } catch (Throwable t) {
                try { isActiveItemStackBlockingMethod = elbClass.getMethod("isActiveItemStackBlocking"); } catch (Throwable ignored) {}
            }
            try { getActiveItemStackMethod = elbClass.getMethod("func_184607_cu"); } catch (Throwable t) {
                try { getActiveItemStackMethod = elbClass.getMethod("getActiveItemStack"); } catch (Throwable ignored) {}
            }
            try { isHandActiveMethod = elbClass.getMethod("func_184587_cr"); } catch (Throwable t) {
                try { isHandActiveMethod = elbClass.getMethod("isHandActive"); } catch (Throwable ignored) {}
            }
            try { getHeldItemMainhandMethod = elbClass.getMethod("func_184614_ca"); } catch (Throwable t) {
                try { getHeldItemMainhandMethod = elbClass.getMethod("getHeldItemMainhand"); } catch (Throwable ignored) {}
            }
            try { attackEntityFromMethod = elbClass.getMethod("func_70097_a", DamageSource.class, float.class); } catch (Throwable t) {
                try { attackEntityFromMethod = elbClass.getMethod("attackEntityFrom", DamageSource.class, float.class); } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}

        try {
            Class<?> epClass = EntityPlayer.class;
            try { isPlayerSleepingMethod = epClass.getMethod("func_70608_bn"); } catch (Throwable t) {
                try { isPlayerSleepingMethod = epClass.getMethod("isPlayerSleeping"); } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}

        // World methods
        try {
            Class<?> wClass = World.class;
            try { isDaytimeMethod = wClass.getMethod("func_72935_r"); } catch (Throwable t) {
                try { isDaytimeMethod = wClass.getMethod("isDaytime"); } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}

        // ItemStack methods
        try {
            Class<?> isClass = ItemStack.class;
            try { getItemMethod = isClass.getMethod("func_77973_b"); } catch (Throwable t) {
                try { getItemMethod = isClass.getMethod("getItem"); } catch (Throwable ignored) {}
            }
            try { getMaxItemUseDurationMethod = isClass.getMethod("func_77988_m"); } catch (Throwable t) {
                try { getMaxItemUseDurationMethod = isClass.getMethod("getMaxItemUseDuration"); } catch (Throwable ignored) {}
            }
            try { getItemUseActionMethod = isClass.getMethod("func_77975_n"); } catch (Throwable t) {
                try { getItemUseActionMethod = isClass.getMethod("getItemUseAction"); } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}

        // Entity.getEntityData()
        try {
            Class<?> eClass = Entity.class;
            try { entityGetEntityDataMethod = eClass.getMethod("getEntityData"); } catch (Throwable ignored) {}
        } catch (Throwable ignored) {}

        // NBTTagCompound & NBTTagList methods
        try {
            Class<?> nbtClass = NBTTagCompound.class;
            try { nbtGetCompoundTagMethod = nbtClass.getMethod("func_74775_l", String.class); } catch (Throwable t) {
                try { nbtGetCompoundTagMethod = nbtClass.getMethod("getCompoundTag", String.class); } catch (Throwable ignored) {}
            }
            try { nbtHasKeyMethod = nbtClass.getMethod("func_74764_b", String.class); } catch (Throwable t) {
                try { nbtHasKeyMethod = nbtClass.getMethod("hasKey", String.class); } catch (Throwable ignored) {}
            }
            try { nbtGetIntegerMethod = nbtClass.getMethod("func_74762_e", String.class); } catch (Throwable t) {
                try { nbtGetIntegerMethod = nbtClass.getMethod("getInteger", String.class); } catch (Throwable ignored) {}
            }
            try { nbtSetIntegerMethod = nbtClass.getMethod("func_74768_a", String.class, int.class); } catch (Throwable t) {
                try { nbtSetIntegerMethod = nbtClass.getMethod("setInteger", String.class, int.class); } catch (Throwable ignored) {}
            }
            try { nbtGetBooleanMethod = nbtClass.getMethod("func_74767_n", String.class); } catch (Throwable t) {
                try { nbtGetBooleanMethod = nbtClass.getMethod("getBoolean", String.class); } catch (Throwable ignored) {}
            }
            try { nbtSetBooleanMethod = nbtClass.getMethod("func_74757_a", String.class, boolean.class); } catch (Throwable t) {
                try { nbtSetBooleanMethod = nbtClass.getMethod("setBoolean", String.class, boolean.class); } catch (Throwable ignored) {}
            }
            try { nbtGetDoubleMethod = nbtClass.getMethod("func_74769_h", String.class); } catch (Throwable t) {
                try { nbtGetDoubleMethod = nbtClass.getMethod("getDouble", String.class); } catch (Throwable ignored) {}
            }
            try { nbtSetDoubleMethod = nbtClass.getMethod("func_74780_a", String.class, double.class); } catch (Throwable t) {
                try { nbtSetDoubleMethod = nbtClass.getMethod("setDouble", String.class, double.class); } catch (Throwable ignored) {}
            }
            try { nbtGetStringMethod = nbtClass.getMethod("func_74779_i", String.class); } catch (Throwable t) {
                try { nbtGetStringMethod = nbtClass.getMethod("getString", String.class); } catch (Throwable ignored) {}
            }
            try { nbtSetStringMethod = nbtClass.getMethod("func_74778_a", String.class, String.class); } catch (Throwable t) {
                try { nbtSetStringMethod = nbtClass.getMethod("setString", String.class, String.class); } catch (Throwable ignored) {}
            }
            try { nbtGetIntArrayMethod = nbtClass.getMethod("func_74759_k", String.class); } catch (Throwable t) {
                try { nbtGetIntArrayMethod = nbtClass.getMethod("getIntArray", String.class); } catch (Throwable ignored) {}
            }
            try { nbtSetIntArrayMethod = nbtClass.getMethod("func_74783_a", String.class, int[].class); } catch (Throwable t) {
                try { nbtSetIntArrayMethod = nbtClass.getMethod("setIntArray", String.class, int[].class); } catch (Throwable ignored) {}
            }
            try { nbtGetTagListMethod = nbtClass.getMethod("func_150295_c", String.class, int.class); } catch (Throwable t) {
                try { nbtGetTagListMethod = nbtClass.getMethod("getTagList", String.class, int.class); } catch (Throwable ignored) {}
            }
            try { nbtRemoveTagMethod = nbtClass.getMethod("func_82580_o", String.class); } catch (Throwable t) {
                try { nbtRemoveTagMethod = nbtClass.getMethod("removeTag", String.class); } catch (Throwable ignored) {}
            }
            try { nbtSetTagMethod = nbtClass.getMethod("func_74782_a", String.class, NBTBase.class); } catch (Throwable t) {
                try { nbtSetTagMethod = nbtClass.getMethod("setTag", String.class, NBTBase.class); } catch (Throwable ignored) {}
            }

            Class<?> listClass = NBTTagList.class;
            try { nbtTagCountMethod = listClass.getMethod("func_74745_c"); } catch (Throwable t) {
                try { nbtTagCountMethod = listClass.getMethod("tagCount"); } catch (Throwable ignored) {}
            }
            try { nbtGetCompoundTagAtMethod = listClass.getMethod("func_150305_b", int.class); } catch (Throwable t) {
                try { nbtGetCompoundTagAtMethod = listClass.getMethod("getCompoundTagAt", int.class); } catch (Throwable ignored) {}
            }
            try { nbtAppendTagMethod = listClass.getMethod("func_74742_a", NBTBase.class); } catch (Throwable t) {
                try { nbtAppendTagMethod = listClass.getMethod("appendTag", NBTBase.class); } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}

        // AxisAlignedBB coordinates
        try {
            Class<?> aabbClass = AxisAlignedBB.class;
            try { aabbMinXField = aabbClass.getField("field_72340_a"); } catch (Throwable t) {
                try { aabbMinXField = aabbClass.getField("minX"); } catch (Throwable ignored) {}
            }
            try { aabbMinYField = aabbClass.getField("field_72338_b"); } catch (Throwable t) {
                try { aabbMinYField = aabbClass.getField("minY"); } catch (Throwable ignored) {}
            }
            try { aabbMinZField = aabbClass.getField("field_72339_c"); } catch (Throwable t) {
                try { aabbMinZField = aabbClass.getField("minZ"); } catch (Throwable ignored) {}
            }
            try { aabbMaxXField = aabbClass.getField("field_72336_d"); } catch (Throwable t) {
                try { aabbMaxXField = aabbClass.getField("maxX"); } catch (Throwable ignored) {}
            }
            try { aabbMaxYField = aabbClass.getField("field_72337_e"); } catch (Throwable t) {
                try { aabbMaxYField = aabbClass.getField("maxY"); } catch (Throwable ignored) {}
            }
            try { aabbMaxZField = aabbClass.getField("field_72334_f"); } catch (Throwable t) {
                try { aabbMaxZField = aabbClass.getField("maxZ"); } catch (Throwable ignored) {}
            }
            try { aabbGrowMethod = aabbClass.getMethod("func_186662_g", double.class); } catch (Throwable t) {
                try { aabbGrowMethod = aabbClass.getMethod("grow", double.class); } catch (Throwable ignored) {}
            }
            try { aabbGrowXYZMethod = aabbClass.getMethod("func_72314_b", double.class, double.class, double.class); } catch (Throwable t) {
                try { aabbGrowXYZMethod = aabbClass.getMethod("grow", double.class, double.class, double.class); } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}

        // Vec3i / BlockPos coordinates & methods
        try {
            Class<?> vec3iClass = net.minecraft.util.math.Vec3i.class;
            try { vec3iGetXMethod = vec3iClass.getMethod("func_177958_n"); } catch (Throwable t) {
                try { vec3iGetXMethod = vec3iClass.getMethod("getX"); } catch (Throwable ignored) {}
            }
            try { vec3iGetYMethod = vec3iClass.getMethod("func_177956_o"); } catch (Throwable t) {
                try { vec3iGetYMethod = vec3iClass.getMethod("getY"); } catch (Throwable ignored) {}
            }
            try { vec3iGetZMethod = vec3iClass.getMethod("func_177952_p"); } catch (Throwable t) {
                try { vec3iGetZMethod = vec3iClass.getMethod("getZ"); } catch (Throwable ignored) {}
            }

            try {
                vec3iXField = vec3iClass.getDeclaredField("field_177962_a");
                vec3iXField.setAccessible(true);
            } catch (Throwable t) {
                try {
                    vec3iXField = vec3iClass.getDeclaredField("x");
                    vec3iXField.setAccessible(true);
                } catch (Throwable t2) {
                    try { vec3iXField = vec3iClass.getField("field_177962_a"); } catch (Throwable t3) {
                        try { vec3iXField = vec3iClass.getField("x"); } catch (Throwable ignored) {}
                    }
                }
            }

            try {
                vec3iYField = vec3iClass.getDeclaredField("field_177960_b");
                vec3iYField.setAccessible(true);
            } catch (Throwable t) {
                try {
                    vec3iYField = vec3iClass.getDeclaredField("y");
                    vec3iYField.setAccessible(true);
                } catch (Throwable t2) {
                    try { vec3iYField = vec3iClass.getField("field_177960_b"); } catch (Throwable t3) {
                        try { vec3iYField = vec3iClass.getField("y"); } catch (Throwable ignored) {}
                    }
                }
            }

            try {
                vec3iZField = vec3iClass.getDeclaredField("field_177961_c");
                vec3iZField.setAccessible(true);
            } catch (Throwable t) {
                try {
                    vec3iZField = vec3iClass.getDeclaredField("z");
                    vec3iZField.setAccessible(true);
                } catch (Throwable t2) {
                    try { vec3iZField = vec3iClass.getField("field_177961_c"); } catch (Throwable t3) {
                        try { vec3iZField = vec3iClass.getField("z"); } catch (Throwable ignored) {}
                    }
                }
            }
        } catch (Throwable ignored) {}

        // Minecraft getTextureManager / renderEngine / bindTexture
        try {
            Class<?> mcClass = net.minecraft.client.Minecraft.class;
            try { getTextureManagerMethod = mcClass.getMethod("func_110434_K"); } catch (Throwable t) {
                try { getTextureManagerMethod = mcClass.getMethod("getTextureManager"); } catch (Throwable ignored) {}
            }
            if (getTextureManagerMethod == null) {
                try {
                    renderEngineField = mcClass.getDeclaredField("field_71446_g");
                    renderEngineField.setAccessible(true);
                } catch (Throwable t) {
                    try {
                        renderEngineField = mcClass.getDeclaredField("renderEngine");
                        renderEngineField.setAccessible(true);
                    } catch (Throwable ignored) {}
                }
            }
        } catch (Throwable ignored) {}

        try {
            Class<?> tmClass = net.minecraft.client.renderer.texture.TextureManager.class;
            try { bindTextureMethod = tmClass.getMethod("func_110577_a", ResourceLocation.class); } catch (Throwable t) {
                try { bindTextureMethod = tmClass.getMethod("bindTexture", ResourceLocation.class); } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}

        // Block.getBlockFromName & Block.REGISTRY
        try {
            Class<?> bClass = Block.class;
            try { getBlockFromNameMethod = bClass.getMethod("func_149684_b", String.class); } catch (Throwable t) {
                try { getBlockFromNameMethod = bClass.getMethod("getBlockFromName", String.class); } catch (Throwable ignored) {}
            }
            try { blockRegistryField = bClass.getField("field_149771_c"); } catch (Throwable t) {
                try { blockRegistryField = bClass.getField("REGISTRY"); } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}

        // Entity / World / Player methods
        try {
            Class<?> eClass = Entity.class;
            try { getEntityBoundingBoxMethod = eClass.getMethod("func_174813_aQ"); } catch (Throwable t) {
                try { getEntityBoundingBoxMethod = eClass.getMethod("getEntityBoundingBox"); } catch (Throwable ignored) {}
            }
            try { boundingBoxField = eClass.getField("field_70121_D"); } catch (Throwable t) {
                try { boundingBoxField = eClass.getField("boundingBox"); } catch (Throwable ignored) {}
            }
            try { getPositionMethod = eClass.getMethod("func_180425_c"); } catch (Throwable t) {
                try { getPositionMethod = eClass.getMethod("getPosition"); } catch (Throwable ignored) {}
            }
            try { getUniqueIDMethod = eClass.getMethod("func_110124_au"); } catch (Throwable t) {
                try { getUniqueIDMethod = eClass.getMethod("getUniqueID"); } catch (Throwable ignored) {}
            }
            try { getServerMethod = eClass.getMethod("func_184102_h"); } catch (Throwable t) {
                try { getServerMethod = eClass.getMethod("getServer"); } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}

        try {
            Class<?> elbClass = EntityLivingBase.class;
            try { addPotionEffectMethod = elbClass.getMethod("func_70690_d", PotionEffect.class); } catch (Throwable t) {
                try { addPotionEffectMethod = elbClass.getMethod("addPotionEffect", PotionEffect.class); } catch (Throwable ignored) {}
            }
            try { removePotionEffectMethod = elbClass.getMethod("func_184589_d", Potion.class); } catch (Throwable t) {
                try { removePotionEffectMethod = elbClass.getMethod("removePotionEffect", Potion.class); } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}

        try {
            Class<?> epClass = EntityPlayer.class;
            try { sendMessageMethod = epClass.getMethod("func_145747_a", ITextComponent.class); } catch (Throwable t) {
                try { sendMessageMethod = epClass.getMethod("sendMessage", ITextComponent.class); } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}

        try {
            Class<?> bsClass = IBlockState.class;
            try { blockStateGetBlockMethod = bsClass.getMethod("func_177230_c"); } catch (Throwable t) {
                try { blockStateGetBlockMethod = bsClass.getMethod("getBlock"); } catch (Throwable ignored) {}
            }
            try { blockStateGetMaterialMethod = bsClass.getMethod("func_185904_a"); } catch (Throwable t) {
                try { blockStateGetMaterialMethod = bsClass.getMethod("getMaterial"); } catch (Throwable ignored) {}
            }
            try { blockStateGetCollisionBoundingBoxMethod = bsClass.getMethod("func_185900_c", IBlockAccess.class, BlockPos.class); } catch (Throwable t) {
                try { blockStateGetCollisionBoundingBoxMethod = bsClass.getMethod("getCollisionBoundingBox", IBlockAccess.class, BlockPos.class); } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}

        try {
            Class<?> primerClass = ChunkPrimer.class;
            try { chunkPrimerGetBlockStateMethod = primerClass.getMethod("func_177856_a", int.class, int.class, int.class); } catch (Throwable t) {
                try { chunkPrimerGetBlockStateMethod = primerClass.getMethod("getBlockState", int.class, int.class, int.class); } catch (Throwable ignored) {}
            }
            try { chunkPrimerSetBlockStateMethod = primerClass.getMethod("func_177855_a", int.class, int.class, int.class, IBlockState.class); } catch (Throwable t) {
                try { chunkPrimerSetBlockStateMethod = primerClass.getMethod("setBlockState", int.class, int.class, int.class, IBlockState.class); } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}

        try {
            Class<?> chunkClass = Chunk.class;
            try { chunkGetBlockStateMethod = chunkClass.getMethod("func_177435_g", BlockPos.class); } catch (Throwable t) {
                try { chunkGetBlockStateMethod = chunkClass.getMethod("getBlockState", BlockPos.class); } catch (Throwable ignored) {}
            }
            try { chunkSetBlockStateMethod = chunkClass.getMethod("func_177436_a", BlockPos.class, IBlockState.class); } catch (Throwable t) {
                try { chunkSetBlockStateMethod = chunkClass.getMethod("setBlockState", BlockPos.class, IBlockState.class); } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}

        try {
            Class<?> mutablePosClass = BlockPos.MutableBlockPos.class;
            try { mutableBlockPosSetPosMethod = mutablePosClass.getMethod("func_181079_c", int.class, int.class, int.class); } catch (Throwable t) {
                try { mutableBlockPosSetPosMethod = mutablePosClass.getMethod("setPos", int.class, int.class, int.class); } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}

        try {
            Class<?> matClass = Material.class;
            try { materialAirField = matClass.getField("field_151579_a"); } catch (Throwable t) {
                try { materialAirField = matClass.getField("AIR"); } catch (Throwable ignored) {}
            }
            try { materialWaterField = matClass.getField("field_151586_h"); } catch (Throwable t) {
                try { materialWaterField = matClass.getField("WATER"); } catch (Throwable ignored) {}
            }
            try { materialLavaField = matClass.getField("field_151587_i"); } catch (Throwable t) {
                try { materialLavaField = matClass.getField("LAVA"); } catch (Throwable ignored) {}
            }
            try { materialRockField = matClass.getField("field_151576_e"); } catch (Throwable t) {
                try { materialRockField = matClass.getField("ROCK"); } catch (Throwable ignored) {}
            }
            try { materialGroundField = matClass.getField("field_151578_c"); } catch (Throwable t) {
                try { materialGroundField = matClass.getField("GROUND"); } catch (Throwable ignored) {}
            }
            try { materialClayField = matClass.getField("field_151571_B"); } catch (Throwable t) {
                try { materialClayField = matClass.getField("CLAY"); } catch (Throwable ignored) {}
            }
            try { materialSandField = matClass.getField("field_151595_p"); } catch (Throwable t) {
                try { materialSandField = matClass.getField("SAND"); } catch (Throwable ignored) {}
            }
            try { materialGrassField = matClass.getField("field_151577_b"); } catch (Throwable t) {
                try { materialGrassField = matClass.getField("GRASS"); } catch (Throwable ignored) {}
            }
            try { materialIceField = matClass.getField("field_151588_w"); } catch (Throwable t) {
                try { materialIceField = matClass.getField("ICE"); } catch (Throwable ignored) {}
            }
            try { materialPackedIceField = matClass.getField("field_151598_x"); } catch (Throwable t) {
                try { materialPackedIceField = matClass.getField("PACKED_ICE"); } catch (Throwable ignored) {}
            }
            try { materialCraftedSnowField = matClass.getField("field_151596_z"); } catch (Throwable t) {
                try { materialCraftedSnowField = matClass.getField("CRAFTED_SNOW"); } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}

        try {
            Class<?> isClass = ItemStack.class;
            try { itemStackIsEmptyMethod = isClass.getMethod("func_190926_b"); } catch (Throwable t) {
                try { itemStackIsEmptyMethod = isClass.getMethod("isEmpty"); } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}

        try {
            Class<?> bClass = Block.class;
            try { blockIsAirMethod = bClass.getMethod("isAir", IBlockState.class, IBlockAccess.class, BlockPos.class); } catch (Throwable ignored) {}
            try { blockGetDefaultStateMethod = bClass.getMethod("func_176223_p"); } catch (Throwable t) {
                try { blockGetDefaultStateMethod = bClass.getMethod("getDefaultState"); } catch (Throwable ignored) {}
            }
            try { blockGetSoundTypeMethod = bClass.getMethod("getSoundType", IBlockState.class, World.class, BlockPos.class, Entity.class); } catch (Throwable ignored) {}
            try { blockSoundTypeField = bClass.getField("field_149762_H"); } catch (Throwable t) {
                try { blockSoundTypeField = bClass.getField("blockSoundType"); } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}

        try {
            Class<?> seClass = SoundEvent.class;
            try {
                soundEventRegistryField = seClass.getDeclaredField("field_187505_a");
                soundEventRegistryField.setAccessible(true);
            } catch (Throwable t) {
                try { soundEventRegistryField = seClass.getField("REGISTRY"); } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}

        try {
            Class<?> rnClass = net.minecraft.util.registry.RegistryNamespaced.class;
            try { registryGetObjectMethod = rnClass.getMethod("func_82594_a", Object.class); } catch (Throwable t) {
                try { registryGetObjectMethod = rnClass.getMethod("getObject", Object.class); } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}

        try {
            Class<?> bpClass = BlockPos.class;
            try { blockPosUpMethod = bpClass.getMethod("func_177984_a"); } catch (Throwable t) {
                try { blockPosUpMethod = bpClass.getMethod("up"); } catch (Throwable ignored) {}
            }
            try { blockPosUpIntMethod = bpClass.getMethod("func_177981_b", int.class); } catch (Throwable t) {
                try { blockPosUpIntMethod = bpClass.getMethod("up", int.class); } catch (Throwable ignored) {}
            }
            try { blockPosDownMethod = bpClass.getMethod("func_177977_a"); } catch (Throwable t) {
                try { blockPosDownMethod = bpClass.getMethod("down"); } catch (Throwable ignored) {}
            }
            try { blockPosDownIntMethod = bpClass.getMethod("func_177979_c", int.class); } catch (Throwable t) {
                try { blockPosDownIntMethod = bpClass.getMethod("down", int.class); } catch (Throwable ignored) {}
            }
            try { blockPosAddMethod = bpClass.getMethod("func_177982_a", int.class, int.class, int.class); } catch (Throwable t) {
                try { blockPosAddMethod = bpClass.getMethod("add", int.class, int.class, int.class); } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}

        try {
            Class<?> msClass = MapStorage.class;
            try { mapStorageGetOrLoadDataMethod = msClass.getMethod("func_75742_a", Class.class, String.class); } catch (Throwable t) {
                try { mapStorageGetOrLoadDataMethod = msClass.getMethod("getOrLoadData", Class.class, String.class); } catch (Throwable ignored) {}
            }
            try { mapStorageSetDataMethod = msClass.getMethod("func_75745_a", String.class, net.minecraft.world.storage.WorldSavedData.class); } catch (Throwable t) {
                try { mapStorageSetDataMethod = msClass.getMethod("setData", String.class, net.minecraft.world.storage.WorldSavedData.class); } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}

        try {
            Class<?> wsdClass = net.minecraft.world.storage.WorldSavedData.class;
            try { worldSavedDataMarkDirtyMethod = wsdClass.getMethod("func_76185_a"); } catch (Throwable t) {
                try { worldSavedDataMarkDirtyMethod = wsdClass.getMethod("markDirty"); } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}

        try {
            Class<?> wClass = World.class;
            try { getBlockStateMethod = wClass.getMethod("func_180495_p", BlockPos.class); } catch (Throwable t) {
                try { getBlockStateMethod = wClass.getMethod("getBlockState", BlockPos.class); } catch (Throwable ignored) {}
            }
            try { isBlockLoadedMethod = wClass.getMethod("func_175667_e", BlockPos.class); } catch (Throwable t) {
                try { isBlockLoadedMethod = wClass.getMethod("isBlockLoaded", BlockPos.class); } catch (Throwable ignored) {}
            }
            try { getEntitiesWithinAABBMethod = wClass.getMethod("func_72872_a", Class.class, AxisAlignedBB.class); } catch (Throwable t) {
                try { getEntitiesWithinAABBMethod = wClass.getMethod("getEntitiesWithinAABB", Class.class, AxisAlignedBB.class); } catch (Throwable ignored) {}
            }
            try { worldGetBiomeMethod = wClass.getMethod("func_180494_b", BlockPos.class); } catch (Throwable t) {
                try { worldGetBiomeMethod = wClass.getMethod("getBiome", BlockPos.class); } catch (Throwable ignored) {}
            }
            try { worldGetSeedMethod = wClass.getMethod("func_72905_C"); } catch (Throwable t) {
                try { worldGetSeedMethod = wClass.getMethod("getSeed"); } catch (Throwable ignored) {}
            }
            try { worldGetMapStorageMethod = wClass.getMethod("func_175693_T"); } catch (Throwable t) {
                try { worldGetMapStorageMethod = wClass.getMethod("getMapStorage"); } catch (Throwable ignored) {}
            }
            try {
                playSoundMethod = wClass.getMethod("func_184378_a", EntityPlayer.class, double.class, double.class, double.class, SoundEvent.class, SoundCategory.class, float.class, float.class);
            } catch (Throwable t) {
                try {
                    playSoundMethod = wClass.getMethod("playSound", EntityPlayer.class, double.class, double.class, double.class, SoundEvent.class, SoundCategory.class, float.class, float.class);
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}

        // Open Glider API
        try {
            Class<?> gliderHelperClass = Class.forName("gr8pefish.openglider.api.helper.GliderHelper");
            getIsPlayerGlidingMethod = gliderHelperClass.getMethod("getIsPlayerGliding", EntityPlayer.class);
            getIsGliderDeployedMethod = gliderHelperClass.getMethod("getIsGliderDeployed", EntityPlayer.class);
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

        // Reskillable reflection initialization
        try {
            playerDataHandlerClass = Class.forName("codersafterdark.reskillable.api.data.PlayerDataHandler");
            playerDataGetMethod = playerDataHandlerClass.getMethod("get", EntityPlayer.class);
            
            reskillableRegistriesClass = Class.forName("codersafterdark.reskillable.api.ReskillableRegistries");
            skillsRegistry = reskillableRegistriesClass.getField("SKILLS").get(null);
            unlockablesRegistry = reskillableRegistriesClass.getField("UNLOCKABLES").get(null);
            
            Class<?> forgeRegistryClass = Class.forName("net.minecraftforge.registries.IForgeRegistry");
            registryGetValueMethod = forgeRegistryClass.getMethod("getValue", net.minecraft.util.ResourceLocation.class);
            
            playerDataClass = Class.forName("codersafterdark.reskillable.api.data.PlayerData");
            Class<?> skillClass = Class.forName("codersafterdark.reskillable.api.skill.Skill");
            getSkillInfoMethod = playerDataClass.getMethod("getSkillInfo", skillClass);
            
            unlockableClass = Class.forName("codersafterdark.reskillable.api.unlockable.Unlockable");
            getParentSkillMethod = unlockableClass.getMethod("getParentSkill");
            
            playerSkillInfoClass = Class.forName("codersafterdark.reskillable.api.data.PlayerSkillInfo");
            isUnlockedMethod = playerSkillInfoClass.getMethod("isUnlocked", unlockableClass);
            
            isReskillableLoaded = true;
        } catch (Exception e) {
            // Not loaded or failed
        }

        // Simple Difficulty reflection
        try {
            if (net.minecraftforge.fml.common.Loader.isModLoaded("simpledifficulty")) {
                sdCapabilitiesClass = Class.forName("com.charles445.simpledifficulty.api.SDCapabilities");
                getThirstDataMethod = sdCapabilitiesClass.getMethod("getThirstData", EntityPlayer.class);
                thirstCapabilityClass = Class.forName("com.charles445.simpledifficulty.api.thirst.IThirstCapability");
                addThirstExhaustionMethod = thirstCapabilityClass.getMethod("addThirstExhaustion", float.class);
                isSimpleDifficultyLoaded = true;
            }
        } catch (Exception e) {
            // Not loaded or failed
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
            boolean deployed = (Boolean) getIsGliderDeployedMethod.invoke(null, player);
            return deployed && !player.onGround && !player.isInWater() && !player.isInLava();
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
        if (Reflect.isRemote(player)) {
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

    public static int getBaseWeight(EntityPlayer player) {
        String[] weights = com.elenai.elenaidodge2.ModConfig.common.weights.weights;
        if (weights == null || weights.length == 0) return 0;

        double totalWeight = 0.0;
        boolean head = false;
        boolean chest = false;
        boolean legs = false;
        boolean feet = false;

        for (String entry : weights) {
            String[] itemAndVal = entry.split("=");
            if (itemAndVal.length < 2) continue;
            net.minecraft.item.Item item = net.minecraft.item.Item.getByNameOrId(itemAndVal[0]);
            if (item == null) continue;

            if (!head && player.getItemStackFromSlot(net.minecraft.inventory.EntityEquipmentSlot.HEAD).getItem() == item) {
                totalWeight += Double.parseDouble(itemAndVal[1]);
                head = true;
            }
            if (!chest && player.getItemStackFromSlot(net.minecraft.inventory.EntityEquipmentSlot.CHEST).getItem() == item) {
                totalWeight += Double.parseDouble(itemAndVal[1]);
                chest = true;
            }
            if (!legs && player.getItemStackFromSlot(net.minecraft.inventory.EntityEquipmentSlot.LEGS).getItem() == item) {
                totalWeight += Double.parseDouble(itemAndVal[1]);
                legs = true;
            }
            if (!feet && player.getItemStackFromSlot(net.minecraft.inventory.EntityEquipmentSlot.FEET).getItem() == item) {
                totalWeight += Double.parseDouble(itemAndVal[1]);
                feet = true;
            }
        }

        int intWeight = (int) Math.round(totalWeight);
        int lightweightLevel = com.elenai.elenaidodge2.util.Utils.getTotalEnchantmentLevel(
            com.elenai.elenaidodge2.init.EnchantmentInit.LIGHTWEIGHT, player
        );
        intWeight -= lightweightLevel;

        boolean halfFeathers = com.elenai.elenaidodge2.ModConfig.common.feathers.half;
        int finalWeight = 0;
        if (!halfFeathers) {
            finalWeight = (int) (Math.floor(intWeight / 2.0) * 2);
        } else {
            finalWeight = intWeight;
        }
        return Math.max(0, finalWeight);
    }

    public static boolean hasUnlockable(EntityPlayer player, String registryId) {
        if (!isReskillableLoaded || registryId == null || registryId.isEmpty()) return false;
        try {
            net.minecraft.util.ResourceLocation res = new net.minecraft.util.ResourceLocation(registryId);
            Object unlockable = registryGetValueMethod.invoke(unlockablesRegistry, res);
            if (unlockable != null) {
                Object data = playerDataGetMethod.invoke(null, player);
                if (data != null) {
                    Object skill = getParentSkillMethod.invoke(unlockable);
                    if (skill != null) {
                        Object info = getSkillInfoMethod.invoke(data, skill);
                        if (info != null) {
                            return (Boolean) isUnlockedMethod.invoke(info, unlockable);
                        }
                    }
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return false;
    }

    public static int getWeight(EntityPlayer player) {
        int baseWeight = getBaseWeight(player);

        // Apply Armor Mastery reduction
        if (ArcanaQuestTweaksConfig.StaminaModuleConfig.reskillable.enableReskillable && 
            hasUnlockable(player, ArcanaQuestTweaksConfig.StaminaModuleConfig.reskillable.armorMasteryPerkId)) {
            int pieces = 0;
            for (net.minecraft.item.ItemStack armor : player.getArmorInventoryList()) {
                if (armor != null && !armor.isEmpty()) {
                    pieces++;
                }
            }
            int reduction = (int) Math.round(pieces * ArcanaQuestTweaksConfig.StaminaModuleConfig.reskillable.armorMasteryReductionPerPiece);
            baseWeight = Math.max(0, baseWeight - reduction);
        }
        return baseWeight;
    }

    public static boolean hasEnoughStamina(EntityPlayer player, int cost) {
        if (isCreative(player)) return true;
        int dodges = 0;
        if (Reflect.isRemote(player)) {
            dodges = com.elenai.elenaidodge2.util.ClientStorage.dodges;
        } else if (player instanceof net.minecraft.entity.player.EntityPlayerMP) {
            dodges = com.elenai.elenaidodge2.api.FeathersHelper.getFeatherLevel((net.minecraft.entity.player.EntityPlayerMP) player);
        }
        int absorption = getAbsorptionFeathers(player);
        int weight = getWeight(player);

        // Model Elenai's spend order: absorption first, overflow to dodges
        int remainingCost = cost;

        // Absorption absorbs as much as it can
        if (absorption > 0) {
            int absorbedByGold = Math.min(absorption, remainingCost);
            remainingCost -= absorbedByGold;
        }

        // Whatever is left comes out of regular dodges
        int dodgesAfterSpend = dodges - remainingCost;

        // Must not go below 0
        if (dodgesAfterSpend < 0) return false;

        // Must not go below weight threshold (iron feathers)
        if (dodgesAfterSpend < weight) return false;

        return true;
    }

    public static void addThirstExhaustion(EntityPlayer player, float amount) {
        if (!isSimpleDifficultyLoaded) return;
        try {
            Object thirst = getThirstDataMethod.invoke(null, player);
            if (thirst != null) {
                addThirstExhaustionMethod.invoke(thirst, amount);
            }
        } catch (Exception e) {
            // ignore
        }
    }

    public static net.minecraft.client.Minecraft getMinecraft() {
        if (getMinecraftMethod != null) {
            try {
                return (net.minecraft.client.Minecraft) getMinecraftMethod.invoke(null);
            } catch (Exception e) {
                // fall through
            }
        }
        return net.minecraftforge.fml.client.FMLClientHandler.instance().getClient();
    }

    public static EntityPlayer getClientPlayer() {
        try {
            EntityPlayer player = net.minecraftforge.fml.client.FMLClientHandler.instance().getClientPlayerEntity();
            if (player != null) return player;
        } catch (Throwable t) {
            // fall through
        }
        if (playerField != null) {
            net.minecraft.client.Minecraft mc = getMinecraft();
            if (mc != null) {
                try {
                    return (EntityPlayer) playerField.get(mc);
                } catch (Exception e) {}
            }
        }
        return null;
    }

    public static Object getMovementInput(EntityPlayer player) {
        if (player != null && movementInputField != null) {
            try {
                return movementInputField.get(player);
            } catch (Exception e) {}
        }
        return null;
    }

    public static boolean isJumpPressed(EntityPlayer player) {
        Object input = getMovementInput(player);
        if (input != null && jumpField != null) {
            try {
                return (Boolean) jumpField.get(input);
            } catch (Exception e) {}
        }
        return false;
    }

    public static void setJumpPressed(EntityPlayer player, boolean val) {
        Object input = getMovementInput(player);
        if (input != null && jumpField != null) {
            try {
                jumpField.set(input, val);
            } catch (Exception e) {}
        }
    }

    public static boolean isSneakPressed(EntityPlayer player) {
        Object input = getMovementInput(player);
        if (input != null && sneakField != null) {
            try {
                return (Boolean) sneakField.get(input);
            } catch (Exception e) {}
        }
        return false;
    }

    public static void setSneakPressed(EntityPlayer player, boolean val) {
        Object input = getMovementInput(player);
        if (input != null && sneakField != null) {
            try {
                sneakField.set(input, val);
            } catch (Exception e) {}
        }
    }

    public static float getMoveForward(EntityPlayer player) {
        Object input = getMovementInput(player);
        if (input != null && moveForwardField != null) {
            try {
                return (Float) moveForwardField.get(input);
            } catch (Exception e) {}
        }
        return 0.0F;
    }

    public static float getMoveStrafe(EntityPlayer player) {
        Object input = getMovementInput(player);
        if (input != null && moveStrafeField != null) {
            try {
                return (Float) moveStrafeField.get(input);
            } catch (Exception e) {}
        }
        return 0.0F;
    }

    public static double getPosX(Entity entity) {
        if (entity != null && posXField != null) {
            try {
                return (Double) posXField.get(entity);
            } catch (Exception e) {}
        }
        return 0.0D;
    }

    public static double getPosY(Entity entity) {
        if (entity != null && posYField != null) {
            try {
                return (Double) posYField.get(entity);
            } catch (Exception e) {}
        }
        return 0.0D;
    }

    public static double getPosZ(Entity entity) {
        if (entity != null && posZField != null) {
            try {
                return (Double) posZField.get(entity);
            } catch (Exception e) {}
        }
        return 0.0D;
    }

    public static double getMotionX(Entity entity) {
        if (entity != null && motionXField != null) {
            try {
                return (Double) motionXField.get(entity);
            } catch (Exception e) {}
        }
        return 0.0D;
    }

    public static void setMotionX(Entity entity, double val) {
        if (entity != null && motionXField != null) {
            try {
                motionXField.set(entity, val);
            } catch (Exception e) {}
        }
    }

    public static double getMotionZ(Entity entity) {
        if (entity != null && motionZField != null) {
            try {
                return (Double) motionZField.get(entity);
            } catch (Exception e) {}
        }
        return 0.0D;
    }

    public static void setMotionZ(Entity entity, double val) {
        if (entity != null && motionZField != null) {
            try {
                motionZField.set(entity, val);
            } catch (Exception e) {}
        }
    }

    public static boolean isOnGround(Entity entity) {
        if (entity != null && onGroundField != null) {
            try {
                return (Boolean) onGroundField.get(entity);
            } catch (Exception e) {}
        }
        return false;
    }

    public static float getRotationYaw(Entity entity) {
        if (entity != null && rotationYawField != null) {
            try {
                return (Float) rotationYawField.get(entity);
            } catch (Exception e) {}
        }
        return 0.0F;
    }

    public static boolean isSpectator(EntityPlayer player) {
        if (player == null) return false;
        if (isSpectatorMethod != null) {
            try {
                return (Boolean) isSpectatorMethod.invoke(player);
            } catch (Exception e) {
                // fall through
            }
        }
        return false;
    }

    public static boolean isSneaking(Entity player) {
        if (player == null) return false;
        if (isSneakingMethod != null) {
            try {
                return (Boolean) isSneakingMethod.invoke(player);
            } catch (Exception e) {
                // fall through
            }
        }
        return false;
    }

    public static boolean isInWater(Entity player) {
        if (player == null) return false;
        if (isInWaterMethod != null) {
            try {
                return (Boolean) isInWaterMethod.invoke(player);
            } catch (Exception e) {
                // fall through
            }
        }
        return false;
    }

    public static boolean isInLava(Entity player) {
        if (player == null) return false;
        if (isInLavaMethod != null) {
            try {
                return (Boolean) isInLavaMethod.invoke(player);
            } catch (Exception e) {
                // fall through
            }
        }
        return false;
    }

    public static boolean isRiding(Entity player) {
        if (player == null) return false;
        if (isRidingMethod != null) {
            try {
                return (Boolean) isRidingMethod.invoke(player);
            } catch (Exception e) {
                // fall through
            }
        }
        return false;
    }

    public static boolean isRemote(net.minecraft.world.World world) {
        if (world == null) return false;
        if (isRemoteField != null) {
            try {
                return (Boolean) isRemoteField.get(world);
            } catch (Exception e) {}
        }
        return !(world instanceof net.minecraft.world.WorldServer);
    }

    public static boolean isRemote(Entity entity) {
        if (entity == null) return false;
        net.minecraft.world.World world = getWorld(entity);
        return isRemote(world);
    }

    public static net.minecraft.world.World getWorld(Entity entity) {
        if (entity == null) return null;
        if (worldField != null) {
            try {
                return (net.minecraft.world.World) worldField.get(entity);
            } catch (Exception e) {}
        }
        return null;
    }

    public static int getTicksExisted(Entity entity) {
        if (entity != null && ticksExistedField != null) {
            try {
                return (Integer) ticksExistedField.get(entity);
            } catch (Exception e) {}
        }
        return 0;
    }

    public static int getDimension(Entity entity) {
        if (entity != null && dimensionField != null) {
            try {
                return (Integer) dimensionField.get(entity);
            } catch (Exception e) {}
        }
        return 0;
    }

    public static boolean isDead(Entity entity) {
        if (entity != null && isDeadField != null) {
            try {
                return (Boolean) isDeadField.get(entity);
            } catch (Exception e) {}
        }
        return false;
    }

    public static Entity getShootingEntity(Entity projectile) {
        if (projectile == null) return null;
        if (arrowShootingEntityField != null && projectile instanceof net.minecraft.entity.projectile.EntityArrow) {
            try {
                return (Entity) arrowShootingEntityField.get(projectile);
            } catch (Exception e) {}
        }
        if (fireballShootingEntityField != null && projectile instanceof net.minecraft.entity.projectile.EntityFireball) {
            try {
                return (Entity) fireballShootingEntityField.get(projectile);
            } catch (Exception e) {}
        }
        return null;
    }

    public static EntityPlayerMP getServerPlayer(net.minecraftforge.fml.common.network.simpleimpl.MessageContext ctx) {
        if (ctx == null) return null;
        try {
            net.minecraft.network.NetHandlerPlayServer handler = ctx.getServerHandler();
            if (handler == null) return null;
            if (netHandlerPlayerField != null) {
                return (EntityPlayerMP) netHandlerPlayerField.get(handler);
            }
            try {
                Field f = handler.getClass().getField("field_147369_b");
                return (EntityPlayerMP) f.get(handler);
            } catch (Throwable t) {
                try {
                    Field f = handler.getClass().getField("player");
                    return (EntityPlayerMP) f.get(handler);
                } catch (Throwable t2) {
                    Field f = handler.getClass().getField("playerEntity");
                    return (EntityPlayerMP) f.get(handler);
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    public static boolean isPlayerGliding(EntityPlayer player) {
        return isGliding(player);
    }

    public static boolean isGliderDeployed(EntityPlayer player) {
        return isGliding(player);
    }

    public static boolean isGrappleHookAttached(EntityPlayer player) {
        return isGrappling(player);
    }

    public static void unattachGrappleHook(EntityPlayer player) {
        detachGrapple(player);
    }

    public static Entity getTrueSource(DamageSource source) {
        if (source == null) return null;
        if (getTrueSourceMethod != null) {
            try {
                return (Entity) getTrueSourceMethod.invoke(source);
            } catch (Exception e) {}
        }
        if (damageSourceEntityField != null) {
            try {
                return (Entity) damageSourceEntityField.get(source);
            } catch (Exception e) {}
        }
        try {
            return source.getTrueSource();
        } catch (Throwable t) {}
        return null;
    }

    public static Entity getImmediateSource(DamageSource source) {
        if (source == null) return null;
        if (getImmediateSourceMethod != null) {
            try {
                return (Entity) getImmediateSourceMethod.invoke(source);
            } catch (Exception e) {}
        }
        try {
            return source.getImmediateSource();
        } catch (Throwable t) {}
        return null;
    }

    public static boolean isDamageAbsolute(DamageSource source) {
        if (source == null) return false;
        if (isDamageAbsoluteMethod != null) {
            try {
                return (Boolean) isDamageAbsoluteMethod.invoke(source);
            } catch (Exception e) {}
        }
        try {
            return source.isDamageAbsolute();
        } catch (Throwable t) {}
        return false;
    }

    public static boolean isUnblockable(DamageSource source) {
        if (source == null) return false;
        if (isUnblockableMethod != null) {
            try {
                return (Boolean) isUnblockableMethod.invoke(source);
            } catch (Exception e) {}
        }
        try {
            return source.isUnblockable();
        } catch (Throwable t) {}
        return false;
    }

    public static boolean isMagicDamage(DamageSource source) {
        if (source == null) return false;
        if (isMagicDamageMethod != null) {
            try {
                return (Boolean) isMagicDamageMethod.invoke(source);
            } catch (Exception e) {}
        }
        try {
            return source.isMagicDamage();
        } catch (Throwable t) {}
        return false;
    }

    public static boolean isActiveItemStackBlocking(EntityLivingBase entity) {
        if (entity == null) return false;
        if (isActiveItemStackBlockingMethod != null) {
            try {
                return (Boolean) isActiveItemStackBlockingMethod.invoke(entity);
            } catch (Exception e) {}
        }
        try {
            return entity.isActiveItemStackBlocking();
        } catch (Throwable t) {}
        return false;
    }

    public static ItemStack getActiveItemStack(EntityLivingBase entity) {
        if (entity == null) return ItemStack.EMPTY;
        if (getActiveItemStackMethod != null) {
            try {
                return (ItemStack) getActiveItemStackMethod.invoke(entity);
            } catch (Exception e) {}
        }
        try {
            return entity.getActiveItemStack();
        } catch (Throwable t) {}
        return ItemStack.EMPTY;
    }

    public static boolean isHandActive(EntityLivingBase entity) {
        if (entity == null) return false;
        if (isHandActiveMethod != null) {
            try {
                return (Boolean) isHandActiveMethod.invoke(entity);
            } catch (Exception e) {}
        }
        try {
            return entity.isHandActive();
        } catch (Throwable t) {}
        return false;
    }

    public static ItemStack getHeldItemMainhand(EntityLivingBase entity) {
        if (entity == null) return ItemStack.EMPTY;
        if (getHeldItemMainhandMethod != null) {
            try {
                return (ItemStack) getHeldItemMainhandMethod.invoke(entity);
            } catch (Exception e) {}
        }
        try {
            return entity.getHeldItemMainhand();
        } catch (Throwable t) {}
        return ItemStack.EMPTY;
    }

    public static boolean attackEntityFrom(Entity entity, DamageSource source, float amount) {
        if (entity == null || source == null) return false;
        if (attackEntityFromMethod != null && entity instanceof EntityLivingBase) {
            try {
                return (Boolean) attackEntityFromMethod.invoke(entity, source, amount);
            } catch (Exception e) {}
        }
        try {
            return entity.attackEntityFrom(source, amount);
        } catch (Throwable t) {}
        return false;
    }

    public static boolean isPlayerSleeping(EntityPlayer player) {
        if (player == null) return false;
        if (isPlayerSleepingMethod != null) {
            try {
                return (Boolean) isPlayerSleepingMethod.invoke(player);
            } catch (Exception e) {}
        }
        try {
            return player.isPlayerSleeping();
        } catch (Throwable t) {}
        return false;
    }

    public static boolean isDaytime(World world) {
        if (world == null) return true;
        if (isDaytimeMethod != null) {
            try {
                return (Boolean) isDaytimeMethod.invoke(world);
            } catch (Exception e) {}
        }
        try {
            return world.isDaytime();
        } catch (Throwable t) {}
        return world.getWorldTime() % 24000L < 12000L;
    }

    public static Item getItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        if (getItemMethod != null) {
            try {
                return (Item) getItemMethod.invoke(stack);
            } catch (Exception e) {}
        }
        try {
            return stack.getItem();
        } catch (Throwable t) {}
        return null;
    }

    public static int getMaxItemUseDuration(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        if (getMaxItemUseDurationMethod != null) {
            try {
                return (Integer) getMaxItemUseDurationMethod.invoke(stack);
            } catch (Exception e) {}
        }
        try {
            return stack.getMaxItemUseDuration();
        } catch (Throwable t) {}
        return 0;
    }

    public static EnumAction getItemUseAction(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return EnumAction.NONE;
        if (getItemUseActionMethod != null) {
            try {
                return (EnumAction) getItemUseActionMethod.invoke(stack);
            } catch (Exception e) {}
        }
        try {
            return stack.getItemUseAction();
        } catch (Throwable t) {}
        return EnumAction.NONE;
    }

    // NBT Helper Methods
    public static NBTTagCompound getEntityData(Entity entity) {
        if (entity == null) return new NBTTagCompound();
        if (entityGetEntityDataMethod != null) {
            try {
                return (NBTTagCompound) entityGetEntityDataMethod.invoke(entity);
            } catch (Exception e) {}
        }
        try {
            return entity.getEntityData();
        } catch (Throwable t) {}
        return new NBTTagCompound();
    }

    public static NBTTagCompound getPersistedTag(EntityPlayer player) {
        NBTTagCompound entityData = getEntityData(player);
        if (!hasKey(entityData, EntityPlayer.PERSISTED_NBT_TAG)) {
            NBTTagCompound persisted = new NBTTagCompound();
            setTag(entityData, EntityPlayer.PERSISTED_NBT_TAG, persisted);
            return persisted;
        }
        NBTTagCompound persisted = getCompoundTag(entityData, EntityPlayer.PERSISTED_NBT_TAG);
        if (persisted == null) {
            persisted = new NBTTagCompound();
            setTag(entityData, EntityPlayer.PERSISTED_NBT_TAG, persisted);
        }
        return persisted;
    }

    public static NBTTagCompound getCompoundTag(NBTTagCompound compound, String key) {
        if (compound == null || key == null) return new NBTTagCompound();
        if (nbtGetCompoundTagMethod != null) {
            try {
                return (NBTTagCompound) nbtGetCompoundTagMethod.invoke(compound, key);
            } catch (Exception e) {}
        }
        try {
            return compound.getCompoundTag(key);
        } catch (Throwable t) {}
        return new NBTTagCompound();
    }

    public static boolean hasKey(NBTTagCompound compound, String key) {
        if (compound == null || key == null) return false;
        if (nbtHasKeyMethod != null) {
            try {
                return (Boolean) nbtHasKeyMethod.invoke(compound, key);
            } catch (Exception e) {}
        }
        try {
            return compound.hasKey(key);
        } catch (Throwable t) {}
        return false;
    }

    public static int getInteger(NBTTagCompound compound, String key) {
        if (compound == null || key == null) return 0;
        if (nbtGetIntegerMethod != null) {
            try {
                return (Integer) nbtGetIntegerMethod.invoke(compound, key);
            } catch (Exception e) {}
        }
        try {
            return compound.getInteger(key);
        } catch (Throwable t) {}
        return 0;
    }

    public static void setInteger(NBTTagCompound compound, String key, int value) {
        if (compound == null || key == null) return;
        if (nbtSetIntegerMethod != null) {
            try {
                nbtSetIntegerMethod.invoke(compound, key, value);
                return;
            } catch (Exception e) {}
        }
        try {
            compound.setInteger(key, value);
        } catch (Throwable t) {}
    }

    public static boolean getBoolean(NBTTagCompound compound, String key) {
        if (compound == null || key == null) return false;
        if (nbtGetBooleanMethod != null) {
            try {
                return (Boolean) nbtGetBooleanMethod.invoke(compound, key);
            } catch (Exception e) {}
        }
        try {
            return compound.getBoolean(key);
        } catch (Throwable t) {}
        return false;
    }

    public static void setBoolean(NBTTagCompound compound, String key, boolean value) {
        if (compound == null || key == null) return;
        if (nbtSetBooleanMethod != null) {
            try {
                nbtSetBooleanMethod.invoke(compound, key, value);
                return;
            } catch (Exception e) {}
        }
        try {
            compound.setBoolean(key, value);
        } catch (Throwable t) {}
    }

    public static double getDouble(NBTTagCompound compound, String key) {
        if (compound == null || key == null) return 0.0D;
        if (nbtGetDoubleMethod != null) {
            try {
                return (Double) nbtGetDoubleMethod.invoke(compound, key);
            } catch (Exception e) {}
        }
        try {
            return compound.getDouble(key);
        } catch (Throwable t) {}
        return 0.0D;
    }

    public static void setDouble(NBTTagCompound compound, String key, double value) {
        if (compound == null || key == null) return;
        if (nbtSetDoubleMethod != null) {
            try {
                nbtSetDoubleMethod.invoke(compound, key, value);
                return;
            } catch (Exception e) {}
        }
        try {
            compound.setDouble(key, value);
        } catch (Throwable t) {}
    }

    public static String getString(NBTTagCompound compound, String key) {
        if (compound == null || key == null) return "";
        if (nbtGetStringMethod != null) {
            try {
                return (String) nbtGetStringMethod.invoke(compound, key);
            } catch (Exception e) {}
        }
        try {
            return compound.getString(key);
        } catch (Throwable t) {}
        return "";
    }

    public static void setString(NBTTagCompound compound, String key, String value) {
        if (compound == null || key == null) return;
        if (nbtSetStringMethod != null) {
            try {
                nbtSetStringMethod.invoke(compound, key, value);
                return;
            } catch (Exception e) {}
        }
        try {
            compound.setString(key, value);
        } catch (Throwable t) {}
    }

    public static int[] getIntArray(NBTTagCompound compound, String key) {
        if (compound == null || key == null) return new int[0];
        if (nbtGetIntArrayMethod != null) {
            try {
                return (int[]) nbtGetIntArrayMethod.invoke(compound, key);
            } catch (Exception e) {}
        }
        try {
            return compound.getIntArray(key);
        } catch (Throwable t) {}
        return new int[0];
    }

    public static void setIntArray(NBTTagCompound compound, String key, int[] value) {
        if (compound == null || key == null) return;
        if (nbtSetIntArrayMethod != null) {
            try {
                nbtSetIntArrayMethod.invoke(compound, key, value);
                return;
            } catch (Exception e) {}
        }
        try {
            compound.setIntArray(key, value);
        } catch (Throwable t) {}
    }

    public static NBTTagList getTagList(NBTTagCompound compound, String key, int type) {
        if (compound == null || key == null) return new NBTTagList();
        if (nbtGetTagListMethod != null) {
            try {
                return (NBTTagList) nbtGetTagListMethod.invoke(compound, key, type);
            } catch (Exception e) {}
        }
        try {
            return compound.getTagList(key, type);
        } catch (Throwable t) {}
        return new NBTTagList();
    }

    public static void removeTag(NBTTagCompound compound, String key) {
        if (compound == null || key == null) return;
        if (nbtRemoveTagMethod != null) {
            try {
                nbtRemoveTagMethod.invoke(compound, key);
                return;
            } catch (Exception e) {}
        }
        try {
            compound.removeTag(key);
        } catch (Throwable t) {}
    }

    public static void setTag(NBTTagCompound compound, String key, NBTBase value) {
        if (compound == null || key == null || value == null) return;
        if (nbtSetTagMethod != null) {
            try {
                nbtSetTagMethod.invoke(compound, key, value);
                return;
            } catch (Exception e) {}
        }
        try {
            compound.setTag(key, value);
        } catch (Throwable t) {}
    }

    public static int tagCount(NBTTagList list) {
        if (list == null) return 0;
        if (nbtTagCountMethod != null) {
            try {
                return (Integer) nbtTagCountMethod.invoke(list);
            } catch (Exception e) {}
        }
        try {
            return list.tagCount();
        } catch (Throwable t) {}
        return 0;
    }

    public static NBTTagCompound getCompoundTagAt(NBTTagList list, int index) {
        if (list == null) return new NBTTagCompound();
        if (nbtGetCompoundTagAtMethod != null) {
            try {
                return (NBTTagCompound) nbtGetCompoundTagAtMethod.invoke(list, index);
            } catch (Exception e) {}
        }
        try {
            return list.getCompoundTagAt(index);
        } catch (Throwable t) {}
        return new NBTTagCompound();
    }

    public static void appendTag(NBTTagList list, NBTBase value) {
        if (list == null || value == null) return;
        if (nbtAppendTagMethod != null) {
            try {
                nbtAppendTagMethod.invoke(list, value);
                return;
            } catch (Exception e) {}
        }
        try {
            list.appendTag(value);
        } catch (Throwable t) {}
    }

    public static AxisAlignedBB getEntityBoundingBox(Entity entity) {
        if (entity == null) return new AxisAlignedBB(0, 0, 0, 0, 0, 0);
        if (getEntityBoundingBoxMethod != null) {
            try {
                return (AxisAlignedBB) getEntityBoundingBoxMethod.invoke(entity);
            } catch (Exception e) {}
        }
        if (boundingBoxField != null) {
            try {
                AxisAlignedBB bb = (AxisAlignedBB) boundingBoxField.get(entity);
                if (bb != null) return bb;
            } catch (Exception e) {}
        }
        double x = getPosX(entity);
        double y = getPosY(entity);
        double z = getPosZ(entity);
        return new AxisAlignedBB(x - 0.3, y, z - 0.3, x + 0.3, y + 1.8, z + 0.3);
    }

    public static double getMinX(AxisAlignedBB bb) {
        if (bb == null) return 0.0;
        if (aabbMinXField != null) {
            try {
                return aabbMinXField.getDouble(bb);
            } catch (Throwable t) {}
        }
        return 0.0;
    }

    public static double getMinY(AxisAlignedBB bb) {
        if (bb == null) return 0.0;
        if (aabbMinYField != null) {
            try {
                return aabbMinYField.getDouble(bb);
            } catch (Throwable t) {}
        }
        return 0.0;
    }

    public static double getMinZ(AxisAlignedBB bb) {
        if (bb == null) return 0.0;
        if (aabbMinZField != null) {
            try {
                return aabbMinZField.getDouble(bb);
            } catch (Throwable t) {}
        }
        return 0.0;
    }

    public static double getMaxX(AxisAlignedBB bb) {
        if (bb == null) return 0.0;
        if (aabbMaxXField != null) {
            try {
                return aabbMaxXField.getDouble(bb);
            } catch (Throwable t) {}
        }
        return 0.0;
    }

    public static double getMaxY(AxisAlignedBB bb) {
        if (bb == null) return 0.0;
        if (aabbMaxYField != null) {
            try {
                return aabbMaxYField.getDouble(bb);
            } catch (Throwable t) {}
        }
        return 0.0;
    }

    public static double getMaxZ(AxisAlignedBB bb) {
        if (bb == null) return 0.0;
        if (aabbMaxZField != null) {
            try {
                return aabbMaxZField.getDouble(bb);
            } catch (Throwable t) {}
        }
        return 0.0;
    }

    public static int getX(BlockPos pos) {
        if (pos == null) return 0;
        if (vec3iGetXMethod != null) {
            try {
                return (Integer) vec3iGetXMethod.invoke(pos);
            } catch (Throwable t) {}
        }
        if (vec3iXField != null) {
            try {
                return vec3iXField.getInt(pos);
            } catch (Throwable t) {}
        }
        return 0;
    }

    public static int getY(BlockPos pos) {
        if (pos == null) return 0;
        if (vec3iGetYMethod != null) {
            try {
                return (Integer) vec3iGetYMethod.invoke(pos);
            } catch (Throwable t) {}
        }
        if (vec3iYField != null) {
            try {
                return vec3iYField.getInt(pos);
            } catch (Throwable t) {}
        }
        return 0;
    }

    public static int getZ(BlockPos pos) {
        if (pos == null) return 0;
        if (vec3iGetZMethod != null) {
            try {
                return (Integer) vec3iGetZMethod.invoke(pos);
            } catch (Throwable t) {}
        }
        if (vec3iZField != null) {
            try {
                return vec3iZField.getInt(pos);
            } catch (Throwable t) {}
        }
        return 0;
    }

    public static AxisAlignedBB grow(AxisAlignedBB bb, double value) {
        if (bb == null) return new AxisAlignedBB(0, 0, 0, 0, 0, 0);
        if (aabbGrowMethod != null) {
            try {
                return (AxisAlignedBB) aabbGrowMethod.invoke(bb, value);
            } catch (Exception e) {}
        }
        if (aabbGrowXYZMethod != null) {
            try {
                return (AxisAlignedBB) aabbGrowXYZMethod.invoke(bb, value, value, value);
            } catch (Exception e) {}
        }
        return new AxisAlignedBB(
            getMinX(bb) - value, getMinY(bb) - value, getMinZ(bb) - value,
            getMaxX(bb) + value, getMaxY(bb) + value, getMaxZ(bb) + value
        );
    }

    public static AxisAlignedBB grow(BlockPos pos, double radius) {
        if (pos == null) return new AxisAlignedBB(0, 0, 0, 0, 0, 0);
        int x = getX(pos);
        int y = getY(pos);
        int z = getZ(pos);
        return new AxisAlignedBB(x - radius, y - radius, z - radius, x + 1 + radius, y + 1 + radius, z + 1 + radius);
    }

    public static double getBoundingBoxMinY(Entity entity) {
        if (entity == null) return 0.0D;
        AxisAlignedBB bb = getEntityBoundingBox(entity);
        if (bb != null) return getMinY(bb);
        return getPosY(entity);
    }

    public static BlockPos getPosition(Entity entity) {
        if (entity == null) return BlockPos.ORIGIN;
        if (getPositionMethod != null) {
            try {
                return (BlockPos) getPositionMethod.invoke(entity);
            } catch (Exception e) {}
        }
        try {
            return entity.getPosition();
        } catch (Throwable t) {}
        return new BlockPos(getPosX(entity), getPosY(entity), getPosZ(entity));
    }

    public static UUID getUniqueID(Entity entity) {
        if (entity == null) return UUID.randomUUID();
        if (getUniqueIDMethod != null) {
            try {
                return (UUID) getUniqueIDMethod.invoke(entity);
            } catch (Exception e) {}
        }
        try {
            return entity.getUniqueID();
        } catch (Throwable t) {}
        return UUID.randomUUID();
    }

    public static MinecraftServer getServer(Entity entity) {
        if (entity == null) return null;
        if (getServerMethod != null) {
            try {
                return (MinecraftServer) getServerMethod.invoke(entity);
            } catch (Exception e) {}
        }
        try {
            return entity.getServer();
        } catch (Throwable t) {}
        return null;
    }

    public static void sendMessage(net.minecraft.command.ICommandSender sender, ITextComponent message) {
        if (sender == null || message == null) return;
        if (sendMessageMethod != null) {
            try {
                sendMessageMethod.invoke(sender, message);
                return;
            } catch (Exception e) {}
        }
        try {
            sender.sendMessage(message);
        } catch (Throwable t) {}
    }

    public static void addPotionEffect(EntityLivingBase entity, PotionEffect effect) {
        if (entity == null || effect == null) return;
        if (addPotionEffectMethod != null) {
            try {
                addPotionEffectMethod.invoke(entity, effect);
                return;
            } catch (Exception e) {}
        }
        try {
            entity.addPotionEffect(effect);
        } catch (Throwable t) {}
    }

    public static void removePotionEffect(EntityLivingBase entity, Potion potion) {
        if (entity == null || potion == null) return;
        if (removePotionEffectMethod != null) {
            try {
                removePotionEffectMethod.invoke(entity, potion);
                return;
            } catch (Exception e) {}
        }
        try {
            entity.removePotionEffect(potion);
        } catch (Throwable t) {}
    }

    public static IBlockState getBlockState(World world, BlockPos pos) {
        if (world == null || pos == null) return getDefaultState(net.minecraft.init.Blocks.AIR);
        if (getBlockStateMethod != null) {
            try {
                return (IBlockState) getBlockStateMethod.invoke(world, pos);
            } catch (Exception e) {}
        }
        try {
            return world.getBlockState(pos);
        } catch (Throwable t) {}
        return getDefaultState(net.minecraft.init.Blocks.AIR);
    }

    public static IBlockState getBlockState(ChunkPrimer primer, int x, int y, int z) {
        if (primer == null) return getAirState();
        if (chunkPrimerGetBlockStateMethod != null) {
            try {
                IBlockState state = (IBlockState) chunkPrimerGetBlockStateMethod.invoke(primer, x, y, z);
                return state != null ? state : getAirState();
            } catch (Exception e) {}
        }
        try {
            IBlockState state = primer.getBlockState(x, y, z);
            return state != null ? state : getAirState();
        } catch (Throwable t) {}
        return getAirState();
    }

    public static void setBlockState(ChunkPrimer primer, int x, int y, int z, IBlockState state) {
        if (primer == null || state == null) return;
        if (chunkPrimerSetBlockStateMethod != null) {
            try {
                chunkPrimerSetBlockStateMethod.invoke(primer, x, y, z, state);
                return;
            } catch (Exception e) {}
        }
        try {
            primer.setBlockState(x, y, z, state);
        } catch (Throwable t) {}
    }

    public static IBlockState getBlockState(Chunk chunk, BlockPos pos) {
        if (chunk == null || pos == null) return getAirState();
        if (chunkGetBlockStateMethod != null) {
            try {
                IBlockState state = (IBlockState) chunkGetBlockStateMethod.invoke(chunk, pos);
                return state != null ? state : getAirState();
            } catch (Exception e) {}
        }
        try {
            IBlockState state = chunk.getBlockState(pos);
            return state != null ? state : getAirState();
        } catch (Throwable t) {}
        return getAirState();
    }

    public static IBlockState setBlockState(Chunk chunk, BlockPos pos, IBlockState state) {
        if (chunk == null || pos == null || state == null) return null;
        if (chunkSetBlockStateMethod != null) {
            try {
                return (IBlockState) chunkSetBlockStateMethod.invoke(chunk, pos, state);
            } catch (Exception e) {}
        }
        try {
            return chunk.setBlockState(pos, state);
        } catch (Throwable t) {}
        return null;
    }

    public static BlockPos.MutableBlockPos setPos(BlockPos.MutableBlockPos pos, int x, int y, int z) {
        if (pos == null) return null;
        if (mutableBlockPosSetPosMethod != null) {
            try {
                Object result = mutableBlockPosSetPosMethod.invoke(pos, x, y, z);
                if (result instanceof BlockPos.MutableBlockPos) {
                    return (BlockPos.MutableBlockPos) result;
                }
                return pos;
            } catch (Exception e) {}
        }
        try {
            return pos.setPos(x, y, z);
        } catch (Throwable t) {}
        return pos;
    }

    public static Material getMaterial(IBlockState state) {
        if (state == null) return getMaterialAir();
        if (blockStateGetMaterialMethod != null) {
            try {
                Material mat = (Material) blockStateGetMaterialMethod.invoke(state);
                return mat != null ? mat : getMaterialAir();
            } catch (Exception e) {}
        }
        try {
            Material mat = state.getMaterial();
            return mat != null ? mat : getMaterialAir();
        } catch (Throwable t) {}
        return getMaterialAir();
    }

    private static Material getMaterialField(Field field) {
        if (field != null) {
            try {
                Object value = field.get(null);
                if (value instanceof Material) return (Material) value;
            } catch (Throwable t) {}
        }
        return null;
    }

    public static Material getMaterialAir() {
        Material mat = getMaterialField(materialAirField);
        if (mat != null) return mat;
        try { return Material.AIR; } catch (Throwable t) { return null; }
    }

    public static Material getMaterialWater() {
        Material mat = getMaterialField(materialWaterField);
        if (mat != null) return mat;
        try { return Material.WATER; } catch (Throwable t) { return null; }
    }

    public static Material getMaterialLava() {
        Material mat = getMaterialField(materialLavaField);
        if (mat != null) return mat;
        try { return Material.LAVA; } catch (Throwable t) { return null; }
    }

    public static Material getMaterialRock() {
        Material mat = getMaterialField(materialRockField);
        if (mat != null) return mat;
        try { return Material.ROCK; } catch (Throwable t) { return null; }
    }

    public static Material getMaterialGround() {
        Material mat = getMaterialField(materialGroundField);
        if (mat != null) return mat;
        try { return Material.GROUND; } catch (Throwable t) { return null; }
    }

    public static Material getMaterialClay() {
        Material mat = getMaterialField(materialClayField);
        if (mat != null) return mat;
        try { return Material.CLAY; } catch (Throwable t) { return null; }
    }

    public static Material getMaterialSand() {
        Material mat = getMaterialField(materialSandField);
        if (mat != null) return mat;
        try { return Material.SAND; } catch (Throwable t) { return null; }
    }

    public static Material getMaterialGrass() {
        Material mat = getMaterialField(materialGrassField);
        if (mat != null) return mat;
        try { return Material.GRASS; } catch (Throwable t) { return null; }
    }

    public static Material getMaterialIce() {
        Material mat = getMaterialField(materialIceField);
        if (mat != null) return mat;
        try { return Material.ICE; } catch (Throwable t) { return null; }
    }

    public static Material getMaterialPackedIce() {
        Material mat = getMaterialField(materialPackedIceField);
        if (mat != null) return mat;
        try { return Material.PACKED_ICE; } catch (Throwable t) { return null; }
    }

    public static Material getMaterialCraftedSnow() {
        Material mat = getMaterialField(materialCraftedSnowField);
        if (mat != null) return mat;
        try { return Material.CRAFTED_SNOW; } catch (Throwable t) { return null; }
    }

    public static boolean isBlockLoaded(World world, BlockPos pos) {
        if (world == null || pos == null) return false;
        if (isBlockLoadedMethod != null) {
            try {
                return (Boolean) isBlockLoadedMethod.invoke(world, pos);
            } catch (Exception e) {}
        }
        try {
            return world.isBlockLoaded(pos);
        } catch (Throwable t) {}
        return false;
    }

    @SuppressWarnings("unchecked")
    public static <T extends Entity> List<T> getEntitiesWithinAABB(World world, Class<? extends T> clazz, AxisAlignedBB aabb) {
        if (world == null || clazz == null || aabb == null) return Collections.emptyList();
        if (getEntitiesWithinAABBMethod != null) {
            try {
                return (List<T>) getEntitiesWithinAABBMethod.invoke(world, clazz, aabb);
            } catch (Exception e) {}
        }
        try {
            return world.getEntitiesWithinAABB(clazz, aabb);
        } catch (Throwable t) {}
        return Collections.emptyList();
    }

    public static void playSound(World world, EntityPlayer player, double x, double y, double z, SoundEvent sound, SoundCategory category, float volume, float pitch) {
        if (world == null || sound == null) return;
        if (playSoundMethod != null) {
            try {
                playSoundMethod.invoke(world, player, x, y, z, sound, category, volume, pitch);
                return;
            } catch (Exception e) {}
        }
        try {
            world.playSound(player, x, y, z, sound, category, volume, pitch);
        } catch (Throwable t) {}
    }

    @SuppressWarnings("unchecked")
    public static SoundEvent getSoundEvent(ResourceLocation loc) {
        if (loc == null) return null;
        if (soundEventRegistryField != null) {
            try {
                Object reg = soundEventRegistryField.get(null);
                if (reg != null) {
                    if (registryGetObjectMethod != null) {
                        return (SoundEvent) registryGetObjectMethod.invoke(reg, loc);
                    }
                    if (reg instanceof net.minecraft.util.registry.RegistryNamespaced) {
                        return ((net.minecraft.util.registry.RegistryNamespaced<ResourceLocation, SoundEvent>) reg).getObject(loc);
                    }
                }
            } catch (Throwable t) {}
        }
        try {
            return SoundEvent.REGISTRY.getObject(loc);
        } catch (Throwable t) {}
        return null;
    }

    public static SoundEvent getSoundEvent(String soundName) {
        if (soundName == null || soundName.trim().isEmpty()) return null;
        return getSoundEvent(new ResourceLocation(soundName.trim()));
    }

    public static Block getBlock(IBlockState state) {
        if (state == null) return net.minecraft.init.Blocks.AIR;
        if (blockStateGetBlockMethod != null) {
            try {
                return (Block) blockStateGetBlockMethod.invoke(state);
            } catch (Exception e) {}
        }
        try {
            return state.getBlock();
        } catch (Throwable t) {}
        return net.minecraft.init.Blocks.AIR;
    }

    public static Block getBlock(World world, BlockPos pos) {
        if (world == null || pos == null) return net.minecraft.init.Blocks.AIR;
        IBlockState state = getBlockState(world, pos);
        return getBlock(state);
    }

    public static AxisAlignedBB getCollisionBoundingBox(IBlockState state, World world, BlockPos pos) {
        if (state == null || world == null || pos == null) return Block.NULL_AABB;
        if (blockStateGetCollisionBoundingBoxMethod != null) {
            try {
                return (AxisAlignedBB) blockStateGetCollisionBoundingBoxMethod.invoke(state, world, pos);
            } catch (Exception e) {}
        }
        try {
            return state.getCollisionBoundingBox(world, pos);
        } catch (Throwable t) {}
        return Block.NULL_AABB;
    }

    public static boolean isEmpty(ItemStack stack) {
        if (stack == null) return true;
        if (itemStackIsEmptyMethod != null) {
            try {
                return (Boolean) itemStackIsEmptyMethod.invoke(stack);
            } catch (Exception e) {}
        }
        try {
            return stack.isEmpty();
        } catch (Throwable t) {}
        return false;
    }

    public static Biome getBiome(World world, BlockPos pos) {
        if (world == null || pos == null) return null;
        if (worldGetBiomeMethod != null) {
            try {
                return (Biome) worldGetBiomeMethod.invoke(world, pos);
            } catch (Exception e) {}
        }
        try {
            return world.getBiome(pos);
        } catch (Throwable t) {}
        return null;
    }

    public static long getSeed(World world) {
        if (world == null) return 0L;
        if (worldGetSeedMethod != null) {
            try {
                return (Long) worldGetSeedMethod.invoke(world);
            } catch (Exception e) {}
        }
        try {
            return world.getSeed();
        } catch (Throwable t) {}
        return 0L;
    }

    public static MapStorage getMapStorage(World world) {
        if (world == null) return null;
        if (worldGetMapStorageMethod != null) {
            try {
                return (MapStorage) worldGetMapStorageMethod.invoke(world);
            } catch (Exception e) {}
        }
        try {
            return world.getMapStorage();
        } catch (Throwable t) {}
        return null;
    }

    public static boolean isAir(Block block, IBlockState state, World world, BlockPos pos) {
        if (block == null || block == net.minecraft.init.Blocks.AIR) return true;
        if (blockIsAirMethod != null) {
            try {
                return (Boolean) blockIsAirMethod.invoke(block, state, world, pos);
            } catch (Exception e) {}
        }
        try {
            return block.isAir(state, world, pos);
        } catch (Throwable t) {}
        return false;
    }

    public static boolean isAir(IBlockState state, World world, BlockPos pos) {
        if (state == null) return true;
        Block block = getBlock(state);
        return isAir(block, state, world, pos);
    }

    public static SoundType getSoundType(Block block, IBlockState state, World world, BlockPos pos, Entity entity) {
        if (block == null) return SoundType.STONE;
        if (blockGetSoundTypeMethod != null) {
            try {
                return (SoundType) blockGetSoundTypeMethod.invoke(block, state, world, pos, entity);
            } catch (Exception e) {}
        }
        if (blockSoundTypeField != null) {
            try {
                SoundType st = (SoundType) blockSoundTypeField.get(block);
                if (st != null) return st;
            } catch (Exception e) {}
        }
        try {
            return block.getSoundType(state, world, pos, entity);
        } catch (Throwable t) {}
        return SoundType.STONE;
    }

    public static BlockPos up(BlockPos pos) {
        if (pos == null) return BlockPos.ORIGIN;
        if (blockPosUpMethod != null) {
            try {
                return (BlockPos) blockPosUpMethod.invoke(pos);
            } catch (Exception e) {}
        }
        return new BlockPos(getX(pos), getY(pos) + 1, getZ(pos));
    }

    public static BlockPos up(BlockPos pos, int n) {
        if (pos == null) return BlockPos.ORIGIN;
        if (blockPosUpIntMethod != null) {
            try {
                return (BlockPos) blockPosUpIntMethod.invoke(pos, n);
            } catch (Exception e) {}
        }
        return new BlockPos(getX(pos), getY(pos) + n, getZ(pos));
    }

    public static BlockPos down(BlockPos pos) {
        if (pos == null) return BlockPos.ORIGIN;
        if (blockPosDownMethod != null) {
            try {
                return (BlockPos) blockPosDownMethod.invoke(pos);
            } catch (Exception e) {}
        }
        return new BlockPos(getX(pos), getY(pos) - 1, getZ(pos));
    }

    public static BlockPos down(BlockPos pos, int n) {
        if (pos == null) return BlockPos.ORIGIN;
        if (blockPosDownIntMethod != null) {
            try {
                return (BlockPos) blockPosDownIntMethod.invoke(pos, n);
            } catch (Exception e) {}
        }
        return new BlockPos(getX(pos), getY(pos) - n, getZ(pos));
    }

    public static BlockPos add(BlockPos pos, int x, int y, int z) {
        if (pos == null) return new BlockPos(x, y, z);
        if (blockPosAddMethod != null) {
            try {
                return (BlockPos) blockPosAddMethod.invoke(pos, x, y, z);
            } catch (Exception e) {}
        }
        return new BlockPos(getX(pos) + x, getY(pos) + y, getZ(pos) + z);
    }

    @SuppressWarnings("unchecked")
    public static <T extends net.minecraft.world.storage.WorldSavedData> T getOrLoadData(MapStorage storage, Class<T> clazz, String dataIdentifier) {
        if (storage == null || clazz == null || dataIdentifier == null) return null;
        if (mapStorageGetOrLoadDataMethod != null) {
            try {
                return (T) mapStorageGetOrLoadDataMethod.invoke(storage, clazz, dataIdentifier);
            } catch (Exception e) {}
        }
        try {
            return (T) storage.getOrLoadData(clazz, dataIdentifier);
        } catch (Throwable t) {}
        return null;
    }

    public static void setData(MapStorage storage, String dataIdentifier, net.minecraft.world.storage.WorldSavedData data) {
        if (storage == null || dataIdentifier == null || data == null) return;
        if (mapStorageSetDataMethod != null) {
            try {
                mapStorageSetDataMethod.invoke(storage, dataIdentifier, data);
                return;
            } catch (Exception e) {}
        }
        try {
            storage.setData(dataIdentifier, data);
        } catch (Throwable t) {}
    }

    public static void markDirty(net.minecraft.world.storage.WorldSavedData data) {
        if (data == null) return;
        if (worldSavedDataMarkDirtyMethod != null) {
            try {
                worldSavedDataMarkDirtyMethod.invoke(data);
                return;
            } catch (Exception e) {}
        }
        try {
            data.markDirty();
        } catch (Throwable t) {}
    }

    public static Block getBlockFromName(String name) {
        if (name == null) return null;
        if (getBlockFromNameMethod != null) {
            try {
                Block b = (Block) getBlockFromNameMethod.invoke(null, name);
                if (b != null) return b;
            } catch (Throwable t) {}
        }
        if (blockRegistryField != null && registryGetObjectMethod != null) {
            try {
                Object reg = blockRegistryField.get(null);
                if (reg != null) {
                    ResourceLocation loc = new ResourceLocation(name.contains(":") ? name : "minecraft:" + name);
                    Block b = (Block) registryGetObjectMethod.invoke(reg, loc);
                    if (b != null && b != getAirBlock()) return b;
                }
            } catch (Throwable t) {}
        }
        return null;
    }

    public static Block getStoneBlock() {
        Block b = getBlockFromName("stone");
        if (b != null) return b;
        return getBlockFromName("minecraft:stone");
    }

    public static Block getBedrockBlock() {
        Block b = getBlockFromName("bedrock");
        if (b != null) return b;
        return getBlockFromName("minecraft:bedrock");
    }

    public static Block getAirBlock() {
        Block b = getBlockFromName("air");
        if (b != null) return b;
        return getBlockFromName("minecraft:air");
    }

    public static Block getLavaBlock() {
        Block b = getBlockFromName("lava");
        if (b != null) return b;
        return getBlockFromName("minecraft:lava");
    }

    public static Block getWaterBlock() {
        Block b = getBlockFromName("water");
        if (b != null) return b;
        return getBlockFromName("minecraft:water");
    }

    public static Block getDeepslateBlock() {
        Block b = getBlockFromName("depthsupdate:deepslate");
        if (b != null) return b;
        return getStoneBlock();
    }

    public static IBlockState getDefaultState(Block block) {
        if (block == null) return null;
        if (blockGetDefaultStateMethod != null) {
            try {
                return (IBlockState) blockGetDefaultStateMethod.invoke(block);
            } catch (Exception e) {}
        }
        try {
            return block.getDefaultState();
        } catch (Throwable t) {}
        return null;
    }

    public static IBlockState getStoneState() {
        return getDefaultState(getStoneBlock());
    }

    public static IBlockState getBedrockState() {
        return getDefaultState(getBedrockBlock());
    }

    public static IBlockState getAirState() {
        return getDefaultState(getAirBlock());
    }

    public static IBlockState getLavaState() {
        return getDefaultState(getLavaBlock());
    }

    public static IBlockState getWaterState() {
        return getDefaultState(getWaterBlock());
    }

    public static IBlockState getDeepslateState() {
        Block b = getDeepslateBlock();
        IBlockState state = getDefaultState(b);
        return state != null ? state : getStoneState();
    }

    public static Biome getPlainsBiome() {
        try {
            Biome b = Biome.REGISTRY.getObject(new ResourceLocation("plains"));
            if (b != null) return b;
        } catch (Throwable t) {}
        return null;
    }

    public static void bindTexture(net.minecraft.client.Minecraft mc, ResourceLocation location) {
        if (mc == null || location == null) return;
        Object textureManager = null;
        if (getTextureManagerMethod != null) {
            try {
                textureManager = getTextureManagerMethod.invoke(mc);
            } catch (Throwable t) {}
        }
        if (textureManager == null && renderEngineField != null) {
            try {
                textureManager = renderEngineField.get(mc);
            } catch (Throwable t) {}
        }
        if (textureManager == null) {
            try {
                textureManager = mc.getTextureManager();
            } catch (Throwable t) {}
        }

        if (textureManager != null) {
            if (bindTextureMethod != null) {
                try {
                    bindTextureMethod.invoke(textureManager, location);
                    return;
                } catch (Throwable t) {}
            }
            try {
                ((net.minecraft.client.renderer.texture.TextureManager) textureManager).bindTexture(location);
            } catch (Throwable t) {}
        }
    }
}
