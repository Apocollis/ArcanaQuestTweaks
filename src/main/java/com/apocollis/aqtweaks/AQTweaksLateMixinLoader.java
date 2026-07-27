package com.apocollis.aqtweaks;

import zone.rong.mixinbooter.ILateMixinLoader;
import java.util.Collections;
import java.util.List;

public class AQTweaksLateMixinLoader implements ILateMixinLoader {
    @Override
    public List<String> getMixinConfigs() {
        return Collections.singletonList("mixins.aqtweaks.json");
    }
}
