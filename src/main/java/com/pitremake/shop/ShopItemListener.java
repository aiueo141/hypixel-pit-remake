package com.pitremake.shop;

import com.pitremake.PitPlugin;
import com.pitremake.combat.DamageContext;
import com.pitremake.combat.DamageModifier;
import com.pitremake.perk.PerkItems;
import com.pitremake.renown.RenownUpgrade;
import com.pitremake.upgrade.Megastreak;
import com.pitremake.upgrade.Passive;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Items の村人のアイテムの効果と、ブロックのルール。
 * <pre>
 * Diamond Sword  : バウンティ持ちに +20% ダメージ
 * Obsidian      : 置いて 120 秒で消える。スポーンには置けない
 * Golden Pickaxe : 置かれた黒曜石を 2 回叩くと縦 5 個まで壊す
 * 丸石 (Mineman) : 置いて 15 秒で消える。スポーンには置けない
 * それ以外       : クリエイティブ以外はブロックを置けない・壊せない (置かれたブロックはダイヤ / 金のピッケルで壊せる)
 * </pre>
 * Pit の真ん中 (マップの floorRadius の内側) にも置けない。消えるまでの時間は Build Battler / Hermit で延びる。
 * Renown の Autobuy は Items の画面で印を付けたものをスポーンで買う。Combat Spade / First-Aid Egg / Pants Bundle の効果もここ。
 */
public final class ShopItemListener implements Listener, DamageModifier {

    private static final double SWORD_BOUNTY_BONUS = 0.2;
    private static final long OBSIDIAN_TICKS = 120 * 20;
    /** Mineman の丸石は 15 秒で消える (pit.wiki)。 */
    private static final long COBBLESTONE_TICKS = 15 * 20;
    private static final double BUILD_BATTLER_PER_TIER = 0.6;
    private static final int PILLAR_HEIGHT = 5;
    private static final long DOUBLE_TAP_MILLIS = 1_000;
    private static final long EGG_COOLDOWN_MILLIS = 30_000;
    private static final long EGG_KILL_REDUCTION_MILLIS = 5_000;
    private static final double EGG_HEAL = 5;

    private final PitPlugin plugin;
    /**
     * 置かれたブロック (消えるまで) → そのブロックの番号。同じ場所に置き直したときに、前のブロックの予定で
     * 今のブロックが残ったり早く消えたりしないよう、番号が同じときだけ消す。
     */
    private final Map<Location, Long> placedBlocks = new HashMap<>();
    private long nextPlacement = 1;
    /** 落ちたり止められたりしても残らないよう、置かれたブロックをファイルにも書く (起動したら消す)。 */
    private final java.io.File placedFile;
    private boolean placedDirty;
    private final Map<UUID, Long> lastTapTime = new HashMap<>();
    private final Map<UUID, Location> lastTapBlock = new HashMap<>();
    /** First-Aid Egg が次に使える時刻。 */
    private final Map<UUID, Long> eggReady = new HashMap<>();

    public ShopItemListener(PitPlugin plugin) {
        this.plugin = plugin;
        this.placedFile = new java.io.File(plugin.getDataFolder(), "placed-blocks.yml");
    }

    /**
     * 起動時: 前回消せなかった (サーバーが止められた・落ちた) 置かれたブロックを消す。
     * それからは 10 秒ごとに変わっていればファイルに書く。
     */
    public void start() {
        org.bukkit.configuration.file.YamlConfiguration yaml = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(placedFile);
        for (String entry : yaml.getStringList("blocks")) {
            String[] parts = entry.split(",");
            if (parts.length != 4 || plugin.getServer().getWorld(parts[0]) == null) {
                continue;
            }
            try {
                Block block = plugin.getServer().getWorld(parts[0]).getBlockAt(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]),
                        Integer.parseInt(parts[3]));
                if (isTemporary(block.getType())) {
                    block.setType(Material.AIR);
                }
            } catch (NumberFormatException ignored) {
                // 壊れた行は飛ばす
            }
        }
        placedDirty = true;
        savePlaced();
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::savePlaced, 200, 200);
    }

    private static boolean isTemporary(Material type) {
        return type == Material.OBSIDIAN || type == Material.COBBLESTONE || type == Material.WOOD || type == Material.BEDROCK;
    }

    private void savePlaced() {
        if (!placedDirty) {
            return;
        }
        placedDirty = false;
        org.bukkit.configuration.file.YamlConfiguration yaml = new org.bukkit.configuration.file.YamlConfiguration();
        List<String> lines = new java.util.ArrayList<>();
        for (Location location : placedBlocks.keySet()) {
            lines.add(location.getWorld().getName() + "," + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ());
        }
        yaml.set("blocks", lines);
        try {
            yaml.save(placedFile);
        } catch (java.io.IOException e) {
            plugin.getLogger().warning("Failed to save placed-blocks.yml: " + e.getMessage());
        }
    }

    /** サーバー停止時: 置かれたブロックを消す。 */
    public void stop() {
        for (Location location : placedBlocks.keySet()) {
            if (isTemporary(location.getBlock().getType())) {
                location.getBlock().setType(Material.AIR);
            }
        }
        placedBlocks.clear();
        placedDirty = true;
        savePlaced();
    }

    /** Diamond Sword: バウンティ持ちに +20% (他の増減と掛け算)。 */
    @Override
    public void modify(DamageContext context) {
        if (context.melee && context.byPlayer() && ShopItem.of(context.attacker.getItemInHand()) == ShopItem.DIAMOND_SWORD
                && plugin.getBountyManager().get(context.victim.getUniqueId()) > 0) {
            context.increase(SWORD_BOUNTY_BONUS);
        }
        combatSpade(context);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }
        Block block = event.getBlockPlaced();
        long ticks;
        ShopItem placed = ShopItem.of(event.getItemInHand());
        if (placed == ShopItem.OBSIDIAN || placed == ShopItem.OBSIDIAN_STACK) {
            ticks = OBSIDIAN_TICKS;
        } else if (PerkItems.of(event.getItemInHand()) == PerkItems.Type.MINEMAN_COBBLE) {
            ticks = COBBLESTONE_TICKS;
        } else if (com.pitremake.combat.SimpleEnchants.isCreativePlank(event.getItemInHand())) {
            // Creative の木材は 30 秒で消える
            ticks = com.pitremake.combat.SimpleEnchants.PLANK_TICKS;
        } else {
            ticks = 0;
        }
        if (ticks == 0 || plugin.getPitSpawn().isProtected(block.getLocation()) || plugin.getPitMap().isCenter(block.getLocation())) {
            event.setCancelled(true);
            return;
        }
        ticks = Math.round(ticks * blockDurationMultiplier(player));
        Location location = block.getLocation();
        long id = nextPlacement++;
        placedBlocks.put(location, id);
        placedDirty = true;
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            // このブロックの予定のときだけ消す (同じ場所に後から置かれたブロックはそのブロックの予定で消える)
            Long current = placedBlocks.get(location);
            if (current != null && current == id) {
                placedBlocks.remove(location);
                placedDirty = true;
                if (isTemporary(location.getBlock().getType())) {
                    location.getBlock().setType(Material.AIR);
                }
            }
        }, ticks);
    }

    /** 置かれた (消える) ブロックか (TNT で壊せるかの判定)。 */
    public boolean isPlaced(Location location) {
        return placedBlocks.containsKey(location);
    }

    /** TNT で壊れたブロックを忘れる (消える予定の処理はブロックの種類が変わっているので何もしない)。 */
    public void forgetPlaced(Location location) {
        placedBlocks.remove(location);
        placedDirty = true;
    }

    /** 置いたブロックが消えるまでの倍率: Build Battler (1 Tier +60%)、Hermit (2 倍)。 */
    private double blockDurationMultiplier(Player player) {
        double multiplier = 1 + BUILD_BATTLER_PER_TIER * plugin.getUpgradeManager().getTier(player.getUniqueId(), Passive.BUILD_BATTLER);
        if (plugin.getUpgradeManager().getMegastreak(player.getUniqueId()) == Megastreak.HERMIT) {
            multiplier *= 2;
        }
        return multiplier;
    }

    /** 死んだとき: First-Aid Egg のクールダウンを消す。 */
    public void onDeath(Player player) {
        eggReady.remove(player.getUniqueId());
    }

    /**
     * スポーンしたとき: Renown の Autobuy があれば、Items の画面で右クリックして印を付けたものを買う
     * (ゴールドが足りるものだけ。防具・剣・道具などは持っていれば買わない)。
     */
    public void autobuy(Player player) {
        UUID id = player.getUniqueId();
        if (!plugin.getUpgradeManager().hasRenown(id, RenownUpgrade.AUTOBUY)) {
            return;
        }
        List<ShopItem> available = ItemShopMenu.available(plugin, player);
        for (String name : plugin.getUpgradeManager().autobuyItems(id)) {
            ShopItem item;
            try {
                item = ShopItem.valueOf(name);
            } catch (IllegalArgumentException e) {
                continue;
            }
            if (!available.contains(item) || (item.isSingle() && ItemShopMenu.has(player, item))) {
                continue;
            }
            if (plugin.getGoldManager().take(id, ItemShopMenu.cost(plugin, player, item))) {
                ItemShopMenu.deliver(plugin, player, item, true);
                player.sendMessage("§a§lAUTOBUY! §6" + item.getDisplayName());
            }
        }
        plugin.getSidebar().update(player);
    }

    /** Combat Spade: 相手のダイヤの防具 1 つにつき +1 ダメージ (攻撃力はアイテムの +7 Attack Damage でダイヤの剣と同じ)。 */
    private void combatSpade(DamageContext context) {
        if (!context.melee || !context.byPlayer() || ShopItem.of(context.attacker.getItemInHand()) != ShopItem.COMBAT_SPADE) {
            return;
        }
        int diamonds = 0;
        for (ItemStack armor : context.victim.getInventory().getArmorContents()) {
            if (armor != null && armor.getType().name().startsWith("DIAMOND_")) {
                diamonds++;
            }
        }
        context.addDamage(diamonds);
    }

    /** 右クリック: First-Aid Egg・Pants Bundle。 */
    @EventHandler(priority = EventPriority.HIGH)
    public void onUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Player player = event.getPlayer();
        ShopItem item = ShopItem.of(event.getItem());
        if (item == ShopItem.PANTS_BUNDLE) {
            event.setCancelled(true);
            PantsBundle.use(player);
        } else if (item == ShopItem.FIRST_AID_EGG) {
            event.setCancelled(true);
            useEgg(player);
        }
    }

    /**
     * First-Aid Egg (pit.wiki): 2.5❤ 回復して Speed を消す。30 秒に 1 回 (その間のキル 1 つにつき -5 秒)。
     * クールダウン中は見た目がスケルトンのスポーンエッグ。
     */
    private void useEgg(Player player) {
        long now = System.currentTimeMillis();
        Long ready = eggReady.get(player.getUniqueId());
        if (ready != null && now < ready) {
            player.sendMessage("§cFirst-Aid Egg is on cooldown! §7(" + (int) Math.ceil((ready - now) / 1000.0) + "s)");
            player.playSound(player.getLocation(), "mob.villager.no", 1.0f, 1.0f);
            return;
        }
        eggReady.put(player.getUniqueId(), now + EGG_COOLDOWN_MILLIS);
        player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + EGG_HEAL));
        player.removePotionEffect(PotionEffectType.SPEED);
        // 使ったときはシルバーフィッシュの声 (ユーザーの指定)
        player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.SILVERFISH_IDLE, 1.0f, 1.0f);
        player.sendMessage("§c§lFIRST-AID! §7Healed §c2.5❤§7!");
        setEggLook(player, ShopItem.SKELETON_EGG);
        scheduleEggReady(player);
    }

    /** キルしたとき: First-Aid Egg のクールダウン -5 秒。 */
    public void onKill(Player killer) {
        Long ready = eggReady.get(killer.getUniqueId());
        if (ready != null && ready > System.currentTimeMillis()) {
            eggReady.put(killer.getUniqueId(), ready - EGG_KILL_REDUCTION_MILLIS);
            scheduleEggReady(killer);
        }
    }

    private void scheduleEggReady(Player player) {
        Long ready = eggReady.get(player.getUniqueId());
        long ticks = ready == null ? 0 : Math.max(0, (ready - System.currentTimeMillis()) / 50);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            Long now = eggReady.get(player.getUniqueId());
            if (player.isOnline() && (now == null || now <= System.currentTimeMillis() + 50)) {
                setEggLook(player, ShopItem.FIRST_AID_EGG.getData());
            }
        }, ticks + 1);
    }

    private static void setEggLook(Player player, int data) {
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            if (ShopItem.of(contents[i]) == ShopItem.FIRST_AID_EGG && contents[i].getDurability() != data) {
                contents[i].setDurability((short) data);
                player.getInventory().setItem(i, contents[i]);
            }
        }
    }
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (event.getPlayer().getGameMode() == GameMode.CREATIVE) {
            return;
        }
        event.setCancelled(true);
        Location location = event.getBlock().getLocation();
        Material tool = event.getPlayer().getItemInHand() == null ? Material.AIR : event.getPlayer().getItemInHand().getType();
        // 置かれた黒曜石だけ、ダイヤか金のピッケルで壊せる (何も落とさない)
        if ((tool == Material.DIAMOND_PICKAXE || tool == Material.GOLD_PICKAXE) && placedBlocks.remove(location) != null) {
            placedDirty = true;
            if (event.getBlock().getType() == Material.OBSIDIAN) {
                plugin.getSimpleEnchants().onObsidianBroken(event.getPlayer(), 1);
            }
            event.getBlock().setType(Material.AIR);
        }
    }

    /** Golden Pickaxe で置かれた黒曜石を 2 回叩くと、上下につながった黒曜石を 5 個まで壊す。 */
    @EventHandler(ignoreCancelled = true)
    public void onTap(BlockDamageEvent event) {
        Player player = event.getPlayer();
        Location location = event.getBlock().getLocation();
        if (ShopItem.of(event.getItemInHand()) != ShopItem.GOLDEN_PICKAXE || !placedBlocks.containsKey(location)) {
            return;
        }
        long now = System.currentTimeMillis();
        Long last = lastTapTime.get(player.getUniqueId());
        Location lastBlock = lastTapBlock.get(player.getUniqueId());
        lastTapTime.put(player.getUniqueId(), now);
        lastTapBlock.put(player.getUniqueId(), location);
        if (last == null || now - last > DOUBLE_TAP_MILLIS || lastBlock == null
                || lastBlock.getBlockX() != location.getBlockX() || lastBlock.getBlockZ() != location.getBlockZ()) {
            return;
        }
        lastTapTime.remove(player.getUniqueId());
        int broken = 0;
        for (int dy = 0; broken < PILLAR_HEIGHT && placedBlocks.containsKey(location.clone().add(0, dy, 0)); dy++) {
            broken += breakPlaced(location.clone().add(0, dy, 0));
        }
        for (int dy = -1; broken < PILLAR_HEIGHT && placedBlocks.containsKey(location.clone().add(0, dy, 0)); dy--) {
            broken += breakPlaced(location.clone().add(0, dy, 0));
        }
        if (broken > 0) {
            plugin.getSimpleEnchants().onObsidianBroken(player, broken);
            player.playSound(location, "dig.stone", 1.0f, 1.0f);
        }
    }

    private int breakPlaced(Location location) {
        placedBlocks.remove(location);
        placedDirty = true;
        location.getBlock().setType(Material.AIR);
        return 1;
    }
}
