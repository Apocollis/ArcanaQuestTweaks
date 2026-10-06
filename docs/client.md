# Client module (1.9)

Last updated: 2026-08-28.

Config: `config/arcanaquesttweaks/aqtweaks_client.cfg`. Client-only. No `@Mod` parent. Optional mixin if Toughness Bar is present.

## Locked intent

HUD and tooltip presentation that is not stamina spend. Do **not** change Elenai feather or thirst icons. Do **not** mixin Metallurgy. Skip `metallurgy:` items so Reforged’s own shift stats are not doubled.

## How the parents work

### Toughness Bar 2.4 (`toughnessbar`)

`EventHandlerClient.onRenderArmorToughnessEvent` listens to `FOOD` Post. Stock overlay uses `GuiIngameForge.right_height` and X `width/2 + 82`, then `IINC` **-8** (hunger column, right to left).

Vanilla armor uses `width/2 - 91` and steps **+8**. **Overloaded Armor Bar** cancels vanilla ARMOR and never increments `left_height`.

### Metallurgy 4 Reforged (`metallurgy`)

`ItemUtils.buildStatsTooltip` (shift-gated) adds harvest stars (`U+2B51`), durability `remaining/max`, and efficiency float with `ScaleFormatting` colors. Harvest stars only on pickaxes in Metallurgy; Tweaks still shows harvest for any mining tool that reports a harvest level. Creative tab string is `Metallurgy Tools`.

## Design plan

### Toughness HUD

Optional late mixin `mixins.aqtweaks.toughnessbar.json` (`required: false`), `MixinEventHandlerClient`.

When `Move Toughness Bar To Armor Side` is true:

- GET `right_height` → `left_height + 10` (sit one row above Overloaded Armor Bar).
- PUT `right_height` → `left_height`.
- Constant `82` → `-91` (armor left edge).
- Stock still `IINC` **-8**. Mixin does not rewrite IINC. Each `fullIcon` / `halfIcon` X is mirrored around the first draw (`2 * origin - x`) so icons fill left to right on that origin.

When false: stock right-side RTL. Missing jar → json skipped.

### Metallurgy tooltip compat

`ClientModule` on `ItemTooltipEvent`. If the flag is on, the stack is a mining tool (`ItemPickaxe` / `ItemAxe` / `ItemSpade` / `ItemTool`, or tool classes `pickaxe`/`axe`/`shovel`), and the registry domain is not `metallurgy`, append:

1. `§9{Name} Tools` — `minecraft` → Vanilla; else Forge mod display name (fallback modid).
2. `Harvest Level:` colored stars — max `getHarvestLevel` over tool classes; star count `clamp(harvest + 1, 1, 7)`; omit if all levels `< 0`.
3. `Durability:` remaining/max — skip if `maxDamage <= 0`. Color: remaining ratio `< 0.33` red, `< 0.66` yellow, else green.
4. `Efficiency:` `item.getDestroySpeed` on stone / log / dirt for the tool class (base speed, not Efficiency enchant). Color index `ceil(clamp(speed - 6, 0, 6))` into the same seven Metallurgy colors.

Always visible (Metallurgy’s own items stay shift-gated by Metallurgy). Swords and hoes are skipped unless they expose those tool classes.

## Client relight sweep

Light reaches the client only inside chunk data, or when the client runs its own check for a block change it receives. Emitters the server lit after it had sent the chunk, or whose client check was skipped because the 17-block area around them was not loaded yet, stayed dark on the client until a torch placed nearby forced a check (F3 light reads low there). Vanilla's own sweep for dark emitters (`Chunk.enqueueRelightChecks`) runs on the server world only.

`world/client/ClientRelightSweep` (`Side.CLIENT` subscriber): `ChunkEvent.Load` on a remote world queues the chunk. On `ClientTickEvent` END, up to 2 sections per tick are scanned once the chunk's 8 neighbors are loaded (or after 30 s); every non-empty section cell whose block has a vanilla `getLightValue()` above 0 is tested: stale if its stored block light is below that value, or an open (opacity below 15) neighbor holds less than value - 1. Liquid emitters (lava) are skipped: the first version found about 8000 stale cells a minute, nearly all underground lava, filled its 8192 queue and starved real torches (a spark of the `[AQ-CLIENT-RELIGHT]` log showed 0 to 52 checks a minute against a full queue). Stale positions (at most 64 per section, never re-queued once checked this session) go to a bounded queue (8192, oldest dropped) and up to 16 per tick run `World.checkLight` for entries within 128 blocks of the player once `isAreaLoaded(pos, 17)` is true; others rotate to the back; dropped after 2 minutes. Client thread only; clears when the client world changes. Config `Enable Client Relight Sweep` (default true) and `Client Relight Debug` (`[AQ-CLIENT-RELIGHT]` once a minute) in `aqtweaks_client.cfg` category `relight`. The server-side `DeferredRelight` ([rtg.md](rtg.md) item 28) is separate and unchanged.

## Files

| Piece | Role |
| --- | --- |
| `ArcanaQuestTweaksConfig.ClientModuleConfig` | `aqtweaks_client.cfg` |
| `client/ClientModule.java` | Tooltip handler; registered in `ClientProxy` |
| `mixin/toughnessbar/MixinEventHandlerClient.java` | Optional Toughness Bar overlay |

## Live config

| Knob | Default | Notes |
| --- | --- | --- |
| Move Toughness Bar To Armor Side | true | Armor column, LTR. Old `aqtweaks_stamina.cfg` HUD key is dead |
| Metallurgy Tooltip Compat | true | Non-Metallurgy mining tools only |

## Do not regress

- Feathers and thirst stay on the right.
- Toughness must not share the armor **row** (keep `left_height + 10` GET).
- Do not import Metallurgy types. Do not add toughness mixin to the required json.
- Durability line must not appear on unbreakable tools (`maxDamage <= 0`).

## Verify

Armor + toughness: leftmost toughness icon over the leftmost heart/armor; extra pips go right (half pip on the right of a partial row). One row above armor. Feathers above thirst. Flag off: stock right RTL. Iron pick: Vanilla Tools + stars + durability + efficiency. Axe efficiency is log speed, not 1.0. Metallurgy pick: no second Tweaks harvest line. Remove toughnessbar jar: mixin skipped; tooltips still work.