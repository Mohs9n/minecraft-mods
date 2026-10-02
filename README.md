# Simple Chest Shop (Minecraft 26.3 Fabric)

A lightweight, server-friendly Chest Shop mod for **Fabric Minecraft 26.3** ("Wilderness Bound"). It turns normal vanilla chests into fully functional player shops without requiring any custom blocks!

---

## 🌟 Features

- **100% Vanilla Chest Compatible**: Works with standard wooden chests, trapped chests, and double chests. No custom blocks or tile entities required.
- **Graphical Paper Creation UI (`/shopcreate` or `/shop create`)**:
  - Run the command to open a visual 3-row menu.
  - Place your desired price item/stack into the **Price Slot**.
  - Place your desired sale item/stack into the **Sale Slot** (or leave empty to sell whatever is in the chest).
  - Click **[ Create Shop Paper ]** to receive the correctly formatted and lore-annotated paper directly in your inventory!
  - Your sample items are safely returned to your inventory (never consumed).
- **Interactive Graphical Buy UI**:
  - Right-clicking any active shop chest opens a dedicated visual shop window!
  - **Product Slot**: Displays the exact item and batch count, chest stock counter, and owner name.
  - **Price Slot**: Displays required payment and your personal current balance.
  - **Interactive Buy Button**:
    - 🟩 **[ CLICK TO BUY ]** (Lime): Deducts payment, deposits money into the chest, gives items to buyer, plays chime (`♪`), and updates stock in real time. Click again to purchase more!
    - 🟨 **[ CANNOT AFFORD ]** (Yellow): Displays when the buyer is missing required funds.
    - 🟥 **[ OUT OF STOCK ]** (Red): Displays when the chest inventory has insufficient stock.
- **Flexible Anvil Syntax (Manual Setup also supported)**:
  - `1 diamond -> 16 oak_log` (Price -> Sale)
  - `2 emeralds = 64 cooked_beef`
  - `16 oak_log for 1 diamond` (Sale for Price)
  - `[Shop] 1 diamond` or `1 diamond` (Dynamic mode: sells whatever items are in the chest for 1 diamond each)
  - Automatically handles plurals (`diamonds` → `diamond`, `apples` → `apple`) and spaced names (`cooked beef` → `cooked_beef`).
- **Complete Anti-Theft Security**:
  - **Chest GUI Lock**: Non-owners cannot open the raw chest inventory to steal items or deposited currency.
  - **Break Protection**: Non-owners cannot break the shop chest in survival mode.
  - **Hopper Anti-Theft**: Hoppers (and hopper minecarts) cannot suck items out of or inject junk into active shop chests.
- **Owner Restocking & Preview**:
  - **Owner Normal Right-Click**: Opens the standard chest inventory to restock goods, collect profits, or adjust the paper.
  - **Owner Sneak + Right-Click**: Opens the graphical Buy Menu in preview mode to test how customers see the shop.
  - **Left-Click (Punch)**: Displays quick chat summary of price, stock, and owner.

---

## 🛒 How to Use

### 1. Creating a Shop
1. Place a normal Chest down.
2. Rename a piece of **Paper** in an Anvil to your desired trade, for example:
   ```
   1 diamond -> 16 oak_log
   ```
3. Put the renamed paper inside the chest along with the items you want to sell (e.g. stacks of Oak Logs).
4. As soon as you interact with the chest, you are automatically registered as the shop owner!

### 2. Buying from a Shop
1. **To view info**: Left-click (punch) the chest with an empty hand.
2. **To buy**: Hold the required payment item (e.g. 1 Diamond) in your main hand and right-click the chest.
3. The payment is transferred to the chest, and your purchased goods are deposited directly into your inventory!

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
All parser formats, plurals, prefixes, and item resolution are automatically validated.
