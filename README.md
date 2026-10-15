# Hypixel Pit Remake

Hypixel の The Pit を Minecraft 1.8.8 (PaperSpigot) で再現するプラグイン。
数値・文言・音・GUI は録画 (リプレイ) と pit.wiki をもとにしています。

## 必要なもの

- JDK 17 以上 (Gradle 実行用)
- Java 8 は Gradle ツールチェーンが自動でダウンロードします
- 1.8.x のクライアント

## コマンド

| コマンド | 内容 |
| --- | --- |
| `.\gradlew.bat build` | プラグインをビルド (テストも実行) → `build/libs/pit-<version>.jar` |
| `.\gradlew.bat runServer` | ビルドしてテストサーバー (Paper 1.8.8) を `run/` で起動 |

テストサーバーには `localhost` に接続します。コンソールで `op <名前>` を実行すると管理者になります。`stop` で終了します。

## マップ

Pit のマップ (Seasons、録画から作ったもの) はプラグインの中 (`src/main/resources/maps/seasons/`) に入っています。
サーバーを起動するたびに、ワールドを読む前にワールドの region をこの元で上書きするので、新しいサーバーにプラグインを入れて起動するだけでマップができます。
Spire や KOTH の途中で落ちて地形が変わっても、次の起動で元に戻ります。

- `/pit map save` で今の地形を元として保存すると (`plugins/Pit/map-template/`)、以降はそちらを使います
- NPC・スポーン地点・ホログラムの位置は `maps/seasons.yml`、Spire / KOTH / Launcher の位置は `spire/` `koth/` `launcher/` にあります
- サーバー側の設定 (`plugins/Pit/npcs.yml`、`config.yml` の `spawn.points` / `holograms.lines`) があればそちらが優先されます

## 主な機能

- 戦闘: ダメージ計算 (加算 / 乗算の切り替え)、Mystic エンチャント (剣・弓・パンツ・Rage / Dark / Sewer / Aqua)
- Mystic Well (強化・Lives・生贄)、Hidden Jewel、Totally Legit Gem、Philosopher's Cactus、Funky Feather、Repair Kit
- パーク・Passive・メガストリーク (Overdrive / Beastmode / Hermit / Highlander / Magnum Opus / To the Moon / Uberstreak)・キルストリーク
- プレステージ・Renown Shop (Upgrades / Perks / Shop Items / Killstreaks / Heresy / Fancy Hat)
- Items の村人 (Non-permanent items)・Autobuy・Pants Bundle
- イベント: Spire・KOTH・Auction・2X REWARDS (Minor Event)、昼夜と Night Quest
- Quest Master (Quests & Contracts)・リーダーボード・統計メニュー・Profile Viewer (`/view`)
- トレード (`/trade`)・Item Stash・ランクとチャット
- バウンティ・キル / アシストの報酬・金インゴット・Bot
- バックアップ (`/pit backup`)

## プレイヤー用のコマンド

| コマンド | 内容 |
| --- | --- |
| `/trade <名前>` | トレードを申し込む |
| `/view <名前>` | Profile Viewer を開く |
| `/spawn` | スポーンに戻る |
| `/oof` | 死んでスポーンに戻る |
| `/cool` | 高プレステージのプレイヤー一覧 |
| `/pit version` `/pit nonce` | バージョン・手に持ったアイテムの nonce を表示 |

## 管理用のコマンド

| コマンド | 内容 |
| --- | --- |
| `/pit <reload\|damage\|debug\|backup>` | 設定の再読み込み・ダメージの重ね方・ダメージ計算の表示・バックアップ |
| `/pit spire <start\|now\|end\|addpos\|removepos\|positions>` | Spire を始める・終わらせる・出す場所の管理 |
| `/pit koth <start\|end\|addpos\|removepos\|positions>` | KOTH を始める・終わらせる・出す場所の管理 |
| `/pit auction <start\|end>` | オークションを始める・終わらせる |
| `/pit multireward <X> start` / `/pit multireward end` | X 倍報酬の Minor Event を始める・終わらせる |
| `/pit time <day\|night>` | すぐに夜明け / 日暮れにする |
| `/pit map save` | 今の地形を起動時に戻す元として保存 |
| `/fresh <sword\|bow\|pants 色\|gem\|cactus\|feather\|archangel\|armageddon\|goldenhelmet\|vile\|repairkit> [数]` | アイテムを出す |
| `/menchant <エンチャント> [レベル]` | 手に持った Mystic にエンチャントを付ける |
| `/fillmystic` | 空きスロットを手に持った Mystic のコピーで埋める |
| `/instantupgrade [ティア]` | インベントリの Mystic を Well なしで強化 |
| `/pitbot <spawn [数] [player\|spawn]\|remove\|count\|list>` | Bot を出す・消す |
| `/pitnpc <spawn <upgrades\|prestige>\|remove\|list>` | NPC を置く・消す |
| `/pitspawn <add\|list\|clear\|area <半径> <最低y>\|tp>` | スポーン地点とスポーンの非 PvP 区域 |
| `/pitgold` `/pitlevel` `/pitprestige` `/pitrenown` `/pitbounty` `/pitstreak` | 数値を変える |

## 構成

```
src/main/java/com/pitremake/
├── PitPlugin.java      メインクラス
├── combat/             ダメージ計算・エンチャントの効果
├── enchant/            エンチャントの登録と説明文
├── well/               Mystic Well
├── gem/ special/       Totally Legit Gem・特別なアイテム
├── upgrade/ perk/      パーク・Passive・メガストリーク・キルストリーク
├── prestige/ renown/   プレステージ・Renown Shop
├── shop/               Items の村人
├── stats/ gold/ bounty/ キル・XP・ゴールド・バウンティ・統計
├── spire/ koth/ auction/ rewards/ night/  イベント・昼夜
├── quest/ leaderboard/ Quest Master・リーダーボード
├── trade/ stash/       トレード・Item Stash
├── rank/               ランク・チャット
├── map/ spawn/ npc/ hologram/  マップ・スポーン・Launcher・NPC・ホログラム
├── bot/                Bot
└── data/               プレイヤーごとのデータ・バックアップ
src/main/resources/
├── maps/               マップ (region) と NPC・スポーン・ホログラムの位置
└── spire/ koth/ launcher/  イベントと Launcher の位置
docs/DATA_STORAGE.md    データの保存先とサーバー運用
```
