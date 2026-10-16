# Hypixel Pit Remake

**English** | [日本語](README.ja.md) | [简体中文](README.zh-CN.md) | [한국어](README.ko.md)

A server plugin that recreates Hypixel's The Pit on Minecraft 1.8.8 (PaperSpigot).
Numbers, messages, sounds and GUIs are based on recordings (replays) of Hypixel and [pit.wiki](https://pit.wiki).

## Requirements

- JDK 17 or newer (to run Gradle). Java 8 is downloaded automatically by the Gradle toolchain
- A Minecraft 1.8.x client

## Build and test server

```powershell
.\gradlew.bat build       # Build the plugin (runs the tests) → build/libs/pit-<version>.jar
.\gradlew.bat runServer   # Build and start a Paper 1.8.8 test server in run/
```

- Connect to the test server at `localhost`
- Run `op <name>` in the console to become an admin. `stop` shuts it down
- To use it on your own server, just put the jar in `plugins/` of a Paper 1.8.8 server (started with Java 8)

## Map

Only the Seasons map is included. It was rebuilt from recordings and is bundled in the plugin (`src/main/resources/maps/seasons/`).

- Every time the server starts, the world's region files are overwritten with this map before the world loads.
  So a new server gets the map just by installing the plugin, and if the server crashes during a Spire or KOTH, the terrain is restored on the next start
- `/pit map save` saves the current terrain (`plugins/Pit/map-template/`) and it is restored from then on
- NPC, spawn point and hologram positions are in `maps/seasons.yml`; Spire / KOTH / Launcher positions are in `spire/` `koth/` `launcher/`.
  Settings on the server (`plugins/Pit/npcs.yml`, `spawn.points` / `holograms.lines` in `config.yml`) take priority if present

## Features

- **Combat**: damage calculation (additive / multiplicative, switchable; additive is how the real Pit calculates damage and is the default), Mystic enchants (sword, bow, pants, Rage / Dark / Sewer / Aqua)
- **Mystics**: Mystic Well (tiering, Lives, sacrifice), Hidden Jewel, Totally Legit Gem, Philosopher's Cactus, Funky Feather, Repair Kit
- **Progression**: perks, passives, megastreaks (Overdrive / Beastmode / Hermit / Highlander / Magnum Opus / To the Moon / Uberstreak), killstreaks
- **Prestige**: Renown Shop (Upgrades / Perks / Shop Items / Killstreaks / Heresy / Fancy Hat)
- **Shop**: Items villager (non-permanent items), Autobuy, Pants Bundle
- **Events**: Spire, KOTH, Auction, 2X REWARDS, day/night and Night Quests (see "Events" below)
- **Other**: Quest Master (Quests & Contracts), leaderboards, stats menu, Profile Viewer, trading, Item Stash, ranks and chat
- Bounties, kill / assist rewards, gold ingots, bots, backups

## Events

Automatic events start on the clock (every `interval-minutes` of each event in the config, from `offset-minutes` past the hour).
Automatic events are off by default (`enabled: false`), so turn them on with `/events auto on` or in the config.

Default schedule (set so nothing overlaps):

| Minute | Event |
| --- | --- |
| :00 | Spire warning (starts :03, ends :08) |
| :10 / :25 / :40 / :55 | Auction |
| :15 / :45 | KOTH |
| :20 / :50 | 2X REWARDS |

If the settings overlap, a warning is logged on startup and on `/pit reload`. `/events` shows the time until the next event.

## Commands

### Players

| Command | Description |
| --- | --- |
| `/events` | The running event and the time until the next events |
| `/trade <name>` | Send a trade request |
| `/view <name>` | Open the Profile Viewer |
| `/spawn` | Go back to spawn |
| `/oof` | Die and go back to spawn |
| `/cool` | List high prestige players |
| `/pit version` `/pit nonce` | Show the version / the nonce of the held item |

### Admins

| Command | Description |
| --- | --- |
| `/pit reload` | Reload the config (including the event schedule) |
| `/pit damage [additive\|multiplicative]` | Show / change how damage modifiers stack |
| `/pit debug` | Show the damage calculation of hits you deal / take in chat |
| `/pit backup` | Zip the world and `plugins/Pit` into `backups/` |
| `/pit map save` | Save the current terrain as the one restored on startup |
| `/pit spire <start\|now\|end>` | Start a Spire (with the warning / right away) or end it |
| `/pit koth <start\|end>` | Start / end a KOTH |
| `/pit auction <start\|end>` | Start / end an Auction |
| `/pit multireward <X> start` / `end` | Start / end an X times rewards event |
| `/pit time <day\|night>` | Start dawn / dusk right away |
| `/pit <spire\|koth> <addpos <name>\|removepos\|positions>` | Manage where Spires / KOTHs appear |
| `/events auto [spire\|koth\|auction\|multireward] [on\|off]` | Turn automatic events on / off (all without a name; a running event finishes) |
| `/fresh <sword\|bow\|pants color\|gem\|cactus\|feather\|archangel\|armageddon\|goldenhelmet\|vile\|repairkit> [amount]` | Give items |
| `/menchant <enchant> [level]` | Add an enchant to the held Mystic |
| `/fillmystic` | Fill empty slots with copies of the held Mystic |
| `/instantupgrade [tiers]` | Upgrade the Mystics in your inventory without the Well |
| `/pitbot <spawn [count] [player\|spawn]\|remove\|auto [on\|off]\|count\|list>` | Spawn / remove bots, turn automatic bots on / off |
| `/pitnpc <spawn <upgrades\|prestige>\|remove\|list>` | Place / remove NPCs |
| `/pitspawn <add\|list\|clear\|area <radius> <min-y>\|tp>` | Spawn points and the no-PvP spawn area |
| `/pitgold` `/pitlevel` `/pitprestige` `/pitrenown` `/pitbounty` `/pitstreak` | Change values (for testing) |
| `/pitpermission <add\|remove> <name> <permission...\|all>` / `list [name]` | Give / take permissions to non-op players |

## Permissions

Operators can use every admin command. Non-op players can be allowed some of them with `/pitpermission` (saved in `plugins/Pit/permissions.yml`).

| Name | Allows |
| --- | --- |
| `renown` `prestige` `level` `gold` `streak` `bounty` | The matching `/pit...` command |
| `fresh` `menchant` `fillmystic` `instantupgrade` `bot` | The matching command |
| `event` | Starting / ending `/pit spire\|koth\|auction\|multireward\|time` and `/events auto` |
| `rank` | The admin-only rank |
| `supporter` | The Supporter Fancy Hat |

`/pit reload|damage|debug|backup|map`, Spire / KOTH positions, `/pitnpc`, `/pitspawn` and `/pitpermission` are op only.

## Layout

```
src/main/java/com/pitremake/
├── PitPlugin.java          Main class
├── combat/                 Damage calculation, enchant effects
├── enchant/                Enchant registry and descriptions
├── well/                   Mystic Well
├── gem/ special/           Totally Legit Gem, special items
├── upgrade/ perk/          Perks, passives, megastreaks, killstreaks
├── prestige/ renown/       Prestige, Renown Shop
├── shop/                   Items villager
├── stats/ gold/ bounty/    Kills, XP, gold, bounties, stats
├── event/                  Automatic event schedule
├── spire/ koth/ auction/ rewards/ night/   Events, day/night
├── quest/ leaderboard/     Quest Master, leaderboards
├── trade/ stash/           Trading, Item Stash
├── rank/ permission/       Ranks, chat, permissions
├── map/ spawn/ npc/ hologram/   Map, spawn, launchers, NPCs, holograms
├── bot/                    Bots
└── data/                   Per-player data, backups
src/main/resources/
├── maps/                   Map (region) and NPC / spawn / hologram positions
└── spire/ koth/ launcher/  Event and launcher positions
docs/DATA_STORAGE.md        Where data is stored, running the server (Japanese)
```
