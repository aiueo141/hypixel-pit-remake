package com.pitremake.auction;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** オークションに出す 1 つの品。 */
public final class AuctionOffer {

    private final String name;
    private final String kind;
    private final ItemStack icon;
    private final List<String> description;
    private final long minimumBid;
    private final Consumer<Player> delivery;

    /**
     * @param name        チャットの名前 (例: §eTier III Yellow Pants)
     * @param kind        画面の 1 行目 (録画: §8Mystic item)
     * @param icon        画面に出すアイテム
     * @param description 名前の下の説明 (ミスティックなら Lore そのまま)
     * @param delivery    落札した人に渡す処理
     */
    AuctionOffer(String name, String kind, ItemStack icon, List<String> description, long minimumBid, Consumer<Player> delivery) {
        this.name = name;
        this.kind = kind;
        this.icon = icon;
        this.description = description;
        this.minimumBid = minimumBid;
        this.delivery = delivery;
    }

    public String getName() {
        return name;
    }

    public long getMinimumBid() {
        return minimumBid;
    }

    public void deliver(Player player) {
        delivery.accept(player);
    }

    /** 画面の Auction Offer の説明 (録画: §8Mystic item / 空行 / 線 / 名前 / Lore / 線)。 */
    public List<String> lore() {
        List<String> lore = new ArrayList<>();
        lore.add(kind);
        lore.add("");
        lore.add("§e§m*-----------------*");
        lore.add(name);
        lore.addAll(description);
        lore.add("§e§m*-----------------*");
        return lore;
    }

    /** 画面の 11 番 (§eAuction Offer)。 */
    public ItemStack icon() {
        ItemStack item = icon.clone();
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§eAuction Offer");
        meta.setLore(lore());
        item.setItemMeta(meta);
        return item;
    }
}
