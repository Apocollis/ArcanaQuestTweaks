package com.apocollis.aqtweaks;

import com.apocollis.aqtweaks.roguelike.CommandWhereAmI;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;

@Mod(modid = ArcanaQuestTweaks.MODID, name = ArcanaQuestTweaks.NAME, version = ArcanaQuestTweaks.VERSION, dependencies = "required-after:elenaidodge2;after:grimoireofgaia;after:thaumcraft;after:bewitchment;after:grapplemod;after:embers")
public class ArcanaQuestTweaks {
    public static final String MODID = "aqtweaks";
    public static final String NAME = "Arcana Quest Tweaks";
    public static final String VERSION = "1.5";

    public static final SimpleNetworkWrapper NETWORK = NetworkRegistry.INSTANCE.newSimpleChannel(MODID);

    @Mod.Instance(MODID)
    public static ArcanaQuestTweaks instance;

    @SidedProxy(clientSide = "com.apocollis.aqtweaks.ClientProxy", serverSide = "com.apocollis.aqtweaks.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
    }

    @Mod.EventHandler
    public void serverStarting(net.minecraftforge.fml.common.event.FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandWhereAmI());
    }
}
