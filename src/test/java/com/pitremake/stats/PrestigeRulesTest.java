package com.pitremake.stats;

import com.pitremake.util.RomanNumerals;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** pit.wiki の Prestige / Level と、録画のチャット・tab の表示。 */
class PrestigeRulesTest {

    @Test
    void xpPerLevelIsMultipliedAndRoundedUp() {
        // wiki の例: プレステージ XV の Lv102 → 103 は 9 × 1,200 = 10,800
        assertEquals(10_800, PitLevels.xpToNext(102, 15));
        assertEquals(17, PitLevels.xpToNext(1, 1));
    }

    /** wiki の表は最初のレベル帯を 10 段で数えている (ここでは Lv1 から始まるので 9 段)。1 段分を足すと一致する。 */
    @Test
    void totalXpMatchesWikiTable() {
        assertEquals(65_950, PitLevels.totalXpFor(120, 0) + PitLevels.xpToNext(1, 0));
        assertEquals(72_560, PitLevels.totalXpFor(120, 1) + PitLevels.xpToNext(1, 1));
        assertEquals(98_940, PitLevels.totalXpFor(120, 5) + PitLevels.xpToNext(1, 5));
        assertEquals(263_800, PitLevels.totalXpFor(120, 10) + PitLevels.xpToNext(1, 10));
        long total = PitLevels.totalXpFor(120, 1);
        assertEquals(120, PitLevels.levelOf(total, 1));
        assertEquals(119, PitLevels.levelOf(total - 1, 1));
    }

    @Test
    void goldRequirements() {
        assertEquals(10_000, PrestigeRules.goldRequired(0));
        assertEquals(150_000, PrestigeRules.goldRequired(15));
        assertEquals(2_000_000, PrestigeRules.goldRequired(49));
        assertEquals(-1, PrestigeRules.goldRequired(50));
    }

    @Test
    void bracketColorsChangeEveryFivePrestiges() {
        assertEquals("§7", PrestigeRules.bracketColor(0));
        assertEquals("§9", PrestigeRules.bracketColor(1));
        assertEquals("§9", PrestigeRules.bracketColor(4));
        assertEquals("§e", PrestigeRules.bracketColor(5));
        assertEquals("§6", PrestigeRules.bracketColor(10));
        assertEquals("§c", PrestigeRules.bracketColor(17));
        assertEquals("§5", PrestigeRules.bracketColor(20));
        assertEquals("§b", PrestigeRules.bracketColor(35));
        assertEquals("§1", PrestigeRules.bracketColor(44));
        assertEquals("§0", PrestigeRules.bracketColor(47));
        assertEquals("§4", PrestigeRules.bracketColor(49));
        assertEquals("§8", PrestigeRules.bracketColor(50));
    }

    @Test
    void tagsMatchRecordings() {
        // チャット: §9[§e§eI§9-§6§l67§9] Tidelit、§c[§e§eXVII§c-§6§l61§c] lvec
        assertEquals("§9[§e§eI§9-§6§l67§9]", PrestigeRules.chatTag(1, 67));
        assertEquals("§c[§e§eXVII§c-§6§l61§c]", PrestigeRules.chatTag(17, 61));
        assertEquals("§7[§71§7]", PrestigeRules.chatTag(0, 1));
        // tab: §6[§f§l112§6] (プレステージ X〜XIV)
        assertEquals("§6[§f§l112§6]", PrestigeRules.levelTag(12, 112));
    }

    @Test
    void extraXpText() {
        // 録画: "§b+1300% §7needed xp than normal!" (XVIII = 14 倍)、"+10%" (I)
        assertEquals("+1300%", PrestigeRules.extraXpPercent(18));
        assertEquals("+10%", PrestigeRules.extraXpPercent(1));
    }

    @Test
    void romanNumeralsUpToFifty() {
        assertEquals("XVIII", RomanNumerals.of(18));
        assertEquals("XXXVIII", RomanNumerals.of(38));
        assertEquals("XLIX", RomanNumerals.of(49));
        assertEquals("L", RomanNumerals.of(50));
    }
}
