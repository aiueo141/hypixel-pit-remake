package com.pitremake.bounty;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** プレイヤーのバウンティ (保存しない) と、続けてキルしたかの判定に使う最後のキルの時刻。 */
public final class BountyManager {

    private final Map<UUID, Integer> bounties = new HashMap<>();
    private final Map<UUID, Long> lastKill = new HashMap<>();

    public int get(UUID player) {
        Integer bounty = bounties.get(player);
        return bounty == null ? 0 : bounty;
    }

    /** コマンドなどで直接付ける (上限なし: 11,000g や 60,000g はコマンドでしか付かない)。 */
    public void set(UUID player, int amount) {
        set(player, amount, Integer.MAX_VALUE);
    }

    private void set(UUID player, int amount, int maxBounty) {
        int bounty = Math.max(0, Math.min(amount, maxBounty));
        if (bounty == 0) {
            bounties.remove(player);
        } else {
            bounties.put(player, bounty);
        }
    }

    /** バウンティの付いているプレイヤー。 */
    public Map<UUID, Integer> all() {
        return Collections.unmodifiableMap(bounties);
    }

    /**
     * キルしたとき。バウンティが付いた / 上がったらその額、そうでなければ 0 を返す。
     * maxBounty: バウンティの上限 (Highlander 発動中は 10,000g)。
     */
    public int onKill(UUID killer, int streak, int maxBounty) {
        long now = System.currentTimeMillis();
        Long last = lastKill.put(killer, now);
        boolean quick = last != null && BountyRules.isQuickKill(now - last);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        if (random.nextDouble() >= BountyRules.chance(streak, quick)) {
            return 0;
        }
        int bump = BountyRules.rollBump(streak, get(killer), maxBounty, random);
        if (bump > 0) {
            set(killer, get(killer) + bump, maxBounty);
        }
        return bump;
    }

    /** 死んだとき。持っていたバウンティを返して消す (キルした人がもらう)。 */
    public int claim(UUID victim) {
        lastKill.remove(victim);
        Integer bounty = bounties.remove(victim);
        return bounty == null ? 0 : bounty;
    }
}
