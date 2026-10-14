# データの保存先とサーバー運用

Pit Remake を将来ちゃんとしたサーバーとして動かすときの、データの置き場所・バックアップ・マップの扱いのまとめ。

## 1. 今どこに何があるか

すべてサーバーのフォルダ (`run/`) の中。

| 場所 | 中身 | 単位 |
|---|---|---|
| `world/` | Pit のマップ (地形)。録画から作り直したもの | ワールド |
| `plugins/Pit/config.yml` | 設定 (マップ・スポーン・Web・バックアップなど) | サーバー |
| `plugins/Pit/stats.yml` | XP・プレステージ・Renown・キル数 | 全員で 1 ファイル |
| `plugins/Pit/gold.yml` | ゴールド・稼いだゴールド | 全員で 1 ファイル |
| `plugins/Pit/upgrades.yml` | パーク・Passive・メガストリーク・キルストリーク・Renown の解放・Fancy Hat など | 全員で 1 ファイル |
| `plugins/Pit/ranks.yml` | ランク | 全員で 1 ファイル |
| `plugins/Pit/well.yml` | Mystic Well に入っているアイテム | 全員で 1 ファイル |
| `plugins/Pit/enderchest.yml` | エンダーチェストの増えた行 (最初の 3 行はバニラのプレイヤーデータ) | 全員で 1 ファイル |
| `plugins/Pit/players/<UUID>.yml` | **新しい** プレイヤーごとのファイル: デス・アシスト・最高ストリーク・プレイ時間・持ち物とエンダーチェストの写し | 1 人 1 ファイル |
| `plugins/Pit/npcs.yml` | NPC の位置 | サーバー |
| `plugins/Pit/skins.yml` | Bot のスキン (MineSkin から) | サーバー |
| `plugins/Pit/placed-blocks.yml` | 置かれた黒曜石などの位置 (落ちても次の起動で消すため) | サーバー |
| `world/playerdata/` | バニラのプレイヤーデータ (持ち物・エンダーチェストの 3 行・位置) | 1 人 1 ファイル |
| `backups/pit-日付-時刻.zip` | バックアップ (下の 3) | - |

## 2. これからの方針

### 2-1. プレイヤーのデータは「1 人 1 ファイル」にまとめる

今は `stats.yml` などが「全員で 1 ファイル」で、保存のたびに全員分を書き直す。
人数が増えると保存が重くなり、途中で落ちると全員分が壊れる危険がある。

- 新しく作った `players/<UUID>.yml` (`PlayerProfiles`) と同じ形に、ゴールド・XP・アップグレードなども順番に移す
- 保存は「変わった人だけ」「ログアウト時と数分ごと」にする
- 移すときは起動時に古いファイルを読んで新しい形で書き、古いファイルは `*.old` に名前を変えて残す

### 2-2. 本番では DB (MySQL / MariaDB) を使えるようにする

- 保存の処理を `PlayerStorage` のようなインターフェースにして、「YAML ファイル」と「DB」を config で切り替えられるようにする
- 1 台で遊ぶうちは YAML (今まで通り)、本番や複数のサーバー (ロビー + Pit など) では DB

### 2-3. UUID について

- 本番は **online-mode=true** にする。offline-mode だと UUID が名前から作られ、名前を変えたり正規の UUID と混ざったりしてデータが分かれる
- データはすべて UUID で持ち、名前は表示用

## 3. バックアップ

- `/pit backup` (権限 `pit.admin`) で、ワールド (地形・置かれたブロック) と `plugins/Pit` を `backups/pit-日付-時刻.zip` に保存する
- 自動: `config.yml` の `backup.interval-minutes` ごと (初期値 60 分、0 で無し)。古いものは `backup.keep` 個 (初期値 10) だけ残す
- 保存の前に Pit のデータを全部ファイルに書き、ワールドを保存してから、別スレッドで zip にする (遊んでいる間も止まらない)

### 戻し方

1. サーバーを止める
2. zip を展開する
3. `worlds/world` を `run/world` に、`plugins/Pit` を `run/plugins/Pit` に置き換える
4. サーバーを起動する

## 4. マップ (地形)

- Pit の地形は `run/world` (録画から作り直した Seasons)。**マップの「元の形」を別に取っておく** のがおすすめ:
  - `maps/seasons/` に今のきれいな `world` をコピーしておく (置かれたブロックなどが無い状態)
  - 壊れたり汚れたりしたら、サーバーを止めてそこからコピーし直す
- 将来マップを切り替える (Abyss / Genesis / Castle など、`PitMapType`) ときも、`maps/<名前>/` に 1 つずつ置いておき、
  起動時に config の `map` のものを `world` にコピーする形にできる
- Pit のマップは遊んでも地形が変わらない (置いたブロックは消える) ので、普段は `world` をそのまま使ってよい
