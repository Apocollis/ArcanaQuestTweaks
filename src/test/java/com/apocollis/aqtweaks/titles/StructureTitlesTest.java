package com.apocollis.aqtweaks.titles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class StructureTitlesTest {

    @Test
    void namesStartWithRoguelikeThenVanillaAndKeepExtrasAfterwards() {
        List<String> names = StructureTitles.names(List.of("Twilight_Hollow", "  ", "village", "Home"));
        assertEquals("RoguelikeDungeon", names.get(0));
        assertEquals("Village", names.get(1));
        assertTrue(names.indexOf("Temple") < names.indexOf("Twilight_Hollow"));
        // blank, a duplicate of "Village" ignoring case, and the home name are dropped
        assertEquals(1, names.stream().filter(n -> n.equalsIgnoreCase("village")).count());
        assertTrue(names.stream().noneMatch(n -> n.equalsIgnoreCase("home")));
        assertTrue(names.stream().noneMatch(String::isBlank));
        assertEquals(StructureTitles.VANILLA.size() + 2, names.size());
    }

    @Test
    void namesAcceptNullExtras() {
        assertEquals(StructureTitles.VANILLA.size() + 1, StructureTitles.names(null).size());
    }

    @Test
    void keysAndLangKeys() {
        assertEquals("desert_pyramid", StructureTitles.keyOf("Desert_Pyramid"));
        assertEquals("aqtweaks.structure.endcity", StructureTitles.langKey("EndCity"));
        assertEquals("aqtweaks.structure.roguelikedungeon", StructureTitles.langKey("RoguelikeDungeon"));
    }

    @Test
    void prettifyMakesReadableNames() {
        assertEquals("Desert Pyramid", StructureTitles.prettify("Desert_Pyramid"));
        assertEquals("End City", StructureTitles.prettify("EndCity"));
        assertEquals("Roguelike Dungeon", StructureTitles.prettify("RoguelikeDungeon"));
        assertEquals("Village", StructureTitles.prettify("village"));
        assertEquals("", StructureTitles.prettify(" "));
    }

    @Test
    void homeTitleUsesServerNameOrPlainHome() {
        assertEquals("Ana's Home", StructureTitles.clientTitle("home", "Ana's Home", "", true, "Home"));
        assertEquals("Home", StructureTitles.clientTitle("home", "Ana's Home", "", false, "Home"));
        assertEquals("Home", StructureTitles.clientTitle("home", "", "", true, "Home"));
    }

    @Test
    void otherTitlesUseTheTranslationElseThePrettifiedName() {
        assertEquals("Abandoned Mineshaft",
                StructureTitles.clientTitle("mineshaft", "Mineshaft", "Abandoned Mineshaft", true, "Home"));
        // an untranslated key comes back from the language lookup unchanged
        assertEquals("Desert Pyramid",
                StructureTitles.clientTitle("desert_pyramid", "Desert_Pyramid",
                        StructureTitles.langKey("desert_pyramid"), true, "Home"));
        assertEquals("Twilight Hollow",
                StructureTitles.clientTitle("twilight_hollow", "Twilight_Hollow", "", true, "Home"));
    }

    @Test
    void enterThenStayThenExit() {
        StructureTitles.Tracker tracker = new StructureTitles.Tracker();
        UUID p = UUID.randomUUID();
        assertEquals(StructureTitles.Kind.NONE, tracker.update(p, null, 0, 200).kind);
        StructureTitles.Transition enter = tracker.update(p, "village", 20, 200);
        assertEquals(StructureTitles.Kind.ENTER, enter.kind);
        assertEquals("village", enter.key);
        assertEquals(StructureTitles.Kind.NONE, tracker.update(p, "village", 40, 200).kind);
        StructureTitles.Transition exit = tracker.update(p, null, 60, 200);
        assertEquals(StructureTitles.Kind.EXIT, exit.kind);
        assertEquals("village", exit.key);
    }

    @Test
    void movingBetweenStructuresAnnouncesTheNewOne() {
        StructureTitles.Tracker tracker = new StructureTitles.Tracker();
        UUID p = UUID.randomUUID();
        tracker.update(p, "village", 0, 200);
        StructureTitles.Transition next = tracker.update(p, "home", 20, 200);
        assertEquals(StructureTitles.Kind.ENTER, next.kind);
        assertEquals("home", next.key);
    }

    @Test
    void reEntryInsideTheCooldownIsNotAnnouncedAgain() {
        StructureTitles.Tracker tracker = new StructureTitles.Tracker();
        UUID p = UUID.randomUUID();
        assertEquals(StructureTitles.Kind.ENTER, tracker.update(p, "village", 0, 200).kind);
        assertEquals(StructureTitles.Kind.EXIT, tracker.update(p, null, 20, 200).kind);
        assertEquals(StructureTitles.Kind.NONE, tracker.update(p, "village", 40, 200).kind);
        // still remembered as inside, so leaving it reports an exit
        assertEquals("village", tracker.lastKey(p));
        assertEquals(StructureTitles.Kind.EXIT, tracker.update(p, null, 60, 200).kind);
        // after the cooldown the same structure announces again
        assertEquals(StructureTitles.Kind.ENTER, tracker.update(p, "village", 400, 200).kind);
    }

    @Test
    void forgetResetsAPlayer() {
        StructureTitles.Tracker tracker = new StructureTitles.Tracker();
        UUID p = UUID.randomUUID();
        tracker.update(p, "village", 0, 200);
        tracker.forget(p);
        assertEquals(StructureTitles.Kind.ENTER, tracker.update(p, "village", 10, 200).kind);
    }
}
