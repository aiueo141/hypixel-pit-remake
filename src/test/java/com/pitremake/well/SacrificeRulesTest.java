package com.pitremake.well;

import com.pitremake.item.MysticData;
import com.pitremake.item.MysticItems;
import com.pitremake.item.PantsColor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SacrificeRulesTest {

    @Test
    void onlyTierThreeNeedsSacrifice() {
        assertFalse(SacrificeRules.isRequired(1));
        assertFalse(SacrificeRules.isRequired(2));
        assertTrue(SacrificeRules.isRequired(3));
    }

    @Test
    void requiredColorFollowsNonceModFive() {
        PantsColor[] expected = {PantsColor.RED, PantsColor.YELLOW, PantsColor.BLUE, PantsColor.ORANGE, PantsColor.GREEN};
        for (int nonce = 1_000_000; nonce < 1_000_010; nonce++) {
            assertEquals(expected[nonce % 5], SacrificeRules.requiredColor(data(nonce, false)), "nonce " + nonce);
        }
    }

    @Test
    void ragePantsNeedRagePants() {
        assertEquals(PantsColor.RAGE, SacrificeRules.requiredColor(data(MysticItems.RAGE_PANTS_NONCE, true)));
    }

    @Test
    void nonceIndexMatchesFromNonce() {
        for (PantsColor color : PantsColor.values()) {
            if (!PantsColor.standardColors().contains(color)) {
                // 特殊なパンツは Nonce 固定なので色の順番には入らない
                assertEquals(-1, color.nonceIndex());
                continue;
            }
            // Fresh Pants は Nonce % 5 == nonceIndex になるので、生贄に必要な色はそのパンツと同じ色
            assertEquals(color, PantsColor.fromNonce(4_567_890 + color.nonceIndex()));
        }
    }

    private static MysticData data(int nonce, boolean rage) {
        MysticData data = new MysticData();
        data.setNonce(nonce);
        data.setTier(2);
        data.setRagePants(rage);
        return data;
    }
}
