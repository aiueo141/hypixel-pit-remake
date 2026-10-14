package com.pitremake.prestige;

import com.pitremake.PitPlugin;
import com.pitremake.perk.PerkItems;
import com.pitremake.util.ClickMenu;
import com.pitremake.util.Menus;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * "Fancy Hat Color" (36)。録画 2026-10-14_21-17-47.mcrc の配置・説明文。
 * <pre>
 * 10〜16 : Suggested color (赤 / オレンジ / 黄 / 緑 / 青 / ピンク / 紫の革のヘルメット)。色を変えるたびに 2,000g
 * 20     : Custom Color (看板)。HEX の色 (チャットに #RRGGBB を入力) も 2,000g
 * 22     : Enable / Disable Glint (赤 / 緑の色付きガラス)。Pit Supporter だけ (ここでは pit.supporter の権限)
 * 24     : Enable / Disable Fancy Hat (赤 / 緑の色付き粘土)
 * 31     : 戻る (Renown Shop - Upgrades)
 * </pre>
 * 色・光沢・かぶるかは保存され、スポーンしたとき (と今かぶっている Fancy Hat) に反映される。
 * 色を変えたときのメッセージは録画に無いので推測。
 */
public final class FancyHatMenu implements ClickMenu {

    private static final int SIZE = 36;
    private static final int CUSTOM_SLOT = 20;
    private static final int GLINT_SLOT = 22;
    private static final int TOGGLE_SLOT = 24;
    private static final int BACK_SLOT = 31;
    private static final int RECOLOR_COST = 2_000;
    static final String HAT_OFF = "fancyhat-off";
    static final String HAT_GLINT = "fancyhat-glint";
    private static final String SUPPORTER_PERMISSION = "pit.supporter";
    private static final DecimalFormat NUMBER = new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(Locale.US));

    /** 録画の Suggested color (名前の色と染料の色)。 */
    private enum Suggested {
        RED(10, "§cRed", 0xFF0000),
        ORANGE(11, "§6Orange", 0xFFA500),
        YELLOW(12, "§eYellow", 0xFFFF00),
        GREEN(13, "§aGreen", 0x008000),
        BLUE(14, "§9Blue", 0x0000FF),
        PINK(15, "§dPink", 0xFF00FF),
        PURPLE(16, "§5Purple", 0x800080);

        final int slot;
        final String name;
        final Color color;

        Suggested(int slot, String name, int rgb) {
            this.slot = slot;
            this.name = name;
            this.color = Color.fromRGB(rgb);
        }
    }

    /** チャットで色の入力を待っている人。 */
    private static final Set<UUID> AWAITING = ConcurrentHashMap.newKeySet();

    private final PitPlugin plugin;
    private final Player player;
    private final Inventory inventory;

    public FancyHatMenu(PitPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, SIZE, "Fancy Hat Color");
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void open() {
        render();
        player.openInventory(inventory);
    }

    private boolean affordable() {
        return plugin.getGoldManager().get(player.getUniqueId()) >= RECOLOR_COST;
    }

    private void render() {
        inventory.clear();
        boolean affordable = affordable();
        for (Suggested suggested : Suggested.values()) {
            ItemStack hat = Menus.item(Material.LEATHER_HELMET, 0, 1, (affordable ? "§e" : "§c") + "Suggested color", Arrays.asList(
                    "§7Color: " + suggested.name,
                    "§7Recolor cost: §6" + NUMBER.format(RECOLOR_COST) + "g",
                    "",
                    affordable ? "§eClick to recolor!" : "§cNot enough gold!"));
            LeatherArmorMeta meta = (LeatherArmorMeta) hat.getItemMeta();
            meta.setColor(suggested.color);
            hat.setItemMeta(meta);
            inventory.setItem(suggested.slot, hat);
        }
        inventory.setItem(CUSTOM_SLOT, Menus.item(Material.SIGN, 0, 1, "§eCustom Color", Arrays.asList(
                "§7Use a HEX color.",
                "",
                affordable ? "§eClick to recolor!" : "§cNot enough gold!")));
        boolean glint = plugin.getUpgradeManager().isUnlocked(player.getUniqueId(), HAT_GLINT);
        boolean supporter = player.hasPermission(SUPPORTER_PERMISSION);
        inventory.setItem(GLINT_SLOT, Menus.item(Material.STAINED_GLASS, glint ? 13 : 14, 1,
                glint ? "§cDisable Glint" : "§aEnable Glint", Arrays.asList(
                        glint ? "§7Currently using Glint" : "§7Currently NOT using Glint",
                        "",
                        !supporter ? "§cRequires Pit Supporter status!" : glint ? "§eClick to disable!" : "§eClick to enable!")));
        boolean enabled = !plugin.getUpgradeManager().isUnlocked(player.getUniqueId(), HAT_OFF);
        inventory.setItem(TOGGLE_SLOT, Menus.item(Material.STAINED_CLAY, enabled ? 13 : 14, 1,
                enabled ? "§cDisable Fancy Hat" : "§aEnable Fancy Hat", Arrays.asList(
                        enabled ? "§7Currently using Fancy Hat" : "§7Currently NOT using Fancy Hat",
                        "",
                        enabled ? "§eClick to disable!" : "§eClick to enable!")));
        inventory.setItem(BACK_SLOT, Menus.item(Material.ARROW, 0, 1, "§aGo Back",
                Collections.singletonList("§7To Renown Shop - Upgrades")));
    }

    static String hex(Color color) {
        return String.format(Locale.US, "#%06X", color.asRGB());
    }

    @Override
    public void click(int slot) {
        if (slot == BACK_SLOT) {
            new RenownShopMenu(plugin, player).openUpgrades(com.pitremake.renown.RenownUpgrade.FANCY_HAT.getPage());
            return;
        }
        if (slot == TOGGLE_SLOT) {
            plugin.getUpgradeManager().toggleFlag(player.getUniqueId(), HAT_OFF);
            refreshHat(plugin, player);
            player.playSound(player.getLocation(), "random.click", 1.0f, 1.0f);
            render();
            return;
        }
        if (slot == GLINT_SLOT) {
            if (!player.hasPermission(SUPPORTER_PERMISSION)) {
                deny("§cThis requires Pit Supporter status!");
                return;
            }
            plugin.getUpgradeManager().toggleFlag(player.getUniqueId(), HAT_GLINT);
            refreshHat(plugin, player);
            player.playSound(player.getLocation(), "random.click", 1.0f, 1.0f);
            render();
            return;
        }
        if (slot == CUSTOM_SLOT) {
            if (!affordable()) {
                deny("§cYou don't have enough gold to afford this!");
                return;
            }
            AWAITING.add(player.getUniqueId());
            player.closeInventory();
            player.sendMessage("§b§lFANCY HAT! §7Type a HEX color in chat as §f#RRGGBB §7(or §ccancel§7).");
            return;
        }
        for (Suggested suggested : Suggested.values()) {
            if (suggested.slot == slot) {
                recolor(plugin, player, suggested.color);
                render();
                return;
            }
        }
    }

    private void deny(String message) {
        player.sendMessage(message);
        player.playSound(player.getLocation(), "mob.villager.no", 1.0f, 1.0f);
    }

    /** 2,000g 払って色を変える (足りなければ何もしない)。 */
    static void recolor(PitPlugin plugin, Player player, Color color) {
        if (!plugin.getGoldManager().take(player.getUniqueId(), RECOLOR_COST)) {
            player.sendMessage("§cYou don't have enough gold to afford this!");
            player.playSound(player.getLocation(), "mob.villager.no", 1.0f, 1.0f);
            return;
        }
        plugin.getUpgradeManager().setHatColor(player.getUniqueId(), color);
        refreshHat(plugin, player);
        plugin.getSidebar().update(player);
        player.sendMessage("§b§lFANCY HAT! §7Recolored to §f" + hex(color) + "§7!");
        player.playSound(player.getLocation(), "random.levelup", 0.55f, 2.0f);
    }

    /** そのプレイヤーの Fancy Hat (色・光沢つき)。 */
    public static ItemStack hat(PitPlugin plugin, Player player) {
        ItemStack hat = PerkItems.fancyHat(plugin.getUpgradeManager().getHatColor(player.getUniqueId()));
        if (plugin.getUpgradeManager().isUnlocked(player.getUniqueId(), HAT_GLINT) && player.hasPermission(SUPPORTER_PERMISSION)) {
            ItemMeta meta = hat.getItemMeta();
            meta.addEnchant(Enchantment.DURABILITY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            hat.setItemMeta(meta);
        }
        return hat;
    }

    /** かぶるか (Disable Fancy Hat していないか)。 */
    public static boolean enabled(PitPlugin plugin, Player player) {
        return !plugin.getUpgradeManager().isUnlocked(player.getUniqueId(), HAT_OFF);
    }

    /** 今かぶっている Fancy Hat を設定に合わせる (外す・かぶる・色)。 */
    static void refreshHat(PitPlugin plugin, Player player) {
        ItemStack helmet = player.getInventory().getHelmet();
        boolean wearingHat = PerkItems.of(helmet) == PerkItems.Type.FANCY_HAT;
        if (!enabled(plugin, player)) {
            if (wearingHat) {
                player.getInventory().setHelmet(null);
            }
            return;
        }
        if (wearingHat || helmet == null || helmet.getType() == Material.AIR) {
            player.getInventory().setHelmet(hat(plugin, player));
        }
    }

    /** #RRGGBB (# は省略可) を色にする。読めなければ null。 */
    static Color parse(String text) {
        String value = text.trim();
        if (value.startsWith("#")) {
            value = value.substring(1);
        }
        if (!value.matches("[0-9a-fA-F]{6}")) {
            return null;
        }
        return Color.fromRGB(Integer.parseInt(value, 16));
    }

    /** チャットの色の入力。 */
    public static final class ChatInput implements Listener {

        private final PitPlugin plugin;

        public ChatInput(PitPlugin plugin) {
            this.plugin = plugin;
        }

        @EventHandler(priority = EventPriority.LOWEST)
        public void onChat(AsyncPlayerChatEvent event) {
            Player player = event.getPlayer();
            if (!AWAITING.remove(player.getUniqueId())) {
                return;
            }
            event.setCancelled(true);
            String message = event.getMessage();
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (message.trim().equalsIgnoreCase("cancel")) {
                    player.sendMessage("§cCancelled.");
                    return;
                }
                Color color = parse(message);
                if (color == null) {
                    player.sendMessage("§cThat's not a HEX color! Use §f#RRGGBB §c(example: §f#FF5555§c).");
                    player.playSound(player.getLocation(), "mob.villager.no", 1.0f, 1.0f);
                    return;
                }
                recolor(plugin, player, color);
            });
        }

        @EventHandler
        public void onQuit(PlayerQuitEvent event) {
            AWAITING.remove(event.getPlayer().getUniqueId());
        }
    }
}
