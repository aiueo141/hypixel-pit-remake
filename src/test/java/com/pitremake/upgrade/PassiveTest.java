package com.pitremake.upgrade;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Passive の説明は Tier の合計で出る (Gold Boost なら +10 / +20 / … / +60%)。 */
class PassiveTest {

    @Test
    void descriptionScalesWithTier() {
        assertEquals(Arrays.asList("§7Earn §6+10% gold (g) §7from kills and", "§7coin pickups."), Passive.GOLD_BOOST.getDescription());
        assertEquals("§7Earn §6+50% gold (g) §7from kills and", Passive.GOLD_BOOST.description(5).get(0));
        assertEquals("§7Earn §6+60% gold (g) §7from kills and", Passive.GOLD_BOOST.description(6).get(0));
        assertEquals(Collections.singletonList("§7Earn §b+30% XP §7from all sources."), Passive.XP_BOOST.description(3));
        assertEquals(Collections.singletonList("§7Deal §c+15% §7bow damage."), Passive.BOW_DAMAGE.description(5));
        assertEquals(Collections.singletonList("§7Receive §9-4% §7damage."), Passive.DAMAGE_REDUCTION.description(4));
        assertEquals(Collections.singletonList("§7Your blocks stay §a+180% §7longer."), Passive.BUILD_BATTLER.description(3));
        assertEquals("§7§dFirst 2 kills §7each life rewards §6+§65g §b+5 XP§7.",
                UpgradeMenu.passiveDescription(Passive.EL_GATO, 2).get(0));
    }
}
