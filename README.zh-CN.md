# Hypixel Pit Remake

[English](README.md) | [日本語](README.ja.md) | **简体中文** | [한국어](README.ko.md)

在 Minecraft 1.8.8 (PaperSpigot) 上复刻 Hypixel 的 The Pit 的服务器插件。

## 环境要求

- JDK 17 或更高版本 (用于运行 Gradle)。Java 8 会由 Gradle 工具链自动下载
- Minecraft 1.8.x 客户端

## 构建与测试服务器

```powershell
.\gradlew.bat build       # 构建插件 (同时运行测试) → build/libs/pit-<version>.jar
.\gradlew.bat runServer   # 构建并在 run/ 中启动 Paper 1.8.8 测试服务器
```

- 通过 `localhost` 连接测试服务器
- 在控制台执行 `op <名字>` 即可成为管理员。`stop` 关闭服务器
- 在自己的服务器上使用时,只需把 jar 放进 Paper 1.8.8 (用 Java 8 启动) 的 `plugins/` 中

## 地图

只包含 Seasons 地图。地图根据录像重建,并打包在插件中 (`src/main/resources/maps/seasons/`)。

- 每次服务器启动时,在加载世界之前会用这张地图覆盖世界的 region 文件。
  因此新服务器只需安装插件并启动就有地图;即使在 Spire 或 KOTH 进行中崩溃导致地形改变,下次启动也会恢复
- 用 `/pit map save` 保存当前地形 (`plugins/Pit/map-template/`) 后,之后会恢复为该地形
- NPC、出生点和全息文字的位置在 `maps/seasons.yml`,Spire / KOTH / 弹射器的位置在 `spire/` `koth/` `launcher/`。
  如果服务器端有设置 (`plugins/Pit/npcs.yml`、`config.yml` 中的 `spawn.points` / `holograms.lines`),则以服务器端为准

## 主要功能

- **战斗**: 伤害计算 (可切换加法 / 乘法叠加;加法是原版 Pit 的计算方式,也是默认设置)、Mystic 附魔 (剑、弓、裤子、Rage / Dark / Sewer / Aqua)
- **Mystic**: Mystic Well (升级、Lives、献祭)、Hidden Jewel、Totally Legit Gem、Philosopher's Cactus、Funky Feather、Repair Kit
- **成长**: 天赋 (Perk)、被动 (Passive)、超级连杀 (Overdrive / Beastmode / Hermit / Highlander / Magnum Opus / To the Moon / Uberstreak)、连杀奖励
- **转生 (Prestige)**: Renown Shop (Upgrades / Perks / Shop Items / Killstreaks / Heresy / Fancy Hat)
- **商店**: Items 村民 (非永久物品)、Autobuy、Pants Bundle
- **活动**: Spire、KOTH、Auction、2X REWARDS、昼夜与 Night Quest (见下方「活动」)
- **其他**: Quest Master (Quests & Contracts)、排行榜、统计菜单、Profile Viewer、交易、Item Stash、头衔与聊天
- 悬赏、击杀 / 助攻奖励、金锭、机器人 (Bot)、备份

## 活动

自动活动按时钟开始 (按配置中各活动的 `interval-minutes` 间隔,从每小时的第 `offset-minutes` 分钟开始)。
自动活动默认关闭 (`enabled: false`),请用 `/events auto on` 或在配置中开启。

默认时间表 (互不重叠):

| 分钟 | 活动 |
| --- | --- |
| :00 | Spire 预告 (:03 开始,:08 结束) |
| :10 / :25 / :40 / :55 | Auction |
| :15 / :45 | KOTH |
| :20 / :50 | 2X REWARDS |

如果设置发生重叠,启动时和执行 `/pit reload` 时会在日志中输出警告。用 `/events` 可查看距下一个活动的时间。

## 命令

### 玩家

| 命令 | 说明 |
| --- | --- |
| `/events` | 正在进行的活动以及距下一个活动的时间 |
| `/trade <名字>` | 发送交易请求 |
| `/view <名字>` | 打开 Profile Viewer |
| `/spawn` | 回到出生点 |
| `/oof` | 自杀并回到出生点 |
| `/cool` | 高转生玩家列表 |
| `/pit version` `/pit nonce` | 显示版本 / 手持物品的 nonce |

### 管理员

| 命令 | 说明 |
| --- | --- |
| `/pit reload` | 重新加载配置 (包括活动时间表) |
| `/pit damage [additive\|multiplicative]` | 查看 / 更改伤害增减的叠加方式 |
| `/pit debug` | 在聊天中显示你造成 / 受到的伤害计算 |
| `/pit backup` | 将世界和 `plugins/Pit` 压缩保存到 `backups/` |
| `/pit map save` | 将当前地形保存为启动时恢复的地形 |
| `/pit spire <start\|now\|end>` | 从预告开始 / 立即开始 / 结束 Spire |
| `/pit koth <start\|end>` | 开始 / 结束 KOTH |
| `/pit auction <start\|end>` | 开始 / 结束 Auction |
| `/pit multireward <X> start` / `end` | 开始 / 结束 X 倍奖励活动 |
| `/pit time <day\|night>` | 立即开始黎明 / 黄昏 |
| `/pit <spire\|koth> <addpos <名字>\|removepos\|positions>` | 管理 Spire / KOTH 的出现位置 |
| `/events auto [spire\|koth\|auction\|multireward] [on\|off]` | 开启 / 关闭自动活动 (不指定名字则为全部;进行中的活动会持续到结束) |
| `/fresh <sword\|bow\|pants 颜色\|gem\|cactus\|feather\|archangel\|armageddon\|goldenhelmet\|vile\|repairkit> [数量]` | 获取物品 |
| `/menchant <附魔> [等级]` | 给手持的 Mystic 添加附魔 |
| `/fillmystic` | 用手持 Mystic 的副本填满空槽位 |
| `/instantupgrade [等级]` | 不使用 Well 直接升级背包中的 Mystic |
| `/pitbot <spawn [数量] [player\|spawn]\|remove\|auto [on\|off]\|count\|list>` | 生成 / 移除机器人,开启 / 关闭自动生成 |
| `/pitnpc <spawn <upgrades\|prestige>\|remove\|list>` | 放置 / 移除 NPC |
| `/pitspawn <add\|list\|clear\|area <半径> <最低y>\|tp>` | 出生点与出生区的禁止 PvP 区域 |
| `/pitgold` `/pitlevel` `/pitprestige` `/pitrenown` `/pitbounty` `/pitstreak` | 修改数值 (测试用) |
| `/pitpermission <add\|remove> <名字> <权限...\|all>` / `list [名字]` | 给非 op 玩家添加 / 移除权限 |

## 权限

op 可以使用所有管理员命令。非 op 玩家可以通过 `/pitpermission` 获得部分权限 (保存在 `plugins/Pit/permissions.yml`)。

| 名称 | 允许使用 |
| --- | --- |
| `renown` `prestige` `level` `gold` `streak` `bounty` | 对应的 `/pit...` 命令 |
| `fresh` `menchant` `fillmystic` `instantupgrade` `bot` | 对应的命令 |
| `event` | `/pit spire\|koth\|auction\|multireward\|time` 的开始 / 结束以及 `/events auto` |
| `rank` | 仅限管理员的头衔 |
| `supporter` | Fancy Hat 的 Supporter |

`/pit reload|damage|debug|backup|map`、Spire / KOTH 的位置、`/pitnpc`、`/pitspawn`、`/pitpermission` 仅限 op。

## 目录结构

```
src/main/java/com/pitremake/
├── PitPlugin.java          主类
├── combat/                 伤害计算、附魔效果
├── enchant/                附魔注册与说明
├── well/                   Mystic Well
├── gem/ special/           Totally Legit Gem、特殊物品
├── upgrade/ perk/          天赋、被动、超级连杀、连杀奖励
├── prestige/ renown/       转生、Renown Shop
├── shop/                   Items 村民
├── stats/ gold/ bounty/    击杀、XP、金币、悬赏、统计
├── event/                  自动活动时间表
├── spire/ koth/ auction/ rewards/ night/   各活动、昼夜
├── quest/ leaderboard/     Quest Master、排行榜
├── trade/ stash/           交易、Item Stash
├── rank/ permission/       头衔、聊天、权限
├── map/ spawn/ npc/ hologram/   地图、出生点、弹射器、NPC、全息文字
├── bot/                    机器人
└── data/                   玩家数据、备份
src/main/resources/
├── maps/                   地图 (region) 以及 NPC / 出生点 / 全息文字的位置
└── spire/ koth/ launcher/  活动和弹射器的位置
docs/DATA_STORAGE.md        数据存储位置与服务器运维 (日语)
```
