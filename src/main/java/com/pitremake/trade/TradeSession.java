package com.pitremake.trade;

import com.pitremake.PitPlugin;
import com.pitremake.perk.PerkItems;
import com.pitremake.util.ClickMenu;
import com.pitremake.util.Menus;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * トレードの画面 "You                  Other" (27)。録画 2026-10-15_12-11-57.mcrc / 12-11-56.mcrc の配置と文字。
 * <pre>
 *  0  1  2  3 | 4 |  5  6  7  8      左 = 自分が出すもの、右 = 相手が出すもの (自分の n 番 → 相手の画面の n+5 番)
 *  9 10 11 12 |13 | 14 15 16 17      4 / 13 / 22 は区切りのガラス (⇦ Your stuff / Their stuff ⇨)
 * 18 19 20 21 |22 | 23 24 25 26      18 Gold transaction (看板で金額) / 21 確定の粘土 / 23 状態 (灰色の染料 → 相手が確定したら黄緑)
 * </pre>
 * 状態: New deal → (中身が変わると 3 秒) Deal timer... The trade changed recently. → Pending their confirm
 *       → 相手が確定 Other player confirmed! Waiting for you to confirm... → 2 人とも確定で TRADE COMPLETE!
 * 音  : 開く haggle (0.66 / 66)、変えた人 haggle (1.0 / 55)・相手 haggle (1.0 / 74)、確定した人 yes (1.0 / 56)・相手 yes (0.65 / 81)、
 *       成立 note.pling (0.6) の 35 / 44 / 53 / 71 を 2 tick あけて 2 回
 * 録画はゴールドだけのトレード。アイテムは自分のインベントリでクリックして出し、画面の自分の欄でクリックして戻す (推測)。
 * "Untradeable" のアイテムとパークのアイテムは出せない。画面を閉じる・死ぬ・抜けるとキャンセル (チャットは推測)。
 */
final class TradeSession {

    static final int[] OWN_SLOTS = {0, 1, 2, 3, 9, 10, 11, 12, 19, 20};
    static final int OTHER_OFFSET = 5;
    static final int[] DIVIDER_SLOTS = {4, 13, 22};
    static final int GOLD_SLOT = 18;
    static final int CONFIRM_SLOT = 21;
    static final int STATUS_SLOT = 23;
    /** 中身が変わってから確定できるまで (録画: Deal timer... が 3 秒)。 */
    static final long DEAL_TIMER_MILLIS = 3_000;
    private static final int[] COMPLETE_PITCHES = {35, 44, 53, 71};
    private static final String TITLE = "You                  Other";

    final class Side implements ClickMenu {
        final Player player;
        final List<ItemStack> items = new ArrayList<>();
        long gold;
        boolean confirmed;
        /** 看板で金額を入れている間 (画面を開き直しても閉じたことにしない)。 */
        boolean reopening;
        final Inventory inventory;

        Side(Player player) {
            this.player = player;
            this.inventory = Bukkit.createInventory(this, 27, TITLE);
        }

        Side other() {
            return this == a ? b : a;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }

        @Override
        public void click(int slot) {
            click(slot, false);
        }

        @Override
        public void click(int slot, boolean rightClick) {
            if (finished) {
                return;
            }
            if (slot == GOLD_SLOT) {
                askGold(this);
            } else if (slot == CONFIRM_SLOT) {
                confirm(this);
            } else {
                int index = indexOf(OWN_SLOTS, slot);
                if (index >= 0) {
                    takeBack(this, index);
                }
            }
        }

        @Override
        public void clickInventory(int slot, boolean rightClick) {
            if (!finished) {
                offer(this, slot);
            }
        }

        @Override
        public void close() {
            if (!finished && !reopening) {
                cancel(player);
            }
        }
    }

    private final TradeManager manager;
    private final PitPlugin plugin;
    final Side a;
    final Side b;
    /** 最後に中身が変わった時刻 (まだ変わっていなければ 0)。 */
    private long changedAt;
    private boolean finished;

    TradeSession(TradeManager manager, Player first, Player second) {
        this.manager = manager;
        this.plugin = manager.plugin();
        this.a = new Side(first);
        this.b = new Side(second);
    }

    void open() {
        for (Side side : new Side[]{a, b}) {
            side.player.closeInventory();
            side.player.playSound(side.player.getLocation(), "mob.villager.haggle", 0.66f, 66 / 63f);
        }
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (finished) {
                return;
            }
            for (Side side : new Side[]{a, b}) {
                render(side);
                side.player.openInventory(side.inventory);
            }
        }, 2);
    }

    // ------------------------------------------------------------------ 表示

    private void render(Side side) {
        Inventory inventory = side.inventory;
        inventory.clear();
        ItemStack divider = Menus.item(Material.STAINED_GLASS_PANE, 7, 1, "§7⇦ Your stuff", Collections.singletonList("§7Their stuff ⇨"));
        for (int slot : DIVIDER_SLOTS) {
            inventory.setItem(slot, divider);
        }
        List<ItemStack> mine = offers(side);
        List<ItemStack> theirs = offers(side.other());
        for (int i = 0; i < OWN_SLOTS.length; i++) {
            inventory.setItem(OWN_SLOTS[i], i < mine.size() ? mine.get(i) : null);
            inventory.setItem(OWN_SLOTS[i] + OTHER_OFFSET, i < theirs.size() ? theirs.get(i) : null);
        }
        inventory.setItem(GOLD_SLOT, Menus.item(Material.GOLD_NUGGET, 0, 1, "§6Gold transaction", Arrays.asList(
                "",
                "§7Daily limit: §a" + format(manager.dailyGold(side.player.getUniqueId())) + "§7/§6"
                        + format(TradeManager.DAILY_GOLD_LIMIT) + "g",
                "",
                "§eClick to add gold!")));
        inventory.setItem(CONFIRM_SLOT, Menus.item(Material.STAINED_CLAY, 13, 1, "§aTrading!", Arrays.asList(
                "§7Click an item in your inventory", "§7to offer it for trade.")));
        inventory.setItem(STATUS_SLOT, status(side));
    }

    /** 出しているもの (ゴールドがあれば先頭に §6100g / Lump-sum amount)。 */
    private static List<ItemStack> offers(Side side) {
        List<ItemStack> list = new ArrayList<>();
        if (side.gold > 0) {
            list.add(Menus.item(Material.GOLD_INGOT, 0, 1, "§6" + format(side.gold) + "g",
                    Collections.singletonList("§7Lump-sum amount")));
        }
        for (ItemStack item : side.items) {
            list.add(item.clone());
        }
        return list;
    }

    private ItemStack status(Side side) {
        String trading = "§7Trading with " + manager.name(side.other().player) + "§7.";
        if (changedAt > 0 && System.currentTimeMillis() - changedAt < DEAL_TIMER_MILLIS) {
            return Menus.item(Material.INK_SACK, 8, 1, "§eDeal timer...", Arrays.asList(trading, "§7The trade changed recently."));
        }
        if (side.other().confirmed) {
            return Menus.item(Material.INK_SACK, 10, 1, "§aOther player confirmed!", Arrays.asList(trading, "§7Waiting for you to confirm..."));
        }
        if (changedAt == 0) {
            return Menus.item(Material.INK_SACK, 8, 1, "§eNew deal", Collections.singletonList(trading));
        }
        return Menus.item(Material.INK_SACK, 8, 1, "§ePending their confirm", Arrays.asList(trading, "§7Waiting for them to confirm..."));
    }

    private void renderBoth() {
        render(a);
        render(b);
    }

    // ------------------------------------------------------------------ 操作

    /** 自分のインベントリのアイテムを出す。 */
    private void offer(Side side, int inventorySlot) {
        ItemStack item = side.player.getInventory().getItem(inventorySlot);
        if (item == null || item.getType() == Material.AIR) {
            return;
        }
        if (!tradeable(item)) {
            side.player.sendMessage("§cYou can't trade this item!");
            side.player.playSound(side.player.getLocation(), "mob.villager.no", 1.0f, 1.0f);
            return;
        }
        if (offers(side).size() >= OWN_SLOTS.length) {
            side.player.sendMessage("§cYou can't offer any more items!");
            side.player.playSound(side.player.getLocation(), "mob.villager.no", 1.0f, 1.0f);
            return;
        }
        side.player.getInventory().setItem(inventorySlot, null);
        side.items.add(item);
        changed(side);
    }

    /** 画面の自分の欄をクリック: 出したものを戻す (ゴールドなら 0g に)。 */
    private void takeBack(Side side, int index) {
        int itemIndex = side.gold > 0 ? index - 1 : index;
        if (side.gold > 0 && index == 0) {
            side.gold = 0;
            changed(side);
            return;
        }
        if (itemIndex < 0 || itemIndex >= side.items.size()) {
            return;
        }
        give(side.player, side.items.remove(itemIndex));
        changed(side);
    }

    /** Gold transaction: 看板で金額を入れる (録画: "" / ^^^^^^ / Enter amount / ----------------)。 */
    private void askGold(Side side) {
        side.reopening = true;
        plugin.getPacketTap().openSign(side.player, new String[]{"", "^^^^^^", "Enter amount", "----------------"}, lines -> {
            if (finished || !side.player.isOnline()) {
                return;
            }
            long amount = parse(lines.length > 0 ? lines[0] : "");
            long have = (long) Math.floor(plugin.getGoldManager().get(side.player.getUniqueId()));
            long left = TradeManager.DAILY_GOLD_LIMIT - manager.dailyGold(side.player.getUniqueId());
            if (amount > have) {
                side.player.sendMessage("§cYou don't have enough gold!");
                side.player.playSound(side.player.getLocation(), "mob.villager.no", 1.0f, 1.0f);
            } else if (amount > left) {
                side.player.sendMessage("§cThat would go over your daily gold limit!");
                side.player.playSound(side.player.getLocation(), "mob.villager.no", 1.0f, 1.0f);
            } else if (amount != side.gold) {
                side.gold = amount;
                changed(side);
            }
            render(side);
            side.player.openInventory(side.inventory);
            side.reopening = false;
        });
    }

    /** 中身が変わった: 2 人の確定を取り消して Deal timer を始める。 */
    private void changed(Side side) {
        changedAt = System.currentTimeMillis();
        a.confirmed = false;
        b.confirmed = false;
        side.player.playSound(side.player.getLocation(), "mob.villager.haggle", 1.0f, 55 / 63f);
        Side other = side.other();
        other.player.playSound(other.player.getLocation(), "mob.villager.haggle", 1.0f, 74 / 63f);
        renderBoth();
        long stamp = changedAt;
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!finished && changedAt == stamp) {
                renderBoth();
            }
        }, DEAL_TIMER_MILLIS / 50 + 1);
    }

    private void confirm(Side side) {
        if (side.confirmed) {
            return;
        }
        if (changedAt == 0 || System.currentTimeMillis() - changedAt < DEAL_TIMER_MILLIS) {
            // 何も出していない・変わったばかりのときは確定できない
            side.player.playSound(side.player.getLocation(), "mob.villager.no", 1.0f, 1.0f);
            return;
        }
        side.confirmed = true;
        side.player.playSound(side.player.getLocation(), "mob.villager.yes", 1.0f, 56 / 63f);
        Side other = side.other();
        other.player.playSound(other.player.getLocation(), "mob.villager.yes", 0.65f, 81 / 63f);
        if (other.confirmed) {
            complete();
        } else {
            renderBoth();
        }
    }

    private void complete() {
        // 払えるか確かめてから渡す
        for (Side side : new Side[]{a, b}) {
            if (plugin.getGoldManager().get(side.player.getUniqueId()) < side.gold) {
                cancel(null);
                a.player.sendMessage("§c§lTRADE CANCELLED! §7Not enough gold.");
                b.player.sendMessage("§c§lTRADE CANCELLED! §7Not enough gold.");
                return;
            }
        }
        finished = true;
        manager.ended(this);
        for (Side side : new Side[]{a, b}) {
            Side other = side.other();
            if (side.gold > 0) {
                // 稼いだゴールドではないので、プレステージの条件 (grinded) には数えない
                plugin.getGoldManager().set(side.player.getUniqueId(), plugin.getGoldManager().get(side.player.getUniqueId()) - side.gold);
                plugin.getGoldManager().set(other.player.getUniqueId(), plugin.getGoldManager().get(other.player.getUniqueId()) + side.gold);
                manager.addDailyGold(side.player.getUniqueId(), side.gold);
            }
            for (ItemStack item : side.items) {
                give(other.player, item);
            }
        }
        for (Side side : new Side[]{a, b}) {
            side.player.closeInventory();
            side.player.sendMessage("§a§lTRADE COMPLETE! §7with " + manager.name(side.other().player) + "§7!");
            for (int i = 0; i < 2; i++) {
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (side.player.isOnline()) {
                        for (int pitch : COMPLETE_PITCHES) {
                            side.player.playSound(side.player.getLocation(), "note.pling", 0.6f, pitch / 63f);
                        }
                    }
                }, i * 2L);
            }
            if (plugin.getSidebar() != null) {
                plugin.getSidebar().update(side.player);
            }
        }
    }

    void cancel(Player who) {
        cancel(who, null);
    }

    /**
     * キャンセル: 出したものを持ち主に返す。who はやめた人 (null なら理由なし)。
     * dead は死んだ人 (死んでいる間は渡さず、リスポーンしてから返す)。
     */
    void cancel(Player who, Player dead) {
        if (finished) {
            return;
        }
        finished = true;
        manager.ended(this);
        for (Side side : new Side[]{a, b}) {
            if (side.player.equals(dead)) {
                manager.returnAfterRespawn(side.player.getUniqueId(), new ArrayList<>(side.items));
            } else {
                for (ItemStack item : side.items) {
                    give(side.player, item);
                }
            }
            side.items.clear();
            if (side.player.isOnline()) {
                if (side.player.getOpenInventory().getTopInventory().getHolder() == side) {
                    side.player.closeInventory();
                }
                if (who != null) {
                    side.player.sendMessage(side.player.equals(who) ? "§c§lTRADE CANCELLED! §7You cancelled the trade."
                            : "§c§lTRADE CANCELLED! §7" + manager.name(who) + " §7cancelled the trade.");
                }
            }
        }
    }

    // ------------------------------------------------------------------ 小物

    /** "Untradeable" と書かれたアイテムとパークのアイテムは出せない。 */
    static boolean tradeable(ItemStack item) {
        if (PerkItems.of(item) != null) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasLore()) {
            for (String line : meta.getLore()) {
                if (ChatColor.stripColor(line).toLowerCase(Locale.ROOT).contains("untradeable")) {
                    return false;
                }
            }
        }
        return true;
    }

    /** インベントリに入れる (いっぱいなら足元。抜けるときの PlayerQuitEvent の中でも入れれば保存される)。 */
    static void give(Player player, ItemStack item) {
        for (ItemStack left : player.getInventory().addItem(item).values()) {
            player.getWorld().dropItem(player.getLocation(), left);
        }
    }

    static long parse(String text) {
        String digits = text == null ? "" : text.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return 0;
        }
        return digits.length() > 12 ? Long.MAX_VALUE / 2 : Long.parseLong(digits);
    }

    static String format(long amount) {
        return String.format(Locale.US, "%,d", amount);
    }

    private static int indexOf(int[] slots, int slot) {
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == slot) {
                return i;
            }
        }
        return -1;
    }
}
