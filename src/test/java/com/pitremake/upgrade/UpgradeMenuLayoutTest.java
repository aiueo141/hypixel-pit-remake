package com.pitremake.upgrade;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** "Choose a perk" の大きさ (録画: パーク 14 個で 36、28 個で 54)。 */
class UpgradeMenuLayoutTest {

    @Test
    void perkMenuGrowsWithPerks() {
        assertEquals(36, UpgradeMenu.perkMenuSize(14));
        assertEquals(54, UpgradeMenu.perkMenuSize(28));
        assertEquals(45, UpgradeMenu.perkMenuSize(15));
    }
}
