package com.pitremake.auction;

import com.pitremake.PitPlugin;
import com.pitremake.enchant.list.PantsEnchants;
import com.pitremake.item.MysticData;
import com.pitremake.item.MysticItems;
import com.pitremake.item.MysticType;
import com.pitremake.item.PantsColor;
import com.pitremake.special.SpecialItems;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.function.LongFunction;

/**
 * オークションの品を決める (品の種類は pit.wiki/Auction)。重みと最低入札額は config の auction.offers.&lt;キー&gt; で変えられる
 * (weight / price。Vile・Feather・Renown の price は 1 個 (1 Renown) あたり)。書かれていなければ下の初期値。
 * <pre>
 * キー            品                                              重み  最低入札額
 * tier3-sword     Tier III Mystic Sword (Mystic Well と同じ抽選)     14    4,500g
 * tier3-bow       Tier III Mystic Bow                              10    4,500g
 * tier3-pants     Tier III Mystic Pants                            14    4,500g   ← 録画 (Tier III Yellow Pants at 4,500g)
 * auction-pants   Tier I パンツ (Pit MBA / Fractional Reserve /     10   10,000g
 *                   Paparazzi I、Lives 5 / 10 / 15)
 * cactus          Philosopher's Cactus 1 個                         8    2,000g
 * vile            Chunk of Vile 3〜6 個                            10      500g × 個数
 * fresh-pants     Fresh Pants 2 本 (普通の 5 色)                     10    1,000g
 * renown          Renown 2〜8                                      10      750g × Renown
 * diamond-armor   ダイヤの防具一式                                    8    1,500g
 * feather         Funky Feather 1〜5 個 (まれ)                       3    1,500g × 個数
 * repair-kit      Mystic Repair Kit (とてもまれ)                     1   15,000g
 * </pre>
 * 確率と最低入札額は録画の 1 回 (Tier III パンツ 4,500g) 以外は推測。
 */
final class AuctionOffers {

    private final PitPlugin plugin;
    private final Random random = new Random();

    private static final class Entry {
        final String key;
        final int weight;
        final long price;
        /** 引数は config の price (最低入札額、または 1 個あたり)。 */
        final LongFunction<AuctionOffer> factory;

        Entry(String key, int weight, long price, LongFunction<AuctionOffer> factory) {
            this.key = key;
            this.weight = weight;
            this.price = price;
            this.factory = factory;
        }
    }

    private final List<Entry> entries = new ArrayList<>();

    AuctionOffers(PitPlugin plugin) {
        this.plugin = plugin;
        entries.add(new Entry("tier3-sword", 14, 4_500, price -> tierThree(MysticItems.freshSword(), MysticType.SWORD, price)));
        entries.add(new Entry("tier3-bow", 10, 4_500, price -> tierThree(MysticItems.freshBow(), MysticType.BOW, price)));
        entries.add(new Entry("tier3-pants", 14, 4_500, price -> tierThree(MysticItems.freshPants(randomColor()), MysticType.PANTS, price)));
        entries.add(new Entry("auction-pants", 10, 10_000, this::auctionPants));
        entries.add(new Entry("cactus", 8, 2_000, price -> items("§aPhilosopher's Cactus", "§8Special item", price, SpecialItems.cactus(1))));
        entries.add(new Entry("vile", 10, 500, price -> {
            int amount = 3 + random.nextInt(4);
            return items("§5" + amount + "x Chunk of Vile", "§8Special item", price * amount, SpecialItems.vile(amount));
        }));
        entries.add(new Entry("fresh-pants", 10, 1_000, this::freshPants));
        entries.add(new Entry("renown", 10, 750, this::renown));
        entries.add(new Entry("diamond-armor", 8, 1_500, price -> items("§bFull Diamond Armor", "§8Armor", price,
                new ItemStack(Material.DIAMOND_HELMET), new ItemStack(Material.DIAMOND_CHESTPLATE),
                new ItemStack(Material.DIAMOND_LEGGINGS), new ItemStack(Material.DIAMOND_BOOTS))));
        entries.add(new Entry("feather", 3, 1_500, price -> {
            int amount = 1 + random.nextInt(5);
            return items("§3" + amount + "x Funky Feather", "§8Special item", price * amount, SpecialItems.feather(amount));
        }));
        entries.add(new Entry("repair-kit", 1, 15_000, price -> items("§dMystic Repair Kit", "§8Special item", price, SpecialItems.repairKit(1))));
    }

    private int weight(Entry entry) {
        return Math.max(0, plugin.getConfig().getInt("auction.offers." + entry.key + ".weight", entry.weight));
    }

    private long price(Entry entry) {
        return Math.max(1, plugin.getConfig().getLong("auction.offers." + entry.key + ".price", entry.price));
    }

    AuctionOffer random() {
        int total = 0;
        for (Entry entry : entries) {
            total += weight(entry);
        }
        if (total <= 0) {
            return entries.get(0).factory.apply(price(entries.get(0)));
        }
        int roll = random.nextInt(total);
        for (Entry entry : entries) {
            roll -= weight(entry);
            if (roll < 0) {
                return entry.factory.apply(price(entry));
            }
        }
        return entries.get(0).factory.apply(price(entries.get(0)));
    }

    private PantsColor randomColor() {
        List<PantsColor> colors = PantsColor.standardColors();
        return colors.get(random.nextInt(colors.size()));
    }

    /** Fresh から Mystic Well と同じ抽選で Tier III にする。 */
    private AuctionOffer tierThree(ItemStack fresh, MysticType type, long price) {
        MysticData data = MysticData.read(fresh);
        for (int i = 0; i < 3; i++) {
            plugin.getWellRoller().upgrade(data, type);
        }
        return mystic(MysticItems.applyDisplay(data.write(fresh), plugin.getEnchantRegistry()), price);
    }

    /** Pit MBA / Fractional Reserve / Paparazzi I の Tier I パンツ (Lives 5 / 10 / 15)。 */
    private AuctionOffer auctionPants(long price) {
        ItemStack fresh = MysticItems.freshPants(randomColor());
        MysticData data = MysticData.read(fresh);
        List<String> keys = new ArrayList<>(PantsEnchants.AUCTION_ONLY);
        Collections.sort(keys);
        data.setEnchant(keys.get(random.nextInt(keys.size())), 1);
        int lives = 5 * (1 + random.nextInt(3));
        data.setTier(1);
        data.setMaxLives(lives);
        data.setLives(lives);
        return mystic(MysticItems.applyDisplay(data.write(fresh), plugin.getEnchantRegistry()), price);
    }

    private static AuctionOffer mystic(ItemStack item, long minimum) {
        String name = item.getItemMeta().getDisplayName();
        List<String> lore = item.getItemMeta().hasLore() ? item.getItemMeta().getLore() : Collections.<String>emptyList();
        return new AuctionOffer(name, "§8Mystic item", item, lore, minimum, player -> give(player, item.clone()));
    }

    private AuctionOffer freshPants(long price) {
        PantsColor color = randomColor();
        ItemStack first = MysticItems.freshPants(color);
        ItemStack second = MysticItems.freshPants(color);
        ItemStack icon = first.clone();
        icon.setAmount(2);
        String name = color.getChatColor() + "2x Fresh " + color.getDisplayName() + " Pants";
        List<String> lore = first.getItemMeta().hasLore() ? first.getItemMeta().getLore() : Collections.<String>emptyList();
        return new AuctionOffer(name, "§8Mystic item", icon, lore, price, player -> {
            give(player, first.clone());
            give(player, second.clone());
        });
    }

    private AuctionOffer renown(long price) {
        int amount = 2 + random.nextInt(7);
        ItemStack icon = new ItemStack(Material.BEACON);
        return new AuctionOffer("§e" + amount + " Renown", "§8Renown", icon,
                Arrays.asList("§7Spend it in the Renown shop."), price * amount, player -> {
            UUID id = player.getUniqueId();
            plugin.getStatsManager().setRenown(id, plugin.getStatsManager().getRenown(id) + amount);
        });
    }

    private static AuctionOffer items(String name, String kind, long minimum, ItemStack... items) {
        ItemStack icon = items[0].clone();
        List<String> lore = icon.hasItemMeta() && icon.getItemMeta().hasLore()
                ? icon.getItemMeta().getLore() : Collections.<String>emptyList();
        return new AuctionOffer(name, kind, icon, lore, minimum, player -> {
            for (ItemStack item : items) {
                give(player, item.clone());
            }
        });
    }

    /** インベントリに入れる (いっぱいなら足元に落とす)。 */
    static void give(Player player, ItemStack item) {
        for (ItemStack left : player.getInventory().addItem(item).values()) {
            player.getWorld().dropItem(player.getLocation(), left);
        }
    }
}
