package com.apocollis.aqtweaks.twilightforest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

class VoidBreachShellTest {
    private static String[] faces(String up, String down, String side) {
        return new String[] {up, down, side, side, side, side};
    }

    private static int[] metas(int m) {
        int[] a = new int[6];
        Arrays.fill(a, m);
        return a;
    }

    @Test
    void defaultShellIsSixVoidMetalBlocks() {
        VoidBreachShell.Resolved r = VoidBreachShell.resolve("thaumcraft:metal_void", "");
        assertTrue(r.warnings.isEmpty());
        String v = "thaumcraft:metal_void";
        assertTrue(VoidBreachShell.matches(faces(v, v, v), metas(0), r));
    }

    @Test
    void capBlockVariant() {
        VoidBreachShell.Resolved r = VoidBreachShell.resolve("thaumcraft:metal_void", "thaumcraft:metal_thaumium");
        assertTrue(VoidBreachShell.matches(faces("thaumcraft:metal_thaumium", "thaumcraft:metal_thaumium", "thaumcraft:metal_void"), metas(0), r));
        assertFalse(VoidBreachShell.matches(faces("thaumcraft:metal_void", "thaumcraft:metal_void", "thaumcraft:metal_void"), metas(0), r));
    }

    @Test
    void mixedOrMissingFaceIsRejected() {
        VoidBreachShell.Resolved r = VoidBreachShell.resolve("thaumcraft:metal_void", "");
        String[] f = faces("thaumcraft:metal_void", "thaumcraft:metal_void", "thaumcraft:metal_void");
        f[3] = "minecraft:stone";
        assertFalse(VoidBreachShell.matches(f, metas(0), r));
        f[3] = null;
        assertFalse(VoidBreachShell.matches(f, metas(0), r));
        assertFalse(VoidBreachShell.matches(new String[] {"thaumcraft:metal_void"}, metas(0), r));
        assertFalse(VoidBreachShell.matches(null, null, r));
    }

    @Test
    void metaIsHonouredWhenGivenAndIgnoredWhenNot() {
        VoidBreachShell.Resolved withMeta = VoidBreachShell.resolve("minecraft:wool:5", "");
        assertTrue(VoidBreachShell.matches(faces("minecraft:wool", "minecraft:wool", "minecraft:wool"), metas(5), withMeta));
        assertFalse(VoidBreachShell.matches(faces("minecraft:wool", "minecraft:wool", "minecraft:wool"), metas(4), withMeta));
        VoidBreachShell.Resolved anyMeta = VoidBreachShell.resolve("minecraft:wool", "");
        assertTrue(VoidBreachShell.matches(faces("minecraft:wool", "minecraft:wool", "minecraft:wool"), metas(9), anyMeta));
    }

    @Test
    void octineShellIsNotAVoidShell() {
        String o = VoidBreachShell.OCTINE_BLOCK;
        assertTrue(VoidBreachShell.isOctineShell(faces(o, o, o)));
        assertFalse(VoidBreachShell.isOctineShell(faces(o, o, "minecraft:stone")));
        VoidBreachShell.Resolved r = VoidBreachShell.resolve("thaumcraft:metal_void", "");
        assertFalse(VoidBreachShell.matches(faces(o, o, o), metas(0), r));
    }

    @Test
    void badOrBannedCasingFallsBackToVoidMetalWithOneWarning() {
        for (String bad : new String[] {"minecraft:air", "twilightforest:tower_device", VoidBreachShell.OCTINE_BLOCK, "nonsense", "", null, "a:b:x", "a:b:99", ":b"}) {
            VoidBreachShell.Resolved r = VoidBreachShell.resolve(bad, "");
            assertEquals("thaumcraft:metal_void", r.casing.name, String.valueOf(bad));
            assertEquals(1, r.warnings.size(), String.valueOf(bad));
        }
    }

    @Test
    void badCapFallsBackToTheCasing() {
        VoidBreachShell.Resolved r = VoidBreachShell.resolve("thaumcraft:metal_void", VoidBreachShell.OCTINE_BLOCK);
        assertEquals("thaumcraft:metal_void", r.cap.name);
        assertEquals(1, r.warnings.size());
        assertTrue(VoidBreachShell.resolve("thaumcraft:metal_void", "   ").warnings.isEmpty());
        assertNull(VoidBreachShell.parse("no_colon"));
    }
}
