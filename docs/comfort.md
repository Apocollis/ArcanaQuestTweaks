# Comfort module

JSON: `config/arcanaquesttweaks/aqtweaks_comfort.json` (legacy filenames are renamed on load). Always registered. No parent “Comfort mod” — this is Tweaks-owned, with optional hooks into other mods’ potions and warp.

## What Tweaks does

A homestead rest loop: scan nearby “cozy” blocks and tamed pets, score with per-category caps, then apply a custom **Homestead** potion plus regen/saturation and (if loaded) Simple Difficulty thermals and Thaumcraft temp-warp drain. Also grants Simple Difficulty **cold resist** while standing in Biomes O' Plenty hot spring water.

## How parent/vanilla pieces work

| Source | What it provides | Tweaks use |
| --- | --- | --- |
| Vanilla | Sleep, sneak, sit, potions, `EntityTameable` | Rest detection, regen/saturation, pet scan |
| Tweaks | `PotionHomestead` | HUD icon `textures/gui/homestead_icon.png`, registered on `RegistryEvent.Register<Potion>` |
| Thaumcraft | Temp warp capability | Same `ThaumcraftHelper.reduceWarp` as the TC module; progress NBT `WarpCleansingProgress` |
| Simple Difficulty | `simpledifficulty:cold_resist` and thermal potions | Applied by registry name if the potion exists |
| Biomes O' Plenty | `biomesoplenty:hot_spring_water` | Fluid/block at feet or head |

There is no mixin into those mods for comfort. Missing mods just skip that benefit.

## How Tweaks implements it

`ComfortConfigLoader` (preInit) writes/loads JSON into `ComfortSystemHandler` maps: block → `{category, weight}`, category limits, pet value, Homestead I/II/III score thresholds.

Every **300 ticks** (15s), server:

1. If not tagged resting: player must be sleeping, sitting, sneaking, or stationary; score ≥ Homestead I → set NBT `AQTComfortResting`, apply benefits.
2. If already resting: allow movement; rescan. Score below I → clear tag.
3. Damage taken or attacking cancels the tag immediately.

Score: 24×5×24 block scan (12 horizontal, thin Y), plus pets in 16 blocks. Per category, sort weights descending and sum only the top N (limits). That stops one huge chandelier farm from dominating.

### Threshold Benefits (Defaults 5 / 15 / 30)

- **Homestead I (Score 5–14)**: Homestead I HUD icon, +9 Warp Cleansing progress per 15s interval.
- **Homestead II (Score 15–29)**: Homestead II HUD icon, Regeneration I, +13 Warp Cleansing progress, plus **Simple Difficulty Thermal Sanctuary**: grants ambient `heat_protection` and `cold_protection` (insulating the player against both extreme heat/Hyperthermia and extreme cold/Hypothermia while in their shelter).
- **Homestead III (Score 30+)**: Homestead III HUD icon, Regeneration II, Saturation I, +25 Warp Cleansing progress (1 temp warp cleansed every 60s), plus Simple Difficulty thermal protections.

Hot springs: every 20 ticks (1s), if the block at feet or head is BOP hot spring water (`biomesoplenty:hot_spring_water`), apply Simple Difficulty `cold_resist`.

## Files

- `comfort/ComfortSystemHandler.java`
- `comfort/ComfortConfig.java` / `ComfortConfigLoader.java`
- `comfort/PotionHomestead.java`

## Do not regress

- Benefits are **ambient, no particles** (`true, false` on `PotionEffect`).
- Keep category caps; uncapped sums break the intended “room” fantasy.
- Comfort warp drain is separate NBT (`WarpCleansingProgress`) from Thaumcraft environmental exposure progress.
- Thermals apply via dynamic resource location lookup so absence of Simple Difficulty never causes classloading errors.

