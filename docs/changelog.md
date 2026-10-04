# Changelog (1.8)

Stay on version **1.8** until a plan bumps `ArcanaQuestTweaks.VERSION`.

## 2026-10-02 — Campfire neighbor notification and Thaumcraft crucible heat

- Simple Difficulty campfires notify neighbors on light, age, and extinguish so an Inspirations cauldron boils from the configured odd metas, and a fluid-filled Thaumcraft crucible heats from a burning campfire. Spec: [thaumcraft.md](thaumcraft.md).

## 2026-09-30 — Simple Tomb slot restoration, displacement, Baubles, and admin backups

- Death captures player inventory and BaublesEX slot indices in NBT. On grave retrieval, items return to exact original slots. If an occupied slot cannot accept the grave item, existing items are non-destructively displaced into empty main inventory slots, or dropped safely at the player's feet if inventory is full.
- Death events also record up to 3 rolling death backups per player in Overworld WorldSavedData (`aqtweaks_death_backups.dat`).
- Added `/aqtomb <list|recover>` OP level 2 admin command for recovering inventory if graves are lost or fail placement.
- Cosmetic Armor is preserved on death by Corpse Complex and explicitly excluded from tomb handling. Spec: [simpletomb.md](simpletomb.md).

## 2026-09-29 — Broken one use, then destroy; repair bands

- The first break leaves Broken with one use (`max - 1`). Spending that use destroys the stack. Repair from 26% up to 51% turns Broken into wear `gray`. Past 51%, wear or Broken restores the saved quality. Spec: [qualitytools.md](qualitytools.md).

## 2026-09-28 — Thaumcraft ring null texture bind

- A null bauble texture is skipped before `bindTexture`, so a worn Thaumcraft ring no longer aborts the rest of the bauble pass. Spec: [thaumcraft.md](thaumcraft.md).

## 2026-09-28 — Elenai armor tooltip weight list

- Armor tooltips read the weight string locally, so hovering armor in the Baubles screen no longer throws `ConcurrentModificationException` from Elenai's static list. Spec: [stamina.md](stamina.md).

## 2026-09-28 — Dawnstone rune reroll skips current Name

- Same-tier rune hammers pick equally among other qualities of that color (loot weight unused). Step-up still uses the full next-color pool. Type match prefers the file that lists the live quality Name.

## 2026-09-28 — Thaumcraft rings skip amulet model

- `thaumcraft:baubles` meta 1, 3, and 5 no longer get `ModelAmulet`, so rings do not draw a second player skin. A null amulet texture also skips that body draw. Spec: [thaumcraft.md](thaumcraft.md).

## 2026-09-25 — Thermia cap kill and slow recovery

- Hypothermia and hyperthermia still cannot push the Elenai max below 1. The next penalty step at the config base drains the last half-feather and kills with that Simple Difficulty source. After the potion ends, the cap returns one half-feather every 10 ticks. Spec: [stamina.md](stamina.md).

## 2026-09-25 — Rancher clock throttle and perk lookup

- Rancher still doubles Animania clocks, applied as 20 steps once per second. Gestation stops at 1 so birth still happens. Armor weight, perk checks, gold feathers, and Simple Difficulty thirst use cached or compile-hard calls. Climb jump packets send on change. Spec: [reskillable.md](reskillable.md), [stamina.md](stamina.md).

## 2026-09-24 — Dark Vision flat mix and sight

- Dark Vision holds the lightmap at 0.4 everywhere the perk is on. Dynamic Stealth treats that player as having night vision. Spec: [reskillable.md](reskillable.md).

## 2026-09-24 — Prospecting pick outline, Dark Vision curve

- Prospector outlines ores a Prospectus pick counted, for 7 seconds, and raises that pick's accuracy by 25. Every pick uses the Tweaks base chances. Dark Vision mixes at 0.8 in pitch dark and eases over 5 ticks. Spec: [reskillable.md](reskillable.md).

## 2026-09-23 — Benevolent, Prospector outline, Dark Vision mix

- Benevolent doubles a Heal focus on any other living target. Prospector draws ore boxes for the mining player. Dark Vision mixes the client lightmap up to 0.7 instead of applying Night Vision. Spec: [reskillable.md](reskillable.md).

## 2026-09-23 — Reskillable perk wishlist

- New perks for mining, gathering, farming, attack, defense, and agility, plus the cost pass on the existing trees. Magic schools require Magic 20 and restamp their `not|trait|` locks after every school is registered. Spec: [reskillable.md](reskillable.md).

## 2026-09-23 — TF/Aether portal sits on grass

- RandomPortals vertical dest frames were one block in the surface (bottom row replaced grass). Tweaks now raises them so the bottom row rests on the grass/island. See [twilightforest.md](twilightforest.md) and [aether.md](aether.md).

## 2026-09-23 — GUI close once, DSS unbound key
