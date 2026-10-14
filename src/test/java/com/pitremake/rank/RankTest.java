package com.pitremake.rank;

import org.bukkit.ChatColor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RankTest {

    @Test
    void prefixesMatchRecordings() {
        // 録画: §a[VIP] bomb_trooper、§b[MVP§f+§b] Crypticz (白い +)
        assertEquals("§a[VIP]", Rank.VIP.prefix(ChatColor.RED, false));
        assertEquals("§b[MVP§f+§b]", Rank.MVP_PLUS.prefix(ChatColor.WHITE, false));
        assertEquals("", Rank.NONE.prefix(ChatColor.RED, false));
    }

    @Test
    void mvpPlusPlusCanBeGoldOrAqua() {
        assertEquals("§6[MVP§c++§6]", Rank.MVP_PLUS_PLUS.prefix(ChatColor.RED, false));
        assertEquals("§b[MVP§2++§b]", Rank.MVP_PLUS_PLUS.prefix(ChatColor.DARK_GREEN, true));
        assertEquals("§6", Rank.MVP_PLUS_PLUS.nameColor(false));
        assertEquals("§b", Rank.MVP_PLUS_PLUS.nameColor(true));
    }

    @Test
    void onlyNonToMvpPlusPlusAreForEveryone() {
        for (Rank rank : Rank.values()) {
            boolean player = rank.ordinal() <= Rank.MVP_PLUS_PLUS.ordinal();
            assertEquals(!player, rank.isAdminOnly(), rank.name());
        }
        assertTrue(Rank.MVP_PLUS.hasPlusColor());
        assertFalse(Rank.VIP_PLUS.hasPlusColor());
        assertEquals(14, RankManager.PLUS_COLORS.size());
    }
}
