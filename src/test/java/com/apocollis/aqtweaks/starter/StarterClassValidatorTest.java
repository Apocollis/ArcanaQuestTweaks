package com.apocollis.aqtweaks.starter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class StarterClassValidatorTest {

    private static final class FakeEnv implements StarterClassValidator.Environment {
        final Set<String> skills = new HashSet<>(Arrays.asList(
                "reskillable:attack", "reskillable:defense", "reskillable:agility", "reskillable:mining",
                "reskillable:gathering", "reskillable:farming", "reskillable:building", "reskillable:magic"));
        final Set<String> kits = new HashSet<>(Arrays.asList(
                "warrior", "archer", "scout", "miner", "homesteader", "scholar"));
        final Set<String> perks = new HashSet<>(Arrays.asList(
                "aqtweaks:riposte", "aqtweaks:steady_draw", "aqtweaks:fleetfoot", "aqtweaks:vein_rhythm",
                "aqtweaks:hearthkeeper", "aqtweaks:studious"));
        List<String> drift = Collections.emptyList();

        @Override
        public boolean skillExists(String skillId) {
            return skills.contains(skillId);
        }

        @Override
        public boolean kitExists(String kit) {
            return kits.contains(kit);
        }

        @Override
        public boolean isClassPerk(String perkId) {
            return perks.contains(perkId);
        }

        @Override
        public List<String> kitDrift(String kit) {
            return drift;
        }
    }

    private static StarterClassDef def(String id, String kit, String perk) {
        return new StarterClassDef(id, id, "flavor", "icon", kit, perk).skill("reskillable:attack", 4);
    }

    @Test
    void theShippedDefaultsAllValidate() {
        StarterClassValidator.Result r = StarterClassValidator.validate(StarterDefaults.create().classes, new FakeEnv());
        assertTrue(r.errors.isEmpty(), r.errors.toString());
        assertEquals(6, r.valid.size());
    }

    @Test
    void duplicateAndEmptyIdsAreRejected() {
        List<StarterClassDef> defs = new ArrayList<>();
        defs.add(def("warrior", "warrior", "aqtweaks:riposte"));
        defs.add(def("warrior", "archer", "aqtweaks:steady_draw"));
        defs.add(def("  ", "scout", "aqtweaks:fleetfoot"));
        defs.add(null);
        StarterClassValidator.Result r = StarterClassValidator.validate(defs, new FakeEnv());
        assertEquals(1, r.valid.size());
        assertEquals(3, r.errors.size());
    }

    @Test
    void unknownKitSkillAndBadLevelAreRejected() {
        StarterClassDef noKit = def("a", "missing", "aqtweaks:riposte");
        StarterClassDef noSkill = def("b", "archer", "aqtweaks:steady_draw").skill("reskillable:nothing", 2);
        StarterClassDef zero = def("c", "scout", "aqtweaks:fleetfoot").skill("reskillable:mining", 0);
        StarterClassValidator.Result r =
                StarterClassValidator.validate(Arrays.asList(noKit, noSkill, zero), new FakeEnv());
        assertTrue(r.valid.isEmpty());
        assertEquals(3, r.errors.size());
    }

    @Test
    void aPerkMustBeAClassPerkAndCannotBeShared() {
        StarterClassDef unknownPerk = def("a", "warrior", "aqtweaks:power_attack");
        StarterClassDef first = def("b", "archer", "aqtweaks:riposte");
        StarterClassDef second = def("c", "scout", "aqtweaks:riposte");
        StarterClassValidator.Result r =
                StarterClassValidator.validate(Arrays.asList(unknownPerk, first, second), new FakeEnv());
        assertEquals(1, r.valid.size());
        assertEquals("b", r.valid.get(0).id);
        assertEquals(2, r.errors.size());
    }

    @Test
    void aClassWithoutAPerkIsAllowed() {
        StarterClassValidator.Result r =
                StarterClassValidator.validate(Arrays.asList(def("a", "warrior", "")), new FakeEnv());
        assertEquals(1, r.valid.size());
    }

    @Test
    void kitDriftRejectsTheClass() {
        FakeEnv env = new FakeEnv();
        env.drift = Arrays.asList("0:8");
        StarterClassValidator.Result r =
                StarterClassValidator.validate(Arrays.asList(def("a", "warrior", "aqtweaks:riposte")), env);
        assertTrue(r.valid.isEmpty());
        assertEquals(1, r.errors.size());
    }

    @Test
    void anEmptyListLeavesNoValidClass() {
        StarterClassValidator.Result r = StarterClassValidator.validate(new ArrayList<StarterClassDef>(), new FakeEnv());
        assertTrue(r.valid.isEmpty());
    }
}
