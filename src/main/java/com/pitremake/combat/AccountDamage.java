package com.pitremake.combat;

import com.pitremake.PitPlugin;
import org.bukkit.entity.Player;

/**
 * アカウントの補正 (逆算レポート「Hypixel Pit ダメージ計算ロジック」、録画 2026-10-14_13-17-35.mcrc の 2,383 件の近接攻撃)。
 * pit.wiki には書いていない値。
 * <pre>
 * 攻撃側: プレステージなし ×1.10、それ以外 ×1.00
 * 被弾側: プレステージなしで Lv79 以下 ×0.82、Lv80 以上 ×0.92 (レポートでは境界は Lv80〜94 のどこか)、
 *         プレステージ I ×0.95、それ以外 ×1.00
 * </pre>
 * レポートで調べたのは近接だけだが、アカウントごとの補正なので矢にも掛ける。Bot はプレステージなし。
 */
public final class AccountDamage implements DamageModifier {

    static final double NO_PRESTIGE_ATTACKER = 0.10;
    static final double NEW_PLAYER_VICTIM = 0.18;
    static final double NO_PRESTIGE_VICTIM = 0.08;
    static final double PRESTIGE_ONE_VICTIM = 0.05;
    /** プレステージなしの被弾側の補正が ×0.82 から ×0.92 に変わるレベル。 */
    static final int VICTIM_BOUNDARY_LEVEL = 80;

    private final PitPlugin plugin;

    public AccountDamage(PitPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void modify(DamageContext context) {
        if (!context.byPlayer()) {
            return;
        }
        context.increase(attackerBonus(prestige(context.attacker)));
        context.reduce(victimReduction(prestige(context.victim), level(context.victim)));
    }

    static double attackerBonus(int prestige) {
        return prestige == 0 ? NO_PRESTIGE_ATTACKER : 0;
    }

    static double victimReduction(int prestige, int level) {
        if (prestige == 0) {
            return level < VICTIM_BOUNDARY_LEVEL ? NEW_PLAYER_VICTIM : NO_PRESTIGE_VICTIM;
        }
        return prestige == 1 ? PRESTIGE_ONE_VICTIM : 0;
    }

    private int prestige(Player player) {
        return plugin.prestigeOf(player);
    }

    private int level(Player player) {
        int bot = plugin.getBotManager().levelOf(player);
        return bot > 0 ? bot : plugin.getStatsManager().getLevel(player.getUniqueId());
    }
}
