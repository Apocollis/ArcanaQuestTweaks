package com.apocollis.aqtweaks.additions;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.google.common.collect.Multimap;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.util.EnumHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

public class ItemInfinityBlade extends ItemSword {
    private static final ToolMaterial MATERIAL = EnumHelper.addToolMaterial(
            "AQTWEAKS_INFINITY_BLADE",
            4,
            6000,
            9.0F,
            13.0F,
            22
    );

    public ItemInfinityBlade() {
        super(MATERIAL);
        this.setCreativeTab(CreativeTabs.COMBAT);
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        return InfinityBladeStats.maxDamage(ArcanaQuestTweaksConfig.AdditionsModuleConfig.infinityBlade.durability);
    }

    @Override
    public int getMaxDamage() {
        return InfinityBladeStats.maxDamage(ArcanaQuestTweaksConfig.AdditionsModuleConfig.infinityBlade.durability);
    }

    @Override
    public float getAttackDamage() {
        double modifier = InfinityBladeStats.attackDamageModifier(
                ArcanaQuestTweaksConfig.AdditionsModuleConfig.infinityBlade.attackDamage);
        return (float) ( modifier - 3.0D );
    }

    @Override
    public Multimap<String, AttributeModifier> getItemAttributeModifiers(EntityEquipmentSlot equipmentSlot) {
        Multimap<String, AttributeModifier> multimap = super.getItemAttributeModifiers(equipmentSlot);
        if (equipmentSlot == EntityEquipmentSlot.MAINHAND) {
            multimap.removeAll(SharedMonsterAttributes.ATTACK_DAMAGE.getName());
            multimap.removeAll(SharedMonsterAttributes.ATTACK_SPEED.getName());

            double damageModifier = InfinityBladeStats.attackDamageModifier(
                    ArcanaQuestTweaksConfig.AdditionsModuleConfig.infinityBlade.attackDamage);
            double speedModifier = InfinityBladeStats.attackSpeedModifier(
                    ArcanaQuestTweaksConfig.AdditionsModuleConfig.infinityBlade.attackSpeed);

            multimap.put(SharedMonsterAttributes.ATTACK_DAMAGE.getName(),
                    new AttributeModifier(ATTACK_DAMAGE_MODIFIER, "Weapon modifier", damageModifier, 0));
            multimap.put(SharedMonsterAttributes.ATTACK_SPEED.getName(),
                    new AttributeModifier(ATTACK_SPEED_MODIFIER, "Weapon modifier", speedModifier, 0));
        }
        return multimap;
    }

    @Override
    public boolean getIsRepairable(ItemStack toRepair, ItemStack repair) {
        if (repair.isEmpty()) {
            return false;
        }
        String repairId = InfinityBladeStats.normalizeRepairItemId(
                ArcanaQuestTweaksConfig.AdditionsModuleConfig.infinityBlade.repairItem);
        if (repairId == null) {
            return false;
        }
        Item repairTarget = Item.REGISTRY.getObject(new ResourceLocation(repairId));
        return repairTarget != null && repair.getItem() == repairTarget;
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return EnumRarity.EPIC;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(TextFormatting.GRAY + "" + TextFormatting.ITALIC
                + I18n.format("tooltip.aqtweaks.infinity_blade.desc"));
    }
}
