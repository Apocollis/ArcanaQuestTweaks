package com.apocollis.aqtweaks;

import zone.rong.mixinbooter.ILateMixinLoader;

import java.util.Arrays;
import java.util.List;

public class AQTweaksLateMixinLoader implements ILateMixinLoader {
    @Override
    public List<String> getMixinConfigs() {
        return Arrays.asList("mixins.aqtweaks.json", "mixins.aqtweaks.grapple.json", "mixins.aqtweaks.dss.json");
    }
}
