package com.apocollis.aqtweaks.weapon;

import com.apocollis.aqtweaks.ArcanaQuestTweaks;
import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig.InfinityBladeModuleConfig;
import com.google.common.collect.Multimap;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.util.ResourceLocation;

/** The Infinity Blade. Damage, speed, durability and the anvil repair item come from config. */
public class ItemInfinityBlade extends ItemSword {

    public ItemInfinityBlade() {
        super(Item.ToolMaterial.DIAMOND);
        setRegistryName(ArcanaQuestTweaks.MODID, "infinity_blade");
        setTranslationKey("aqtweaks.infinity_blade");
        setMaxDamage(InfinityBladeModuleConfig.general.durability);
        setMaxStackSize(1);
        setCreativeTab(CreativeTabs.COMBAT);
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        return InfinityBladeModuleConfig.general.durability;
    }

    /** The tier is set through the rarity overhaul's Custom Item Rarities; natively this is Epic. */
    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return EnumRarity.EPIC;
    }

    @Override
    public boolean getIsRepairable(ItemStack toRepair, ItemStack repair) {
        if (repair.isEmpty()) return false;
        ResourceLocation id = repair.getItem().getRegistryName();
        return id != null && InfinityBladeStats.repairMatches(InfinityBladeModuleConfig.general.repairItem, id.toString());
    }

    @Override
    public float getAttackDamage() {
        return (float) Math.max(0.0D, InfinityBladeModuleConfig.general.attackDamage - 4.0D);
    }

    @Override
    public Multimap<String, AttributeModifier> getItemAttributeModifiers(EntityEquipmentSlot equipmentSlot) {
        Multimap<String, AttributeModifier> multimap = super.getItemAttributeModifiers(equipmentSlot);
        if (equipmentSlot == EntityEquipmentSlot.MAINHAND) {
            multimap.removeAll(SharedMonsterAttributes.ATTACK_DAMAGE.getName());
            multimap.removeAll(SharedMonsterAttributes.ATTACK_SPEED.getName());
            multimap.put(SharedMonsterAttributes.ATTACK_DAMAGE.getName(), new AttributeModifier(ATTACK_DAMAGE_MODIFIER,
                    "Weapon modifier", InfinityBladeStats.damageModifier(InfinityBladeModuleConfig.general.attackDamage), 0));
            multimap.put(SharedMonsterAttributes.ATTACK_SPEED.getName(), new AttributeModifier(ATTACK_SPEED_MODIFIER,
                    "Weapon modifier", InfinityBladeStats.speedModifier(InfinityBladeModuleConfig.general.attackSpeed), 0));
        }
        return multimap;
    }
}
