package com.pitremake.item;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RewardItemsTest {

    @Test
    void livesLineMatchesHypixel() {
        assertEquals("§7Lives: §a100§7/100", RewardItems.livesLine(100, 100));
        assertEquals("§7Lives: §a36§7/50", RewardItems.livesLine(36, 50));
    }

    /** 録画: Tier I Dark Pants の 1/5 は赤、5/10・10/100・16/50 は緑。3 以下で赤。 */
    @Test
    void lowLivesAreRed() {
        assertEquals("§7Lives: §c1§7/5", RewardItems.livesLine(1, 5));
        assertEquals("§7Lives: §c3§7/50", RewardItems.livesLine(3, 50));
        assertEquals("§7Lives: §a4§7/50", RewardItems.livesLine(4, 50));
        assertEquals("§7Lives: §a5§7/10", RewardItems.livesLine(5, 10));
    }

    @Test
    void wikiNumbers() {
        assertEquals(100, RewardItems.ARCHANGEL_LIVES);
        assertEquals(66, RewardItems.ARMAGEDDON_LIVES);
        assertEquals(50, RewardItems.GOLDEN_HELMET_LIVES);
        assertEquals(0.9, RewardItems.GUARDIAN_MULTIPLIER);
        assertEquals(0.10, RewardItems.ROYALTY_XP);
    }
}
