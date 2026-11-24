package com.apocollis.aqtweaks.starter;

/**
 * What the choice screen shows for one class. {@code icon} is an item id ({@code minecraft:iron_sword}) or
 * {@code tex:<namespace>:<path>} for a texture under {@code assets/<ns>/textures/gui/starter/}.
 */
public final class StarterClassView {
    public final String id;
    public final String name;
    public final String flavor;
    public final String icon;

    public StarterClassView(String id, String name, String flavor, String icon) {
        this.id = id;
        this.name = name;
        this.flavor = flavor;
        this.icon = icon;
    }
}
