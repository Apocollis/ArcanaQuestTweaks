package com.apocollis.aqtweaks.thaumcraft.battlemage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CtFocusLineTest {

    private static final String HEAL = "<thaumcraft:focus_2>.withTag({display: {Name: \"Heal Self\"}, package: {index: 0, power: 1.0 as float, complexity: 20, nodes: [{type: \"MEDIUM\", key: \"ROOT\"}, {type: \"EFFECT\", key: \"thaumcraft.HEAL\", \"setting.power\": 5}]}, color: -2228219, srt: -1733616132})";

    @Test
    void splitsItemAndTag() {
        CtFocusLine line = CtFocusLine.parse(HEAL);
        assertNotNull(line);
        assertEquals("thaumcraft:focus_2", line.itemId);
        assertTrue(line.nbt.startsWith("{display:"));
        assertTrue(line.nbt.endsWith("srt: -1733616132}"));
    }

    @Test
    void floatCastsBecomeSuffixes() {
        CtFocusLine line = CtFocusLine.parse(HEAL);
        assertTrue(line.nbt.contains("power: 1.0f"));
        assertFalse(line.nbt.contains("as float"));
    }

    @Test
    void bracketLineWithoutTagHasNullNbt() {
        CtFocusLine line = CtFocusLine.parse("<thaumcraft:focus_2>");
        assertNotNull(line);
        assertNull(line.nbt);
    }

    @Test
    void nonBracketTextIsRejected() {
        assertNull(CtFocusLine.parse("thaumcraft:focus_2"));
        assertNull(CtFocusLine.parse(null));
    }

    @Test
    void nestedSplitPackagesSurvive() {
        String line = "<thaumcraft:focus_3>.withTag({package: {nodes: [{type: \"MOD\", key: \"thaumcraft.SPLITTARGET\", packages: {packages: [{index: 0, power: 1.0 as float, complexity: 0, nodes: []}]}}]}})";
        CtFocusLine parsed = CtFocusLine.parse(line);
        assertTrue(parsed.nbt.contains("power: 1.0f"));
        assertTrue(parsed.nbt.contains("packages: {packages:"));
    }
}
