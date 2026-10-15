package com.pitremake.combat;

import com.pitremake.item.MysticData;
import com.pitremake.item.MysticItems;
import com.pitremake.item.RewardItems;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mystic のエンチャントが効くかどうか (ユーザーの指定)。
 * <pre>
 * Combo: Venom : 毒にされた人は 12 秒、毒にした本人は 24 秒、Mystic のエンチャントが全部効かない
 *                (着ていても・殴っても・撃っても効果なし)。毒の間は Somber と同じで、相手のエンチャントも効かない
 * Somber       : Somber を履いた人と戦うとき、相手の Mystic のエンチャントは全部効かない。
 *                自分の軽減 (Guardian など) も効かず、受けるダメージの E に +0.20
 * Armageddon Boots : 履いていると相手の Somber を無視する (自分のエンチャントが効く)。毒の相手には効かない
 * </pre>
 */
public final class MysticAccess {

    /** Venom で効かなくなっている人 → 終わる時刻。 */
    private static final Map<UUID, Long> SUPPRESSED = new ConcurrentHashMap<>();

    private MysticAccess() {
    }

    /** Venom などで millis の間、その人の Mystic のエンチャントを止める (長い方が残る)。 */
    public static void suppress(Player player, long millis) {
        long until = System.currentTimeMillis() + millis;
        SUPPRESSED.merge(player.getUniqueId(), until, Math::max);
    }

    /** Venom などで今エンチャントが止まっているか。 */
    public static boolean isSuppressed(Player player) {
        Long until = SUPPRESSED.get(player.getUniqueId());
        if (until == null) {
            return false;
        }
        if (until <= System.currentTimeMillis()) {
            SUPPRESSED.remove(player.getUniqueId());
            return false;
        }
        return true;
    }

    public static void clear(Player player) {
        SUPPRESSED.remove(player.getUniqueId());
    }

    /** Somber を履いているか (Dark エンチャントなので Heresy が無いと効かない)。 */
    public static boolean wearsSomber(Player player) {
        ItemStack pants = player.getInventory().getLeggings();
        return MysticItems.isMysticPants(pants) && MysticData.read(pants).getEnchants().containsKey("somber") && hasHeresy(player);
    }

    /**
     * Somber を履いている人は Mystic のアイテム (剣・弓) を使えない (ユーザーの指定): 剣・弓のエンチャントが全部効かない
     * (キルの報酬のリソース系や Hidden Jewel も)。剣としての攻撃力はそのまま。
     * Fresh Dark Pants や Somber の無い Dark Pants はただのパンツ (Somber は Heresy が無いと効かないので、そのときも使える)。
     */
    public static boolean mysticItemsBlocked(Player player) {
        return wearsSomber(player);
    }

    /** Dark Pants のエンチャントが効くか (Renown の Heresy が 1 Tier 以上、pit.wiki の Dark Pants)。 */
    public static boolean hasHeresy(Player player) {
        com.pitremake.PitPlugin plugin = com.pitremake.PitPlugin.getInstance();
        return plugin == null || plugin.getBotManager().isBot(player)
                || plugin.getUpgradeManager().hasRenown(player.getUniqueId(), com.pitremake.renown.RenownUpgrade.HERESY);
    }

    /** パンツのエンチャント (Dark Pants は Heresy が無いと何も効かない)。 */
    public static java.util.Map<String, Integer> pantsEnchants(Player player) {
        ItemStack pants = player.getInventory().getLeggings();
        if (!MysticItems.isMysticPants(pants)) {
            return java.util.Collections.emptyMap();
        }
        MysticData data = MysticData.read(pants);
        if (data.isDarkPants() && !hasHeresy(player)) {
            return java.util.Collections.emptyMap();
        }
        return data.getEnchants();
    }

    /** Sybil: 人数を数えるエンチャント・パークで何人分に数えるか (Sybil I / II / III で 2 / 3 / 4 人、無ければ 1)。 */
    public static int playerWeight(Player player) {
        if (isSuppressed(player)) {
            return 1;
        }
        Integer sybil = pantsEnchants(player).get("sybil");
        return sybil == null ? 1 : 1 + Math.min(sybil, 3);
    }

    public static boolean wearsArmageddon(Player player) {
        return RewardItems.is(player.getInventory().getBoots(), RewardItems.Type.ARMAGEDDON_BOOTS);
    }

    /**
     * owner の Mystic のエンチャントが、opponent との戦いで効かないか。
     * Venom で止まっている、相手が Venom の毒になっている (Somber と同じ扱いで Armageddon Boots でも効かない)、
     * または相手が Somber を履いていて自分が Armageddon Boots を履いていない (逆算レポート-1)。
     */
    public static boolean disabled(Player owner, Player opponent) {
        if (owner == null) {
            return false;
        }
        if (isSuppressed(owner)) {
            return true;
        }
        if (opponent == null || opponent.equals(owner)) {
            return false;
        }
        return isSuppressed(opponent) || wearsSomber(opponent) && !wearsArmageddon(owner);
    }

    /**
     * 殴られた人の軽減 (Mystic のパンツ・Guardian) が効かないか: {@link #disabled} に加えて、自分が Somber を履いていても効かない
     * (逆算レポート-1: Somber の着用者は自分の Guardian なども効かない)。
     */
    public static boolean defenseDisabled(Player victim, Player attacker) {
        return disabled(victim, attacker) || wearsSomber(victim);
    }
}
