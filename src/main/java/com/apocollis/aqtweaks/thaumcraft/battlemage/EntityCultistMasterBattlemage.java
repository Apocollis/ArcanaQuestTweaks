package com.apocollis.aqtweaks.thaumcraft.battlemage;

import net.minecraft.world.World;

/** The Master Battlemage ({@code aqtweaks:cultist_master_battlemage}): the Tier 2 Battlemage, same brain, Tier 2 library entry. */
public class EntityCultistMasterBattlemage extends EntityCultistBattlemage {

    public EntityCultistMasterBattlemage(World world) {
        super(world);
    }

    @Override
    public int getTier() {
        return 2;
    }
}
