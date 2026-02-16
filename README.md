# 🛡️ Turrets - Advanced Sentry Defense System (1.21)

![Version](https://img.shields.io/badge/Version-2.1-brightgreen) ![API](https://img.shields.io/badge/Spigot-1.21-orange) ![License](https://img.shields.io/badge/License-MIT-blue)

**Turrets** is a powerful and fully customizable **Minecraft Spigot plugin** that allows players to craft, place, and upgrade defensive sentry guns. Perfect for **SMPs**, **Factions**, or **Survival** servers, these turrets automatically target and eliminate threats.

Designed for **Minecraft 1.21**, this plugin features a progression system where turrets gain XP through kills, leveling up to increase damage, range, and fire rate.

---

## 🔥 Key Features

* **Deployable Sentry Guns**: Place a specific Dispenser to create a working turret.
* **Leveling System (1-20)**: Turrets gain XP upon killing mobs or players. Higher levels unlock better stats (Damage, Range, Ammo Capacity).
* **Ammo Management**: Turrets require **Gold Nuggets** to function. Players can reload them via an interactive GUI.
* **Smart Targeting**: Toggle between **"All Entities"** (targets players & mobs) or **"Hostile Only"** (targets only monsters). Ignores the owner automatically.
* **Interactive GUI**: Right-click a turret to view stats, reload ammo, change targeting mode, or pick it up safely.
* **Holographic Displays**: Real-time stats (Owner, Level, Ammo Bar) displayed floating above the turret.
* **Fully Configurable**: Edit damage, range, fire rate, and kill requirements for every single level in `config.yml`.

---

## 🛠️ Installation

1.  Download `Turrets-2.1.jar`.
2.  **Requirement:** Install [DecentHolograms](https://www.spigotmc.org/resources/decentholograms.96927/) (Required dependency).
3.  Place both files into your server's `plugins` folder.
4.  Restart your server.
5.  (Optional) Configure `config.yml` to tweak turret balance.

---

## 💻 Commands & Permissions

Manage your turrets with the following commands.

| Command | Description | Permission |
| :--- | :--- | :--- |
| `/turrets give <player> <level> <amount>` | Give a turret item to a player. | `turrets.give` |
| `/turrets reload` | Reload configuration and messages. | `turrets.reload` |

**Permissions:**
* `turrets.use` - Allows players to place turrets (Default: true).
* `turrets.admin` - Bypass ownership checks (open/break any turret).

---

## ⚙️ Configuration & Levels

You can customize the progression of turrets in `config.yml`. Define up to 20 levels of power!

```yaml
max-turrets-per-player: 5

levels:
  1:
    kills: 0          # Kills required to reach this level
    damage: 2.0       # Damage per shot
    ammo: 300         # Max ammo capacity
    range: 10.0       # Shooting range in blocks
    shoot-delay: 1000 # Delay between shots (in ms)
