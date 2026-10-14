package com.pitremake.well;

import com.pitremake.PitPlugin;
import com.pitremake.enchant.PitEnchant;
import com.pitremake.gold.GoldManager;
import com.pitremake.item.MysticData;
import com.pitremake.item.MysticItems;
import com.pitremake.item.MysticLore;
import com.pitremake.item.MysticPrefix;
import com.pitremake.item.MysticType;
import com.pitremake.item.PantsColor;
import com.pitremake.renown.RenownUpgrade;
import com.pitremake.util.PlayerPackets;
import com.pitremake.util.RomanNumerals;
import com.pitremake.util.UpgradeAnnouncer;
import net.minecraft.server.v1_8_R3.EnumParticle;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Mystic Well の GUI。プレイヤーごとに 1 つ作り、開いている間は毎 tick アニメーションを描画する。
 * 置いたアイテムと生贄のパンツは {@link MysticWellStorage} に保存され、GUI を閉じても井戸の中に残る。
 *
 * <pre>
 * 配置: 中央 20 / リング 10,11,12,21,30,29,28,19 / エンチャントテーブル 24
 *       Tier II のアイテムが置かれている (次が Tier III) ときは 生贄スロット 23 / テーブル 25
 * 状態:
 *   待機 (空 or Fresh)  : 灰色のリングを桃色のガラスが時計回りに回る
 *   待機 (強化済み)      : 回るガラスがそのアイテムの Tier の色
 *   抽選直後            : リング全体が強化後の Tier の色
 *   抽選中              : 全体が 2 回点滅 → リングを Tier の色が回り、中央を染料が切り替わる → 結果表示
 * </pre>
 */
public final class MysticWellMenu implements InventoryHolder {

    static final int CENTER = 20;
    static final int SACRIFICE = 23;
    private static final int TABLE = 24;
    private static final int TABLE_WITH_SACRIFICE = 25;
    /** 左上から時計回り。 */
    private static final int[] RING = {10, 11, 12, 21, 30, 29, 28, 19};

    private static final short GLASS_GRAY = 7;
    private static final short GLASS_PINK = 6;
    private static final short CLAY_RED = 14;

    /** 待機中に回るガラスが 1 マス進む間隔 (tick)。 */
    private static final int IDLE_STEP_TICKS = 6;
    /** 点滅: 点灯 0-2, 消灯 3-6, 点灯 7-10, 消灯 11-14 (録画のタイミング)。 */
    private static final int FLASH_TICKS = 15;
    private static final int SPIN_STEP_TICKS = 3;
    private static final int DYE_STEP_TICKS = 2;
    /** テーブルの表示が "It's rollin'!" になる tick。 */
    private static final int ROLLIN_TICK = 8;
    /** 結果が出たときの爆発: 煙の玉の数と散らばる幅 (ブロック)。 */
    private static final int EXPLOSION_COUNT = 6;
    private static final float EXPLOSION_SPREAD = 0.5f;
    private static final String PANTS_EXPENDED = ChatColor.LIGHT_PURPLE.toString() + ChatColor.BOLD + "THIS ITEM WILL BE EXPENDED!";

    private final PitPlugin plugin;
    private final Player player;
    /** エンチャントテーブルの上の中心。 */
    private final Location well;
    private final Inventory inventory;
    private BukkitTask task;
    private int tick;

    /** 井戸に置かれているアイテム。抽選中は null。 */
    private ItemStack item;
    private int itemTier;
    /** 置かれているアイテムが強化できる最大の Tier と、次の強化の費用。 */
    private int itemMaxTier;
    /** Mysticism (Renown) が足りなくてこれ以上強化できないか。 */
    private boolean mysticismLimited;
    /** Rage Pants なのに Renown の Pure Rage を解放していない。 */
    private boolean rageLocked;
    /** Dark Pants なのに Heresy が足りない (Tier I は Heresy I、Tier II は Heresy III と Mysticism IX)。 */
    private boolean darkLocked;
    private int upgradeCost;
    /** Tier III への強化に必要なパンツの色 (item が Tier II のときのみ)。 */
    private PantsColor requiredPants;
    /** 生贄として置かれているパンツ。 */
    private ItemStack sacrifice;
    /** 抽選中の強化が生贄ありだったか (生贄スロットの配置)。 */
    private boolean rollingSacrifice;
    /** 抽選中の強化後アイテム。抽選中でなければ null。 */
    private ItemStack result;
    private int targetTier;
    private int rollTick;
    /** 抽選が終わった直後 (リング全体を Tier の色にする)。 */
    private boolean justRolled;
    /** 抽選の演出が終わったら (または途中で閉じたら) 流す全体アナウンス。無ければ null。 */
    private String pendingAnnouncement;
    /** 抽選の効果音 (結果表示の後も少し続くので演出とは別に数える)。0 なら鳴らしていない。 */
    private int soundTier;
    private int soundTick;

    /** @param well 開いたエンチャントテーブルの位置 (強化の爆発エフェクトを出す) */
    public MysticWellMenu(PitPlugin plugin, Player player, Location well) {
        this.plugin = plugin;
        this.player = player;
        this.well = well.clone().add(0.5, 1.0, 0.5);
        this.inventory = Bukkit.createInventory(this, 45, "Mystic Well");
        // 前回井戸に置いたままのアイテムがあれば表示する
        setItem(plugin.getWellStorage().get(player.getUniqueId()));
        this.sacrifice = plugin.getWellStorage().getSacrifice(player.getUniqueId());
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void open() {
        if (sacrifice != null && !needsSacrifice()) {
            // 生贄が不要な状態で残っていたら返却する
            returnSacrifice();
        }
        render();
        player.openInventory(inventory);
        WellSounds.open(player);
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1, 1);
    }

    boolean isRolling() {
        return result != null;
    }

    boolean isTableSlot(int slot) {
        return slot == tableSlot();
    }

    boolean canPlace() {
        return !isRolling() && item == null;
    }

    void place(ItemStack stack) {
        setItem(stack);
        justRolled = false;
        plugin.getWellStorage().set(player.getUniqueId(), item);
        WellSounds.place(player);
        render();
    }

    /** 中央のアイテムが Tier II で、まだ生贄が無く、stack がパンツなら true (色は強化するときに確認する)。 */
    boolean canPlaceSacrifice(ItemStack stack) {
        return !isRolling() && needsSacrifice() && sacrifice == null && MysticItems.isMysticPants(stack);
    }

    void placeSacrifice(ItemStack stack) {
        sacrifice = stack;
        plugin.getWellStorage().setSacrifice(player.getUniqueId(), sacrifice);
        WellSounds.place(player);
        render();
    }

    /** 置いてあるアイテム (と生贄) をインベントリに戻す。 */
    void returnItem() {
        if (isRolling() || item == null) {
            return;
        }
        giveBackSacrifice();
        plugin.getWellStorage().set(player.getUniqueId(), null);
        giveBack(item);
        setItem(null);
        justRolled = false;
        WellSounds.remove(player);
        render();
    }

    /** 生贄のパンツをインベントリに戻す。 */
    void returnSacrifice() {
        if (isRolling() || sacrifice == null) {
            return;
        }
        giveBackSacrifice();
        WellSounds.remove(player);
        render();
    }

    private void giveBackSacrifice() {
        if (sacrifice == null) {
            return;
        }
        plugin.getWellStorage().setSacrifice(player.getUniqueId(), null);
        giveBack(sacrifice);
        sacrifice = null;
    }

    void tryUpgrade() {
        if (!isRolling() && item != null && darkLocked) {
            player.sendMessage(ChatColor.RED + "Upgrade Heresy in the Renown shop to enchant Dark Pants further!");
            WellSounds.deny(player);
            return;
        }
        if (!isRolling() && item != null && rageLocked) {
            player.sendMessage(ChatColor.RED + "Unlock Pure Rage in the Renown shop to enchant Rage Pants!");
            WellSounds.deny(player);
            return;
        }
        if (!isRolling() && item != null && mysticismLimited) {
            player.sendMessage(ChatColor.RED + "Upgrade Mysticism in the Renown shop to enchant this item further!");
            WellSounds.deny(player);
            return;
        }
        if (isRolling() || item == null || itemTier >= itemMaxTier) {
            return;
        }
        int target = itemTier + 1;
        boolean needsSacrifice = requiredPants != null;
        if (needsSacrifice && (sacrifice == null || !SacrificeRules.isValid(sacrifice, requiredPants))) {
            player.sendMessage(ChatColor.RED + "You need " + requiredPants.getDisplayName() + " pants to upgrade this item!");
            WellSounds.deny(player);
            return;
        }
        GoldManager gold = plugin.getGoldManager();
        if (!gold.take(player.getUniqueId(), upgradeCost)) {
            player.sendMessage(ChatColor.RED + "You don't have enough gold!");
            WellSounds.deny(player);
            return;
        }
        if (needsSacrifice) {
            sacrifice = null;
            plugin.getWellStorage().setSacrifice(player.getUniqueId(), null);
        }

        MysticData data = MysticData.read(item);
        Set<String> enchantsBefore = new HashSet<>(data.getEnchants().keySet());
        MysticPrefix prefixBefore = MysticPrefix.of(data, plugin.getEnchantRegistry());
        plugin.getWellRoller().upgrade(data, MysticType.of(item));
        result = MysticItems.applyDisplay(data.write(item), plugin.getEnchantRegistry());
        pendingAnnouncement = announcementFor(data, enchantsBefore, prefixBefore);
        // 演出の途中で閉じられても強化済みのアイテムが井戸に残るよう、先に保存しておく
        plugin.getWellStorage().set(player.getUniqueId(), result);
        targetTier = target;
        rollingSacrifice = needsSacrifice;
        setItem(null);
        rollTick = 0;
        soundTier = target;
        soundTick = 0;
        WellSounds.roll(player, soundTier, soundTick);
        render();
    }

    /** 結果が表示される tick (録画: Tier I = 60, Tier II = 70, Tier III = 80)。 */
    private static int resultTick(int tier) {
        return 50 + 10 * Math.min(tier, 3);
    }

    /**
     * GUI が閉じられたとき。アイテムと生贄は井戸 (ストレージ) に残る。
     * 抽選中なら演出をスキップする (強化済みアイテムは {@link #tryUpgrade} で保存済み)。
     */
    void close() {
        if (task != null) {
            task.cancel();
        }
        if (isRolling()) {
            // 演出を飛ばしたときは爆発のエフェクトは出さない
            announce();
        }
        result = null;
        setItem(null);
        sacrifice = null;
    }

    private void setItem(ItemStack stack) {
        item = stack;
        if (stack == null) {
            itemTier = 0;
            itemMaxTier = WellRules.MAX_TIER;
            mysticismLimited = false;
            upgradeCost = 0;
            requiredPants = null;
            return;
        }
        MysticData data = MysticData.read(stack);
        rageLocked = data.isRagePants() && !plugin.getUpgradeManager().hasRenown(player.getUniqueId(), RenownUpgrade.PURE_RAGE);
        int heresy = plugin.getUpgradeManager().getRenownTier(player.getUniqueId(), RenownUpgrade.HERESY);
        darkLocked = data.isDarkPants() && (data.getTier() == 0 ? heresy < 1 : heresy < 3);
        itemTier = data.getTier();
        itemMaxTier = WellRules.maxTier(data);
        mysticismLimited = false;
        int mysticism = plugin.getUpgradeManager().getRenownTier(player.getUniqueId(), RenownUpgrade.MYSTICISM);
        int allowed = WellRules.mysticismMaxTier(mysticism, data, MysticItems.isMysticPants(stack));
        if (allowed < itemMaxTier) {
            // Mysticism (Renown) が足りない
            mysticismLimited = itemTier >= allowed;
            itemMaxTier = Math.max(itemTier, allowed);
        }
        // Renown の Scam Artist で安くなる
        upgradeCost = itemTier < itemMaxTier ? plugin.getRenownEffects().discounted(player, WellRules.cost(itemTier + 1, data)) : 0;
        requiredPants = itemTier < itemMaxTier && SacrificeRules.isRequired(itemTier + 1, data)
                ? SacrificeRules.requiredColor(data) : null;
    }

    /** 中央のアイテムの次の強化に生贄が必要か。 */
    private boolean needsSacrifice() {
        return item != null && requiredPants != null;
    }

    /** 生贄スロットを表示する配置か (抽選中は強化後の Tier で判定)。 */
    private boolean sacrificeLayout() {
        return isRolling() ? rollingSacrifice : needsSacrifice();
    }

    private int tableSlot() {
        return sacrificeLayout() ? TABLE_WITH_SACRIFICE : TABLE;
    }

    private void tick() {
        tick++;
        if (soundTier > 0) {
            WellSounds.roll(player, soundTier, ++soundTick);
            if (soundTick >= WellSounds.endTick(soundTier)) {
                soundTier = 0;
            }
        }
        if (isRolling() && ++rollTick >= resultTick(targetTier)) {
            explode();
            announce();
            setItem(result);
            result = null;
            justRolled = true;
        }
        render();
    }

    /**
     * 全体アナウンスの種類。新しく RARE (見た目だけ RARE を含む) が付いたら "RARE!"、
     * そうでなく新しい称号が付いたら "WOAH!"。両方なら RARE! を優先する。どちらでもなければ null。
     */
    private String announcementFor(MysticData data, Set<String> enchantsBefore, MysticPrefix prefixBefore) {
        for (String key : data.getEnchants().keySet()) {
            PitEnchant enchant = plugin.getEnchantRegistry().get(key);
            if (!enchantsBefore.contains(key) && enchant != null && enchant.showsRareTag()) {
                return UpgradeAnnouncer.RARE;
            }
        }
        MysticPrefix prefixAfter = MysticPrefix.of(data, plugin.getEnchantRegistry());
        return prefixAfter != null && prefixAfter != prefixBefore ? UpgradeAnnouncer.WOAH : null;
    }

    /** 演出を最後まで見て結果が出たとき、井戸の上に本人にだけ見える爆発。 */
    private void explode() {
        PlayerPackets.particle(player, EnumParticle.EXPLOSION_LARGE, well, EXPLOSION_SPREAD, EXPLOSION_COUNT);
    }

    private void announce() {
        if (pendingAnnouncement != null) {
            UpgradeAnnouncer.announce(pendingAnnouncement, player, result);
            pendingAnnouncement = null;
        }
    }

    private void render() {
        boolean sacrificeLayout = sacrificeLayout();
        int table = tableSlot();
        inventory.setItem(table, tableItem());
        inventory.setItem(table == TABLE ? TABLE_WITH_SACRIFICE : TABLE, null);
        inventory.setItem(SACRIFICE, sacrificeLayout && !isRolling() ? sacrificeSlotItem() : null);
        if (isRolling()) {
            renderRoll();
        } else {
            renderIdle();
        }
    }

    private void renderIdle() {
        inventory.setItem(CENTER, item);
        if (justRolled && item != null) {
            // 抽選直後: リング全体が Tier の色
            ItemStack pane = pane(tierGlass(itemTier), MysticLore.tierColor(itemTier) + "Click the item to pick it up!");
            for (int slot : RING) {
                inventory.setItem(slot, pane);
            }
            return;
        }
        int lit = (tick / IDLE_STEP_TICKS) % RING.length;
        boolean tiered = item != null && itemTier > 0;
        short spinColor = tiered ? tierGlass(itemTier) : GLASS_PINK;
        ChatColor spinText = tiered ? MysticLore.tierColor(itemTier) : ChatColor.LIGHT_PURPLE;
        String text = item == null ? "Click an item in your inventory!" : "Item in well!";
        for (int i = 0; i < RING.length; i++) {
            inventory.setItem(RING[i], i == lit ? pane(spinColor, spinText + text) : pane(GLASS_GRAY, ChatColor.GRAY + text));
        }
    }

    private void renderRoll() {
        short color = tierGlass(targetTier);
        if (rollTick < FLASH_TICKS) {
            boolean lit = rollTick < 3 || (rollTick >= 7 && rollTick < 11);
            ItemStack pane = lit ? pane(color, MysticLore.tierColor(targetTier) + " ") : pane(GLASS_GRAY, " ");
            inventory.setItem(CENTER, pane);
            for (int slot : RING) {
                inventory.setItem(slot, pane);
            }
            return;
        }
        int spinTick = rollTick - FLASH_TICKS;
        int lit = (spinTick / SPIN_STEP_TICKS) % RING.length;
        for (int i = 0; i < RING.length; i++) {
            inventory.setItem(RING[i], pane(i == lit ? color : GLASS_GRAY, " "));
        }
        // 染料を 15 (白) → 0 (黒) → 1 (赤) … の順に切り替える
        short dye = (short) ((15 + spinTick / DYE_STEP_TICKS) % 16);
        inventory.setItem(CENTER, filler(Material.INK_SACK, dye));
    }

    /** 生贄スロット: 置かれていればそのパンツ (名前は "THIS ITEM WILL BE EXPENDED!")、無ければ必要な色を示すガラス。 */
    private ItemStack sacrificeSlotItem() {
        if (sacrifice != null) {
            ItemStack shown = sacrifice.clone();
            ItemMeta meta = shown.getItemMeta();
            meta.setDisplayName(PANTS_EXPENDED);
            shown.setItemMeta(meta);
            return shown;
        }
        ItemStack pane = new ItemStack(Material.STAINED_GLASS_PANE, 1, GLASS_GRAY);
        ItemMeta meta = pane.getItemMeta();
        ChatColor color = requiredPants.getChatColor();
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Pants Slot");
        meta.setLore(Arrays.asList(
                // 録画 2026-10-14_20-50-01.mcrc: "§7This Tier III upgrade requires" / "§7§6Orange Pants§7." (Dark Pants は Tier II)
                ChatColor.GRAY + "This Tier " + com.pitremake.util.RomanNumerals.of(itemTier + 1) + " upgrade requires",
                ChatColor.GRAY + "" + color + requiredPants.getDisplayName() + " Pants" + ChatColor.GRAY + ".",
                "",
                ChatColor.YELLOW + "Click pants in your inventory!"));
        pane.setItemMeta(meta);
        return pane;
    }

    private ItemStack tableItem() {
        if (!isRolling() && item != null && itemTier >= itemMaxTier) {
            ItemStack maxed = new ItemStack(Material.STAINED_CLAY, 1, CLAY_RED);
            ItemMeta meta = maxed.getItemMeta();
            meta.setDisplayName(ChatColor.RED + "Mystic Well");
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "This item cannot be upgraded any",
                    ChatColor.GRAY + "further.",
                    "",
                    ChatColor.RED + "Maxed out upgrade tier!"));
            maxed.setItemMeta(meta);
            return maxed;
        }

        ItemStack table = new ItemStack(Material.ENCHANTMENT_TABLE);
        ItemMeta meta = table.getItemMeta();
        if (isRolling()) {
            meta.setDisplayName(ChatColor.LIGHT_PURPLE + (rollTick >= ROLLIN_TICK ? "It's rollin'!" : "Mystic Well"));
        } else {
            meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Mystic Well");
            if (item == null) {
                meta.setLore(Arrays.asList(
                        ChatColor.GRAY + "Click a mystic item in your",
                        ChatColor.GRAY + "inventory to place it!"));
            } else {
                int target = itemTier + 1;
                String gold = ChatColor.GOLD + GoldManager.format(upgradeCost);
                String cost = needsSacrifice()
                        ? SacrificeRules.displayName(requiredPants) + ChatColor.GRAY + " + " + gold
                        : gold;
                meta.setLore(Arrays.asList(
                        ChatColor.GRAY + "Upgrade: " + MysticLore.tierColor(target) + "Tier " + RomanNumerals.of(target),
                        ChatColor.GRAY + "Cost: " + cost,
                        "",
                        ChatColor.YELLOW + "Click to upgrade!"));
            }
        }
        table.setItemMeta(meta);
        return table;
    }

    private void giveBack(ItemStack stack) {
        for (ItemStack leftover : player.getInventory().addItem(stack).values()) {
            player.getWorld().dropItem(player.getLocation(), leftover);
        }
    }

    /** Tier の色のステンドグラス (I = 黄緑, II = 黄, III = 赤)。 */
    private static short tierGlass(int tier) {
        switch (tier) {
            case 1:
                return 5;
            case 2:
                return 4;
            default:
                return 14;
        }
    }

    private static ItemStack filler(Material material, short data) {
        ItemStack stack = new ItemStack(material, 1, data);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(" ");
        stack.setItemMeta(meta);
        return stack;
    }

    /** リングのガラス (名前付き)。 */
    private static ItemStack pane(short color, String name) {
        ItemStack stack = new ItemStack(Material.STAINED_GLASS_PANE, 1, color);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(name);
        stack.setItemMeta(meta);
        return stack;
    }
}
