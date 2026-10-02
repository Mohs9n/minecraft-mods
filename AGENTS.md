# Developer & Agent Guide: Minecraft 26.3 Fabric Mods

This document provides critical technical knowledge, architectural findings, build instructions, and best practices for future developers and AI agents working on this multi-project codebase.

---

## 📌 Projects Overview

This repository is structured as a **Gradle Multi-Project** workspace supporting multiple independent Fabric mods for **Minecraft 26.3 ("Wilderness Bound")**:

```
.
├── settings.gradle                   # Multi-project definition
├── build.gradle                      # Root repository buildscript
├── gradle.properties                 # Shared versions & Loom settings
├── simplechestshop/                  # Simple Chest Shop mod
│   ├── build.gradle
│   ├── gradle.properties
│   ├── README.md
│   └── src/
└── simpleclaims/                     # Simple Claims anti-grief mod
    ├── build.gradle
    ├── gradle.properties
    ├── README.md
    └── src/
```

### 1. Simple Chest Shop (`simplechestshop`)
- **Philosophy**: 100% Vanilla chest compatibility (single, trapped, double chests) without custom blocks or client packets.
- **Features**: Visual `/shopcreate` creator GUI, multi-price options (`or`, `/`, `|`), interactive graphical Buy GUI with stock lore, real-time instant paper registration, and bedrock-level anti-theft and blast invulnerability.

### 2. Simple Claims (`simpleclaims`)
- **Philosophy**: Lightweight, vanilla-compatible land claim and anti-griefing protection.
- **Features**: 2D coordinate bounding box protecting all Y levels (bedrock to sky limit), member trust sharing (`/claim trust`), golden hoe selection wand (`/claim wand`), border transition notifications, explosion/boss invulnerability, and persistent JSON storage.

---

## 🛠️ Toolchain & Build Environment

### Key Versions
- **Minecraft**: 26.3
- **Fabric Loader**: 0.19.5+
- **Fabric Loom**: 1.18.2
- **Fabric API**: 0.161.0+26.3
- **Gradle**: 9.8.0 (Wrapper `gradlew.bat` / `gradlew`)
- **Java**: OpenJDK 25 (Required for Minecraft 26.3)

### Running Gradle Commands (Windows PowerShell)
Always ensure `JAVA_HOME` points to the JDK 25 installation before executing Gradle commands:
```powershell
$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-25.0.4.101-hotspot"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

# Test all subprojects
.\gradlew.bat test

# Build all production jars
.\gradlew.bat build

# Or build individual subprojects
.\gradlew.bat :simplechestshop:build
.\gradlew.bat :simpleclaims:build
```
Built outputs:
- `simplechestshop/build/libs/simplechestshop-1.0.0.jar`
- `simpleclaims/build/libs/simpleclaims-1.0.0.jar`

---

## 🔍 Minecraft 26.3 Specific Architecture Findings

Minecraft 26.3 introduces several naming and structural changes compared to 1.20 / 1.21:

1. **Unobfuscated Distribution**:
   - `fabric.loom.disableObfuscation=true` must be set in `gradle.properties`.
   - No `mappings` block is needed in `build.gradle`.
   - Mod dependencies should use `implementation` instead of `modImplementation`.

2. **Package & Class Renames**:
   - `net.minecraft.resources.ResourceLocation` is now **`net.minecraft.resources.Identifier`**.
   - Dimension identifier is retrieved via `level.dimension().identifier().toString()`.
   - `Painting` entity is in `net.minecraft.world.entity.decoration.painting.Painting`.
   - Item maximum stack size is retrieved via `item.getDefaultMaxStackSize()` on `Item` (or `stack.getMaxStackSize()`).
   - Grouped colored blocks/items (like stained glass panes) use `ColorCollection`:
     ```java
     Items.STAINED_GLASS_PANE.gray();
     Items.STAINED_GLASS_PANE.lime();
     Items.STAINED_GLASS_PANE.red();
     Items.STAINED_GLASS_PANE.yellow();
     Items.STAINED_GLASS_PANE.lightBlue();
     ```
   - Container users use the `ContainerUser` interface (`user.getLivingEntity()`) rather than direct player references in block entities.
   - `Player.closeContainer()` has protected access on `Player`, but is public on `ServerPlayer`:
     ```java
     if (player instanceof ServerPlayer sp) {
         sp.closeContainer();
     }
     ```
   - Operator and admin checks use `player.isCreative()` and `player.level().getServer().getPlayerList().isOp(player.nameAndId())`.
   - Item dropping on server requires `Prediction.SERVER_ONLY`:
     ```java
     player.drop(stack, false, Prediction.SERVER_ONLY);
     ```

3. **Server-Side Item Names & Chat Bug**:
   - **Problem**: Calling `item.getName(ItemStack.EMPTY).getString()` on a dedicated server returns an empty string `""` because the server does not load client-side language `.json` files.
   - **Solution**: Always use `Component.translatableWithFallback(...)`:
     ```java
     public static Component getItemComponent(Item item) {
         if (item == null) return Component.literal("Unknown");
         String fallback = getHumanReadableName(item);
         return Component.translatableWithFallback(item.getDescriptionId(), fallback);
     }
     ```

---

## 🏛️ Simple Claims Architecture

```
com.simpleclaims
├── SimpleClaimsMod.java              # Mod entrypoint & event listeners
├── Claim.java                        # Model representing an owned 2D box (all Y levels)
├── ClaimManager.java                 # In-memory index, permissions, wand tracking, borders
├── ClaimStorage.java                 # Persistent JSON claims storage (world/simple_claims.json)
├── command/
│   └── ClaimCommands.java            # /claim wand, pos1, pos2, create, trust, untrust, list
└── mixin/
    ├── ServerExplosionMixin.java     # Strips claims from explosion blocks and fire
    ├── ExplosionDamageCalculatorMixin.java # Bedrock-grade resistance at claim borders
    ├── EnderDragonMixin.java         # Redirects dragon checkWalls removeBlock in claims
    └── WitherBossMixin.java          # Redirects wither customServerAiStep destroyBlock in claims
```

---

## 🛡️ Anti-Griefing & Invulnerability Details

1. **Explosion Defense (TNT, Creepers, Ghasts, Crystals, Beds, Anchors, Wind Charges)**:
   - `ExplosionDamageCalculatorMixin` returns `Optional.of(3600000.0F)` (Bedrock blast resistance) and `shouldBlockExplode = false`.
   - `ServerExplosionMixin` removes any position within claims from `interactWithBlocks` and `createFire`.
2. **Boss Protection**:
   - `EnderDragonMixin` redirects `checkWalls` call to `level.removeBlock`, skipping claim blocks. (Crucial: Never inject cancellation directly into `Level.removeBlock`, as vanilla `ServerPlayerGameMode.destroyBlock` calls `Level.removeBlock(pos, false)` for ALL player block mining!).
   - `WitherBossMixin` redirects `customServerAiStep` call to `level.destroyBlock`, skipping claim blocks.
3. **Player Anti-Grief**:
   - `AttackBlockCallback` & `PlayerBlockBreakEvents.BEFORE` prevent block mining by non-members.
   - `UseBlockCallback` prevents block placing, bucket emptying, and container/door/button interactions.
   - `AttackEntityCallback` & `UseEntityCallback` protect animals, villagers, pets, item frames, and armor stands.
4. **Member Trust System**:
   - Permissions check both `UUID` and `username` (case-insensitive) to prevent offline-mode, LAN, or name desyncs.
   - Owner can trust multiple friends via `/claim trust <player>`.
   - Members gain immediate full build, break, container, and redstone rights within the claim.

---

## 🧪 Testing Best Practices

- All JUnit 5 unit tests reside in `subproject/src/test/java/`.
- Validate bounds normalization, all-Y level coverage, overlapping collision detection, and member trust lists.
