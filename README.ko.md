# Hypixel Pit Remake

[English](README.md) | [日本語](README.ja.md) | [简体中文](README.zh-CN.md) | **한국어**

Hypixel의 The Pit을 Minecraft 1.8.8 (PaperSpigot)에서 재현하는 서버 플러그인입니다.

## 필요한 것

- JDK 17 이상 (Gradle 실행용). Java 8은 Gradle 툴체인이 자동으로 다운로드합니다
- Minecraft 1.8.x 클라이언트

## 빌드와 테스트 서버

```powershell
.\gradlew.bat build       # 플러그인 빌드 (테스트도 실행) → build/libs/pit-<version>.jar
.\gradlew.bat runServer   # 빌드한 뒤 run/ 에서 Paper 1.8.8 테스트 서버 실행
```

- 테스트 서버에는 `localhost`로 접속합니다
- 콘솔에서 `op <이름>`을 실행하면 관리자가 됩니다. `stop`으로 종료합니다
- 직접 운영하는 서버에서 쓸 때는 jar를 Paper 1.8.8 (Java 8로 실행)의 `plugins/`에 넣기만 하면 됩니다

## 맵

맵은 Seasons만 있습니다. 녹화를 바탕으로 다시 만든 맵이 플러그인 안 (`src/main/resources/maps/seasons/`)에 들어 있습니다.

- 서버를 시작할 때마다 월드를 불러오기 전에 월드의 region 파일을 이 맵으로 덮어씁니다.
  그래서 새 서버도 플러그인을 넣고 시작하기만 하면 맵이 생기고, Spire나 KOTH 도중 서버가 꺼져 지형이 바뀌어도 다음 시작 때 원래대로 돌아갑니다
- `/pit map save`로 현재 지형을 저장하면 (`plugins/Pit/map-template/`) 이후에는 그 지형으로 되돌립니다
- NPC, 스폰 지점, 홀로그램 위치는 `maps/seasons.yml`, Spire / KOTH / 런처 위치는 `spire/` `koth/` `launcher/`에 있습니다.
  서버 쪽 설정 (`plugins/Pit/npcs.yml`, `config.yml`의 `spawn.points` / `holograms.lines`)이 있으면 그쪽이 우선됩니다

## 주요 기능

- **전투**: 데미지 계산 (합연산 / 곱연산 전환 가능. 합연산이 원래 Pit의 계산 방식이며 기본값도 합연산), Mystic 인챈트 (검, 활, 바지, Rage / Dark / Sewer / Aqua)
- **Mystic**: Mystic Well (강화, Lives, 희생), Hidden Jewel, Totally Legit Gem, Philosopher's Cactus, Funky Feather, Repair Kit
- **성장**: 퍽, 패시브, 메가스트릭 (Overdrive / Beastmode / Hermit / Highlander / Magnum Opus / To the Moon / Uberstreak), 킬스트릭
- **프레스티지**: Renown Shop (Upgrades / Perks / Shop Items / Killstreaks / Heresy / Fancy Hat)
- **상점**: Items 주민 (비영구 아이템), Autobuy, Pants Bundle
- **이벤트**: Spire, KOTH, Auction, 2X REWARDS, 낮과 밤, Night Quest (아래 "이벤트" 참고)
- **기타**: Quest Master (Quests & Contracts), 리더보드, 통계 메뉴, Profile Viewer, 거래, Item Stash, 랭크와 채팅
- 현상금, 킬 / 어시스트 보상, 금괴, 봇, 백업

## 이벤트

자동 이벤트는 시계에 맞춰 시작됩니다 (config의 각 이벤트 `interval-minutes` 간격으로, 매시 `offset-minutes`분부터).
자동 이벤트는 기본값이 꺼짐 (`enabled: false`)이므로 `/events auto on` 또는 config에서 켜 주세요.

기본 시간표 (서로 겹치지 않음):

| 분 | 이벤트 |
| --- | --- |
| :00 | Spire 예고 (:03 시작, :08 종료) |
| :10 / :25 / :40 / :55 | Auction |
| :15 / :45 | KOTH |
| :20 / :50 | 2X REWARDS |

설정이 겹치면 시작할 때와 `/pit reload` 때 로그에 경고가 나옵니다. `/events`로 다음 이벤트까지 남은 시간을 볼 수 있습니다.

## 명령어

### 플레이어

| 명령어 | 내용 |
| --- | --- |
| `/events` | 진행 중인 이벤트와 다음 이벤트까지 남은 시간 |
| `/trade <이름>` | 거래 요청 보내기 |
| `/view <이름>` | Profile Viewer 열기 |
| `/spawn` | 스폰으로 돌아가기 |
| `/oof` | 죽고 스폰으로 돌아가기 |
| `/cool` | 높은 프레스티지 플레이어 목록 |
| `/pit version` `/pit nonce` | 버전 / 손에 든 아이템의 nonce 표시 |

### 관리자

| 명령어 | 내용 |
| --- | --- |
| `/pit reload` | 설정 다시 불러오기 (이벤트 시간표 포함) |
| `/pit damage [additive\|multiplicative]` | 데미지 증감의 중첩 방식 보기 / 바꾸기 |
| `/pit debug` | 주는 / 받는 데미지 계산을 채팅에 표시 |
| `/pit backup` | 월드와 `plugins/Pit`을 `backups/`에 zip으로 저장 |
| `/pit map save` | 현재 지형을 시작 시 되돌릴 지형으로 저장 |
| `/pit spire <start\|now\|end>` | Spire를 예고부터 / 바로 시작, 종료 |
| `/pit koth <start\|end>` | KOTH 시작 / 종료 |
| `/pit auction <start\|end>` | Auction 시작 / 종료 |
| `/pit multireward <X> start` / `end` | X배 보상 이벤트 시작 / 종료 |
| `/pit time <day\|night>` | 바로 새벽 / 해질녘 시작 |
| `/pit <spire\|koth> <addpos <이름>\|removepos\|positions>` | Spire / KOTH가 나오는 위치 관리 |
| `/events auto [spire\|koth\|auction\|multireward] [on\|off]` | 자동 이벤트 켜기 / 끄기 (이름 없으면 전부. 진행 중인 이벤트는 끝까지 계속) |
| `/fresh <sword\|bow\|pants 색\|gem\|cactus\|feather\|archangel\|armageddon\|goldenhelmet\|vile\|repairkit> [개수]` | 아이템 받기 |
| `/menchant <인챈트> [레벨]` | 손에 든 Mystic에 인챈트 추가 |
| `/fillmystic` | 빈 슬롯을 손에 든 Mystic의 복사본으로 채우기 |
| `/instantupgrade [티어]` | Well 없이 인벤토리의 Mystic 강화 |
| `/pitbot <spawn [개수] [player\|spawn]\|remove\|auto [on\|off]\|count\|list>` | 봇 소환 / 제거, 자동 소환 켜기 / 끄기 |
| `/pitnpc <spawn <upgrades\|prestige>\|remove\|list>` | NPC 배치 / 제거 |
| `/pitspawn <add\|list\|clear\|area <반경> <최저y>\|tp>` | 스폰 지점과 스폰의 PvP 금지 구역 |
| `/pitgold` `/pitlevel` `/pitprestige` `/pitrenown` `/pitbounty` `/pitstreak` | 수치 변경 (테스트용) |
| `/pitpermission <add\|remove> <이름> <권한...\|all>` / `list [이름]` | op가 아닌 플레이어에게 권한 부여 / 제거 |

## 권한

op는 모든 관리자 명령어를 쓸 수 있습니다. op가 아닌 플레이어에게는 `/pitpermission`으로 일부만 허용할 수 있습니다 (`plugins/Pit/permissions.yml`에 저장).

| 이름 | 쓸 수 있게 되는 것 |
| --- | --- |
| `renown` `prestige` `level` `gold` `streak` `bounty` | 해당 `/pit...` 명령어 |
| `fresh` `menchant` `fillmystic` `instantupgrade` `bot` | 해당 명령어 |
| `event` | `/pit spire\|koth\|auction\|multireward\|time`의 시작 / 종료와 `/events auto` |
| `rank` | 관리자 전용 랭크 |
| `supporter` | Fancy Hat의 Supporter |

`/pit reload|damage|debug|backup|map`, Spire / KOTH 위치, `/pitnpc`, `/pitspawn`, `/pitpermission`은 op만 쓸 수 있습니다.

## 구성

```
src/main/java/com/pitremake/
├── PitPlugin.java          메인 클래스
├── combat/                 데미지 계산, 인챈트 효과
├── enchant/                인챈트 등록과 설명
├── well/                   Mystic Well
├── gem/ special/           Totally Legit Gem, 특수 아이템
├── upgrade/ perk/          퍽, 패시브, 메가스트릭, 킬스트릭
├── prestige/ renown/       프레스티지, Renown Shop
├── shop/                   Items 주민
├── stats/ gold/ bounty/    킬, XP, 골드, 현상금, 통계
├── event/                  자동 이벤트 시간표
├── spire/ koth/ auction/ rewards/ night/   각 이벤트, 낮과 밤
├── quest/ leaderboard/     Quest Master, 리더보드
├── trade/ stash/           거래, Item Stash
├── rank/ permission/       랭크, 채팅, 권한
├── map/ spawn/ npc/ hologram/   맵, 스폰, 런처, NPC, 홀로그램
├── bot/                    봇
└── data/                   플레이어별 데이터, 백업
src/main/resources/
├── maps/                   맵 (region)과 NPC / 스폰 / 홀로그램 위치
└── spire/ koth/ launcher/  이벤트와 런처 위치
```

## 미구현 인챈트

다음 인챈트는 존재하지만 (아이템에 붙고 설명도 나옵니다) 아직 효과가 없습니다.

| 종류 | 인챈트 |
| --- | --- |
| Sword | Combo: XP |
| Bow | Push comes to shove, What doesn't kill you |
| Pants | Negotiator, Steaks, Assassin, Martyrdom, Retro-Gravity Microcosm, Snowballs, Wolf Pack |
| Rage | Really Toxic, Think of the People |
| Dark | Hedge Fund, Lycanthropy, Golden Handcuffs, Heartripper |
| Sewer | Aegis, Worm, Trash Panda |
| Aqua | Club Rod, Grandmaster, Luck of the Pond, Portable Pond, Rodback, Rogue, Tough Crew, Trophy, Unite, Stereo |
