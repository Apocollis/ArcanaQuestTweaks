package com.apocollis.aqtweaks.client.thaumcraft;

import com.apocollis.aqtweaks.ArcanaQuestTweaks;
import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.thaumcraft.VisBatteryCharge;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

/** Client side of the portable Vis Battery: item models for damage 0..10 and the stored-vis tooltip. */
@Mod.EventBusSubscriber(value = Side.CLIENT, modid = ArcanaQuestTweaks.MODID)
public final class VisBatteryClient {
    private static final ResourceLocation NAME = new ResourceLocation("thaumcraft", "vis_battery");

    private VisBatteryClient() {}

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        if (!Loader.isModLoaded("thaumcraft")) return;
        Item item = Item.REGISTRY.getObject(NAME);
        if (item == null) return;
        boolean charged = ArcanaQuestTweaksConfig.ThaumcraftConfig.chargedBatteryModel;
        for (int damage = 1; damage <= VisBatteryCharge.MAX; damage++) {
            ModelResourceLocation model = charged
                    ? new ModelResourceLocation(NAME, "charge=" + damage)
                    : new ModelResourceLocation(NAME, "inventory");
            ModelLoader.setCustomModelResourceLocation(item, damage, model);
        }
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        if (!ArcanaQuestTweaksConfig.ThaumcraftConfig.visBatteryChargeTooltip) return;
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty() || stack.getMetadata() <= 0 || !NAME.equals(stack.getItem().getRegistryName())) return;
        event.getToolTip().add(TextFormatting.AQUA + I18n.format("aqtweaks.vis_battery.charge",
                Math.min(stack.getMetadata(), VisBatteryCharge.MAX)));
    }
}
