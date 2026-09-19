package com.apocollis.aqtweaks.animania;

import com.animania.addons.farm.common.entity.chickens.EntityHenBase;
import com.animania.addons.farm.common.entity.goats.EntityAnimaniaGoat;
import com.animania.addons.farm.common.entity.sheep.EntityAnimaniaSheep;
import net.minecraft.entity.Entity;

/**
 * Farm addon clocks. Class is only entered after {@link Class#forName} succeeds
 * so a missing Farm jar does not crash Tweaks load.
 */
public final class AnimaniaFarmClocks {

    private AnimaniaFarmClocks() {}

    public static void extraProduction(Entity entity) {
        if (entity instanceof EntityAnimaniaSheep sheep) {
            int wool = sheep.getWoolRegrowthTimer();
            if (wool > 0) sheep.setWoolRegrowthTimer(wool - 1);
        }
        if (entity instanceof EntityAnimaniaGoat goat) {
            int wool = goat.getWoolRegrowthTimer();
            if (wool > 0) goat.setWoolRegrowthTimer(wool - 1);
        }
        if (entity instanceof EntityHenBase hen) {
            int laid = hen.getLaidTimer();
            if (laid > 0) hen.setLaidTimer(laid - 1);
        }
    }
}
