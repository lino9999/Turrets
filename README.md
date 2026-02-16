# 🛡️ Turrets - Ultimate Sentry Defense Plugin (1.21)

![Version](https://img.shields.io/badge/Version-2.1-brightgreen) ![API](https://img.shields.io/badge/Spigot-1.21-orange) ![License](https://img.shields.io/badge/License-MIT-blue)

**Turrets** is a high-performance **Minecraft Spigot plugin** designed for **SMP**, **Factions**, and **Survival** servers. It allows players to deploy automated sentry guns that target monsters and enemy players.

Built natively for **Minecraft 1.21**, this plugin features a deep **progression system**: turrets gain XP from kills, leveling up to increase their damage, range, fire rate, and ammo capacity.

---

## 🔥 Key Features

* **Deployable Sentry Guns**: Place a specialized Dispenser to create a fully functional turret.
* **Leveling System (1-20)**: Turrets automatically gain XP upon getting kills. Higher levels unlock massive stat boosts (Damage, Range, Fire Rate).
* **Ammo Mechanics**: Turrets require **Gold Nuggets** to fire. Players can manage ammo via an interactive GUI.
* **Smart Targeting**:
    * **All Entities**: Attacks hostile mobs and players (ignores the owner).
    * **Hostile Only**: Attacks only monsters (Zombies, Creepers, etc.).
* **Holographic Displays**: Real-time stats floating above the turret (Owner, Level, Ammo Bar).
* **Interactive GUI**: Right-click to access the control panel, reload ammo, or pick up the turret.
* **Persistence**: Turrets are saved in a local SQLite database (`turrets.db`), ensuring no data loss on restart.

---

## 🛠️ Installation

1.  Download `Turrets-2.1.jar`.
2.  **IMPORTANT:** Install [DecentHolograms](https://www.spigotmc.org/resources/decentholograms.96927/) (Required dependency for the holographic displays).
3.  Upload both files to your server's `plugins` folder.
4.  Restart the server.
5.  (Optional) Edit `config.yml` and `messages.yml` to customize the experience.

---

## 💻 Commands & Permissions

Manage your server's defenses with these commands.

| Command | Description | Permission |
| :--- | :--- | :--- |
| `/turrets give <player> <level> <amount>` | Give a turret item to a player. | `turrets.give` |
| `/turrets reload` | Reload the configuration files. | `turrets.reload` |

### Permissions List
* `turrets.use` - Allows players to place turrets (Default: true).
* `turrets.admin` - Allows opening, breaking, or modifying **any** turret, regardless of ownership.
* `turrets.give` - Admin permission to give turrets.
* `turrets.reload` - Admin permission to reload the plugin.

---

## 🎮 How to Play

1.  **Obtain a Turret**: Get a turret item (Dispenser with special NBT data) from an admin or custom shop.
2.  **Placement**: Place the turret on the ground. A hologram will appear indicating it's active.
3.  **Reloading**:
    * Right-click the turret to open the **Menu**.
    * Click the **Ammo** icon while having **Gold Nuggets** in your inventory to reload.
4.  **Upgrading**:
    * The turret starts at **Level 1**.
    * It gains **XP** for every kill.
    * Once it reaches the required kills, it levels up automatically!
5.  **Management**: Use the GUI to switch targeting modes or pick up the turret (saving its stats).

---

## ⚙️ Configuration

The `config.yml` allows you to balance every aspect of the turrets. You can define up to 20 levels!

```yaml
# Maximum number of turrets a single player can place
max-turrets-per-player: 5

levels:
  1:
    kills: 0          # Kills needed for this level
    damage: 2.0       # Damage per shot (in hearts)
    ammo: 300         # Maximum ammo capacity
    range: 10.0       # Detection range in blocks
    shoot-delay: 1000 # Delay between shots (milliseconds)
  2:
    kills: 500
    damage: 2.5
    ammo: 360
    range: 10.5
    shoot-delay: 950
  # ... configuration continues up to level 20
