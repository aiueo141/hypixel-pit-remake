package com.pitremake.well;

import com.pitremake.enchant.EnchantCategory;
import com.pitremake.enchant.EnchantRarity;
import com.pitremake.enchant.EnchantRegistry;
import com.pitremake.enchant.PitEnchant;
import com.pitremake.enchant.DescribedEnchant;
import com.pitremake.enchant.list.AquaEnchants;
import com.pitremake.enchant.list.BowEnchants;
import com.pitremake.enchant.list.SewerEnchants;
import com.pitremake.item.SpecialPants;
import com.pitremake.enchant.list.DarkEnchants;
import com.pitremake.enchant.list.PantsEnchants;
import com.pitremake.enchant.list.RageEnchants;
import com.pitremake.enchant.list.ResourceEnchants;
import com.pitremake.enchant.list.SwordEnchants;
import com.pitremake.item.MysticData;
import com.pitremake.item.MysticPrefix;
import com.pitremake.item.MysticType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MysticWellRollerTest {

    private static final int SEEDS = 5000;
    /** 高いレベル数は出にくいので、"一度は出る" を確かめるテストは多めに回す。 */
    private static final int RARE_SEEDS = 60000;

    private enum Outcome { ADD_ONLY, UPGRADE_ONLY, BOTH, NONE }

    private EnchantRegistry registry;
    private MysticWellRoller roller;

    @BeforeEach
    void setUp() {
        registry = new EnchantRegistry();
        ResourceEnchants.registerAll(registry);
        SwordEnchants.registerAll(registry);
        BowEnchants.registerAll(registry);
        PantsEnchants.registerAll(registry);
        RageEnchants.registerAll(registry);
        DarkEnchants.registerAll(registry);
        SewerEnchants.registerAll(registry);
        AquaEnchants.registerAll(registry);
        roller = new MysticWellRoller(registry);
    }

    @Test
    void ragePantsGetOneRageEnchantAtTierOneThenNormalPantsRules() {
        Set<Integer> livesSeen = new HashSet<>();
        boolean rareLookingAtTwoSeen = false;
        boolean maxedRageSeen = false;
        for (long seed = 0; seed < SEEDS; seed++) {
            Random random = new Random(seed);
            MysticData data = new MysticData();
            data.setRagePants(true);

            roller.upgrade(data, MysticType.PANTS, random);
            assertEquals(1, data.getEnchants().size(), "seed " + seed);
            Map.Entry<String, Integer> rage = data.getEnchants().entrySet().iterator().next();
            PitEnchant rageEnchant = registry.get(rage.getKey());
            assertEquals(EnchantCategory.RAGE, rageEnchant.getCategory(), "seed " + seed);
            assertTrue(rage.getValue() >= 1 && rage.getValue() <= 2, "seed " + seed);
            assertTrue(data.getMaxLives() == 4 || data.getMaxLives() == 9, "seed " + seed);
            livesSeen.add(data.getMaxLives());
            // Rage エンチャントはすべてルール上 COMMON (Regularity などは見た目だけ RARE)
            assertEquals(EnchantRarity.COMMON, rageEnchant.getRarity(), "seed " + seed);
            // 見た目だけ RARE なので追加コストがかからず、Tier I で Lv II にもなる
            rareLookingAtTwoSeen |= rageEnchant.formatName(1).contains("RARE!") && rage.getValue() == 2;

            roller.upgrade(data, MysticType.PANTS, random);
            roller.upgrade(data, MysticType.PANTS, random);
            assertTrue(data.getEnchants().size() <= 3, "seed " + seed);
            for (String key : data.getEnchants().keySet()) {
                PitEnchant enchant = registry.get(key);
                assertTrue(enchant.canApplyTo(MysticType.PANTS));
                // Tier II 以降に新しい Rage エンチャントは付かない
                assertTrue(enchant.getCategory() != EnchantCategory.RAGE || key.equals(rage.getKey()), "seed " + seed);
            }
            maxedRageSeen |= data.getEnchantLevel(rage.getKey()) == 3;
        }
        assertEquals(new HashSet<>(Arrays.asList(4, 9)), livesSeen);
        assertTrue(rareLookingAtTwoSeen);
        assertTrue(maxedRageSeen, "Rage enchant should be able to reach III (Malding)");
    }

    @Test
    void darkPantsGetSomberThenOneDarkEnchant() {
        boolean rareSeen = false;
        boolean heartripperSeen = false;
        int evilCount = 0;
        for (long seed = 0; seed < SEEDS; seed++) {
            Random random = new Random(seed);
            MysticData data = new MysticData();
            data.setDarkPants(true);

            roller.upgrade(data, MysticType.PANTS, random);
            assertEquals(1, data.getTier());
            assertEquals(Collections.singleton(DarkEnchants.SOMBER), data.getEnchants().keySet(), "seed " + seed);
            assertTrue(data.getMaxLives() >= 5 && data.getMaxLives() <= 20 && data.getMaxLives() % 5 == 0, "seed " + seed);
            int livesBefore = data.getMaxLives();

            roller.upgrade(data, MysticType.PANTS, random);
            assertEquals(2, data.getTier());
            assertEquals(2, data.getEnchants().size(), "seed " + seed);
            for (String key : data.getEnchants().keySet()) {
                PitEnchant enchant = registry.get(key);
                assertEquals(EnchantCategory.DARK, enchant.getCategory(), "seed " + seed);
                assertEquals(1, data.getEnchantLevel(key));
                rareSeen |= enchant.getRarity() == EnchantRarity.RARE;
            }
            int lives = data.getMaxLives();
            if (data.getEnchants().containsKey(DarkEnchants.HEARTRIPPER)) {
                assertEquals(livesBefore, lives, "Heartripper should not add lives, seed " + seed);
                heartripperSeen = true;
            } else {
                // 通常は 10〜25、低確率で 100〜120 (Evil Pants)。強化前より多い
                boolean normal = lives >= 10 && lives <= 25;
                boolean evil = lives >= 100 && lives <= 120;
                assertTrue((normal || evil) && lives > livesBefore, "seed " + seed + " lives " + lives);
                if (evil) {
                    evilCount++;
                }
            }
            assertEquals(0, lives % 5, "Dark Pants lives are multiples of 5, seed " + seed);
        }
        assertTrue(rareSeen);
        assertTrue(heartripperSeen);
        // Evil Pants は低確率 (約 1%)
        double evilRate = (double) evilCount / SEEDS;
        assertTrue(evilRate > 0.002 && evilRate < 0.02, "evil rate " + evilRate);
        assertEquals(2, MysticWellRoller.maxTier(darkData()));
    }

    @Test
    void sewerPantsGetOneSewerEnchantAndTenOrTwentyLives() {
        boolean trashPandaSeen = false;
        for (long seed = 0; seed < SEEDS; seed++) {
            MysticData data = special(SpecialPants.SEWER);
            roller.upgrade(data, MysticType.PANTS, new Random(seed));
            assertEquals(1, data.getTier());
            assertEquals(1, data.getEnchants().size(), "seed " + seed);
            String key = data.getEnchants().keySet().iterator().next();
            assertEquals(EnchantCategory.SEWER, registry.get(key).getCategory());
            if (key.equals(SewerEnchants.TRASH_PANDA)) {
                assertTrue(data.getMaxLives() == 30 || data.getMaxLives() == 40, "seed " + seed);
                trashPandaSeen = true;
            } else {
                assertTrue(data.getMaxLives() == 10 || data.getMaxLives() == 20, "seed " + seed);
            }
        }
        assertTrue(trashPandaSeen);
        assertEquals(1, MysticWellRoller.maxTier(special(SpecialPants.SEWER)));
    }

    @Test
    void aquaPantsGetClubRodPlusOneAquaEnchantAndFiveLives() {
        Set<String> seen = new HashSet<>();
        for (long seed = 0; seed < SEEDS; seed++) {
            MysticData data = special(SpecialPants.AQUA);
            roller.upgrade(data, MysticType.PANTS, new Random(seed));
            assertEquals(1, data.getTier());
            assertEquals(5, data.getMaxLives());
            assertEquals(2, data.getEnchants().size(), "seed " + seed);
            assertTrue(data.getEnchants().containsKey(AquaEnchants.CLUB_ROD));
            for (String key : data.getEnchants().keySet()) {
                assertEquals(EnchantCategory.AQUA, registry.get(key).getCategory());
                seen.add(key);
            }
        }
        // Club Rod + 9 種類すべてが出る (RARE の Stereo も)
        assertEquals(10, seen.size());
        assertEquals(1, MysticWellRoller.maxTier(special(SpecialPants.AQUA)));
    }

    private static MysticData special(SpecialPants special) {
        MysticData data = new MysticData();
        data.setSpecialPants(special);
        return data;
    }

    private static MysticData darkData() {
        MysticData data = new MysticData();
        data.setDarkPants(true);
        return data;
    }

    @Test
    void raiseTierKeepsEnchantsAndRollsLivesLikeAnUpgrade() {
        for (long seed = 0; seed < 500; seed++) {
            Random random = new Random(seed);
            MysticData data = new MysticData();
            data.setEnchant("melee_literally_p2w", 3);

            roller.raiseTier(data, random);
            assertEquals(1, data.getTier());
            assertTrue(data.getMaxLives() >= 3 && data.getMaxLives() <= 5, "seed " + seed);
            int previous = data.getMaxLives();

            roller.raiseTier(data, random);
            assertEquals(2, data.getTier());
            assertTrue(data.getMaxLives() > previous && data.getMaxLives() <= 20, "seed " + seed);
            assertEquals(1, data.getEnchants().size());
            assertEquals(3, data.getEnchantLevel("melee_literally_p2w"));
        }

        MysticData rage = new MysticData();
        rage.setRagePants(true);
        roller.raiseTier(rage, new Random(1));
        assertTrue(rage.getMaxLives() == 4 || rage.getMaxLives() == 9);
    }

    @Test
    void simplifiedEnchantNamesAreUnique() {
        // /menchant は名前を小文字・英数字だけにして探すので、重複しないこと
        Set<String> names = new HashSet<>();
        for (PitEnchant enchant : registry.getAll()) {
            String name = enchant.getName().toLowerCase().replaceAll("[^a-z0-9]", "");
            assertTrue(names.add(name), "duplicate name: " + name);
        }
    }

    @Test
    void bowAndPantsFollowTheSameTierRules() {
        for (MysticType type : new MysticType[]{MysticType.BOW, MysticType.PANTS}) {
            boolean ownRareSeen = false;
            for (long seed = 0; seed < SEEDS; seed++) {
                Random random = new Random(seed);
                MysticData data = new MysticData();
                for (int tier = 1; tier <= 3; tier++) {
                    roller.upgrade(data, type, random);
                    int count = data.getEnchants().size();
                    assertTrue(count >= 1 && count <= (tier == 1 ? 2 : 3), type + " seed " + seed + " tier " + tier);

                    int total = 0;
                    int rareLevels = 0;
                    for (Map.Entry<String, Integer> entry : data.getEnchants().entrySet()) {
                        PitEnchant enchant = registry.get(entry.getKey());
                        assertTrue(enchant.canApplyTo(type), type + " got " + entry.getKey());
                        assertTrue(enchant.getCategory() != EnchantCategory.RAGE, type + " got rage " + entry.getKey());
                        assertTrue(!enchant.getCategory().isSpecialPantsOnly(), type + " got special " + entry.getKey());
                        if (tier == 1) {
                            assertEquals(EnchantRarity.COMMON, enchant.getRarity(), type + " seed " + seed);
                        }
                        if (enchant.getRarity() == EnchantRarity.RARE) {
                            rareLevels += entry.getValue();
                            if (tier == 2) {
                                assertEquals(1, (int) entry.getValue(), type + " seed " + seed);
                            }
                            ownRareSeen |= enchant.getCategory().name().equals(type.name());
                        }
                        total += entry.getValue();
                    }
                    assertTrue(total <= WellRules.MAX_TOKENS && rareLevels <= WellRules.RARE_LEVEL_CAP, type + " seed " + seed);
                }
            }
            assertTrue(ownRareSeen, type + " should roll its own RARE enchants");
        }
    }

    @Test
    void levelBudgetPerTierAndNewRareAtThreeChangesNothingElse() {
        int[] maxBudget = {0, WellRules.maxBudget(1), WellRules.maxBudget(2), WellRules.maxBudget(3)};
        boolean tierThreeFourLevels = false;
        boolean tierThreeRareAtThree = false;
        boolean tierThreeRareAtTwoWithOthers = false;
        boolean tierTwoRareWithOthers = false;
        for (long seed = 0; seed < RARE_SEEDS; seed++) {
            Random random = new Random(seed);
            MysticData data = new MysticData();
            for (int tier = 1; tier <= 3; tier++) {
                Map<String, Integer> before = new HashMap<>(data.getEnchants());
                roller.upgrade(data, MysticType.SWORD, random);

                int gained = levelSum(data.getEnchants()) - levelSum(before);
                String newRare = null;
                for (String key : data.getEnchants().keySet()) {
                    if (!before.containsKey(key) && registry.get(key).getRarity() == EnchantRarity.RARE) {
                        newRare = key;
                    }
                }
                assertTrue(gained >= 1 && gained <= maxBudget[tier], "seed " + seed + " tier " + tier);

                if (tier == 2 && newRare != null && gained > 1) {
                    // RARE に追加コストは無いので、Tier II でも RARE I と同時に他も追加・強化される
                    tierTwoRareWithOthers = true;
                }
                if (tier == 3 && newRare == null && gained == 4) {
                    tierThreeFourLevels = true;
                }
                if (tier == 3 && newRare != null && data.getEnchantLevel(newRare) == 3) {
                    // 新しい RARE がいきなり III になったときは、他のエンチャントは何も変わらない
                    // (例: Sniper I, MLB I → Sniper I, MLB I, Telebow III)
                    Map<String, Integer> others = new HashMap<>(data.getEnchants());
                    others.remove(newRare);
                    assertEquals(before, others, "seed " + seed);
                    tierThreeRareAtThree = true;
                }
                if (tier == 3 && newRare != null && data.getEnchantLevel(newRare) == 2 && gained > 2) {
                    tierThreeRareAtTwoWithOthers = true;
                }
            }
        }
        assertTrue(tierTwoRareWithOthers);
        assertTrue(tierThreeFourLevels);
        assertTrue(tierThreeRareAtThree);
        assertTrue(tierThreeRareAtTwoWithOthers);
    }

    @Test
    void rareLevelsAreCappedAtFourAndTotalAtEight() {
        boolean capReached = false;
        for (long seed = 0; seed < RARE_SEEDS * 5; seed++) {
            MysticData data = upgradeFromFresh(3, seed);
            int rareLevels = rareLevelSum(data);
            assertTrue(rareLevels <= WellRules.RARE_LEVEL_CAP, "seed " + seed);
            assertTrue(levelSum(data.getEnchants()) <= WellRules.MAX_TOKENS, "seed " + seed);
            capReached |= rareLevels == WellRules.RARE_LEVEL_CAP;
        }
        assertTrue(capReached);
    }

    @Test
    void rareCapIsLiftedWhenTierOneHadRareThree() {
        boolean aboveFour = false;
        for (long seed = 0; seed < SEEDS; seed++) {
            Random random = new Random(seed);
            // Tier I の時点で Gamble III が付いている特殊なアイテム
            MysticData data = new MysticData();
            data.setTier(1);
            data.setMaxLives(4);
            data.setLives(4);
            data.setEnchant("melee_gamble", 3);
            roller.upgrade(data, MysticType.SWORD, random);
            roller.upgrade(data, MysticType.SWORD, random);

            int rareLevels = rareLevelSum(data);
            assertTrue(rareLevels <= WellRules.MAX_TOKENS, "seed " + seed);
            assertTrue(levelSum(data.getEnchants()) <= WellRules.MAX_TOKENS, "seed " + seed);
            aboveFour |= rareLevels > WellRules.RARE_LEVEL_CAP;
        }
        assertTrue(aboveFour);
    }

    @Test
    void artifactOnlyAtTierThreeAboutOnePercent() {
        int samples = 20000;
        int artifacts = 0;
        for (long seed = 0; seed < samples; seed++) {
            Random random = new Random(seed);
            MysticData data = new MysticData();
            roller.upgrade(data, MysticType.SWORD, random);
            roller.upgrade(data, MysticType.SWORD, random);
            assertTrue(data.getMaxLives() < WellRules.ARTIFACT_LIVES, "seed " + seed);
            roller.upgrade(data, MysticType.SWORD, random);
            if (data.getMaxLives() == WellRules.ARTIFACT_LIVES) {
                assertEquals(WellRules.ARTIFACT_LIVES, data.getLives());
                artifacts++;
            }
        }
        double rate = (double) artifacts / samples;
        assertTrue(rate > 0.006 && rate < 0.015, "artifact rate " + rate);
    }

    private int rareLevelSum(MysticData data) {
        int sum = 0;
        for (Map.Entry<String, Integer> entry : data.getEnchants().entrySet()) {
            if (registry.get(entry.getKey()).getRarity() == EnchantRarity.RARE) {
                sum += entry.getValue();
            }
        }
        return sum;
    }

    @Test
    void tierOneBudgetIsMostlyOne() {
        int samples = 20000;
        int twos = 0;
        for (long seed = 0; seed < samples; seed++) {
            if (WellRules.rollBudget(new Random(seed), 1) == 2) {
                twos++;
            }
        }
        double rate = (double) twos / samples;
        assertTrue(rate > 0.17 && rate < 0.23, "Tier I budget 2 rate " + rate);
    }

    private static int levelSum(Map<String, Integer> enchants) {
        int sum = 0;
        for (int level : enchants.values()) {
            sum += level;
        }
        return sum;
    }

    @Test
    void atMostOneNewRarePerUpgrade() {
        boolean secondRareOnItemSeen = false;
        for (long seed = 0; seed < RARE_SEEDS; seed++) {
            Random random = new Random(seed);
            MysticData data = new MysticData();
            for (int tier = 1; tier <= 3; tier++) {
                Map<String, Integer> before = new HashMap<>(data.getEnchants());
                roller.upgrade(data, MysticType.SWORD, random);

                int newRares = 0;
                int raresOnItem = 0;
                for (String key : data.getEnchants().keySet()) {
                    if (registry.get(key).getRarity() == EnchantRarity.RARE) {
                        raresOnItem++;
                        if (!before.containsKey(key)) {
                            newRares++;
                        }
                    }
                }
                assertTrue(newRares <= 1, "seed " + seed + " tier " + tier);
                secondRareOnItemSeen |= raresOnItem >= 2;
            }
        }
        // 別々の強化 (Tier II と Tier III) なら RARE が 2 種類になることはある
        assertTrue(secondRareOnItemSeen);
    }

    @Test
    void tierOneUsesOnlyResourceOrCommonAndAtMostTwoEnchants() {
        Set<Integer> countsSeen = new HashSet<>();
        boolean levelTwoSeen = false;
        for (long seed = 0; seed < SEEDS; seed++) {
            MysticData data = upgradeFromFresh(1, seed);
            assertEquals(1, data.getTier());
            int count = data.getEnchants().size();
            assertTrue(count >= 1 && count <= 2, "seed " + seed);
            countsSeen.add(count);

            for (Map.Entry<String, Integer> entry : data.getEnchants().entrySet()) {
                PitEnchant enchant = registry.get(entry.getKey());
                assertNotNull(enchant);
                assertEquals(EnchantRarity.COMMON, enchant.getRarity(), "seed " + seed);
                assertTrue(enchant.canApplyTo(MysticType.SWORD));
                levelTwoSeen |= entry.getValue() == 2;
            }
        }
        // 2 種類付く場合も、1 種類が Lv II になる場合もある
        assertEquals(new HashSet<>(Arrays.asList(1, 2)), countsSeen);
        assertTrue(levelTwoSeen);
    }

    @Test
    void tierOneCanPickResourceEnchants() {
        boolean resourceSeen = false;
        for (long seed = 0; seed < SEEDS && !resourceSeen; seed++) {
            String key = upgradeFromFresh(1, seed).getEnchants().keySet().iterator().next();
            resourceSeen = registry.get(key).getCategory() == EnchantCategory.RESOURCE;
        }
        assertTrue(resourceSeen);
    }

    @Test
    void tierTwoKeepsRaresAtLevelOneAndAtMostThreeEnchants() {
        int rares = 0;
        boolean threeSeen = false;
        for (long seed = 0; seed < SEEDS; seed++) {
            MysticData data = upgradeFromFresh(2, seed);
            int count = data.getEnchants().size();
            assertTrue(count <= 3, "seed " + seed);
            threeSeen |= count == 3;

            for (Map.Entry<String, Integer> entry : data.getEnchants().entrySet()) {
                if (registry.get(entry.getKey()).getRarity() == EnchantRarity.RARE) {
                    assertEquals(1, (int) entry.getValue(), "seed " + seed);
                    rares++;
                }
            }
        }
        assertTrue(rares > 0, "Tier II should be able to add RARE enchants");
        assertTrue(threeSeen, "Tier II should be able to reach 3 enchants");
    }

    @Test
    void tierThreeAllowsNewlyAddedRaresAboveLevelOne() {
        boolean found = false;
        for (long seed = 0; seed < SEEDS * 4 && !found; seed++) {
            Random random = new Random(seed);
            MysticData data = new MysticData();
            roller.upgrade(data, MysticType.SWORD, random);
            roller.upgrade(data, MysticType.SWORD, random);
            Map<String, Integer> before = new HashMap<>(data.getEnchants());
            roller.upgrade(data, MysticType.SWORD, random);

            for (Map.Entry<String, Integer> entry : data.getEnchants().entrySet()) {
                if (!before.containsKey(entry.getKey())
                        && registry.get(entry.getKey()).getRarity() == EnchantRarity.RARE
                        && entry.getValue() > 1) {
                    found = true;
                }
            }
        }
        assertTrue(found);
    }

    @Test
    void neverDuplicatesExceedsMaxLevelOrPicksForeignEnchants() {
        for (long seed = 0; seed < SEEDS; seed++) {
            Random random = new Random(seed);
            MysticData data = new MysticData();
            for (int tier = 1; tier <= 3; tier++) {
                Map<String, Integer> before = new HashMap<>(data.getEnchants());
                roller.upgrade(data, MysticType.SWORD, random);

                int total = 0;
                for (Map.Entry<String, Integer> entry : data.getEnchants().entrySet()) {
                    PitEnchant enchant = registry.get(entry.getKey());
                    assertNotNull(enchant);
                    assertTrue(enchant.canApplyTo(MysticType.SWORD), "seed " + seed);
                    assertTrue(entry.getValue() >= 1 && entry.getValue() <= enchant.getMaxLevel(), "seed " + seed);
                    // 既存エンチャントは下がらない (再追加でリセットされていない)
                    Integer old = before.get(entry.getKey());
                    assertTrue(old == null || entry.getValue() >= old, "seed " + seed);
                    total += entry.getValue();
                }
                assertTrue(total <= WellRules.MAX_TOKENS, "seed " + seed);
                assertTrue(data.getEnchants().keySet().containsAll(before.keySet()), "seed " + seed);
            }
        }
    }

    @Test
    void allThreeOutcomesArePossible() {
        Set<Outcome> outcomes = EnumSet.noneOf(Outcome.class);
        for (long seed = 0; seed < SEEDS; seed++) {
            Random random = new Random(seed);
            MysticData data = new MysticData();
            roller.upgrade(data, MysticType.SWORD, random);
            for (int tier = 2; tier <= 3; tier++) {
                Map<String, Integer> before = new HashMap<>(data.getEnchants());
                roller.upgrade(data, MysticType.SWORD, random);
                outcomes.add(classify(before, data.getEnchants()));
            }
        }
        assertTrue(outcomes.containsAll(EnumSet.of(Outcome.ADD_ONLY, Outcome.UPGRADE_ONLY, Outcome.BOTH)),
                "outcomes: " + outcomes);
    }

    @Test
    void terminatesWhenNoCandidatesRemain() {
        EnchantRegistry single = new EnchantRegistry();
        single.register(new DescribedEnchant("onlyenc", "OnlyEnc", EnchantCategory.SWORD, EnchantRarity.COMMON,
                DescribedEnchant.levels(DescribedEnchant.lines("I"), DescribedEnchant.lines("II"), DescribedEnchant.lines("III"))));
        MysticWellRoller singleRoller = new MysticWellRoller(single);

        for (long seed = 0; seed < 200; seed++) {
            Random random = new Random(seed);
            MysticData data = new MysticData();
            for (int tier = 1; tier <= 3; tier++) {
                singleRoller.upgrade(data, MysticType.SWORD, random);
            }
            assertEquals(3, data.getTier());
            assertEquals(1, data.getEnchants().size());
            assertTrue(data.getEnchantLevel("onlyenc") <= 3);
        }

        // 候補が 1 つも無いレジストリでも止まる
        MysticData empty = new MysticData();
        new MysticWellRoller(new EnchantRegistry()).upgrade(empty, MysticType.SWORD, new Random(1));
        assertEquals(1, empty.getTier());
        assertTrue(empty.getEnchants().isEmpty());
    }

    @Test
    void tierThreeNeverExceedsThreeEnchants() {
        int samples = 20000;
        int[] histogram = new int[10];
        Map<String, Integer> prefixes = new TreeMap<>();
        double sum = 0;
        for (long seed = 0; seed < samples; seed++) {
            MysticData data = upgradeFromFresh(3, seed);
            int count = data.getEnchants().size();
            histogram[count]++;
            sum += count;
            MysticPrefix prefix = MysticPrefix.of(data, registry);
            prefixes.merge(prefix == null ? "(none)" : prefix.getDisplayName(), 1, Integer::sum);
        }
        StringBuilder prefixReport = new StringBuilder("Tier III sword prefixes:");
        for (Map.Entry<String, Integer> entry : prefixes.entrySet()) {
            prefixReport.append(String.format("  %s=%.2f%%", entry.getKey(), 100.0 * entry.getValue() / samples));
        }
        System.out.println(prefixReport);
        double average = sum / samples;

        StringBuilder report = new StringBuilder("Tier III sword enchant count: avg ")
                .append(String.format("%.2f", average));
        for (int i = 0; i < histogram.length; i++) {
            if (histogram[i] > 0) {
                report.append(String.format("  %d=%.1f%%", i, 100.0 * histogram[i] / samples));
            }
        }
        System.out.println(report);

        for (int i = 4; i < histogram.length; i++) {
            assertEquals(0, histogram[i], report.toString());
        }
        assertTrue(histogram[3] > 0, report.toString());
    }

    private MysticData upgradeFromFresh(int tiers, long seed) {
        Random random = new Random(seed);
        MysticData data = new MysticData();
        for (int tier = 1; tier <= tiers; tier++) {
            roller.upgrade(data, MysticType.SWORD, random);
        }
        return data;
    }

    private static Outcome classify(Map<String, Integer> before, Map<String, Integer> after) {
        boolean added = false;
        boolean upgraded = false;
        for (Map.Entry<String, Integer> entry : after.entrySet()) {
            Integer old = before.get(entry.getKey());
            if (old == null) {
                added = true;
            } else if (entry.getValue() > old) {
                upgraded = true;
            }
        }
        if (added && upgraded) {
            return Outcome.BOTH;
        }
        if (added) {
            return Outcome.ADD_ONLY;
        }
        return upgraded ? Outcome.UPGRADE_ONLY : Outcome.NONE;
    }

    @Test
    void auctionOnlyEnchantsNeverComeFromTheWell() {
        for (MysticType type : MysticType.values()) {
            for (PitEnchant enchant : roller.newEnchantCandidates(new MysticData(), type, TierEnchantRules.of(3, false), true)) {
                assertTrue(!PantsEnchants.AUCTION_ONLY.contains(enchant.getKey()), type + " " + enchant.getKey());
            }
        }
        for (long seed = 0; seed < SEEDS; seed++) {
            MysticData data = new MysticData();
            Random random = new Random(seed);
            for (int tier = 1; tier <= 3; tier++) {
                roller.upgrade(data, MysticType.PANTS, random);
            }
            for (String key : PantsEnchants.AUCTION_ONLY) {
                assertEquals(0, data.getEnchantLevel(key), "seed " + seed);
            }
        }
    }

    @Test
    void auctionPantsCanBeUpgradedAtTheWell() {
        int[] levels = new int[4];
        int samples = 10_000;
        for (long seed = 0; seed < samples; seed++) {
            Random random = new Random(seed);
            // オークションの Tier I パンツ (Paparazzi I、Lives 10)
            MysticData data = new MysticData();
            data.setEnchant(PantsEnchants.PAPARAZZI, 1);
            data.setTier(1);
            data.setMaxLives(10);
            data.setLives(10);
            roller.upgrade(data, MysticType.PANTS, random);
            assertEquals(2, data.getTier());
            // Tier II では RARE は上がらない
            assertEquals(1, data.getEnchantLevel(PantsEnchants.PAPARAZZI), "seed " + seed);
            roller.upgrade(data, MysticType.PANTS, random);
            assertEquals(3, data.getTier());
            assertTrue(data.getMaxLives() > 10, "seed " + seed);
            int level = data.getEnchantLevel(PantsEnchants.PAPARAZZI);
            assertTrue(level >= 1 && level <= 3, "seed " + seed);
            levels[level]++;
            // ほかのオークション限定のエンチャントは付かない
            assertEquals(0, data.getEnchantLevel(PantsEnchants.PIT_MBA), "seed " + seed);
            assertEquals(0, data.getEnchantLevel(PantsEnchants.FRACTIONAL_RESERVE), "seed " + seed);
        }
        System.out.printf("Paparazzi after Tier III: I=%.1f%% II=%.1f%% III=%.1f%%%n",
                100.0 * levels[1] / samples, 100.0 * levels[2] / samples, 100.0 * levels[3] / samples);
        assertTrue(levels[3] > 0);
    }
}
