package com.apocollis.aqtweaks.thaumcraft.battlemage.client;

import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import thaumcraft.client.renderers.entity.mob.RenderCultist;

/**
 * The Battlemage wears the stock Crimson Cultist body (plan 3.8): same model, texture and armor layer, and
 * {@code LayerHeldItem}, which draws both hands, so the main-hand weapon and the off-hand gauntlet (with its
 * focus-coloured orb) show with no extra layer. Appearance beyond that comes from equipment.
 */
@SideOnly(Side.CLIENT)
public class RenderCultistBattlemage extends RenderCultist {

    public RenderCultistBattlemage(RenderManager manager) {
        super(manager);
    }
}
