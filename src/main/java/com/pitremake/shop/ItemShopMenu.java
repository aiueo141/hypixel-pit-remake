package com.pitremake.shop;

import com.pitremake.PitPlugin;
import com.pitremake.item.MysticItems;
import com.pitremake.renown.RenownUpgrade;
import com.pitremake.util.ClickMenu;
import com.pitremake.util.Menus;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Items の村人の画面 "Non-permanent items" (54)。録画 2026-10-14_08-53-15.mcrc のとおり、
 * 買えないアイテムは名前が赤で "Not enough gold!"、買うと "PURCHASE!" と音が鳴って画面を開き直す。
 * 53 は初期装備の剣と弓をもらうかの切り替え。値段は Renown の Scam Artist で安くなる。
 * <pre>
 * 並び     : ユーザーのスクリーンショットの位置 (ShopItem#getSlot)。Renown で解放していないものは出さない
 * 右クリック : Renown の Autobuy があれば Autobuy の印を切り替える (印のあるものはエンチャントの光沢と "Autobuy ON!")
 * 1 つだけ : 防具・剣・道具などは持っている間は買えない
 * </pre>
 */
public final class ItemShopMenu implements ClickMenu {

    /** 縦 6 × 横 9 (ユーザーの指定)。 */
    static final int SIZE = 54;
    /** 初期装備の切り替え: 一番下の右端 (45 個のときの録画では 44)。 */
    static final int TOGGLE_SLOT = 53;
    private static final DecimalFormat NUMBER = new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(Locale.US));

    private final PitPlugin plugin;
    private final Player player;
    private final Inventory inventory;
    private final Map<Integer, ShopItem> slots = new HashMap<>();

    public ItemShopMenu(PitPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, SIZE, "Non-permanent items");
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void open() {
        render();
        player.openInventory(inventory);
    }

    /** Scam Artist で安くなった値段。 */
    static int cost(PitPlugin plugin, Player player, ShopItem shopItem) {
        return plugin.getRenownEffects().discounted(player, shopItem.getCost());
    }

    /** その人が買えるアイテム (Renown Shop で解放したものも)。 */
    static List<ShopItem> available(PitPlugin plugin, Player player) {
        List<ShopItem> items = new ArrayList<>();
        for (ShopItem item : ShopItem.values()) {
            if (item.isDefault() || plugin.getUpgradeManager().isUnlocked(player.getUniqueId(), item.unlockKey())) {
                items.add(item);
            }
        }
        return items;
    }

    private void render() {
        inventory.clear();
        slots.clear();
        double gold = plugin.getGoldManager().get(player.getUniqueId());
        boolean autobuy = plugin.getUpgradeManager().hasRenown(player.getUniqueId(), RenownUpgrade.AUTOBUY);
        for (ShopItem shopItem : available(plugin, player)) {
            int slot = shopItem.getSlot();
            slots.put(slot, shopItem);
            int cost = cost(plugin, player, shopItem);
            boolean affordable = gold >= cost;
            // スクリーンショットの形: 説明 / (Autobuy ON!) / Lost on death. / Cost / Click to purchase!
            List<String> lore = new ArrayList<>(shopItem.getDescription());
            boolean marked = autobuy && plugin.getUpgradeManager().isAutobuy(player.getUniqueId(), shopItem.name());
            if (marked) {
                lore.add("");
                lore.add("§aAutobuy ON!");
            }
            lore.add("");
            if (shopItem != ShopItem.PANTS_BUNDLE) {
                lore.add("§7§oLost on death.");
            }
            lore.add("§7Cost: §6" + NUMBER.format(cost) + "g");
            lore.add(affordable ? "§eClick to purchase!" : "§cNot enough gold!");
            ItemStack icon = Menus.item(shopItem.getMaterial(), shopItem.getData(), shopItem.getAmount(),
                    (affordable ? "§e" : "§c") + shopItem.getDisplayName(), lore);
            if (marked) {
                ItemMeta meta = icon.getItemMeta();
                meta.addEnchant(Enchantment.DURABILITY, 1, true);
                icon.setItemMeta(meta);
            }
            inventory.setItem(slot, icon);
        }
        boolean enabled = plugin.getUpgradeManager().hasSpawnItems(player.getUniqueId());
        inventory.setItem(TOGGLE_SLOT, Menus.item(Material.IRON_SWORD, 0, 1, "§eToggle Spawn Items", Arrays.asList(
                "§7Choose whether you want to spawn",
                "§7with the default sword and bow.",
                "",
                "§7Enabled: " + (enabled ? "§aON" : "§cOFF"),
                "",
                "§eClick to toggle!")));
    }

    @Override
    public void click(int slot) {
        click(slot, false);
    }

    @Override
    public void click(int slot, boolean rightClick) {
        if (slot == TOGGLE_SLOT) {
            boolean enabled = !plugin.getUpgradeManager().hasSpawnItems(player.getUniqueId());
            plugin.getUpgradeManager().setSpawnItems(player.getUniqueId(), enabled);
            player.playSound(player.getLocation(), "random.click", 1.0f, 1.0f);
            render();
            return;
        }
        ShopItem shopItem = slots.get(slot);
        if (shopItem == null) {
            return;
        }
        if (rightClick && plugin.getUpgradeManager().hasRenown(player.getUniqueId(), RenownUpgrade.AUTOBUY)) {
            boolean on = plugin.getUpgradeManager().toggleAutobuy(player.getUniqueId(), shopItem.name());
            player.sendMessage("§a§lAUTOBUY! §6" + shopItem.getDisplayName() + (on ? " §aenabled" : " §cdisabled"));
            player.playSound(player.getLocation(), "random.click", 1.0f, on ? 1.5f : 0.8f);
            render();
            return;
        }
        buy(shopItem);
    }

    private void buy(ShopItem shopItem) {
        // 自分で買うときは何個でも買える (1 つだけなのは Autobuy のとき)
        if (!plugin.getGoldManager().take(player.getUniqueId(), cost(plugin, player, shopItem))) {
            // 録画のメッセージと音
            player.sendMessage("§cYou don't have enough gold to afford this!");
            player.playSound(player.getLocation(), "mob.villager.no", 1.0f, 1.0f);
            return;
        }
        deliver(plugin, player, shopItem);
        player.sendMessage("§a§lPURCHASE! §6" + shopItem.getDisplayName());
        player.playSound(player.getLocation(), "random.levelup", 0.55f, 2.0f);
        plugin.getSidebar().update(player);
        new ItemShopMenu(plugin, player).open();
    }

    /** そのショップのアイテムを持っている (着ている) か。Iron Pack は 3 つとも着ているとき。 */
    static boolean has(Player player, ShopItem shopItem) {
        PlayerInventory inventory = player.getInventory();
        List<ItemStack> all = new ArrayList<>(Arrays.asList(inventory.getContents()));
        all.addAll(Arrays.asList(inventory.getArmorContents()));
        if (shopItem == ShopItem.IRON_PACK) {
            int pieces = 0;
            for (ItemStack item : all) {
                if (ShopItem.of(item) == ShopItem.IRON_PACK) {
                    pieces++;
                }
            }
            return pieces >= 3;
        }
        for (ItemStack item : all) {
            if (ShopItem.of(item) == shopItem) {
                return true;
            }
        }
        return false;
    }

    /** 買ったアイテムを渡す (防具は "Auto-equips on buy!")。Barbarian なら Diamond Sword はすぐ斧に。 */
    static void deliver(PitPlugin plugin, Player player, ShopItem shopItem) {
        for (ItemStack item : shopItem.createAll()) {
            if (shopItem.isArmor()) {
                equip(player, item, shopItem == ShopItem.IRON_PACK);
            } else {
                for (ItemStack leftover : player.getInventory().addItem(item).values()) {
                    player.getWorld().dropItem(player.getLocation(), leftover);
                }
            }
        }
        plugin.getPerkManager().refresh(player);
    }

    /**
     * 防具の場所に着せ、元の防具はインベントリへ。onlyIfBetter (Iron Pack) のときは、空いている場所か
     * 鉄より弱いもの (革・チェーン。ミスティックパンツは除く) の場所だけに着せ、それ以外はインベントリへ。
     */
    private static void equip(Player player, ItemStack armor, boolean onlyIfBetter) {
        PlayerInventory inventory = player.getInventory();
        ItemStack old;
        String type = armor.getType().name();
        if (type.endsWith("_CHESTPLATE")) {
            old = inventory.getChestplate();
        } else if (type.endsWith("_LEGGINGS")) {
            old = inventory.getLeggings();
        } else {
            old = inventory.getBoots();
        }
        if (onlyIfBetter && old != null && old.getType() != Material.AIR
                && (MysticItems.isMystic(old) || !(old.getType().name().startsWith("LEATHER_") || old.getType().name().startsWith("CHAINMAIL_")))) {
            for (ItemStack leftover : inventory.addItem(armor).values()) {
                player.getWorld().dropItem(player.getLocation(), leftover);
            }
            return;
        }
        if (type.endsWith("_CHESTPLATE")) {
            inventory.setChestplate(armor);
        } else if (type.endsWith("_LEGGINGS")) {
            inventory.setLeggings(armor);
        } else {
            inventory.setBoots(armor);
        }
        if (old != null && old.getType() != Material.AIR) {
            for (ItemStack leftover : inventory.addItem(old).values()) {
                player.getWorld().dropItem(player.getLocation(), leftover);
            }
        }
    }
}
