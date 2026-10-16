# Hypixel Pit Remake

[English](README.md) | **日本語** | [简体中文](README.zh-CN.md) | [한국어](README.ko.md)

Hypixel の The Pit を Minecraft 1.8.8 (PaperSpigot) で再現するサーバープラグインです。

## 必要なもの

- JDK 17 以上 (Gradle を動かす用)。Java 8 は Gradle ツールチェーンが自動でダウンロードします
- Minecraft 1.8.x のクライアント

## ビルドとテストサーバー

```powershell
.\gradlew.bat build       # プラグインをビルド (テストも実行) → build/libs/pit-<version>.jar
.\gradlew.bat runServer   # ビルドして Paper 1.8.8 のテストサーバーを run/ で起動
```

- テストサーバーには `localhost` で接続します
- コンソールで `op <名前>` を実行すると管理者になります。`stop` で終了します
- 自分のサーバーで使うときは、jar を Paper 1.8.8 (Java 8 で起動) の `plugins/` に入れるだけです

## マップ

マップは Seasons だけです。録画から作り直したものがプラグインの中 (`src/main/resources/maps/seasons/`) に入っています。

- サーバーを起動するたびに、ワールドを読む前にワールドの region をこのマップで上書きします。
  なので新しいサーバーでもプラグインを入れて起動するだけでマップができ、Spire や KOTH の途中で落ちて地形が変わっても次の起動で元に戻ります
- `/pit map save` で今の地形を保存すると (`plugins/Pit/map-template/`)、以降はそちらに戻します
- NPC・スポーン地点・ホログラムの位置は `maps/seasons.yml`、Spire / KOTH / Launcher の位置は `spire/` `koth/` `launcher/` に入っています。
  サーバー側 (`plugins/Pit/npcs.yml`、`config.yml` の `spawn.points` / `holograms.lines`) に設定があればそちらが優先されます

## 主な機能

- **戦闘**: ダメージ計算 (加算 / 乗算を切り替えられる。加算が本来の Pit の計算方法で、初期設定も加算)、Mystic エンチャント (剣・弓・パンツ・Rage / Dark / Sewer / Aqua)
- **Mystic**: Mystic Well (強化・Lives・生贄)、Hidden Jewel、Totally Legit Gem、Philosopher's Cactus、Funky Feather、Repair Kit
- **成長**: パーク・Passive・メガストリーク (Overdrive / Beastmode / Hermit / Highlander / Magnum Opus / To the Moon / Uberstreak)・キルストリーク
- **プレステージ**: Renown Shop (Upgrades / Perks / Shop Items / Killstreaks / Heresy / Fancy Hat)
- **ショップ**: Items の村人 (Non-permanent items)・Autobuy・Pants Bundle
- **イベント**: Spire・KOTH・Auction・2X REWARDS、昼夜と Night Quest (下の「イベント」)
- **その他**: Quest Master (Quests & Contracts)・リーダーボード・統計メニュー・Profile Viewer・トレード・Item Stash・ランクとチャット
- バウンティ・キル / アシストの報酬・金インゴット・Bot・バックアップ

## イベント

自動のイベントは時計に合わせて始まります (config の各イベントの `interval-minutes` ごと、毎時 `offset-minutes` 分から)。
初期設定では自動イベントはオフ (`enabled: false`) なので、`/events auto on` か config で有効にしてください。

初期の時間割 (重ならないようにしてあります):

| 分 | イベント |
| --- | --- |
| :00 | Spire の予告 (:03 開始、:08 終了) |
| :10 / :25 / :40 / :55 | Auction |
| :15 / :45 | KOTH |
| :20 / :50 | 2X REWARDS |

重なる設定にすると、起動時と `/pit reload` でログに警告が出ます。`/events` で次のイベントまでの時間を見られます。

## コマンド

### プレイヤー

| コマンド | 内容 |
| --- | --- |
| `/events` | 開催中のイベントと、次のイベントまでの時間 |
| `/trade <名前>` | トレードを申し込む |
| `/view <名前>` | Profile Viewer を開く |
| `/spawn` | スポーンに戻る |
| `/oof` | 死んでスポーンに戻る |
| `/cool` | 高プレステージのプレイヤー一覧 |
| `/pit version` `/pit nonce` | バージョン・手に持ったアイテムの nonce を表示 |

### 管理者

| コマンド | 内容 |
| --- | --- |
| `/pit reload` | 設定を読み直す (イベントの時間割も) |
| `/pit damage [additive\|multiplicative]` | ダメージの増減の重ね方を見る・変える |
| `/pit debug` | 自分が与える / 受けるダメージの計算をチャットに出す |
| `/pit backup` | ワールドと `plugins/Pit` を `backups/` に zip で保存 |
| `/pit map save` | 今の地形を起動時に戻す元として保存 |
| `/pit spire <start\|now\|end>` | Spire を予告から / すぐに始める・終わらせる |
| `/pit koth <start\|end>` | KOTH を始める・終わらせる |
| `/pit auction <start\|end>` | Auction を始める・終わらせる |
| `/pit multireward <X> start` / `end` | X 倍報酬のイベントを始める・終わらせる |
| `/pit time <day\|night>` | すぐに夜明け / 日暮れにする |
| `/pit <spire\|koth> <addpos <名前>\|removepos\|positions>` | Spire / KOTH を出す場所の管理 |
| `/events auto [spire\|koth\|auction\|multireward] [on\|off]` | 自動イベントのオン/オフ (名前なしで全部。開催中のものは最後まで続く) |
| `/fresh <sword\|bow\|pants 色\|gem\|cactus\|feather\|archangel\|armageddon\|goldenhelmet\|vile\|repairkit> [数]` | アイテムを出す |
| `/menchant <エンチャント> [レベル]` | 手に持った Mystic にエンチャントを付ける |
| `/fillmystic` | 空きスロットを手に持った Mystic のコピーで埋める |
| `/instantupgrade [ティア]` | インベントリの Mystic を Well なしで強化 |
| `/pitbot <spawn [数] [player\|spawn]\|remove\|auto [on\|off]\|count\|list>` | Bot を出す・消す・自動で湧くかのオン/オフ |
| `/pitnpc <spawn <upgrades\|prestige>\|remove\|list>` | NPC を置く・消す |
| `/pitspawn <add\|list\|clear\|area <半径> <最低y>\|tp>` | スポーン地点とスポーンの非 PvP 区域 |
| `/pitgold` `/pitlevel` `/pitprestige` `/pitrenown` `/pitbounty` `/pitstreak` | 数値を変える (テスト用) |
| `/pitpermission <add\|remove> <名前> <権限...\|all>` / `list [名前]` | op 以外のプレイヤーに権限を付ける・外す |

## 権限

管理者のコマンドは op なら全部使えます。op でないプレイヤーには `/pitpermission` で一部だけ許可できます (`plugins/Pit/permissions.yml` に保存)。

| 名前 | 使えるようになるもの |
| --- | --- |
| `renown` `prestige` `level` `gold` `streak` `bounty` | それぞれの `/pit...` コマンド |
| `fresh` `menchant` `fillmystic` `instantupgrade` `bot` | それぞれのコマンド |
| `event` | `/pit spire\|koth\|auction\|multireward\|time` の開始・終了と `/events auto` |
| `rank` | 管理者だけのランク |
| `supporter` | Fancy Hat の Supporter |

`/pit reload|damage|debug|backup|map`、Spire / KOTH の場所、`/pitnpc`、`/pitspawn`、`/pitpermission` は op だけが使えます。

## 構成

```
src/main/java/com/pitremake/
├── PitPlugin.java          メインクラス
├── combat/                 ダメージ計算・エンチャントの効果
├── enchant/                エンチャントの登録と説明文
├── well/                   Mystic Well
├── gem/ special/           Totally Legit Gem・特別なアイテム
├── upgrade/ perk/          パーク・Passive・メガストリーク・キルストリーク
├── prestige/ renown/       プレステージ・Renown Shop
├── shop/                   Items の村人
├── stats/ gold/ bounty/    キル・XP・ゴールド・バウンティ・統計
├── event/                  自動イベントの時間割
├── spire/ koth/ auction/ rewards/ night/   各イベント・昼夜
├── quest/ leaderboard/     Quest Master・リーダーボード
├── trade/ stash/           トレード・Item Stash
├── rank/ permission/       ランク・チャット・権限
├── map/ spawn/ npc/ hologram/   マップ・スポーン・Launcher・NPC・ホログラム
├── bot/                    Bot
└── data/                   プレイヤーごとのデータ・バックアップ
src/main/resources/
├── maps/                   マップ (region) と NPC・スポーン・ホログラムの位置
└── spire/ koth/ launcher/  イベントと Launcher の位置
docs/DATA_STORAGE.md        データの保存先とサーバー運用
```
