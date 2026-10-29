package com.apocollis.aqtweaks.spawning.rules;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/** A {@code mincount}/{@code maxcount} value: an amount, what is counted, and how the amount scales. */
public final class CountSpec {
    public enum Scale { NONE, PER_PLAYER, PER_CHUNK }

    public enum Kind {
        /** The mob being spawned (a bare number). */
        SELF,
        /** The listed mobs, added together. */
        MOBS,
        /** All mobs of a mod. */
        MOD,
        HOSTILE,
        PASSIVE
    }

    public final int amount;
    public final Scale scale;
    public final Kind kind;
    public final Set<String> mobs;
    public final String mod;
    /** For a mod count: only its hostile (or only its passive) mobs. */
    public final boolean hostileOnly;
    public final boolean passiveOnly;

    private CountSpec(int amount, Scale scale, Kind kind, Set<String> mobs, String mod, boolean hostileOnly,
                      boolean passiveOnly) {
        this.amount = amount;
        this.scale = scale;
        this.kind = kind;
        this.mobs = Collections.unmodifiableSet(mobs);
        this.mod = mod;
        this.hostileOnly = hostileOnly;
        this.passiveOnly = passiveOnly;
    }

    /** Reads In Control's three forms: {@code 5}, {@code "5,modid:mob"}, or an object. Input is already validated. */
    static CountSpec parse(JsonElement value) {
        if (value.isJsonPrimitive()) {
            if (value.getAsJsonPrimitive().isNumber()) {
                return new CountSpec(value.getAsInt(), Scale.NONE, Kind.SELF, new LinkedHashSet<String>(), null, false, false);
            }
            String[] parts = value.getAsString().split(",");
            Set<String> mobs = new LinkedHashSet<String>();
            if (parts.length > 1) {
                mobs.add(parts[1].trim());
            }
            return new CountSpec(Integer.parseInt(parts[0].trim()), Scale.NONE,
                    mobs.isEmpty() ? Kind.SELF : Kind.MOBS, mobs, null, false, false);
        }
        JsonObject object = value.getAsJsonObject();
        Scale scale = flag(object, "perplayer") ? Scale.PER_PLAYER : flag(object, "perchunk") ? Scale.PER_CHUNK : Scale.NONE;
        Set<String> mobs = new LinkedHashSet<String>();
        if (object.has("mob")) {
            JsonElement mob = object.get("mob");
            if (mob.isJsonArray()) {
                for (JsonElement element : mob.getAsJsonArray()) {
                    mobs.add(element.getAsString());
                }
            } else {
                mobs.add(mob.getAsString());
            }
        }
        // In Control checks mod, then hostile, then passive, then the mob list, then the mob itself
        Kind kind;
        String mod = null;
        if (object.has("mod")) {
            kind = Kind.MOD;
            mod = object.get("mod").getAsString();
        } else if (flag(object, "hostile")) {
            kind = Kind.HOSTILE;
        } else if (flag(object, "passive")) {
            kind = Kind.PASSIVE;
        } else {
            kind = mobs.isEmpty() ? Kind.SELF : Kind.MOBS;
        }
        return new CountSpec(object.get("amount").getAsInt(), scale, kind, mobs, mod, flag(object, "hostile"),
                flag(object, "passive"));
    }

    private static boolean flag(JsonObject object, String key) {
        return object.has(key) && object.get(key).getAsBoolean();
    }
}
