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
Pit のマップ (`run/world`) はリポジトリに含めていません。

## 主な機能

- 戦闘: ダメージ計算 (加算 / 乗算の切り替え)、Mystic エンチャント (剣・弓・パンツ・Rage / Dark / Sewer / Aqua)
- Mystic Well (強化・Lives・生贄)、Hidden Jewel、Totally Legit Gem、Philosopher's Cactus、Funky Feather
- パーク・Passive・メガストリーク (Overdrive / Beastmode / Hermit / Highlander / Magnum Opus / To the Moon / Uberstreak)・キルストリーク
- プレステージ・Renown Shop (Upgrades / Perks / Shop Items / Killstreaks / Heresy / Fancy Hat)
- Items の村人 (Non-permanent items)・Autobuy・Pants Bundle
- バウンティ・キル / アシストの報酬・金インゴット・Bot
- バックアップ (`/pit backup`)

## 管理用のコマンド

| コマンド | 内容 |
| --- | --- |
| `/pit <reload\|damage\|debug\|backup>` | 設定の再読み込み・ダメージの重ね方・ダメージ計算の表示・バックアップ |
| `/fresh <sword\|bow\|pants 色\|gem\|cactus\|feather\|vile\|…>` | アイテムを出す |
| `/menchant` | エンチャントを付ける |
| `/pitbot` | Bot を出す・消す |
| `/pitgold` `/pitlevel` `/pitprestige` `/pitrenown` `/pitbounty` `/pitstreak` | 数値を変える |

## 構成

```
src/main/java/com/pitremake/
├── PitPlugin.java      メインクラス
├── combat/             ダメージ計算・エンチャントの効果
├── enchant/            エンチャントの登録と説明文
├── well/               Mystic Well
├── upgrade/            パーク・Passive・メガストリーク・キルストリーク
├── prestige/ renown/   プレステージ・Renown Shop
├── shop/               Items の村人
├── stats/ gold/ bounty/ キル・XP・ゴールド・バウンティ
├── bot/                Bot
└── data/               プレイヤーごとのデータ・バックアップ
docs/DATA_STORAGE.md    データの保存先とサーバー運用
```
