package com.pitremake.combat;

import com.pitremake.enchant.DescribedEnchant;
import com.pitremake.enchant.EnchantCategory;
import com.pitremake.enchant.EnchantRarity;
import com.pitremake.enchant.PitEnchant;
import com.pitremake.gold.GoldManager;
import org.junit.jupiter.api.Test;

import static com.pitremake.enchant.DescribedEnchant.levels;
import static com.pitremake.enchant.DescribedEnchant.lines;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DamageFormulaTest {

    private static final double EPS = 1e-9;

    /** 増減はすべて掛け算 (逆算レポートの実測例)。 */
    @Test
    void increasesAndReductionsMultiply() {
        // プレステージなしの攻撃者 ×1.10 と Melee Damage V ×1.05 = 1.155 (足し算なら 1.15)
        assertEquals(1.155, DamageContext.multiplier(new double[]{0.10, 0.05}, new double[0]), EPS);
        // Solitude -60% と Critically Funky -40% → 0.4 × 0.6
        assertEquals(0.24, DamageContext.multiplier(new double[0], new double[]{0.6, 0.4}), EPS);
        // Sharp III +12% と Protection III -10%
        assertEquals(1.008, DamageContext.multiplier(new double[]{0.12}, new double[]{0.10}), EPS);
        assertEquals(0, DamageContext.multiplier(new double[0], new double[]{1.5}), EPS);
        // 鉄の剣 7 → プレステージなし Lv32 (×0.82)・防具 11 (×0.56) = 3.214
        assertEquals(3.214, 7 * DamageContext.multiplier(new double[0], new double[]{0.18}) * 0.56, 1e-3);
    }

    /** 加算: Solitude III -60% と Critically Funky III -40% で -100%、Billionaire III +100% で相殺。 */
    @Test
    void additiveStacking() {
        assertEquals(0, DamageContext.additive(0, 0.6 + 0.4), EPS);
        assertEquals(1, DamageContext.additive(1.0, 0.6 + 0.4), EPS);
        assertEquals(1.15, DamageContext.additive(0.10 + 0.05, 0), EPS);
        assertEquals(0, DamageContext.additive(0, 1.5), EPS);
    }

    /** アカウントの補正 (逆算レポート): 7 × 1.10 × 1.05 × 0.82 × 0.56 = 3.713 (Lv94 → Lv32、Melee Damage V)。 */
    @Test
    void accountModifiers() {
        assertEquals(0.10, AccountDamage.attackerBonus(0), EPS);
        assertEquals(0, AccountDamage.attackerBonus(1), EPS);
        assertEquals(0.18, AccountDamage.victimReduction(0, 32), EPS);
        assertEquals(0.08, AccountDamage.victimReduction(0, 108), EPS);
        assertEquals(0.05, AccountDamage.victimReduction(1, 50), EPS);
        assertEquals(0, AccountDamage.victimReduction(4, 50), EPS);
        double factor = DamageContext.multiplier(new double[]{AccountDamage.attackerBonus(0), 0.05},
                new double[]{AccountDamage.victimReduction(0, 32)});
        assertEquals(3.713, 7 * factor * 0.56, 1e-3);
    }

    @Test
    void numbersAreReadFromDescriptions() {
        PitEnchant regularity = new DescribedEnchant("regularity", "Regularity", EnchantCategory.RAGE, EnchantRarity.COMMON, levels(
                lines("§7If the final damage of your strike", "§7deals less than §c1.1❤ §7damage, strike",
                        "§7again in §a0.1s §7for §c50% §7damage."),
                lines("§7If the final damage of your strike", "§7deals less than §c1.5❤ §7damage, strike",
                        "§7again in §a0.1s §7for §c75% §7damage.")));
        assertEquals(2.2, EnchantNumbers.hearts(regularity, 1, 0), EPS);
        assertEquals(0.50, EnchantNumbers.percent(regularity, 1, 0), EPS);
        assertEquals(3.0, EnchantNumbers.hearts(regularity, 2, 0), EPS);
        assertEquals(0.75, EnchantNumbers.percent(regularity, 2, 0), EPS);

        PitEnchant billionaire = new DescribedEnchant("melee_literally_p2w", "Billionaire", EnchantCategory.SWORD, EnchantRarity.RARE,
                levels(lines("§7Hits with this swords deal §c1.33x", "§cdamage §7but cost §6100g"),
                        lines("§7Hits with this swords deal §c2x", "§cdamage §7but cost §6350g")));
        assertEquals(1.33, EnchantNumbers.times(billionaire, 1), EPS);
        assertEquals(2.0, EnchantNumbers.times(billionaire, 2), EPS);

        PitEnchant funky = new DescribedEnchant("power_against_crits", "Critically Funky", EnchantCategory.PANTS, EnchantRarity.UNCOMMON,
                levels(lines("§7Critical hits against you deal", "§7§950% §7of the damage they", "§7normally would and empower your",
                        "§7next strike for §c+30% §7damage")));
        assertEquals(0.50, EnchantNumbers.percent(funky, 1, 0), EPS);
        assertEquals(0.30, EnchantNumbers.percent(funky, 1, 1), EPS);
    }

    @Test
    void goldOverTenThousandHasNoDecimals() {
        assertEquals("23.70g", GoldManager.formatExact(23.7));
        assertEquals("9,999.99g", GoldManager.formatExact(9999.99));
        assertEquals("10,000g", GoldManager.formatExact(10000.5));
        assertEquals("912,364,354g", GoldManager.formatExact(912364354.75));
    }
}
