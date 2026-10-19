package zone.rong.mixinbooter;

import java.util.List;

public interface ILateMixinLoader {
    List<String> getMixinConfigs();

    default boolean shouldMixinConfigQueue(String mixinConfig) {
        return true;
    }
}
