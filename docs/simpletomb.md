# Simple Tomb module (1.9)

Last updated: 2026-09-30.

Optional mixin and event layer if Simple Tomb is present (`simpletomb-1.12.2-1.0.0.jar`, modid `simpletomb`, package `com.lothrazar.simpletomb`). Late mixin configuration: `mixins.aqtweaks.simpletomb.json` (`required: false`).

## Locked intent

1. **Exact Inventory Slot Mapping**:
   When a player dies, items stored in their grave must restore to the exact inventory slots they previously occupied (main inventory slots 0–35, armor slots 0–3, offhand slot 0).
2. **Baubles Integration**:
   Death must also map and restore worn Baubles (via BaublesEX `IBaublesItemHandler`, slots 0–6) into their matching slots upon retrieving the grave.
3. **Cosmetic Armor Exclusion**:
   Cosmetic Armor is handled independently by Corpse Complex (`Keep Cosmetic Armor=true`). Simple Tomb must **not** touch or restore Cosmetic Armor slots to prevent duplication or conflicting retention.
4. **Non-destructive Item Displacement**:
   If an inventory or bauble slot is already occupied when the player opens their grave (e.g. they picked up a flower, respawned with a soulbound item, or equipped temporary armor), the grave item takes priority and restores into its original slot. The conflicting item is non-destructively displaced into the first available empty main inventory slot (`addItemStackToInventory`). If the inventory is completely full, the displaced item is safely dropped at the player's feet (`entityDropItem`). No items are ever deleted, overwritten, or destroyed.
5. **Curse of Binding Safety**:
   If an equipped armor piece has `Curse of Binding`, it cannot be unequipped; the incoming grave armor piece is safely diverted to the main inventory (or player feet if full).
6. **Death Backups & Admin Recovery**:
   Every death saves a rolling backup record (up to 3 per player UUID) in Overworld `WorldSavedData` (`aqtweaks_death_backups.dat`). Operators (permission level 2) can view and recover backups via `/aqtomb list` and `/aqtomb recover` if a tomb fails to place or is inaccessible.

---

## How the parent works

Jar: `simpletomb-1.12.2-1.0.0.jar` (modid: `simpletomb`).
Key classes:
- `com.lothrazar.simpletomb.block.TileEntityTomb`:
  TileEntity holding an `ItemStackHandler inventory = new ItemStackHandler(100)` and owner UUID / string.
  - `writeToNBT` (`func_189515_b`) / `readFromNBT` (`func_145839_a`): Persists grave items using vanilla item stack handlers.
  - `getUpdateTag` (`func_189517_E_`): Sends tile data to clients on chunk load.
  - `giveInventory(EntityPlayer)`: Triggered when player opens or activates their grave. Stock implementation iterates `inventory` and calls `ItemHandlerHelper.insertItemStacked(new PlayerMainInvWrapper(player.inventory), stack, false)` — discarding all slot indices and dumping armor/hotbar into whatever slots happen to be empty.
  - `removeGraveBy(EntityPlayer)`: Private method that plays break effects, drops remaining items if any, and removes the grave block.
- `com.lothrazar.simpletomb.event.PlayerTombEvents`:
  Listens to Forge death drops:
  - `onPlayerDrops(PlayerDropsEvent)` at `HIGHEST`: Gathers `event.getDrops()`, attempts to locate a valid ground position, places `BlockTomb`, and calls `ItemHandlerHelper.insertItemStacked(te.getInventory(), ...)` to populate the tomb.

---

## Technical Design

### 1. Transient Slot Tagging & Redirect Injection

Simple Tomb populates its tile entity from `PlayerDropsEvent` drops. However, `EntityItem` drops do not natively store player slot coordinates.
- In `SimpleTombModule.onPlayerDeath` (`LivingDeathEvent` at `LOWEST`), Tweaks inspects the dying player's `InventoryPlayer` (main 0–35, armor 0–3, offhand 0) and worn Baubles (`IBaublesItemHandler` 0–6).
- Tweaks tags each non-empty stack with transient compound NBT (`AQTweaks_GraveSlot`: `{type: int, slot: int}`).
- In `MixinPlayerTombEvents`, Tweaks redirects the call to `ItemHandlerHelper.insertItemStacked(te.getInventory(), dropStack)`:
  - If the stack has `AQTweaks_GraveSlot`, Tweaks finds the first empty slot in `te.getInventory()` and calls `setStackInSlot(targetSlot, stack)`. This preserves 1:1 stack identity without stack-merging or slot scrambling.
  - Tweaks simultaneously records `targetSlot -> SlotMapping(type, slot)` into the active `DeathContext`.
  - Stacks lacking tags (e.g. third-party late drops) fall back to `insertItemStacked` and their target slots are recorded as unmapped.

### 2. BaublesEX Drop Sweeping

BaublesEX drops items during death events, which may either be dropped during `PlayerDropsEvent` or after.
- In `SimpleTombModule.onPlayerDrops` (`PlayerDropsEvent` at `LOWEST`), after Simple Tomb places the tomb tile, Tweaks sweeps any remaining undropped baubles or late drops matching player bauble slots, inserts them directly into the captured tomb's `IItemHandler`, and records the mapping.
- Transient tags on any leftover undropped items are stripped cleanly.

### 3. Slot Mapping NBT & Network Security

The mapping (`AQTweaks_SlotMap`) is written directly into `TileEntityTomb` compound NBT:
- `MixinTileEntityTomb` intercepts `writeToNBT` and `readFromNBT` to persist `AQTweaks_SlotMap`.
- `MixinTileEntityTomb` intercepts `getUpdateTag` to strip `AQTweaks_SlotMap` before serializing to client update packets, keeping client network synchronization minimal and pure vanilla Simple Tomb compatible.

### 4. Slot Restoration & Non-destructive Displacement

`MixinTileEntityTomb` injects at `HEAD` of `giveInventory(EntityPlayer player)` with `cancellable = true`:
1. If the player does not match the tomb owner, it returns early (stock behavior).
2. Tweaks reads `AQTweaks_SlotMap` from tile NBT.
3. For each slot in `te.getInventory()`:
   - If mapped:
     - **Main / Hotbar (Type 0)**: Checked against `player.inventory.mainInventory.get(slot)`. If occupied, existing item is displaced via `addItemStackToInventory(existing)` (or `player.dropItem` if inventory full). The grave stack is then set into `mainInventory.set(slot, graveStack)`.
     - **Armor (Type 1)**: Checked against `player.inventory.armorInventory.get(slot)`. If occupied and has `EnchantmentBindingCurse`, grave item diverts safely to main/feet. Otherwise, the existing armor is displaced to main/feet, and the grave armor is set into `armorInventory.set(slot, graveStack)`.
     - **Offhand (Type 2)**: Checked against `player.inventory.offHandInventory.get(0)`. Displaced to main/feet; grave stack set into offhand.
     - **Baubles (Type 3)**: Checked against `baublesHandler.getStackInSlot(slot)`. If occupied or invalid for that slot, existing bauble is displaced to main inventory / feet; grave bauble is restored and synced to client via `PacketHandler.INSTANCE.sendTo(new PacketSync(player, slot), player)`.
   - If unmapped:
     - Directly deposited into main inventory via `addItemStackToInventory`, remainder dropped at feet.
4. Clears restored items from tomb inventory.
5. Calls `@Invoker removeGraveBy(player)` to trigger stock block removal and particle effects.
6. Notifies player via `player.sendMessage(MessageType.RETRIEVE.get(ownerName))`.
7. Cancels original `giveInventory` execution.

### 5. Death Backups & Admin Recovery

To guard against void deaths, tomb placement failures in modded dimensions, or unretrievable locations:
- In `SimpleTombModule.onPlayerDrops`, after tomb processing, a snapshot of all saved items and slot mappings is written to `TombBackupSaveData` (`aqtweaks_death_backups.dat` attached to overworld dimension 0).
- Stores up to 3 most recent backups per player UUID.
- Each backup entry records:
  - Timestamp (UTC epoch ms)
  - Death dimension and XYZ coordinates
  - Grave dimension and XYZ coordinates (and boolean flag if grave block successfully placed)
  - List of stripped `ItemStack` entries with full NBT and slot mappings.
- **Admin Command `/aqtomb`** (Permission Level 2):
  - `/aqtomb list <player>`: Displays available death backups (0: newest, 1, 2) with timestamp, death coordinates, and grave status.
  - `/aqtomb recover <player> [backupIndex] [targetPlayer]`: Non-destructively restores backup items into target player's inventory using the same displacement logic as tombs. Removes the restored backup from save data to prevent duplication exploits.

---

### 3a. Slot map handoff and cleanup

`MixinTileEntityTomb` implements `simpletomb/TombSlotMapAccess`; `SimpleTombModule.onPlayerDrops` calls `aqtweaks$setSlotMap` directly (no `writeToNBT`/`readFromNBT` round trip). Grave-slot tags are stripped from the player's remaining inventory after drops, and `SimpleTombModule.onRespawn` clears any stale death context and tags when a death never reached `PlayerDropsEvent`.

## Files

| File | Role |
| --- | --- |
| `simpletomb/TombSlotMaps.java` | Slot mapping data structures, tagging/stripping logic, NBT serialization, `redirectInsertItemStacked`, and `restoreItemToSlot` displacement engine. |
| `simpletomb/TombBaubleSlots.java` | BaublesEX integration: tagging worn baubles, sweeping undropped baubles, restoring bauble slots, and firing network sync packets. |
| `simpletomb/TombBackupSaveData.java` | Overworld `WorldSavedData` (`aqtweaks_death_backups.dat`) storing rolling 3-death backup history per UUID. |
| `simpletomb/SimpleTombModule.java` | Forge event bus handler listening to `LivingDeathEvent` and `PlayerDropsEvent` to coordinate tagging, sweeping, NBT writing, and backup storage. |
| `simpletomb/CommandAqTomb.java` | OP level 2 admin command `/aqtomb <list\|recover>` for backup recovery. |
| `mixin/simpletomb/MixinTileEntityTomb.java` | Injects into `TileEntityTomb` to capture tile instance, persist slot mappings in NBT, strip network update tag, invoke `removeGraveBy`, and override `giveInventory` with custom restoration. |
| `mixin/simpletomb/MixinPlayerTombEvents.java` | Mixin redirect on `ItemHandlerHelper.insertItemStacked` in `PlayerTombEvents.onPlayerDrops` to route into `TombSlotMaps`. |
| `mixins.aqtweaks.simpletomb.json` | Late mixin configuration (`required: false`). |

---

## Verification & Smoke Test

1. **Vanilla Slot Retention**:
   - Die with items in specific hotbar slots, armor, and offhand.
   - Respawn, navigate to grave, retrieve grave.
   - Confirm hotbar items, armor, and offhand return to identical slots.
2. **Bauble Slot Retention**:
   - Equip items across multiple BaublesEX slots (amulet, rings, belt, charm).
   - Die and retrieve grave.
   - Confirm baubles return to identical slots with correct client sync.
3. **Cosmetic Armor Safety**:
   - Equip cosmetic armor in Cosmetic Armor Reworked slots.
   - Die and respawn.
   - Confirm cosmetic armor remains equipped on respawn (handled by Corpse Complex).
   - Retrieve grave and confirm no cosmetic duplicates or conflicts occur.
4. **Non-destructive Displacement**:
   - Die with full armor and hotbar.
   - Respawn and equip leather boots and place cobblestone in slot 0.
   - Retrieve grave: confirm grave diamond boots replace leather boots (leather boots moved to empty inventory slot); grave hotbar item takes slot 0 (cobblestone moved to empty slot).
   - Fill inventory completely with cobblestone; retrieve grave with leather armor equipped: confirm displaced leather armor drops safely at player feet.
5. **Admin Recovery Command**:
   - Die in lava / void.
   - Run `/aqtomb list <player>` as OP: verify death entry is listed with dimension and coordinates.
   - Run `/aqtomb recover <player> 0`: confirm items are restored into original slots and baubles.
   - Run `/aqtomb list <player>`: confirm backup 0 has been consumed and cleared.
