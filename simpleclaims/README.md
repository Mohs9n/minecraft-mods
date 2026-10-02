# Simple Claims (Minecraft 26.3 Fabric)

A lightweight, server-friendly land claim and anti-griefing mod for **Fabric Minecraft 26.3** ("Wilderness Bound"). It enables players to claim owned territories, share them with trusted friends, and completely block all forms of player, mob, and explosion griefing.

---

## 🌟 Features

- **Full Vertical Coverage (All Y Levels Owned)**:
  - Simply define two horizontal corners `(x1, z1)` and `(x2, z2)`.
  - The claim protects the **entire vertical column from bedrock to the sky limit** (all Y levels).
- **Multi-Player Sharing & Trust**:
  - Owners can trust multiple friends with `/claim trust <player>`.
  - Trusted members have full permissions to build, break, use chests, doors, redstone, and farm.
  - Owners can revoke access anytime with `/claim untrust <player>`.
- **Complete Anti-Grief Protection (For Non-Members)**:
  - 🚫 **Block Breaking**: Cannot break or mine any blocks inside the claim.
  - 🧱 **Block Placing**: Cannot place blocks inside the claim.
  - 📦 **Container & Redstone Lock**: Cannot open chests, barrels, shulkers, furnaces, hoppers, or use doors, trapdoors, buttons, levers, and pressure plates.
  - 🪣 **Fluid & Fire Protection**: Cannot empty lava/water buckets or ignite blocks with flint & steel.
  - 🐾 **Entity Protection**: Friendly animals, villagers, tamed pets, armor stands, item frames, and paintings cannot be attacked or robbed.
  - 💥 **Explosion-Proof**: TNT, Creepers, Ghast fireballs, End crystals, Beds, Respawn anchors, and Wind charges cannot destroy blocks or create fire inside claims.
  - 💀 **Boss-Proof**: Wither boss block consumption (`destroyBlock`) and Ender Dragon wall clearing (`removeBlock`) are halted at claim borders.
  - 🚪 **Piston Proof**: Blocks cannot be pushed into or pulled out of claims by unauthorized pistons.
- **Zero-Friction Selection & Commands**:
  - **Claim Wand (`/claim wand`)**: Left-click a block for Corner 1, Right-click a block for Corner 2.
  - **Quick Commands**: `/claim pos1`, `/claim pos2`, and `/claim create <name>`.
  - **Direct Coordinates**: `/claim create <name> <x1> <z1> <x2> <z2>`.
- **Graphical Claim Manager (`/claim gui`)**:
  - Lists every claim you own; click one to open its management screen.
  - Trust/untrust members by clicking player heads instead of typing commands.
  - Delete a claim with an armed "click again to confirm" button.
- **Border Notifications**:
  - Automatically notifies players in chat when crossing claim borders:
    `§6[Claims] §7Entering §eSteve's Claim §8(Base)`
    `§6[Claims] §7Leaving §eSteve's Claim §7(Wilderness)`
- **Overlap Prevention & Data Persistence**:
  - Claims cannot overlap with other players' claims.
  - Automatically saved to `world/simple_claims.json`.

---

## 📜 Command Reference

| Command | Description |
| :--- | :--- |
| `/claim wand` | Gives you a golden hoe selection wand (Left-click pos 1, Right-click pos 2). |
| `/claim pos1` | Sets Corner 1 to your current block position. |
| `/claim pos2` | Sets Corner 2 to your current block position. |
| `/claim create <name>` | Claims the area selected between pos1 and pos2. |
| `/claim create <name> <x1> <z1> <x2> <z2>` | Claims a specific coordinate bounding box directly. |
| `/claim trust <player>` | Trusts a friend, giving them full access to build and interact. |
| `/claim untrust <player>` | Removes a trusted friend from the claim. |
| `/claim list` | Lists all claims owned by you with dimensions and block counts. |
| `/claim info` | Displays details (owner, members, coordinates, size) of the claim you are in. |
| `/claim delete` or `/claim abandon` | Deletes the claim you are standing in (owner or admin only). |
| `/claim gui` or `/claim menu` | Opens the graphical claim manager (list, trust, untrust, delete). |
| `/claim help` | Displays the in-game command cheat-sheet. |

---

## 🛠️ Building & Installation

### Requirements
- **Minecraft**: 26.3
- **Fabric Loader**: >= 0.19.5
- **Fabric API**: 0.161.0+26.3
- **Java**: OpenJDK 25

### Build from Source
From the project root directory:
```bash
# Build Simple Claims
gradlew.bat :simpleclaims:build

# Or build all mods
gradlew.bat build
```

Compiled jar location:
```
simpleclaims/build/libs/simpleclaims-1.0.0.jar
```
Drop this file into your Minecraft or server `mods/` directory along with Fabric API.
