package com.pitremake.auction;

import com.pitremake.util.ClickMenu;
import com.pitremake.util.Menus;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * "Auction Event" の画面 (27)。録画 2026-10-15_11-11-03.mcrc の配置と文字。
 * <pre>
 * 11 Auction Offer (品そのもの) / 13 Bid History (地図) / 15 Participate (金の馬鎧)
 * Participate: 左クリックで入札。右クリックは「New bid (最低額)」と「Custom bid (看板で入力)」の切り替え
 *   入札が無いうち : Minimum bid / Time left / 返金の説明 / 足りなければ §cNot enough gold!
 *   入札があれば   : Top bid / By / Time left / New bid か Custom bid / 返金の説明 / §bRight-click to … / 状態
 * </pre>
 * 状態の行は §cBid too small! (最高額以下) → §cNot enough gold! の順 (録画)。入札できるときの §eClick to bid! と、
 * 自分が最高額のときの §aYou are the top bidder! (このときは入札も金額の切り替えもできない) は推測。
 */
final class AuctionMenu implements ClickMenu {

    static final int OFFER_SLOT = 11;
    static final int HISTORY_SLOT = 13;
    static final int PARTICIPATE_SLOT = 15;

    private final AuctionManager manager;
    private final Player player;
    private final Inventory inventory;

    AuctionMenu(AuctionManager manager, Player player) {
        this.manager = manager;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 27, "Auction Event");
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    Player getPlayer() {
        return player;
    }

    void open() {
        render();
        player.openInventory(inventory);
    }

    /** 中身を今の状態に書き換える (毎秒と入札のたびに呼ばれる)。 */
    void render() {
        AuctionManager.Auction auction = manager.getAuction();
        if (auction == null) {
            return;
        }
        inventory.setItem(OFFER_SLOT, auction.offer.icon());
        inventory.setItem(HISTORY_SLOT, Menus.item(Material.MAP, 0, 1, "§eBid History", history(auction)));
        inventory.setItem(PARTICIPATE_SLOT, Menus.item(Material.GOLD_BARDING, 0, 1, "§6Participate", participate(auction)));
    }

    private static List<String> history(AuctionManager.Auction auction) {
        if (auction.bids.isEmpty()) {
            return Arrays.asList("§7No bids have been placed on this", "§7item yet.", "", "§7Be the first to bid on it!");
        }
        List<String> lore = new ArrayList<>();
        lore.add("§7Total bids: §a" + auction.bids.size() + " bids");
        long now = System.currentTimeMillis();
        for (int i = auction.bids.size() - 1; i >= 0; i--) {
            AuctionManager.Bid bid = auction.bids.get(i);
            lore.add("§8§m------------");
            lore.add("§7Bid: §6§6" + AuctionRules.gold(bid.amount));
            lore.add("§7By: " + bid.name);
            lore.add("§b" + AuctionRules.ago(now - bid.time));
        }
        return lore;
    }

    private List<String> participate(AuctionManager.Auction auction) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        String time = "§7Time left: §a" + AuctionRules.time(auction.secondsLeft());
        AuctionManager.Bid top = auction.top();
        Long custom = auction.customBids.get(player.getUniqueId());
        long amount = manager.bidAmount(player);
        if (top == null) {
            lore.add("§7Minimum bid: §6" + AuctionRules.gold(auction.offer.getMinimumBid()));
            lore.add(time);
        } else {
            lore.add("§7Top bid: §6" + AuctionRules.gold(top.amount));
            lore.add("§7By: " + top.name);
            lore.add(time);
            lore.add("");
            lore.add(custom == null ? "§7New bid: §6" + AuctionRules.gold(amount) : "§7Custom bid: §6" + AuctionRules.gold(amount));
        }
        lore.add("");
        lore.add("§7§8You will be reimbursed at the");
        lore.add("§8end if you lose the auction.");
        lore.add("");
        if (top != null && !top.player.equals(player.getUniqueId())) {
            lore.add(custom == null ? "§bRight-click to set amount!" : "§bRight-click to use minimum!");
        }
        lore.add(manager.status(player, amount));
        return lore;
    }

    @Override
    public void click(int slot) {
        click(slot, false);
    }

    @Override
    public void click(int slot, boolean rightClick) {
        if (slot != PARTICIPATE_SLOT || manager.getAuction() == null) {
            return;
        }
        if (!rightClick) {
            manager.bid(player);
            return;
        }
        manager.toggleCustomBid(player);
    }

    @Override
    public void close() {
        manager.closed(this);
    }
}
