package com.apocollis.aqtweaks.gaia.client;

import com.apocollis.aqtweaks.gaia.EntityDeepDwarf;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class GaiaDeepDwarfClient {

    private GaiaDeepDwarfClient() {}

    public static void preInit() {
        RenderingRegistry.registerEntityRenderingHandler(EntityDeepDwarf.class, RenderDeepDwarf::new);
    }
}
