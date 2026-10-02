# Simple Chest Shop (Minecraft 26.3 Fabric)

A lightweight, server-friendly Chest Shop mod for **Fabric Minecraft 26.3** ("Wilderness Bound"). It turns normal vanilla chests into fully functional player shops without requiring any custom blocks!

---

## 🌟 Features

- **100% Vanilla Chest Compatible**: Works with standard wooden chests, trapped chests, and double chests. No custom blocks or tile entities required.
- **Zero-Extra-Step Instant Registration**:
  - Simply open your chest and put your shop paper inside: **the shop is registered instantly the moment the paper enters the chest slot!**
  - No separate right-click, punching, or interaction step needed.
  - A friendly chime and chat confirmation (`✔ Shop registered! You are now the owner of this shop.`) confirms ownership right away while you are in the chest GUI.
  - If you ever take the paper out, the shop automatically unregisters and becomes a regular chest again.
- **Total Protection Against ALL Breaking Methods**:
  - 💥 **Explosion-Proof**: TNT, Creepers, Ghast Fireballs, Wither Skulls, End Crystals, Beds (Nether/End), Respawn Anchors, and Wind Charges cannot destroy shop chests or burn them. The chest has Bedrock-grade blast resistance.
  - 💀 **Boss-Proof**: The Wither (`destroyBlock`) and Ender Dragon (`removeBlock`) cannot destroy or remove shop chests.
  - 🔒 **Player Break Protection**: Non-owners cannot break the chest in survival mode.
  - 🛑 **Chest GUI Lock**: Non-owners cannot open the raw chest inventory to steal contents or money; right-clicking opens the graphical Buy Menu instead.
  - 🚫 **Hopper Anti-Theft**: Hoppers and hopper minecarts cannot extract items from or inject junk into active shop chests.
  - 🧲 **Piston-Proof**: Pistons cannot push or pull a registered shop chest, so it can't be relocated to sidestep the protections above.
- **Shop Co-Management (`/shop trust`)**:
  - Owners can share management of a shop with friends via `/shop trust <player>` (look at the chest first).
  - Trusted co-managers get the same restock/preview access as the owner, but only the real owner can break the chest or grant/revoke trust.
  - `/shop trusted` lists who currently manages the shop you're looking at.
- **Multiple Alternative Prices (Multi-Currency)**:
  - Sell the same product for multiple different currencies/prices!
  - Example: `1 diamond or 10 iron_ingot -> 64 cooked_beef`
  - Also supports `/` or `|` delimiters: `1 diamond / 10 iron_ingot / 32 coal -> 64 cooked_beef`
  - Customers can choose whichever currency they prefer directly in the Buy UI.
- **Graphical Paper Creation UI (`/shopcreate` or `/shop create`)**:
  - Run `/shopcreate` to open an interactive visual creator GUI.
  - **Price 1 (Primary)**, **Price 2 (Optional)**, and **Price 3 (Optional)** slots allow setting alternative currencies with ease.
  - Place your sale item into the **Sale Slot** (or leave empty to sell whatever is in the chest).
  - Click **[ Create Shop Paper ]** (Emerald) to generate the formatted, lore-annotated paper directly in your inventory!
  - Sample items placed into slots are 100% safely returned to your inventory upon paper creation or menu close.
- **Interactive Graphical Buy UI**:
  - Right-clicking any active shop chest opens a dedicated visual shop window!
  - **Multi-Trade Support**: Displays each trade option on its own clean row:
    `[ Product + Stock Lore ]  ➡ FOR ➡  [ Price + Balance Lore ]  ➡ BUY ➡  [ Buy Button ]`
  - **Interactive Status Buttons**:
    - 🟩 **[ CLICK TO BUY ]** (Lime): Deducts payment, deposits money into the chest, gives items to buyer, plays chime (`♪`), and updates stock in real time.
    - 🟨 **[ CANNOT AFFORD ]** (Yellow): Displays when the buyer is missing required funds.
    - 🟥 **[ OUT OF STOCK ]** (Red): Displays when the chest inventory has insufficient stock.
- **Full Item Name Support in Chat & UI**:
  - Chat notifications always display human-readable, localized item names (e.g. `✔ Purchased 64x Cooked Beef for 1x Diamond!`).
  - Shop owners receive instant notifications when items are bought.
- **Flexible Anvil / Rename Syntax**:
  - `1 diamond or 10 iron_ingot -> 64 cooked_beef` (Multi-price alternative)
  - `1 diamond -> 16 oak_log` (Standard Price -> Sale)
  - `2 emeralds = 64 cooked_beef`
  - `16 oak_log for 1 diamond` (Sale for Price)
  - `[Shop] 1 diamond` or `1 diamond` (Dynamic mode: sells whatever is in chest for 1 diamond)
  - Automatically handles plurals (`diamonds` → `diamond`, `apples` → `apple`) and spaced names (`cooked beef` → `cooked_beef`).
- **Owner Restocking & Preview**:
  - **Owner Normal Right-Click**: Opens the standard chest inventory to restock goods, collect profits, or adjust the paper.
  - **Owner Sneak + Right-Click**: Opens the graphical Buy Menu in preview mode to test how customers see the shop.
  - **Left-Click (Punch)**: Displays quick chat summary of all available prices, stock, and owner.

---

## 🛒 How to Use

### 1. Creating a Shop
1. Place a normal Chest down and put your stock inside (e.g. Cooked Beef).
2. Get a shop paper via `/shopcreate` or rename a paper in an Anvil (e.g. `1 diamond or 10 iron_ingot -> 64 cooked_beef`).
3. Open the chest and place the paper inside: **you are immediately registered as the owner!**

### 2. Buying from a Shop
1. **To view info**: Left-click (punch) the chest with an empty hand.
2. **To buy via GUI**: Right-click the chest to open the visual Buy Menu. Left-click the green **[ CLICK TO BUY ]** button to buy one batch.
3. **To buy in bulk**: Shift+left-click the **[ CLICK TO BUY ]** button to instantly buy as many batches as your balance, the chest's stock, and your inventory space allow.

---

## 🛠️ Building & Installation

### Requirements
- **Minecraft**: 26.3
- **Fabric Loader**: >= 0.19.5
- **Fabric API**: 0.161.0+26.3
- **Java**: OpenJDK 25

### Build from Source
```bash
# On Windows
gradlew.bat build

# On Linux / macOS
./gradlew build
```

The compiled mod jar will be available in:
```
build/libs/simplechestshop-1.0.0.jar
```
Drop this file into your Minecraft or server `mods/` directory along with Fabric API.

---

## 🧪 Testing

Run the included JUnit 5 test suite:
```bash
gradlew.bat test
```
Validates single and multi-price syntax (`or`, `/`, `|`), plurals, prefixes, double chests, and ownership lifecycle.
