package com.apocollis.aqtweaks.thaumcraft.battlemage.client;

import com.apocollis.aqtweaks.thaumcraft.battlemage.EntityCultistBattlemage;
import com.apocollis.aqtweaks.thaumcraft.battlemage.EntityCultistBombardierMage;
import com.apocollis.aqtweaks.thaumcraft.battlemage.EntityCultistMasterBattlemage;
import com.apocollis.aqtweaks.thaumcraft.battlemage.EntityCultistSwarmMage;
import com.apocollis.aqtweaks.thaumcraft.battlemage.EntityEscortPortal;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import thaumcraft.client.renderers.entity.construct.RenderCultistPortalLesser;

/** Call only when Thaumcraft is loaded and the Battlemage is enabled. One renderer serves all four entities. */
@SideOnly(Side.CLIENT)
public final class BattlemageClient {

    private BattlemageClient() {}

    public static void preInit() {
        RenderingRegistry.registerEntityRenderingHandler(EntityCultistBattlemage.class, RenderCultistBattlemage::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityCultistMasterBattlemage.class, RenderCultistBattlemage::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityCultistSwarmMage.class, RenderCultistBattlemage::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityCultistBombardierMage.class, RenderCultistBattlemage::new);
        // The escort portal is drawn by Thaumcraft's own Lesser Cultist Portal renderer (plan 18.7).
        RenderingRegistry.registerEntityRenderingHandler(EntityEscortPortal.class, RenderCultistPortalLesser::new);
    }
}
