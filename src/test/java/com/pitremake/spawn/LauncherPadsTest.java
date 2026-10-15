package com.pitremake.spawn;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LauncherPadsTest {

    private static LauncherPads pads;

    @BeforeAll
    static void load() throws Exception {
        pads = LauncherPads.load(LauncherPadsTest.class.getClassLoader().getResourceAsStream("launcher/seasons.yml"));
        assertNotNull(pads);
    }

    @Test
    void seasonsHasFourCornerPadsWithTargetsAndTemplates() {
        assertEquals(4, pads.pads.size());
        for (LauncherPads.Pad pad : pads.pads) {
            assertTrue(pad.targets.size() >= 4, pad.name);
            assertTrue(pad.totalWeight > 0, pad.name);
        }
        assertTrue(pads.templates.size() >= 10);
        for (LauncherPads.Template template : pads.templates) {
            assertArrayEquals(new double[]{0, 0}, template.path[0], 0.01);
            assertTrue(template.path.length >= 45 && template.path.length <= 82);
        }
    }

    @Test
    void launchesOnlyFromTheCornersAtThePadRadius() {
        // 録画の自分の打ち上げ: 北西 (-12.25, -11.84) r=17.04、北東 (15.56, -15.91) r=22.25
        assertEquals("NW", pads.crossed(-11.8, -11.4, -12.25, -11.84).name);
        assertEquals("NE", pads.crossed(15.3, -15.6, 15.56, -15.91).name);
        // 内側を歩いているだけ・東の端 (パッドが無い) では飛ばない
        assertNull(pads.crossed(-10, -10, -10.2, -10.2));
        assertNull(pads.crossed(22, 0.5, 23.5, 0.5));
    }

    @Test
    void picksEveryTargetByWeight() {
        LauncherPads.Pad northWest = pads.crossed(-11.8, -11.4, -12.25, -11.84);
        Set<String> seen = new HashSet<>();
        Random random = new Random(1);
        for (int i = 0; i < 2000; i++) {
            double[] target = LauncherPads.pickTarget(northWest, random);
            seen.add(target[0] + "," + target[2]);
        }
        assertEquals(northWest.targets.size(), seen.size());
    }

    @Test
    void nearestTemplateMatchesTheDistance() {
        // 録画の一番長い弧 (Death.mcrc の 86.56 ブロックと 10-32-13 の 86.61 ブロック、どちらも 81 tick)
        LauncherPads.Template far = pads.nearest(90);
        assertEquals(86.6, far.distance(), 0.1);
        assertEquals(82, far.path.length);
        assertTrue(pads.nearest(34).distance() < 40);
    }

    @Test
    void pathKeepsDescendingAfterItEnds() {
        double[][] path = {{0, 0}, {1, 1}, {2, 0.5}};
        assertArrayEquals(new double[]{2, 0.5}, Launcher.point(path, 2), 1e-9);
        assertArrayEquals(new double[]{4, -0.5}, Launcher.point(path, 4), 1e-9);
    }
}
