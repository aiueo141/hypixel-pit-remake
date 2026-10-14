package com.pitremake.renown;

import com.pitremake.PitPlugin;
import com.pitremake.item.MysticItems;
import com.pitremake.item.PantsColor;
import com.pitremake.perk.PerkItems;
import com.pitremake.stats.KillRewards;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Renown Shop の Upgrades の効果 (数値は pit.wiki の Renown)。
 * <pre>
 * Tenacity            : キルで 0.5❤ × Tier 回復
 * Scam Artist         : Items の村人・Mystic Well の値段 -5% × Tier
 * Renown Gold Boost   : キルのゴールド +2.5% × Tier (ほかの出どころの倍率と掛け算)
 * Renown XP Bump      : キルの XP +1 × Tier (足し算の分)
 * Extra Hearts        : 最大体力 +1❤ × Tier
 * Armory              : スポーンで矢 +8 × Tier
 * Fancy Hat           : スポーンで革のヘルメット (Safety First があればそちら)
 * Impatient           : スポーンの中で Speed II
 * Yummy!              : 金リンゴ・Golden Head を食べると +6g
 * Very Yummy!         : Golden Head の再生 +2 秒・衝撃吸収 +0.5❤
 * Divine Intervention : 死んでも 5% × Tier で持ち物がそのまま
 * Fast Pass           : プレステージ後はレベル 50 から
 * Experience-Industrial Complex : ストリークの XP ボーナスの上限 +60、キルの XP の上限 +50
 * Celebrity           : キル・アシストのゴールドを最後に 2 倍 (ほかの % とは足さない)
 * Mysticism           : キルで Mystic のアイテムが落ちる (約 0.2% × (1 + 増加)、IV からパンツも)
 * </pre>
 * Extra Perk Slot / The Way / One Upper / Promotion!! / Raw Numbers / Cool Conclave / 打ち上げの軌跡はそれぞれの場所で見る。
 */
public final class RenownEffects implements Listener {

    static final double TENACITY_HEAL = 1.0;
    static final double SCAM_ARTIST_DISCOUNT = 0.05;
    static final double GOLD_BOOST_PER_TIER = 0.025;
    static final double EXTRA_HEART = 2.0;
    static final int ARMORY_ARROWS = 8;
    static final double YUMMY_GOLD = 6;
    static final double DIVINE_INTERVENTION_CHANCE = 0.05;
    public static final int FAST_PASS_LEVEL = 50;
    static final int EIC_STREAK_XP_CAP = 60;
    static final int EIC_MAX_XP = 50;
    /** Celebrity: キルとアシストのゴールドを最後に 2 倍。 */
    static final double CELEBRITY_GOLD = 2.0;
    /** Impatient の Speed の時間と付け直す間隔 (スポーンから出たら 1 秒以内に切れるように短く。ユーザーの指定)。 */
    private static final int IMPATIENT_TICKS = 20;
    private static final long IMPATIENT_CHECK_TICKS = 10;
    /** Mystic のアイテムが落ちる元の確率 (約 1/500)。 */
    static final double MYSTIC_DROP_CHANCE = 0.002;
    /** Mysticism の Tier I〜X までの増加の合計 (録画: IX で +150%、次の X で +200%)。 */
    private static final double[] MYSTICISM_BONUS = {0, 0.30, 0.50, 0.50, 0.70, 1.00, 1.25, 1.50, 1.50, 2.00};
    /** この Tier からパンツも落ちる。 */
    static final int MYSTICISM_PANTS_TIER = 4;

    private final PitPlugin plugin;

    public RenownEffects(PitPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (!player.isDead()) {
                    impatient(player);
                }
            }
        }, IMPATIENT_CHECK_TICKS, IMPATIENT_CHECK_TICKS);
    }

    public int tier(UUID player, RenownUpgrade upgrade) {
        return plugin.getUpgradeManager().getRenownTier(player, upgrade);
    }

    public boolean has(Player player, RenownUpgrade upgrade) {
        return tier(player.getUniqueId(), upgrade) > 0;
    }

    /** Scam Artist で安くなった値段。 */
    public int discounted(Player player, int cost) {
        return discounted(cost, tier(player.getUniqueId(), RenownUpgrade.SCAM_ARTIST));
    }

    static int discounted(int cost, int scamArtistTier) {
        return (int) Math.round(cost * (1 - SCAM_ARTIST_DISCOUNT * scamArtistTier));
    }

    /** キルの報酬 (アシストの元の値にも) に足す分。 */
    public void addBoosts(Player player, KillRewards.Boosts boosts) {
        UUID id = player.getUniqueId();
        boosts.addGold(GOLD_BOOST_PER_TIER * tier(id, RenownUpgrade.RENOWN_GOLD_BOOST));
        boosts.flatXp += tier(id, RenownUpgrade.RENOWN_XP_BUMP);
        if (tier(id, RenownUpgrade.CELEBRITY) > 0) {
            boosts.goldFinalMultiplier *= CELEBRITY_GOLD;
        }
        if (tier(id, RenownUpgrade.EXPERIENCE_INDUSTRIAL_COMPLEX) > 0) {
            boosts.streakXpCap = EIC_STREAK_XP_CAP;
            boosts.maxXp += EIC_MAX_XP;
        }
    }

    /** キルしたとき: Tenacity。 */
    public void onKill(Player killer) {
        int tenacity = tier(killer.getUniqueId(), RenownUpgrade.TENACITY);
        if (tenacity > 0 && !killer.isDead()) {
            killer.setHealth(Math.min(killer.getMaxHealth(), killer.getHealth() + TENACITY_HEAL * tenacity));
        }
    }

    /** Extra Hearts の分の最大体力。 */
    public double extraHealth(Player player) {
        return EXTRA_HEART * tier(player.getUniqueId(), RenownUpgrade.EXTRA_HEARTS);
    }

    /** Armory の分の矢。 */
    public int extraArrows(Player player) {
        return ARMORY_ARROWS * tier(player.getUniqueId(), RenownUpgrade.ARMORY);
    }

    /** スポーンしたとき (パークのアイテムの後): Fancy Hat。 */
    public void giveSpawnItems(Player player) {
        PlayerInventory inventory = player.getInventory();
        if (has(player, RenownUpgrade.FANCY_HAT) && inventory.getHelmet() == null
                && com.pitremake.prestige.FancyHatMenu.enabled(plugin, player)) {
            inventory.setHelmet(com.pitremake.prestige.FancyHatMenu.hat(plugin, player));
        }
    }

    /** 2 秒ごと (パークの更新から)。Impatient は別に 0.5 秒ごと。 */
    public void refresh(Player player) {
    }

    /** Impatient: スポーンの中で Speed II (1 秒。0.5 秒ごとに付け直すので、出ると 1 秒以内に切れる)。 */
    private void impatient(Player player) {
        if (has(player, RenownUpgrade.IMPATIENT) && plugin.getPitSpawn().isProtected(player.getLocation())) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, IMPATIENT_TICKS, 1, true, false), true);
        }
    }

    /** Divine Intervention で持ち物がそのまま残るか (死ぬたびに 1 回だけ引く)。 */
    public boolean rollDivineIntervention(Player player) {
        int tier = tier(player.getUniqueId(), RenownUpgrade.DIVINE_INTERVENTION);
        return tier > 0 && ThreadLocalRandom.current().nextDouble() < DIVINE_INTERVENTION_CHANCE * tier;
    }

    /**
     * Mysticism: キルで Mystic のアイテムが落ちるか引く (Tier I から)。落ちるならそのアイテム、落ちなければ null。
     * 確率は約 0.2% × (1 + Tier までの増加の合計 + Pants Radar + Uberdrop の Perma Mystic Chance 1% × 回数) (pit.wiki の Renown: 増加はどれも足し算)。
     * Tier IV までは剣と弓が半分ずつ、IV からは剣 30% / 弓 30% / パンツ 40%。
     */
    public ItemStack rollMysticDrop(Player killer) {
        int tier = tier(killer.getUniqueId(), RenownUpgrade.MYSTICISM);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        if (tier <= 0 || random.nextDouble() >= MYSTIC_DROP_CHANCE * (1 + mysticismBonus(tier) + pantsRadar(killer)
                + com.pitremake.special.Uberdrop.PERMA_MYSTIC_CHANCE * plugin.getUpgradeManager().getPermaMystic(killer.getUniqueId()))) {
            return null;
        }
        double roll = random.nextDouble();
        if (tier >= MYSTICISM_PANTS_TIER) {
            if (roll < 0.3) {
                return MysticItems.freshSword();
            }
            if (roll < 0.6) {
                return MysticItems.freshBow();
            }
            PantsColor[] colors = {PantsColor.RED, PantsColor.ORANGE, PantsColor.YELLOW, PantsColor.GREEN, PantsColor.BLUE};
            return MysticItems.freshPants(colors[random.nextInt(colors.length)]);
        }
        return roll < 0.5 ? MysticItems.freshSword() : MysticItems.freshBow();
    }

    /**
     * Pants Radar (+30% / +60% …): 手に持つ Mystic の武器と履いている Mystic Pants の分。
     * Mysticism の増加と足し算する (掛け算にはしない)。
     */
    private double pantsRadar(Player killer) {
        double total = 0;
        for (ItemStack item : new ItemStack[]{killer.getItemInHand(), killer.getInventory().getLeggings()}) {
            if (MysticItems.isMystic(item)) {
                Integer level = com.pitremake.item.MysticData.read(item).getEnchants().get("pants_radar");
                if (level != null) {
                    total += com.pitremake.combat.EnchantNumbers.percent(plugin.getEnchantRegistry().get("pants_radar"), level, 0);
                }
            }
        }
        return total;
    }

    /** Mysticism の Tier (1 から) までの増加の合計 (+30 / +20 / 0 / +20 / +30 / +25 / +25 / 0 / +50%)。 */
    public static double mysticismBonus(int tier) {
        return tier <= 0 ? 0 : MYSTICISM_BONUS[Math.min(tier, MYSTICISM_BONUS.length) - 1];
    }

    static double mysticDropChance(int tier) {
        return tier <= 0 ? 0 : MYSTIC_DROP_CHANCE * (1 + mysticismBonus(tier));
    }

    /** Yummy!: 金リンゴ・Golden Head を食べたとき +6g。 */
    public void onEat(Player player) {
        if (has(player, RenownUpgrade.YUMMY)) {
            plugin.getGoldManager().add(player.getUniqueId(), YUMMY_GOLD);
            plugin.getSidebar().update(player);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();
        if (item != null && item.getType() == Material.GOLDEN_APPLE) {
            onEat(event.getPlayer());
        }
    }
}
