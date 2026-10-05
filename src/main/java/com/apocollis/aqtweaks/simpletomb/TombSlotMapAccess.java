package com.apocollis.aqtweaks.simpletomb;

import java.util.List;

/** Implemented by {@code MixinTileEntityTomb}: lets the module set the slot map without an NBT round trip. */
public interface TombSlotMapAccess {

    void aqtweaks$setSlotMap(List<TombSlotMaps.SlotMapping> mappings);
}
