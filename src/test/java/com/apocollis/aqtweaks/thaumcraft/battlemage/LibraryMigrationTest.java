package com.apocollis.aqtweaks.thaumcraft.battlemage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import org.junit.jupiter.api.Test;

class LibraryMigrationTest {

    @Test
    void aStageOneFileWithNoVersionIsUpgraded() {
        assertNull(LibraryMigration.readVersion("{\"tiers\":[]}"));
        assertTrue(LibraryMigration.needsUpgrade(LibraryMigration.readVersion("{\"tiers\":[]}")));
    }

    @Test
    void brokenJsonCountsAsOldAndIsBackedUpNotLost() {
        assertNull(LibraryMigration.readVersion("{not json"));
        assertTrue(LibraryMigration.needsUpgrade(null));
    }

    @Test
    void olderVersionUpgradesCurrentAndNewerAreKept() {
        assertTrue(LibraryMigration.needsUpgrade(1));
        assertTrue(LibraryMigration.needsUpgrade(2), "a schema 2 copy predates the variants and the new pacing");
        assertTrue(LibraryMigration.needsUpgrade(3), "a schema 3 copy carries the wrong Thaumcraft escort ids");
        assertEquals(4, LibraryMigration.CURRENT);
        assertFalse(LibraryMigration.needsUpgrade(LibraryMigration.CURRENT));
        assertFalse(LibraryMigration.needsUpgrade(LibraryMigration.CURRENT + 5));
        assertEquals(Integer.valueOf(2), LibraryMigration.readVersion("{\"schemaVersion\":2}"));
    }

    @Test
    void backupNamesNeverOverwrite() {
        String base = "cultist_battlemages.json";
        assertEquals(base + ".bak", LibraryMigration.backupName(base, new HashSet<String>()));
        assertEquals(base + ".bak2", LibraryMigration.backupName(base, new HashSet<>(Arrays.asList(base + ".bak"))));
        assertEquals(base + ".bak3", LibraryMigration.backupName(base,
                new HashSet<>(Arrays.asList(base + ".bak", base + ".bak2"))));
    }

    @Test
    void theBundledFileCarriesTheCurrentVersion() throws Exception {
        try (InputStream in = LibraryMigrationTest.class.getResourceAsStream("/assets/aqtweaks/battlemage/cultist_battlemages.json")) {
            assertNotNull(in);
            String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            assertEquals(Integer.valueOf(LibraryMigration.CURRENT), LibraryMigration.readVersion(json));
            assertFalse(LibraryMigration.needsUpgrade(LibraryMigration.readVersion(json)));
        }
    }
}
