package com.apocollis.aqtweaks.additions;

import com.apocollis.aqtweaks.ArcanaQuestTweaks;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.GameRegistry;

@Mod.EventBusSubscriber(modid = ArcanaQuestTweaks.MODID)
public final class AdditionsModule {
    public static final String INFINITY_BLADE_ID = "infinity_blade";
    public static final ResourceLocation INFINITY_BLADE_REGISTRY_NAME =
            new ResourceLocation(ArcanaQuestTweaks.MODID, INFINITY_BLADE_ID);

    @GameRegistry.ObjectHolder(ArcanaQuestTweaks.MODID + ":" + INFINITY_BLADE_ID)
    public static final Item INFINITY_BLADE = null;

    private AdditionsModule() {
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        ItemInfinityBlade blade = new ItemInfinityBlade();
        blade.setRegistryName(INFINITY_BLADE_REGISTRY_NAME);
        blade.setTranslationKey(ArcanaQuestTweaks.MODID + "." + INFINITY_BLADE_ID);
        blade.addPropertyOverride(new ResourceLocation("blocking"),
                (ItemStack stack, net.minecraft.world.World world, EntityLivingBase entity) ->
                        entity != null && entity.isHandActive() && entity.getActiveItemStack() == stack ? 1.0F : 0.0F);
        event.getRegistry().register(blade);
    }

    @SubscribeEvent
    public static void registerEntities(RegistryEvent.Register<EntityEntry> event) {
        if (Loader.isModLoaded("geckolib3") && Loader.isModLoaded("thaumcraft")) {
            NamelessBossRegistry.register(event);
        }
    }
}
