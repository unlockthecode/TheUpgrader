# TheUpgrader

**TheUpgrader** is a gambling-style item upgrade plugin for Paper servers from 1.21 to the latest version. Risk your items for a chance to upgrade them into better, more valuable ones. Featuring a sleek GUI, crate-style animations, win streaks, fireworks, statistics, achievements, leaderboards, and chat announcements for big wins.

## Features

| Feature | Description |
|---------|-------------|
| Drag & Drop Wagering | Simply drag items from your inventory into the wager slot |
| Target Selection | Choose targets via presets (×2, ×4, ×8, 30%, 50%, 70%), catalog, or direct click |
| Count Adjustment | Seven count buttons: −32, −16, −1, +1, +16, +32 with click-to-reset display |
| Crate-Style Animation | Smooth 5-slot centered reel animation with pitch progression |
| Win Streak Tracking | Progressive visual feedback: Clock → Fire Charge → Magma Cream → Blaze Rod → Nether Star |
| Fireworks & Announcements | Spectacular effects for big wins (<10%, <25%, <50% chance) |
| Near-Miss Feedback | Special effects when you lose by a tiny margin (1-5% chance) |
| Item Catalog | Browse 9 categories with 45 items per page |
| Statistics System | Persistent per-player stats: wins, losses, streaks, biggest win/loss, favorites |
| **Stats GUI Shortcut** | **Quick access button in main GUI (slot 22, enchanted Blaze Rod)** |
| **Fully Configurable Stats GUI** | **All slots for main menu, leaderboards, achievements, and server stats** |
| Achievements | 28 achievements across 5 rarity tiers with celebration effects |
| Leaderboards | 8 categories: biggest win/loss, best streak, total wagered/won, net profit, total wins, win rate |
| Smart Valuation | Recipe-based item values with 1.20/1.21+ material support |
| Version Independent | Works on Paper 1.21+ with graceful fallbacks |
| Addictive Gameplay | Balanced odds, house edge, cooldowns, and attempt limits |

## Quick Start

### Installation

1. Download the latest `theupgrader-1.0.jar` from [Releases](https://github.com/unlockthecode/TheUpgrader/releases)
2. Place it in your server's `plugins/` folder
3. Restart or reload your server
4. Run `/upgrade` in-game to open the GUI

### Requirements

- **Paper version 1.21 and above**
- **Java 21 and above**

## How to Play

### Basic Gameplay

1. **Open GUI**: Run `/upgrade` or `/upgrade config` (admin)
2. **Place Wager**: Drag an item from your inventory into the **WAGER** slot (top-left)
3. **Select Target**:
   - Click **preset buttons** (×2, ×4, ×8, 30%, 50%, 70%)
   - Open **Catalog** (chest icon) to browse categories
   - Right-click an item in your inventory to set as target
4. **Adjust Count**: Use `−32`, `−16`, `−1`, `+1`, `+16`, `+32` buttons or click count display to reset to 1
5. **Spin!**: Click the **UPGRADE** button (netherite sword) to roll
6. **Quick Stats**: Click the **Statistics** button (enchanted Blaze Rod, slot 22) to open the Stats GUI

### Statistics & Achievements

- **View Stats**: Run `/upgrade stats` to open your personal statistics, or click the Stats button in the main GUI
- **Leaderboards**: Compete with other players across 8 categories
- **Achievements**: Unlock 28 achievements with rarity tiers (Common → Legendary)

### Statistics & Achievements

- **View Stats**: Run `/upgrade stats` to open your personal statistics
- **Leaderboards**: Compete with other players across 8 categories
- **Achievements**: Unlock 28 achievements with rarity tiers (Common → Legendary)

### Understanding Odds

- **House Edge**: Default 0.9 (10% house advantage)
- **Formula**: `chance = clamp(houseEdge × wagerValue / (targetValue × count), minChance, maxChance)`
- **Presets**: Calculate target value for specific multipliers or chances
- **Win**: Receive target item(s) | **Lose**: Wager is destroyed

### Win Streaks

Consecutive wins increase your streak with visual progression:

| Streak | Icon | Color |
|--------|------|-------|
| 1-2 | Clock | Yellow |
| 3-4 | Fire Charge | Gold |
| 5-7 | Magma Cream | Red |
| 8-11 | Blaze Rod | Dark Red |
| 12+ | Nether Star | Light Purple |

### Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/upgrade` | `theupgrader.use` | Open the upgrade GUI |
| `/upgrade config` | `theupgrader.admin` | Open the upgrade GUI (admin) |
| `/upgrade reload` | `theupgrader.admin` | Reload configuration |
| `/upgrade set <setting> <value>` | `theupgrader.admin` | Set a config value |
| `/upgrade get <setting>` | `theupgrader.admin` | Get a config value |
| `/upgrade stats` | `theupgrader.use` | View your statistics |

## Item Categories

The catalog features 9 categories with 45 items per page (slots 0-44, navigation in the bottom row):

| Category | Icon | Description |
|----------|------|-------------|
| All Items | Chest | Every upgradable item |
| Blocks | Grass Block | Building blocks, stone, wood, etc. |
| Ores & Ingots | Gold Ingot | Coal, iron, gold, diamond, emerald, etc. |
| Tools & Utility | Iron Pickaxe | Pickaxes, shovels, axes, utility items |
| Weapons & Armor | Diamond Sword | Swords, armor, bows, shields |
| Food | Cooked Beef | Meat, crops, cooked items |
| Farming & Nature | Wheat | Seeds, saplings, flowers, nature items |
| Redstone & Rails | Redstone | Redstone components, rails, pistons |
| Everything Else | Paper | Everything not categorized above |

## For Developers

### Building from Source

```bash
git clone https://github.com/unlockthecode/TheUpgrader.git
cd TheUpgrader
mvn clean package
```

Output: `target/theupgrader-1.0.jar`

### Project Structure

```
TheUpgrader/
├── src/main/java/com/unlockthecode/upgrader/
│   ├── UpgraderPlugin.java           # Main plugin class
│   ├── UpgraderGUI.java              # GUI implementation (54-slot chest)
│   ├── UpgraderCommand.java          # Command handler
│   ├── config/
│   │   └── UpgraderConfig.java       # Configuration management
│   ├── value/
│   │   ├── ItemValues.java           # Recipe-based valuation
│   │   ├── UpgradeOdds.java          # Chance calculation
│   │   └── ItemCategory.java         # Catalog categories
│   ├── stats/
│   │   ├── PlayerStats.java          # Per-player persistent statistics
│   │   ├── StatsManager.java         # YAML storage manager
│   │   ├── AchievementManager.java   # 28 achievements system
│   │   ├── LeaderboardManager.java   # 8 leaderboard categories
│   │   └── StatsGUI.java             # Statistics GUI (5 pages)
│   ├── listener/
│   │   ├── GUIListener.java          # Inventory click/drag handling
│   │   └── AttemptLimiter.java       # Rate limiting
│   └── util/
│       └── VersionUtil.java          # Version-independent particles/sounds
├── src/main/resources/
│   ├── plugin.yml                    # Plugin metadata
│   └── config.yml                    # Default configuration
└── pom.xml                           # Maven build file
```

### API Integration

The plugin uses a clean internal API. For external integrations, use the command interface or listen to `InventoryCloseEvent` with `UpgraderHolder`.

## Troubleshooting

| Issue | Solution |
|-------|----------|
| "Command not found" | Ensure JAR is in `plugins/` and server restarted |
| Items disappear on close | Fixed — wager is returned on GUI close |
| Can't pick up display items | Fixed — display slots are now protected |
| Animation too fast/slow | Adjust `spin-ticks` in config |
| Custom items not valued | Add to `value-overrides` or `value-multipliers` |
| Statistics not saving | Check `plugins/TheUpgrader/stats.yml` permissions |

## Credits

This plugin was inspired by and references concepts from the **Upgrade Items** mod. See [modrinth.com/mod/upgrade-items](https://modrinth.com/mod/upgrade-items) for the original mod that inspired this plugin's gameplay mechanics.

## Contributing

Contributions are welcome! Please:

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Submit a PR with a clear description

## License

MIT License — see [LICENSE](LICENSE) for details.

## Support

- **Issues**: [GitHub Issues](https://github.com/unlockthecode/TheUpgrader/issues)
- **Wiki**: [Documentation](https://github.com/unlockthecode/TheUpgrader/wiki)