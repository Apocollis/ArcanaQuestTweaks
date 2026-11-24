package com.apocollis.aqtweaks.thaumcraft.battlemage;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.Set;

/**
 * Applies a gear stack's Quality Tools modifiers to a mob. Quality Tools (tmtravlr) writes {@code Quality.AttributeModifiers}
 * onto the stack but only applies them to players (its {@code CommonEventHandler} takes an {@code EntityPlayer}), so a
 * mob that wears a Quality item would get the base item and none of the quality bonuses. The Battlemage's gear carries
 * Quality NBT (plan 14, 15, 15a); this reads that list and applies each modifier straight to the mob's attribute map.
 *
 * <p>An attribute the mob does not have (the mob has no attack speed attribute, PotionCore's magic shielding, jump
 * height, step height and magic damage exist only where PotionCore registers them) is skipped and logged once. A
 * modifier whose UUID is already on the attribute is left alone, so calling this twice, or after a reload (modifiers are
 * saved with the entity), never stacks.
 */
public final class GearAttributes {

    private static final Logger LOG = LogManager.getLogger("AQTweaks-Battlemage");
    private static final Set<String> MISSING_LOGGED = new HashSet<>();

    private GearAttributes() {}

    /** @return the number of modifiers applied */
    public static int applyQuality(EntityLivingBase mob, ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasTagCompound()) {
            return 0;
        }
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null || !tag.hasKey("Quality", 10)) {
            return 0;
        }
        NBTTagList mods = tag.getCompoundTag("Quality").getTagList("AttributeModifiers", 10);
        int applied = 0;
        for (int i = 0; i < mods.tagCount(); i++) {
            NBTTagCompound m = mods.getCompoundTagAt(i);
            String attribute = m.getString("AttributeName");
            IAttributeInstance inst = mob.getAttributeMap().getAttributeInstanceByName(attribute);
            if (inst == null) {
                if (MISSING_LOGGED.add(attribute)) {
                    LOG.info("Battlemage gear modifier on {} skipped: the mob has no such attribute", attribute);
                }
                continue;
            }
            AttributeModifier mod = SharedMonsterAttributes.readAttributeModifierFromNBT(m);
            if (mod == null || inst.getModifier(mod.getID()) != null) {
                continue;
            }
            inst.applyModifier(mod);
            applied++;
        }
        return applied;
    }
}
