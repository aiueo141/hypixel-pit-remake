package com.pitremake.stats;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** 録画 Assist Kill Bounty Sound logs.mcrc のキルの表示。 */
class KillLabelTest {

    @Test
    void multiKillNames() {
        assertEquals("§a§lKILL!", KillListener.killLabel(1));
        assertEquals("§a§lDOUBLE KILL!", KillListener.killLabel(2));
        assertEquals("§a§lTRIPLE KILL!", KillListener.killLabel(3));
        assertEquals("§a§lQUADRA KILL!", KillListener.killLabel(4));
        assertEquals("§a§lPENTA KILL! §7(5)", KillListener.killLabel(5));
        assertEquals("§a§lMULTI KILL! §7(7)", KillListener.killLabel(7));
    }
}
