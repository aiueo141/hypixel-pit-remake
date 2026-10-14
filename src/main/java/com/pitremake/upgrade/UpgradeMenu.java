package com.pitremake.upgrade;

import com.pitremake.PitPlugin;
import com.pitremake.gold.GoldManager;
import com.pitremake.perk.Perk;
import com.pitremake.perk.PerkItems;
import com.pitremake.perk.PerkRules;
import com.pitremake.renown.RenownUpgrade;
import com.pitremake.stats.PrestigeRules;
import com.pitremake.util.ClickMenu;
import com.pitremake.util.RomanNumerals;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Upgrades の村人の画面 (録画 2026-10-14_01-08-56 / 08-53-15 / UpgradesExtraUpgradeless.mcrc と同じ題名・配置)。
 * <pre>
 * Permanent upgrades (45) : Extra Perk Slot あり 11 Killstreaks / 12〜15 Perk Slot #1〜#4
 *                           なし          12〜14 Perk Slot #1〜#3 / 15 Killstreaks
 *                           28〜34 Passive (レベルが足りない間は "Unknown upgrade")
 * Killstreaks (27)        : Extra Killstreak あり 10 / 12 / 14 Perk Slot / 16 メガストリーク
 *                           なし            11 / 13 Perk Slot / 15 メガストリーク / 22 戻る
 * Choose a killstreak (27): 10〜16 メガストリーク (14 は Magnum Opus で未実装) / 22 戻る
 * Choose a killstreak (54): キルストリークの枠に入れるもの ({@link Killstreak}) / 49 戻る
 * Choose a perk           : 見えるパーク (Renown パークはそのプレステージから) を 10 から順に並べ、行数で大きさが変わる
 *                           (2 行なら 36: 31 戻る / 32 No perk、4 行なら 54: 49 / 50)。レベルが足りないものは "Unknown perk"
 * </pre>
 * パークは録画の通り: 買うと確認画面 → その枠に入れて Permanent upgrades へ、
 * 選ぶ・外すと note.pling (音量 8、高さ 2) で Permanent upgrades へ。どれかの枠に入っているパークは "Already selected!"。
 * 買えない理由のメッセージは録画に無いので推測。
 */
public final class UpgradeMenu implements ClickMenu {

    enum Page {
        MAIN("Permanent upgrades", 45),
        KILLSTREAKS("Killstreaks", 27),
        MEGASTREAKS("Choose a killstreak", 27),
        /** キルストリークの枠に入れるキルストリーク (録画 2026-10-14_20-50-01.mcrc)。 */
        KILLSTREAK_PICK("Choose a killstreak", 54),
        /** 大きさはパークの数で決める。 */
        PERKS("Choose a perk", 0),
        /** 買う前の確認 (録画 2026-10-14_08-53-15.mcrc)。 */
        CONFIRM("Are you sure?", 27);

        final String title;
        final int size;

        Page(String title, int size) {
            this.title = title;
            this.size = size;
        }
    }

    static final int BACK = 22;
    static final int CONFIRM_SLOT = 11;
    static final int CANCEL_SLOT = 15;
    /** Choose a perk でパークを並べる位置 (7 個ずつ 4 行)。 */
    private static final int[] PERK_GRID = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43};
    /** キルストリークの枠が使えるレベル (#2 は録画の 75、#3 は Extra Killstreak で 100)。 */
    private static final int[] KILLSTREAK_SLOT_LEVELS = {10, 75, 100};
    /** Choose a killstreak (54) の戻る。 */
    private static final int KILLSTREAK_PICK_BACK = 49;
    // 録画の音: 買えない = 村人の声、選択済み = エンダーマンのワープ、選択 = ノートブロック、購入 = レベルアップ (高い音)
    private static final String DENY_SOUND = "mob.villager.no";
    private static final String ALREADY_SELECTED_SOUND = "mob.endermen.portal";
    private static final String SELECT_SOUND = "note.pling";
    private static final String BUY_SOUND = "random.levelup";
    private static final float PERK_SELECT_VOLUME = 8.0f;
    private static final DecimalFormat NUMBER = new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(Locale.US));
    private static final String NOT_ENOUGH_GOLD = "§cYou don't have enough gold to afford this!";

    private final PitPlugin plugin;
    private final Player player;
    private final Page page;
    private final Inventory inventory;
    /** Choose a perk に並べるパーク (並べる順)。 */
    private final List<Perk> shownPerks;
    /** 確認画面で買おうとしているもの (どれか)。 */
    private Passive pendingPassive;
    private Megastreak pendingMegastreak;
    private Perk pendingPerk;
    private Killstreak pendingKillstreak;
    /** Choose a killstreak (54) で選んでいるキルストリークの枠 (0 から)。 */
    private int killstreakSlot = -1;
    /** Choose a perk / パークの確認画面で選んでいる Perk Slot (0 から)。 */
    private int perkSlot = -1;

    public UpgradeMenu(PitPlugin plugin, Player player) {
        this(plugin, player, Page.MAIN);
    }

    private UpgradeMenu(PitPlugin plugin, Player player, Page page) {
        this.plugin = plugin;
        this.player = player;
        this.page = page;
        this.shownPerks = page == Page.PERKS ? shownPerks(plugin, player) : Collections.<Perk>emptyList();
        int size = page == Page.PERKS ? perkMenuSize(shownPerks.size()) : page.size;
        this.inventory = Bukkit.createInventory(this, size, page.title);
    }

    private static UpgradeMenu confirm(PitPlugin plugin, Player player, Passive passive, Megastreak megastreak) {
        UpgradeMenu menu = new UpgradeMenu(plugin, player, Page.CONFIRM);
        menu.pendingPassive = passive;
        menu.pendingMegastreak = megastreak;
        return menu;
    }

    private static UpgradeMenu killstreaks(PitPlugin plugin, Player player, int slot) {
        UpgradeMenu menu = new UpgradeMenu(plugin, player, Page.KILLSTREAK_PICK);
        menu.killstreakSlot = slot;
        return menu;
    }

    private static UpgradeMenu confirmKillstreak(PitPlugin plugin, Player player, Killstreak killstreak, int slot) {
        UpgradeMenu menu = new UpgradeMenu(plugin, player, Page.CONFIRM);
        menu.pendingKillstreak = killstreak;
        menu.killstreakSlot = slot;
        return menu;
    }

    private static UpgradeMenu perks(PitPlugin plugin, Player player, int perkSlot) {
        UpgradeMenu menu = new UpgradeMenu(plugin, player, Page.PERKS);
        menu.perkSlot = perkSlot;
        return menu;
    }

    private static UpgradeMenu confirmPerk(PitPlugin plugin, Player player, Perk perk, int perkSlot) {
        UpgradeMenu menu = new UpgradeMenu(plugin, player, Page.CONFIRM);
        menu.pendingPerk = perk;
        menu.perkSlot = perkSlot;
        return menu;
    }

    /** 普通のパークと、プレステージが届いている Renown パーク (enum の順)。 */
    private static List<Perk> shownPerks(PitPlugin plugin, Player player) {
        int prestige = plugin.getStatsManager().getPrestige(player.getUniqueId());
        List<Perk> perks = new ArrayList<>();
        for (Perk perk : Perk.values()) {
            if (!perk.isRenownPerk() || prestige >= perk.getRequiredPrestige()) {
                perks.add(perk);
            }
        }
        return perks;
    }

    /** 上下に 1 行ずつ空けて、パーク 7 個で 1 行 (録画: 14 個で 36、28 個で 54)。 */
    static int perkMenuSize(int perks) {
        int rows = Math.max(1, (perks + 6) / 7);
        return (rows + 2) * 9;
    }

    private int perkBackSlot() {
        return inventory.getSize() - 5;
    }

    private int noPerkSlot() {
        return inventory.getSize() - 4;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void open() {
        render();
        player.openInventory(inventory);
    }

    private UUID id() {
        return player.getUniqueId();
    }

    private boolean hasRenown(RenownUpgrade upgrade) {
        return plugin.getUpgradeManager().hasRenown(id(), upgrade);
    }

    /** Permanent upgrades の Perk Slot の位置 (Extra Perk Slot が無ければ 3 つ)。 */
    private int[] mainPerkSlots() {
        return hasRenown(RenownUpgrade.EXTRA_PERK_SLOT) ? new int[]{12, 13, 14, 15} : new int[]{12, 13, 14};
    }

    private int killstreaksButton() {
        return hasRenown(RenownUpgrade.EXTRA_PERK_SLOT) ? 11 : 15;
    }

    /** Killstreaks のキルストリークの枠 (Extra Killstreak が無ければ 2 つ)。 */
    private int[] killstreakSlots() {
        return hasRenown(RenownUpgrade.EXTRA_KILLSTREAK) ? new int[]{10, 12, 14} : new int[]{11, 13};
    }

    private int megastreakButton() {
        return hasRenown(RenownUpgrade.EXTRA_KILLSTREAK) ? 16 : 15;
    }

    private void render() {
        inventory.clear();
        switch (page) {
            case MAIN:
                // アイコンは選んでいるメガストリークのアイコン (ユーザーの指定)
                inventory.setItem(killstreaksButton(), item(selected().getIcon(), 0, 1, "§aKillstreaks", Arrays.asList(
                        "§7Choose killstreak perks which",
                        "§7trigger every time you get X kills.",
                        "",
                        "§7Megastreak§7: §a" + selected().getDisplayName(),
                        "",
                        "§eClick to edit killstreaks!")));
                int[] perkSlots = mainPerkSlots();
                for (int i = 0; i < perkSlots.length; i++) {
                    inventory.setItem(perkSlots[i], perkSlotItem(i));
                }
                for (Passive passive : Passive.values()) {
                    inventory.setItem(passive.getSlot(), passiveItem(passive));
                }
                break;
            case PERKS:
                for (int i = 0; i < shownPerks.size() && i < PERK_GRID.length; i++) {
                    inventory.setItem(PERK_GRID[i], perkItem(shownPerks.get(i)));
                }
                inventory.setItem(perkBackSlot(), back("§7To Permanent upgrades"));
                if (currentPerk() != null) {
                    inventory.setItem(noPerkSlot(), item(Material.DIAMOND_BLOCK, 0, 1, "§cNo perk", Arrays.asList(
                            "§7Are you hardcore enough that you",
                            "§7don't need any perk for this slot?",
                            "",
                            "§eClick to remove perk!")));
                }
                break;
            case KILLSTREAKS:
                int[] slots = killstreakSlots();
                for (int i = 0; i < slots.length; i++) {
                    inventory.setItem(slots[i], killstreakSlotItem(i));
                }
                Megastreak megastreak = selected();
                List<String> lore = new ArrayList<>();
                lore.add("§7Selected: §a" + megastreak.getDisplayName());
                lore.add("");
                lore.addAll(megastreak.getLore());
                lore.add("");
                lore.add("§eClick to switch megastreak!");
                inventory.setItem(megastreakButton(), item(megastreak.getIcon(), 0, slots.length + 1, "§eMegastreak", lore));
                inventory.setItem(BACK, back("§7To Permanent upgrades"));
                break;
            case MEGASTREAKS:
                for (Megastreak option : Megastreak.values()) {
                    inventory.setItem(option.getSlot(), megastreakItem(option));
                }
                inventory.setItem(BACK, back("§7To Killstreaks"));
                break;
            case KILLSTREAK_PICK:
                for (int[] header : Killstreak.HEADERS) {
                    inventory.setItem(header[0], item(Material.ITEM_FRAME, 0, 1, "§c" + header[1] + " Kills", Collections.<String>emptyList()));
                }
                for (Killstreak option : Killstreak.values()) {
                    inventory.setItem(option.getSlot(), killstreakItem(option));
                }
                inventory.setItem(KILLSTREAK_PICK_BACK, back("§7To Killstreaks"));
                break;
            case CONFIRM:
                inventory.setItem(CONFIRM_SLOT, item(Material.STAINED_CLAY, 13, 1, "§aConfirm", Arrays.asList(
                        "§7Purchasing: §6" + pendingName(),
                        "§7Cost: §6" + GoldManager.formatExact(pendingCost()))));
                inventory.setItem(CANCEL_SLOT, item(Material.STAINED_CLAY, 14, 1, "§cCancel",
                        Collections.singletonList("§7§7Return to previous menu.")));
                break;
            default:
                break;
        }
    }

    private String pendingName() {
        if (pendingPerk != null) {
            return pendingPerk.getDisplayName();
        }
        if (pendingKillstreak != null) {
            return pendingKillstreak.getDisplayName();
        }
        if (pendingPassive != null) {
            return pendingPassive.getDisplayName() + " "
                    + RomanNumerals.of(plugin.getUpgradeManager().getTier(id(), pendingPassive) + 1);
        }
        return pendingMegastreak.getDisplayName();
    }

    private int pendingCost() {
        if (pendingPerk != null) {
            return pendingPerk.getCost();
        }
        if (pendingKillstreak != null) {
            return pendingKillstreak.getCost();
        }
        if (pendingPassive != null) {
            return pendingPassive.cost(plugin.getUpgradeManager().getTier(id(), pendingPassive) + 1);
        }
        return pendingMegastreak.getCost();
    }

    @Override
    public void click(int slot) {
        if (page == Page.CONFIRM) {
            if (slot == CONFIRM_SLOT) {
                completePurchase();
            } else if (slot == CANCEL_SLOT) {
                if (pendingPerk != null) {
                    perks(plugin, player, perkSlot).open();
                } else if (pendingKillstreak != null) {
                    killstreaks(plugin, player, killstreakSlot).open();
                } else {
                    new UpgradeMenu(plugin, player, pendingPassive != null ? Page.MAIN : Page.MEGASTREAKS).open();
                }
            }
            return;
        }
        switch (page) {
            case MAIN:
                if (slot == killstreaksButton()) {
                    new UpgradeMenu(plugin, player, Page.KILLSTREAKS).open();
                    return;
                }
                int[] perkSlots = mainPerkSlots();
                for (int i = 0; i < perkSlots.length; i++) {
                    if (perkSlots[i] == slot) {
                        openPerkSlot(i);
                        return;
                    }
                }
                for (Passive passive : Passive.values()) {
                    if (passive.getSlot() == slot) {
                        buy(passive);
                    }
                }
                break;
            case KILLSTREAKS:
                if (slot == megastreakButton()) {
                    new UpgradeMenu(plugin, player, Page.MEGASTREAKS).open();
                } else if (slot == BACK) {
                    new UpgradeMenu(plugin, player, Page.MAIN).open();
                } else {
                    int[] slots = killstreakSlots();
                    for (int i = 0; i < slots.length; i++) {
                        if (slots[i] == slot) {
                            openKillstreakSlot(i);
                        }
                    }
                }
                break;
            case KILLSTREAK_PICK:
                if (slot == KILLSTREAK_PICK_BACK) {
                    new UpgradeMenu(plugin, player, Page.KILLSTREAKS).open();
                    return;
                }
                for (Killstreak option : Killstreak.values()) {
                    if (option.getSlot() == slot) {
                        choose(option);
                    }
                }
                break;
            case MEGASTREAKS:
                if (slot == BACK) {
                    new UpgradeMenu(plugin, player, Page.KILLSTREAKS).open();
                    return;
                }
                for (Megastreak option : Megastreak.values()) {
                    if (option.getSlot() == slot) {
                        choose(option);
                    }
                }
                break;
            case PERKS:
                if (slot == perkBackSlot()) {
                    new UpgradeMenu(plugin, player, Page.MAIN).open();
                    return;
                }
                if (slot == noPerkSlot() && currentPerk() != null) {
                    plugin.getUpgradeManager().setPerk(id(), perkSlot, null);
                    perksChanged();
                    return;
                }
                for (int i = 0; i < shownPerks.size() && i < PERK_GRID.length; i++) {
                    if (PERK_GRID[i] == slot) {
                        choose(shownPerks.get(i));
                    }
                }
                break;
            default:
                break;
        }
    }

    private Perk currentPerk() {
        return perkSlot < 0 ? null : plugin.getUpgradeManager().getPerk(id(), perkSlot);
    }

    /** The Way (Renown) があればパーク・Perk Slot・キルストリーク・キルストリークの枠・メガストリークのレベル制限は無い。 */
    private boolean levelOk(int required) {
        return level() >= required || hasRenown(RenownUpgrade.THE_WAY);
    }

    private void openPerkSlot(int slot) {
        int required = PerkRules.slotLevel(slot);
        if (!levelOk(required)) {
            deny("§cYou need to be level " + required + " to use this perk slot!");
            return;
        }
        perks(plugin, player, slot).open();
    }

    private void choose(Perk perk) {
        UpgradeManager upgrades = plugin.getUpgradeManager();
        UUID id = id();
        if (upgrades.ownsPerk(id, perk)) {
            if (upgrades.hasPerk(id, perk)) {
                player.sendMessage("§cThis perk is already selected!");
                player.playSound(player.getLocation(), ALREADY_SELECTED_SOUND, 1.0f, 0.5f);
                return;
            }
            upgrades.setPerk(id, perkSlot, perk);
            perksChanged();
            return;
        }
        if (!upgrades.isPerkUnlocked(id, perk)) {
            deny("§cUnlock this perk in the Renown shop first!");
            return;
        }
        if (!levelOk(perk.getRequiredLevel())) {
            deny("§cYou need to be level " + perk.getRequiredLevel() + " to unlock this!");
            return;
        }
        if (!perk.isAvailable()) {
            deny("§cThis perk is not available yet!");
            return;
        }
        if (plugin.getGoldManager().get(id) < perk.getCost()) {
            deny(NOT_ENOUGH_GOLD);
            return;
        }
        confirmPerk(plugin, player, perk, perkSlot).open();
    }

    /** パークを入れた・外した後: 録画の音で Permanent upgrades に戻る。 */
    private void perksChanged() {
        player.playSound(player.getLocation(), SELECT_SOUND, PERK_SELECT_VOLUME, 2.0f);
        plugin.getMegastreakManager().refresh(player);
        new UpgradeMenu(plugin, player, Page.MAIN).open();
    }

    private Megastreak selected() {
        return plugin.getUpgradeManager().getMegastreak(id());
    }

    private int level() {
        return plugin.getStatsManager().getLevel(id());
    }

    private int prestige() {
        return plugin.getStatsManager().getPrestige(id());
    }

    private void buy(Passive passive) {
        UpgradeManager upgrades = plugin.getUpgradeManager();
        int tier = upgrades.getTier(id(), passive) + 1;
        if (tier > upgrades.maxPassiveTier(id())) {
            deny("§cYou already unlocked the last tier!");
            return;
        }
        if (level() < passive.requiredLevel(tier)) {
            deny("§cYou need to be level " + passive.requiredLevel(tier) + " to purchase this!");
            return;
        }
        if (plugin.getGoldManager().get(id()) < passive.cost(tier)) {
            deny(NOT_ENOUGH_GOLD);
            return;
        }
        confirm(plugin, player, passive, null).open();
    }

    private void choose(Megastreak megastreak) {
        UpgradeManager upgrades = plugin.getUpgradeManager();
        if (megastreak == selected()) {
            // 録画のメッセージと音
            player.sendMessage("§cThis killstreak is already selected!");
            player.playSound(player.getLocation(), ALREADY_SELECTED_SOUND, 1.0f, 0.5f);
            return;
        }
        int streak = plugin.getStatsManager().getStreak(id());
        if (MegastreakRules.isActive(selected(), streak)) {
            deny("§cYou can't switch megastreak while it is active!");
            return;
        }
        if (upgrades.owns(id(), megastreak)) {
            upgrades.selectMegastreak(id(), megastreak);
            player.playSound(player.getLocation(), SELECT_SOUND, 1.0f, 2.0f);
            plugin.getMegastreakManager().refresh(player);
            new UpgradeMenu(plugin, player, Page.KILLSTREAKS).open();
            return;
        }
        if (!megastreak.isDefault() && !upgrades.isUnlocked(id(), megastreak.unlockKey())) {
            deny("§cUnlock this killstreak in the Renown shop first!");
            return;
        }
        if (!levelOk(megastreak.getRequiredLevel())) {
            deny("§cYou need to be level " + megastreak.getRequiredLevel() + " to unlock this!");
            return;
        }
        if (plugin.getGoldManager().get(id()) < megastreak.getCost()) {
            deny(NOT_ENOUGH_GOLD);
            return;
        }
        confirm(plugin, player, null, megastreak).open();
    }

    /** 確認画面で Confirm を押したとき。 */
    private void completePurchase() {
        UpgradeManager upgrades = plugin.getUpgradeManager();
        String name = pendingName();
        if (!plugin.getGoldManager().take(id(), pendingCost())) {
            deny(NOT_ENOUGH_GOLD);
            player.closeInventory();
            return;
        }
        Page next;
        if (pendingPerk != null) {
            upgrades.buyPerk(id(), pendingPerk);
            upgrades.setPerk(id(), perkSlot, pendingPerk);
            plugin.getMegastreakManager().refresh(player);
            next = Page.MAIN;
        } else if (pendingPassive != null) {
            upgrades.setTier(id(), pendingPassive, upgrades.getTier(id(), pendingPassive) + 1);
            next = Page.MAIN;
        } else if (pendingKillstreak != null) {
            upgrades.buyKillstreak(id(), pendingKillstreak);
            putKillstreak(pendingKillstreak, killstreakSlot);
            next = Page.KILLSTREAKS;
        } else {
            upgrades.unlock(id(), pendingMegastreak);
            upgrades.selectMegastreak(id(), pendingMegastreak);
            plugin.getMegastreakManager().refresh(player);
            next = Page.KILLSTREAKS;
        }
        player.sendMessage("§a§lPURCHASE! §6" + name);
        player.playSound(player.getLocation(), BUY_SOUND, 0.55f, 2.0f);
        plugin.getSidebar().update(player);
        new UpgradeMenu(plugin, player, next).open();
    }

    private void deny(String message) {
        player.sendMessage(message);
        player.playSound(player.getLocation(), DENY_SOUND, 1.0f, 1.0f);
    }

    private ItemStack megastreakItem(Megastreak megastreak) {
        boolean owned = plugin.getUpgradeManager().owns(id(), megastreak);
        List<String> lore = new ArrayList<>(megastreak.getLore());
        lore.add("");
        if (megastreak == selected()) {
            lore.add("§aAlready selected!");
        } else if (owned) {
            lore.add("§eClick to select!");
        } else {
            lore.add("§7Cost: §6" + NUMBER.format(megastreak.getCost()) + "g");
            if (!megastreak.isDefault() && !plugin.getUpgradeManager().isUnlocked(id(), megastreak.unlockKey())) {
                lore.add("§cUnlock in the Renown Shop!");
            } else if (!levelOk(megastreak.getRequiredLevel())) {
                lore.add("§cRequires level " + megastreak.getRequiredLevel() + "!");
            } else {
                lore.add(plugin.getGoldManager().get(id()) < megastreak.getCost() ? "§cNot enough gold!" : "§eClick to purchase!");
            }
        }
        return item(megastreak.getIcon(), 0, 1, (owned ? "§a" : "§c") + megastreak.getDisplayName(), lore);
    }

    /**
     * Killstreaks のキルストリークの枠 (録画: 使える枠は金ブロック、使えない枠は岩盤)。
     * 入っているときはそのキルストリークのアイコン (Perk Slot の選択中と同じ形。録画に無いので推測)。
     */
    private ItemStack killstreakSlotItem(int slot) {
        int number = slot + 1;
        int required = KILLSTREAK_SLOT_LEVELS[slot];
        if (!levelOk(required)) {
            return item(Material.BEDROCK, 0, number, "§cPerk Slot #" + number,
                    Collections.singletonList("§7Required level: " + PrestigeRules.levelTag(prestige(), required)));
        }
        Killstreak killstreak = plugin.getUpgradeManager().getKillstreak(id(), slot);
        if (killstreak == null) {
            return item(Material.GOLD_BLOCK, 0, number, "§aPerk Slot #" + number, Arrays.asList(
                    "§7Select a killstreak for this slot.",
                    "",
                    "§eClick to select killstreak!"));
        }
        List<String> lore = new ArrayList<>();
        lore.add("§7Selected: §a" + killstreak.getDisplayName());
        lore.add("");
        lore.add("§7Every: §c" + killstreak.getEvery() + " kills");
        lore.add("");
        lore.addAll(killstreakDescription(killstreak));
        lore.add("");
        lore.add("§eClick to select killstreak!");
        return item(killstreak.getIcon(), killstreak.getData(), number, "§ePerk Slot #" + number, lore);
    }

    private void openKillstreakSlot(int slot) {
        int required = KILLSTREAK_SLOT_LEVELS[slot];
        if (!levelOk(required)) {
            deny("§cYou need to be level " + required + " to use this killstreak slot!");
            return;
        }
        killstreaks(plugin, player, slot).open();
    }

    /** 説明文 (Gold Stack / XP Stack の上限はプレステージで変わる)。 */
    private List<String> killstreakDescription(Killstreak killstreak) {
        DecimalFormat format = new DecimalFormat("0.#", DecimalFormatSymbols.getInstance(Locale.US));
        List<String> lines = new ArrayList<>();
        for (String line : killstreak.getDescription()) {
            lines.add(line.replace("{goldmax}", format.format(KillstreakManager.goldStackMax(prestige())))
                    .replace("{xpmax}", format.format(KillstreakManager.xpStackMax(prestige()))));
        }
        return lines;
    }

    /**
     * Choose a killstreak (54) の 1 つ (録画の形):
     * 買えない = §c名前 / Cost / Not enough gold!、Renown で未解放 = 岩盤 / Unlocked in Renown shop! / Bundle: …、
     * 持っている = §a名前 / Click to select!、この枠に入っている = 光る / Already selected!
     */
    private ItemStack killstreakItem(Killstreak killstreak) {
        UpgradeManager upgrades = plugin.getUpgradeManager();
        boolean owned = upgrades.ownsKillstreak(id(), killstreak);
        boolean locked = killstreak.bundleKey() != null && !upgrades.isUnlocked(id(), killstreak.bundleKey());
        List<String> lore = new ArrayList<>();
        lore.add("§7Every: §c" + killstreak.getEvery() + " kills");
        lore.add("");
        lore.addAll(killstreakDescription(killstreak));
        lore.add("");
        boolean here = killstreakSlot >= 0 && upgrades.getKillstreak(id(), killstreakSlot) == killstreak;
        String color;
        if (here) {
            lore.add("§aAlready selected!");
            color = "§a";
        } else if (owned) {
            lore.add("§eClick to select!");
            color = "§a";
        } else {
            lore.add("§7Cost: §6" + NUMBER.format(killstreak.getCost()) + "g");
            if (locked) {
                lore.add("§cUnlocked in Renown shop!");
                lore.add("§cBundle: " + killstreak.bundleName());
            } else if (!levelOk(killstreak.getRequiredLevel())) {
                lore.add("§cRequired level: " + PrestigeRules.levelTag(prestige(), killstreak.getRequiredLevel()));
            } else {
                lore.add(plugin.getGoldManager().get(id()) < killstreak.getCost() ? "§cNot enough gold!" : "§eClick to purchase!");
            }
            color = "§c";
        }
        ItemStack icon = locked && !owned ? item(Material.BEDROCK, 0, 1, color + killstreak.getDisplayName(), lore)
                : item(killstreak.getIcon(), killstreak.getData(), killstreak.getAmount(), color + killstreak.getDisplayName(), lore);
        if (here) {
            ItemMeta meta = icon.getItemMeta();
            meta.addEnchant(org.bukkit.enchantments.Enchantment.DURABILITY, 1, true);
            icon.setItemMeta(meta);
        }
        return icon;
    }

    private void choose(Killstreak killstreak) {
        UpgradeManager upgrades = plugin.getUpgradeManager();
        if (upgrades.getKillstreak(id(), killstreakSlot) == killstreak) {
            player.sendMessage("§cThis killstreak is already selected!");
            player.playSound(player.getLocation(), ALREADY_SELECTED_SOUND, 1.0f, 0.5f);
            return;
        }
        if (upgrades.ownsKillstreak(id(), killstreak)) {
            putKillstreak(killstreak, killstreakSlot);
            player.playSound(player.getLocation(), SELECT_SOUND, 1.0f, 2.0f);
            new UpgradeMenu(plugin, player, Page.KILLSTREAKS).open();
            return;
        }
        if (killstreak.bundleKey() != null && !upgrades.isUnlocked(id(), killstreak.bundleKey())) {
            deny("§cUnlock this killstreak in the Renown shop first!");
            return;
        }
        if (!levelOk(killstreak.getRequiredLevel())) {
            deny("§cYou need to be level " + killstreak.getRequiredLevel() + " to unlock this!");
            return;
        }
        if (plugin.getGoldManager().get(id()) < killstreak.getCost()) {
            deny(NOT_ENOUGH_GOLD);
            return;
        }
        confirmKillstreak(plugin, player, killstreak, killstreakSlot).open();
    }

    /** 枠に入れる (他の枠に同じものがあればそこは空ける)。 */
    private void putKillstreak(Killstreak killstreak, int slot) {
        UpgradeManager upgrades = plugin.getUpgradeManager();
        for (int i = 0; i < 3; i++) {
            if (i != slot && upgrades.getKillstreak(id(), i) == killstreak) {
                upgrades.setKillstreak(id(), i, null);
            }
        }
        upgrades.setKillstreak(id(), slot, killstreak);
    }

    /** Permanent upgrades の Perk Slot (録画の空き・選択中・使えない枠の表示)。 */
    private ItemStack perkSlotItem(int slot) {
        int number = slot + 1;
        int required = PerkRules.slotLevel(slot);
        if (!levelOk(required)) {
            return item(Material.BEDROCK, 0, number, "§cPerk Slot #" + number,
                    Collections.singletonList("§7Required level: " + PrestigeRules.levelTag(prestige(), required)));
        }
        Perk perk = plugin.getUpgradeManager().getPerk(id(), slot);
        if (perk == null) {
            return item(Material.DIAMOND_BLOCK, 0, number, "§aPerk Slot #" + number, Arrays.asList(
                    "§7Select a perk to fill this slot.",
                    "",
                    "§eClick to choose perk!"));
        }
        List<String> lore = new ArrayList<>();
        lore.add("§7Selected: §a" + perk.getDisplayName());
        lore.add("");
        lore.addAll(perk.getDescription());
        lore.add("");
        lore.add("§eClick to choose perk!");
        return perkIcon(perk, number, "§ePerk Slot #" + number, lore);
    }

    /** Choose a perk のパーク (録画の "Click to purchase!" / "Not enough gold!" / "Unlocked in Renown shop!" / "Unknown perk")。 */
    private ItemStack perkItem(Perk perk) {
        UpgradeManager upgrades = plugin.getUpgradeManager();
        UUID id = id();
        List<String> lore = new ArrayList<>(perk.getDescription());
        lore.add("");
        if (upgrades.ownsPerk(id, perk)) {
            lore.add(upgrades.hasPerk(id, perk) ? "§aAlready selected!" : "§eClick to select!");
            return perkIcon(perk, 1, "§a" + perk.getDisplayName(), lore);
        }
        if (!levelOk(perk.getRequiredLevel())) {
            return item(Material.BEDROCK, 0, 1, "§cUnknown perk", Collections.singletonList(
                    "§7Required level: " + PrestigeRules.levelTag(prestige(), perk.getRequiredLevel())));
        }
        lore.add("§7Cost: §6" + NUMBER.format(perk.getCost()) + "g");
        if (!upgrades.isPerkUnlocked(id, perk)) {
            lore.add("§cUnlocked in Renown shop!");
            return item(Material.BEDROCK, 0, 1, "§c" + perk.getDisplayName(), lore);
        }
        boolean canBuy = false;
        if (!perk.isAvailable()) {
            lore.add("§cPerk disabled!");
        } else if (plugin.getGoldManager().get(id) < perk.getCost()) {
            lore.add("§cNot enough gold!");
        } else {
            lore.add("§eClick to purchase!");
            canBuy = true;
        }
        return perkIcon(perk, 1, (canBuy ? "§e" : "§c") + perk.getDisplayName(), lore);
    }

    private static ItemStack perkIcon(Perk perk, int amount, String name, List<String> lore) {
        ItemStack icon = item(perk.getIcon(), perk.getData(), amount, name, lore);
        return perk.getIcon() == Material.SKULL_ITEM ? PerkItems.withHeadTexture(icon) : icon;
    }

    /** Passive (録画: レベルが足りない間は "Unknown upgrade"、買えないときは "Not enough gold!")。 */
    private ItemStack passiveItem(Passive passive) {
        int tier = plugin.getUpgradeManager().getTier(id(), passive);
        int maxTier = plugin.getUpgradeManager().maxPassiveTier(id());
        if (tier == 0 && level() < passive.requiredLevel(1)) {
            return item(Material.BEDROCK, 0, 1, "§cUnknown upgrade", Collections.singletonList(
                    "§7Required level: " + PrestigeRules.levelTag(prestige(), passive.requiredLevel(1))));
        }
        // 持っている Tier があれば今の合計 (例: Gold Boost V なら +50%) と次の Tier (+60%) を出す
        List<String> lore = new ArrayList<>();
        if (tier > 0) {
            lore.add("§7Current:");
            lore.addAll(passiveDescription(passive, tier));
            lore.add("");
        }
        if (tier < maxTier) {
            lore.add(tier == 0 && passive != Passive.EL_GATO ? "§7Each tier:" : "§7Next tier:");
            lore.addAll(passiveDescription(passive, tier + 1));
            lore.add("");
        }
        if (tier >= maxTier) {
            lore.add("§aMax tier unlocked!");
        } else {
            lore.add("§7Cost: §6" + NUMBER.format(passive.cost(tier + 1)) + "g");
            if (level() < passive.requiredLevel(tier + 1)) {
                lore.add("§cRequires level " + passive.requiredLevel(tier + 1) + "!");
            } else {
                lore.add(plugin.getGoldManager().get(id()) < passive.cost(tier + 1) ? "§cNot enough gold!" : "§eClick to purchase!");
            }
        }
        String name = "§e" + passive.getDisplayName() + (tier > 0 ? " " + RomanNumerals.of(tier) : "");
        return item(passive.getIcon(), passive.getData(), Math.max(1, tier), name, lore);
    }

    /** その Tier までの合計の説明。El Gato は "最初の N キル" (録画の文)。 */
    static List<String> passiveDescription(Passive passive, int tier) {
        if (passive == Passive.EL_GATO) {
            return Collections.singletonList(tier == 1 ? "§7§dFirst kill §7each life rewards §6+§65g §b+5 XP§7."
                    : "§7§dFirst " + tier + " kills §7each life rewards §6+§65g §b+5 XP§7.");
        }
        return passive.description(tier);
    }

    private static ItemStack back(String to) {
        return item(Material.ARROW, 0, 1, "§aGo Back", Collections.singletonList(to));
    }

    private static ItemStack item(Material material, int data, int amount, String name, List<String> lore) {
        ItemStack item = new ItemStack(material, amount, (short) data);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }
}
