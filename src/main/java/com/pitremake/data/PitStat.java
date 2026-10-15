package com.pitremake.data;

/**
 * My pit stats (録画 2026-10-15_10-32-13.mcrc) の数えるだけの記録。plugins/Pit/players/&lt;UUID&gt;.yml の counters に保存する。
 * キル・デス・アシスト・最高ストリーク・XP・プレイ時間・稼いだゴールド・Night Quest は別に記録している。
 */
public enum PitStat {
    SWORD_HITS("sword-hits"),
    ARROWS_SHOT("arrows-shot"),
    ARROW_HITS("arrow-hits"),
    MELEE_DAMAGE_DEALT("melee-damage-dealt"),
    BOW_DAMAGE_DEALT("bow-damage-dealt"),
    DAMAGE_TAKEN("damage-taken"),
    MELEE_DAMAGE_TAKEN("melee-damage-taken"),
    BOW_DAMAGE_TAKEN("bow-damage-taken"),
    LEFT_CLICKS("left-clicks"),
    DIAMOND_ITEMS_PURCHASED("diamond-items-purchased"),
    CHAT_MESSAGES("chat-messages"),
    BLOCKS_PLACED("blocks-placed"),
    BLOCKS_BROKEN("blocks-broken"),
    JUMPS_INTO_PIT("jumps-into-pit"),
    LAUNCHER_LAUNCHES("launcher-launches"),
    GOLDEN_APPLES_EATEN("golden-apples-eaten"),
    GOLDEN_HEADS_EATEN("golden-heads-eaten"),
    LAVA_BUCKETS_EMPTIED("lava-buckets-emptied"),
    FISHING_RODS_LAUNCHED("fishing-rods-launched");

    private final String key;

    PitStat(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
