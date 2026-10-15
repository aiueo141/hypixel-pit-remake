package com.pitremake.special;

import com.pitremake.PitPlugin;
import com.pitremake.item.MysticData;
import com.pitremake.item.MysticItems;
import com.pitremake.item.RewardItems;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mystic Repair Kit を右クリックしたときの画面 (Gem の Totally Legit Selector と同じ形)。pit.wiki/Mystic_Repair_Kit:
 * インベントリ (と履いているパンツ) の Lives が減っているミスティックを並べ、選んだものの Lives を最大にして Kit を 1 つ使う。
 * Golden Helmet / Archangel Chestplate / Armageddon Boots は直せない。画面の名前・チャット・音は録画に無いので推測。
 * 選んだら Passive の購入と同じ確認画面 (Are you sure? / 11 Confirm / 15 Cancel) を出す。
 */
public final class RepairKitMenu implements ClickMenu {

    /** 履いているパンツの位置。 */
    private static final int LEGGINGS = -1;
    private static final String REPAIR_SOUND = "random.anvil_use";

    private final PitPlugin plugin;
    private final Player player;
    private final Inventory inventory;
    /** 画面のスロット → プレイヤーのアイテムの位置。 */
    private final Map<Integer, Integer> targets = new HashMap<>();

    public RepairKitMenu(PitPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 45, "Mystic Repair Kit");
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    /** 直せるアイテムが 1 つも無ければ開かずに false を返す。 */
    public boolean open() {
        int slot = 0;
        List<Integer> positions = new ArrayList<>();
        for (int i = 0; i < 36; i++) {
            positions.add(i);
        }
        positions.add(LEGGINGS);
        for (int position : positions) {
            ItemStack item = getItem(position);
            if (!isRepairable(item) || slot >= inventory.getSize()) {
                continue;
            }
            inventory.setItem(slot, icon(item));
            targets.put(slot, position);
            slot++;
        }
        if (targets.isEmpty()) {
            return false;
        }
        player.openInventory(inventory);
        return true;
    }

    /** Lives が最大より少ないミスティック (Golden Helmet などは除く)。 */
    static boolean isRepairable(ItemStack item) {
        if (!MysticItems.isMystic(item)) {
            return false;
        }
        for (RewardItems.Type type : RewardItems.Type.values()) {
            if (RewardItems.is(item, type)) {
                return false;
            }
        }
        MysticData data = MysticData.read(item);
        return data.getMaxLives() > 0 && data.getLives() < data.getMaxLives();
    }

    private static ItemStack icon(ItemStack item) {
        ItemStack icon = item.clone();
        ItemMeta meta = icon.getItemMeta();
        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<String>();
        lore.add("");
        lore.add(ChatColor.YELLOW + "Click to repair!");
        meta.setLore(lore);
        icon.setItemMeta(meta);
        return icon;
    }

    private ItemStack getItem(int position) {
        return position == LEGGINGS ? player.getInventory().getLeggings() : player.getInventory().getItem(position);
    }

    private void setItem(int position, ItemStack item) {
        if (position == LEGGINGS) {
            player.getInventory().setLeggings(item);
        } else {
            player.getInventory().setItem(position, item);
        }
    }

    @Override
    public void click(int slot) {
        Integer position = targets.get(slot);
        if (position == null) {
            return;
        }
        if (!isRepairable(getItem(position))) {
            player.closeInventory();
            return;
        }
        new Confirm(position).open();
    }

    /** 確認画面 (UpgradeMenu の Passive の購入確認と同じ配置: 11 Confirm / 15 Cancel)。 */
    private final class Confirm implements ClickMenu {

        private static final int CONFIRM_SLOT = 11;
        private static final int CANCEL_SLOT = 15;

        private final int position;
        private final Inventory confirm;

        Confirm(int position) {
            this.position = position;
            this.confirm = Bukkit.createInventory(this, 27, "Are you sure?");
        }

        @Override
        public Inventory getInventory() {
            return confirm;
        }

        void open() {
            ItemStack item = getItem(position);
            MysticData data = MysticData.read(item);
            confirm.setItem(CONFIRM_SLOT, Menus.item(Material.STAINED_CLAY, 13, 1, "§aConfirm", Arrays.asList(
                    "§7Repairing: " + name(item),
                    "§7Lives: §c" + data.getLives() + " §7➟ §a" + data.getMaxLives(),
                    "§7Cost: §d1 Mystic Repair Kit")));
            confirm.setItem(CANCEL_SLOT, Menus.item(Material.STAINED_CLAY, 14, 1, "§cCancel",
                    Collections.singletonList("§7§7Return to previous menu.")));
            player.openInventory(confirm);
        }

        @Override
        public void click(int slot) {
            if (slot == CONFIRM_SLOT) {
                repair(position);
            } else if (slot == CANCEL_SLOT) {
                if (!new RepairKitMenu(plugin, player).open()) {
                    player.closeInventory();
                }
            }
        }
    }

    private static String name(ItemStack item) {
        return item.hasItemMeta() && item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : "item";
    }

    /** 確認画面の Confirm: Lives を最大にして Kit を 1 つ使う。 */
    private void repair(int position) {
        ItemStack item = getItem(position);
        if (!isRepairable(item) || !takeKit()) {
            player.closeInventory();
            return;
        }
        MysticData data = MysticData.read(item);
        data.setLives(data.getMaxLives());
        ItemStack repaired = MysticItems.applyDisplay(data.write(item), plugin.getEnchantRegistry());
        setItem(position, repaired);
        player.closeInventory();
        player.sendMessage("§d§lREPAIRED! §7" + name(repaired) + " §7now has §a" + data.getMaxLives() + " §7lives!");
        player.playSound(player.getLocation(), REPAIR_SOUND, 1.0f, 1.0f);
    }

    /** Kit を 1 つ使う (手に持っているものを優先)。無ければ false。 */
    private boolean takeKit() {
        ItemStack hand = player.getItemInHand();
        if (SpecialItems.isRepairKit(hand)) {
            if (hand.getAmount() > 1) {
                hand.setAmount(hand.getAmount() - 1);
            } else {
                player.setItemInHand(null);
            }
            return true;
        }
        for (int i = 0; i < 36; i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (SpecialItems.isRepairKit(item)) {
                if (item.getAmount() > 1) {
                    item.setAmount(item.getAmount() - 1);
                } else {
                    player.getInventory().setItem(i, null);
                }
                return true;
            }
        }
        return false;
    }
}
