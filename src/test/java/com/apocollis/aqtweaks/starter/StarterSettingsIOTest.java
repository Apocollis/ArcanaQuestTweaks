package com.apocollis.aqtweaks.starter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParseException;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class StarterSettingsIOTest {

    @Test
    void defaultsRoundTripThroughJson() {
        StarterSettings original = StarterDefaults.create();
        StarterSettings back = StarterSettingsIO.parse(StarterSettingsIO.toJson(original));
        assertEquals(original.intro, back.intro);
        assertEquals(original.prompt, back.prompt);
        assertEquals(original.resetIntro, back.resetIntro);
        assertEquals(original.resetPrompt, back.resetPrompt);
        assertEquals(original.revealHoldSeconds, back.revealHoldSeconds);
        assertEquals(6, back.classes.size());
        assertEquals(original.classes.get(5).commands, back.classes.get(5).commands);
        assertEquals(original.classes.get(0).skills, back.classes.get(0).skills);
        assertEquals("aqtweaks:riposte", back.classes.get(0).perk);
        assertEquals("warrior", back.classes.get(0).csgKit);
    }

    @Test
    void theSixDefaultClassesFollowThePlan() {
        StarterSettings s = StarterDefaults.create();
        Set<String> perks = new HashSet<>();
        for (StarterClassDef d : s.classes) {
            int total = 0;
            for (int level : d.skills.values()) total += level;
            assertEquals(8, total, d.id + " starter levels");
            assertFalse(d.flavor.isEmpty(), d.id);
            assertTrue(d.icon.startsWith("tex:aqtweaks:class_"), d.id);
            assertEquals(d.id, d.csgKit);
            assertTrue(perks.add(d.perk), "perk shared: " + d.perk);
        }
        StarterClassDef scholar = s.classes.get(5);
        assertEquals("scholar", scholar.id);
        assertEquals(4, (int) scholar.skills.get("reskillable:magic"));
        assertEquals(1, scholar.commands.size());
        assertTrue(scholar.commands.get(0).contains("FIRSTSTEPS"));
    }

    @Test
    void blanksAreFilledAfterAHandEditedFile() {
        String json = "{\"classes\":[{\"id\":\" mage \"}]}";
        StarterSettings s = StarterSettingsIO.parse(json);
        StarterClassDef d = s.classes.get(0);
        assertEquals("mage", d.id);
        assertEquals("mage", d.name);
        assertTrue(d.skills.isEmpty());
        assertTrue(d.commands.isEmpty());
        assertEquals("", d.perk);
        assertEquals(StarterDefaults.PROMPT, s.prompt);
        assertEquals(StarterDefaults.RESET_INTRO, s.resetIntro);
        assertEquals(StarterDefaults.RESET_PROMPT, s.resetPrompt);
    }

    @Test
    void anEmptyOrBrokenFileIsAnError() {
        assertThrows(JsonParseException.class, () -> StarterSettingsIO.parse(""));
        assertThrows(JsonParseException.class, () -> StarterSettingsIO.parse("{ not json"));
    }
}
