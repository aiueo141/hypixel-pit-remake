package com.pitremake.stats;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** pit.wiki の XP / Gold のページの計算。 */
class KillRewardsTest {

    /** 同じレベル・同じ防具、ストリーク無しの普通のキル (4 キル目以降)。 */
    private static KillRewards.Kill plain(int streak) {
        return new KillRewards.Kill(50, 50, streak, 0, 8, 8, 0, null);
    }

    @Test
    void baseRewards() {
        assertEquals(5, KillRewards.xp(plain(4)) - KillRewards.streakXp(4));
        assertEquals(10.0, KillRewards.gold(plain(4)));
    }

    @Test
    void flatFirstThenEachSourceMultiplies() {
        // ユーザーの例: 剣 Gold Boost II / Moctezuma III、パンツ Gold Boost II / Gold Bump II / Moctezuma III、
        // Highlander (+110%)、Passive Gold Boost V (+50%)、Renown Gold Boost X (+25%)、Celebrity (×2)
        Map<String, Integer> sword = new HashMap<>();
        sword.put("gold_boost", 2);
        sword.put("gold_strictly_kills", 3);
        Map<String, Integer> pants = new HashMap<>();
        pants.put("gold_boost", 2);
        pants.put("gold_per_kill", 2);
        pants.put("gold_strictly_kills", 3);
        KillRewards.Boosts boosts = new KillRewards.Boosts();
        boosts.addGold(1.10);
        boosts.addGold(0.50);
        boosts.addGold(0.25);
        KillRewards.Kill kill = new KillRewards.Kill(100, 100, 60, 0, 8, 8, 0, Arrays.asList(sword, pants)).with(boosts);
        // (10 + 18 + 18 + 8) × 1.6 (2 つの Gold Boost は足し算) × 2.1 × 1.5 × 1.25
        assertEquals(340.2, KillRewards.gold(kill), 1e-9);
        // Celebrity は最後に文字通り 2 倍
        boosts.goldFinalMultiplier = 2;
        assertEquals(680.4, KillRewards.gold(kill), 1e-9);
        // ダイヤ防具の相手 (防具差 +16) なら 900g 近く
        KillRewards.Kill diamondVictim = new KillRewards.Kill(100, 100, 60, 0, 0, 16, 4, Arrays.asList(sword, pants)).with(boosts);
        assertEquals(882.0, KillRewards.gold(diamondVictim), 1e-9);
    }

    @Test
    void firstThreeKillsGetBonus() {
        assertEquals(9, KillRewards.xp(plain(1)));
        assertEquals(14.0, KillRewards.gold(plain(1)));
        assertEquals(9 + 3, KillRewards.xp(plain(3)));
    }

    @Test
    void higherLevelVictimGivesXp() {
        // 5 レベル上ごとに +1 (Lv50 → Lv74 は +4)
        assertEquals(9 + 4, KillRewards.xp(new KillRewards.Kill(50, 74, 1, 0, 8, 8, 0, null)));
        // 相手が下でも減らない
        assertEquals(9, KillRewards.xp(new KillRewards.Kill(100, 50, 1, 0, 8, 8, 0, null)));
    }

    @Test
    void noobVictimGivesTenPercentLess() {
        KillRewards.Kill kill = new KillRewards.Kill(20, 20, 6, 0, 8, 8, 0, null);
        assertEquals(Math.round((5 + 5) * 0.9), KillRewards.xp(kill));
        assertEquals(9.0, KillRewards.gold(kill));
    }

    @Test
    void streakBonus() {
        assertEquals(0, KillRewards.streakXp(2));
        assertEquals(3, KillRewards.streakXp(3));
        assertEquals(5, KillRewards.streakXp(5));
        assertEquals(5, KillRewards.streakXp(19));
        assertEquals(6, KillRewards.streakXp(20));
        assertEquals(30, KillRewards.streakXp(100));
        assertEquals(30, KillRewards.streakXp(500));
    }

    @Test
    void shutdownBonus() {
        assertEquals(0, KillRewards.shutdownXp(2));
        assertEquals(15, KillRewards.shutdownXp(4));
        assertEquals(25, KillRewards.shutdownXp(10));
        assertEquals(30, KillRewards.shutdownXp(20));
        assertEquals(300, KillRewards.shutdownXp(1000));
        assertEquals(0.0, KillRewards.shutdownGold(2));
        assertEquals(20.0, KillRewards.shutdownGold(10));
        assertEquals(80.0, KillRewards.shutdownGold(100));
    }

    @Test
    void armorDifferenceGivesGold() {
        // 鉄フル (8) でダイヤフル (16) をキル → +8g
        assertEquals(10 + 8.0, KillRewards.gold(new KillRewards.Kill(50, 50, 4, 0, 8, 16, 4, null)));
        // 自分の方が強い防具なら 0
        assertEquals(0, KillRewards.armorBonus(16, 8));
        assertEquals(16, KillRewards.armorBonus(0, 16));
    }

    @Test
    void enchantsOnSwordAndPantsStack() {
        Map<String, Integer> sword = new HashMap<>();
        sword.put("gold_boost", 3);
        sword.put("xp_per_kill", 3);
        Map<String, Integer> pants = new HashMap<>();
        pants.put("gold_per_kill", 3);
        pants.put("xp_boost", 3);
        List<Map<String, Integer>> items = Arrays.asList(sword, pants);
        KillRewards.Kill kill = new KillRewards.Kill(50, 50, 4, 0, 8, 8, 0, items);
        // (5 + 3 + 6) × 1.3
        assertEquals(Math.round((5 + 3 + 6) * 1.3), KillRewards.xp(kill));
        // (10 + 12) × 1.45
        assertEquals(31.9, KillRewards.gold(kill), 1e-9);
    }

    @Test
    void sierraPaysPerDiamondPiece() {
        List<Map<String, Integer>> items = Collections.singletonList(Collections.singletonMap("gold_per_diamond_piece", 1));
        assertEquals(10 + 2 * 30.0, KillRewards.gold(new KillRewards.Kill(50, 50, 4, 0, 16, 16, 2, items)));
    }

    @Test
    void bigShutdown() {
        // 5 + レベル差 119/5=23 + ストリーク 100 で +30 + シャットダウン最大 300
        assertEquals(358, KillRewards.xp(new KillRewards.Kill(1, 120, 100, 1000, 0, 0, 0, null)));
        // Sweaty III でストリークボーナス +60%
        List<Map<String, Integer>> sweaty = Collections.singletonList(Collections.singletonMap("streak_xp", 3));
        assertEquals(376, KillRewards.xp(new KillRewards.Kill(1, 120, 100, 1000, 0, 0, 0, sweaty)));
    }
}
