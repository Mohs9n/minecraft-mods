# Developer & Agent Guide: Simple Chest Shop (Minecraft 26.3 Fabric)

This document provides critical technical knowledge, architectural findings, build instructions, and best practices for future developers and AI agents working on this codebase.

---

## 📌 Project Overview

- **Mod Name**: Simple Chest Shop (`simplechestshop`)
- **Platform**: Fabric Loader (Minecraft 26.3 "Wilderness Bound")
- **Core Philosophy**:
  - **100% Vanilla Chest Compatibility**: No custom blocks, tile entities, or custom network packets. Works with standard vanilla chests (`ChestBlock`), trapped chests, and double chests.
  - **Server-Side Native**: Compatible with vanilla clients; graphical interfaces are powered via standard server-side `ChestMenu` containers.
  - **Zero-Friction UX**: Instant paper registration upon placing paper into the chest, visual `/shopcreate` creator GUI, and interactive multi-trade Buy GUI.

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

# Run tests
.\gradlew.bat test

# Build production jar
.\gradlew.bat build
```
Built output: `build/libs/simplechestshop-1.0.0.jar`.

---

## 🔍 Minecraft 26.3 Specific Architecture Findings

Minecraft 26.3 introduces several naming and structural changes compared to 1.20 / 1.21:

1. **Unobfuscated Distribution**:
   - `fabric.loom.disableObfuscation=true` must be set in `gradle.properties`.
   - No `mappings` block is needed in `build.gradle`.
   - Mod dependencies should use `implementation` instead of `modImplementation`.

2. **Package & Class Renames**:
   - `net.minecraft.resources.ResourceLocation` is now **`net.minecraft.resources.Identifier`**.
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
   - When building chat or lore components, append the `Component` object directly into the component tree rather than converting it to a plain string.

---

## 🏛️ Core Architecture & Components

```
com.simplechestshop
├── SimpleChestShopMod.java           # Mod entrypoint & event listeners
├── ChestShopData.java                # Persistent JSON owner storage & double-chest sync
├── ChestShopManager.java             # Core shop logic, payments, stock, and item naming
├── ShopTrade.java                    # Model representing an individual trade option
├── ShopParser.java                   # Robust syntax parser for config papers
├── command/
│   └── ShopCommands.java             # /shopcreate and /shop command registration
├── gui/
│   ├── ShopCreationMenu.java         # /shopcreate visual creator GUI (multi-price)
│   └── ShopBuyMenu.java              # Interactive graphical Buy Menu (1 to 6 trades)
└── mixin/
    ├── HopperBlockEntityMixin.java   # Prevents hopper theft & injection
    ├── ServerExplosionMixin.java     # Removes shop chests from explosion damage & fire
    ├── ExplosionDamageCalculatorMixin.java # Bedrock blast resistance (3,600,000)
    ├── LevelMixin.java               # Cancels destroyBlock (Wither) & removeBlock (Dragon)
    ├── ChestBlockEntityMixin.java    # stopOpen hook (verification & auto-unregister)
    └── RandomizableContainerBlockEntityMixin.java # setItem hook (instant registration)
```

---

## 🛡️ Security & Invulnerability Implementation

A shop chest is registered if it contains valid trade paper and has an entry in `ChestShopData`. Registered chests have impenetrable defense against all destruction vectors:

1. **Explosions (TNT, Creeper, Ghast, Wither Skull, Wind Charge, Bed, Respawn Anchor)**:
   - `ExplosionDamageCalculatorMixin` intercepts `getBlockExplosionResistance` and returns `3600000.0F` (Bedrock rating), halting explosion raycasting.
   - `ExplosionDamageCalculatorMixin` intercepts `shouldBlockExplode` and returns `false`.
   - `ServerExplosionMixin` strips all registered shop chests from `interactWithBlocks` and `createFire`.
2. **Boss Destruction**:
   - `LevelMixin.destroyBlock` cancels Wither block consumption (`WitherBoss.destroyBlocks`).
   - `LevelMixin.removeBlock` prevents Ender Dragon collision deletion (`EnderDragon.checkWalls`).
3. **Player Griefing**:
   - `PlayerBlockBreakEvents.BEFORE` cancels survival block breaking for non-owners.
   - Non-owners right-clicking the chest open `ShopBuyMenu` rather than the chest container.
4. **Automation / Hopper Theft**:
   - `HopperBlockEntityMixin` cancels item extraction and insertion on shop chests.
5. **Double Chest Synchronization**:
   - `ChestBlock.getConnectedBlockPos(pos, state)` is used in `ChestShopData` to register, look up, and delete records for both halves synchronously.

---

## 📝 Shop Paper Syntax & Parsing

The parser (`ShopParser.java`) extracts trades using clean regex patterns:

- **Arrow / Colon / Equals Syntax**:
  - `1 diamond -> 64 cooked_beef`
  - `2 emeralds = 16 oak_log`
  - `1 diamond: 32 bread`
- **For Syntax**:
  - `64 cooked_beef for 1 diamond`
- **Multi-Price Alternatives**:
  - `1 diamond or 10 iron_ingot -> 64 cooked_beef`
  - `1 diamond / 10 iron_ingot / 32 coal -> 64 cooked_beef`
  - `1 diamond | 10 iron_ingot -> 64 cooked_beef`
- **Dynamic Sale Item**:
  - `1 diamond` or `[Shop] 1 diamond` (sells any non-paper items currently in the chest)
- **Automatic Normalization**:
  - Automatically handles plural names (`diamonds` → `diamond`, `apples` → `apple`).
  - Converts spaces to underscores (`cooked beef` → `cooked_beef`).

---

## 🧪 Testing Best Practices

- All JUnit 5 unit tests reside in `src/test/java/com/simplechestshop/`.
- Minecraft registries and data components must be initialized in `@BeforeAll`:
  ```java
  @BeforeAll
  public static void setup() {
      SharedConstants.tryDetectVersion();
      Bootstrap.bootStrap();
  }
  ```
- Keep tests fast and decoupled from network layers. Validate parser regexes, item resolution, pricing tokens, and storage maps.
