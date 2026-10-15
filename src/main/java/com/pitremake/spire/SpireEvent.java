package com.pitremake.spire;

import com.pitremake.PitPlugin;
import com.pitremake.combat.DamageContext;
import com.pitremake.combat.DamageModifier;
import com.pitremake.perk.PerkItems;
import com.pitremake.util.NbtUtil;
import com.pitremake.util.PlayerPackets;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

/**
 * Spire (Major Event)。録画 2026-10-15_13-57-51.mcrc と pit.wiki/Spire。
 * <pre>
 * 3 分前 : §5§lMAJOR EVENT! §5§lSPIRE §7in 3 mins §7[§e§lINFO§7] (クリックでルールの本)
 * 2 分前 : 同じ予告 + §5§lSPIRE! §dSpawning in §f§lWinter §dquarter! と塔が下から建つ (約 5 秒)
 * 1 分前 : 同じ予告 + Will be accessible from the center of the pit! / NOTE! Everyone will be respawned on event start!
 *          スポーンの下に通路 (床はエンドポータルの枠、スポーンの真ん中は虹色のガラスで蓋)
 * 開始   : starting now、タイトル PIT EVENT! / SPIRE (色を変えて点滅)、蓋を開けて全員スポーンへ
 *          スポーンの真ん中から落ちて通路の床に着くと 1 階へ。持ち物は預かり (パークのアイテムは残す)、各階の装備で戦う
 * キル   : その階の Souls を得て 1 階上へ (9 階は上がらない)。死亡: Souls を失って 1 階下へ (1 階は下がらない)
 *          キルの XP・ゴールドは階の倍率 (-50%〜+250%)。タイトル YOU KILLED / YOU DIED と階、チャットに階と Souls
 * 5 分後 : PIT EVENT ENDED、報酬 (イベント中のキル + 順位 + Floor Bonus)、上位 3 人。持ち物を返してスポーンへ、塔を壊して地形を戻す
 * </pre>
 * Vampire と Barbarian は効かず、キルの回復アイテムは Fractured Soul、アシストの Soup は無し (pit.wiki)。
 * Floor Bonus 成功のチャット、7〜9 階の色は推測。Spire の間は Bot を消し、終わったらまた出す。
 */
public final class SpireEvent implements Listener, DamageModifier {

    private enum Phase { IDLE, WARNING, RUNNING }

    private static final String EVENT_ITEM_KEY = "PitEventItem";
    private static final double SOULRIPPER_PER_HIT = 0.15;
    private static final double SOULRIPPER_MAX = 0.75;
    private static final int FRACTURED_SOUL_LIMIT = 2;
    /** 通路の床 (エンドポータルの枠) の範囲: Pit の真ん中から半径 6.5、この高さより下。 */
    private static final double TUNNEL_RADIUS = 6.5;
    private static final double TUNNEL_FLOOR_Y = 84;

    /** 録画の塔の 1 階の床の真ん中のブロック。 */
    private static final int SPIRE_X = 35;
    private static final int SPIRE_Y = 84;
    private static final int SPIRE_Z = 45;

    public com.pitremake.map.EventPositions getPositions() {
        return positions;
    }

    /** 塔の場所を選び、録画の場所との差だけずらす (通路と蓋はスポーンの真ん中なので動かさない)。今のマップに場所が無ければ false。 */
    private boolean choosePosition() {
        position = positions.random();
        if (position == null) {
            return false;
        }
        structure.setOffset("build", position.x - SPIRE_X, position.y - SPIRE_Y, position.z - SPIRE_Z);
        return true;
    }

    private int dx() {
        return position == null ? 0 : position.x - SPIRE_X;
    }

    private int dy() {
        return position == null ? 0 : position.y - SPIRE_Y;
    }

    private int dz() {
        return position == null ? 0 : position.z - SPIRE_Z;
    }

    static final class Participant {
        final UUID id;
        int floor = 1;
        int maxFloor = 1;
        /** 入ったときにはいていたパンツの Paparazzi のレベル (0 = 無し)。 */
        int paparazzi;
        int souls;
        int kills;
        double xp;
        double gold;
        boolean bot;
        String name;

        Participant(UUID id) {
            this.id = id;
        }
    }

    private final PitPlugin plugin;
    private final SpireStructure structure;
    /** 塔を出す場所 (config の spire.positions。録画は Winter quarter の (35, 84, 45) = 1 階の床の真ん中)。 */
    private final com.pitremake.map.EventPositions positions;
    private com.pitremake.map.EventPositions.Position position;
    private final Map<UUID, Participant> participants = new HashMap<>();
    /** Soulripper: 攻撃した人 → {相手, 続けて当てた回数}。 */
    private final Map<UUID, Object[]> soulripper = new HashMap<>();
    private final java.util.Set<UUID> pendingRespawn = new java.util.HashSet<>();
    private final File stashFolder;
    private Phase phase = Phase.IDLE;
    /** 開始 (予告中) / 終了 (開催中) の時刻。 */
    private long phaseEnd;
    private long nextEvent;
    private int lastAnnouncedMinute = -1;
    private boolean available;
    private BukkitTask task;

    public SpireEvent(PitPlugin plugin) {
        this.plugin = plugin;
        this.positions = new com.pitremake.map.EventPositions(plugin, "spire.positions", "spire/positions.yml",
                new com.pitremake.map.EventPositions.Position(SPIRE_X, SPIRE_Y, SPIRE_Z, "Winter"));
        this.structure = new SpireStructure(plugin);
        this.stashFolder = new File(plugin.getDataFolder(), "spire-stash");
    }

    public void start() {
        available = structure.load(plugin.getPitMap().getName()) && structure.phaseNames().contains("build");
        // 塔の中 (各階の部屋) と上 3 ブロックも空気で置く (別の場所に出したとき地形が中に残らないように)
        structure.fillAir("build", 3);
        structure.recover();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        scheduleNext();
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 20, 20);
        // 前回 Spire の途中でサーバーが落ちた: 預かっていた持ち物は Item Stash へ (入ったときに /pickupstash で受け取る)
        File[] leftovers = stashFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (leftovers != null) {
            for (File file : leftovers) {
                try {
                    toItemStash(UUID.fromString(file.getName().substring(0, file.getName().length() - 4)));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Unknown spire stash file " + file);
                }
            }
        }
    }

    /** Spire で預かっていた持ち物 (と防具) を Item Stash に移す。 */
    private void toItemStash(UUID player) {
        File file = new File(stashFolder, player + ".yml");
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        List<ItemStack> items = new ArrayList<>();
        for (String key : new String[]{"contents", "armor"}) {
            for (Object raw : yaml.getList(key, new ArrayList<>())) {
                if (raw instanceof ItemStack && ((ItemStack) raw).getType() != Material.AIR) {
                    items.add((ItemStack) raw);
                }
            }
        }
        plugin.getItemStash().add(player, items);
        if (!file.delete()) {
            plugin.getLogger().warning("Could not delete " + file);
        }
    }

    public void stop() {
        if (task != null) {
            task.cancel();
        }
        if (phase != Phase.IDLE) {
            for (Participant p : new ArrayList<>(participants.values())) {
                Player player = plugin.getServer().getPlayer(p.id);
                if (player != null) {
                    leave(player, false);
                }
            }
            participants.clear();
            structure.restore(world());
            phase = Phase.IDLE;
            plugin.getBotManager().setPaused(false);
            plugin.getEventBossBar().hide();
        }
    }

    private World world() {
        return plugin.getServer().getWorlds().get(0);
    }

    private void scheduleNext() {
        nextEvent = System.currentTimeMillis() + (long) (plugin.getConfig().getDouble("spire.interval-minutes", 60) * 60_000);
    }

    public boolean isRunning() {
        return phase == Phase.RUNNING;
    }

    public boolean isActive() {
        return phase != Phase.IDLE;
    }

    public boolean inSpire(Player player) {
        return phase == Phase.RUNNING && participants.containsKey(player.getUniqueId());
    }

    // ------------------------------------------------------------------ 進行

    private void tick() {
        long now = System.currentTimeMillis();
        switch (phase) {
            case IDLE:
                if (available && plugin.getConfig().getBoolean("spire.enabled", false) && now >= nextEvent) {
                    if (plugin.isEventActive()) {
                        // 別のイベント (Auction / KOTH) の最中は終わるまで待つ
                        nextEvent = now + 60_000;
                    } else if (realPlayers() >= plugin.getConfig().getInt("spire.min-players", 1)) {
                        warn(SpireRules.WARNING_SECONDS);
                    } else {
                        scheduleNext();
                    }
                }
                break;
            case WARNING:
                long left = (phaseEnd - now + 999) / 1000;
                int minutes = (int) ((left + 59) / 60);
                if (left <= 0) {
                    begin();
                } else if (minutes < lastAnnouncedMinute) {
                    announceMinute(minutes);
                }
                break;
            case RUNNING:
                if (now >= phaseEnd) {
                    end();
                } else if (plugin.getSidebar() != null) {
                    plugin.getSidebar().updateAll();
                }
                break;
            default:
                break;
        }
    }

    private int realPlayers() {
        int count = 0;
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (!plugin.getBotManager().isBot(player)) {
                count++;
            }
        }
        return count;
    }

    /** /pit spire start: 予告から始める (seconds 秒後に開始)。 */
    public boolean warn(int seconds) {
        if (!available || phase != Phase.IDLE || plugin.isEventActive()) {
            return false;
        }
        if (!choosePosition()) {
            return false;
        }
        phase = Phase.WARNING;
        phaseEnd = System.currentTimeMillis() + seconds * 1000L;
        long warnStart = System.currentTimeMillis() - (SpireRules.WARNING_SECONDS - seconds) * 1000L;
        // ボスバー (録画: §5§lMAJOR EVENT! §5§lSPIRE! §7Starting in §e03:00§7、体力は 1 から満タンへ)
        plugin.getEventBossBar().show(bar("§7Starting in §e", warnStart, SpireRules.WARNING_SECONDS, true));
        lastAnnouncedMinute = -1;
        announceMinute((seconds + 59) / 60);
        // 予告の途中から始めたときは、もう出ているはずの塔・通路をすぐ置く
        if (seconds <= 120) {
            structure.place(world(), "build");
        }
        if (seconds <= 60) {
            structure.place(world(), "tunnel");
        }
        return true;
    }

    private void announceMinute(int minutes) {
        lastAnnouncedMinute = minutes;
        broadcastClickable("§5§lMAJOR EVENT! §5§lSPIRE §7in " + minutes + (minutes == 1 ? " min" : " mins") + " §7[§e§lINFO§7]",
                "Click to view details!");
        if (minutes == 2) {
            broadcast("§5§lSPIRE! §dSpawning in §f§l" + position.name + " §dquarter!");
            structure.placeAnimated(world(), "build", 100);
        } else if (minutes == 1) {
            broadcast("§5§lSPIRE! §7Will be accessible from the center of the pit!");
            broadcast("§e§lNOTE! §7Everyone will be respawned on event start!");
            structure.place(world(), "tunnel");
        }
    }

    /** /pit spire now: すぐ始める (予告中ならそのまま開始)。 */
    public boolean beginNow() {
        if (!available || phase == Phase.RUNNING) {
            return false;
        }
        if (phase == Phase.IDLE && plugin.isEventActive()) {
            return false;
        }
        if (phase == Phase.IDLE && !choosePosition()) {
            return false;
        }
        if (phase == Phase.IDLE) {
            structure.place(world(), "build");
            structure.place(world(), "tunnel");
        }
        begin();
        return true;
    }

    private void begin() {
        phase = Phase.RUNNING;
        phaseEnd = System.currentTimeMillis() + SpireRules.DURATION_SECONDS * 1000L;
        participants.clear();
        structure.place(world(), "open");
        // ボスバー (録画: §7Ending in §a05:00§7、満タンから減る)
        plugin.getEventBossBar().show(bar("§7Ending in §a", System.currentTimeMillis(), SpireRules.DURATION_SECONDS, false));
        broadcast("§5§lMAJOR EVENT! §5§lSPIRE §7starting now");
        // 録画: PIT EVENT! (金 / 紫) と SPIRE を 4 tick ごとに色を変えて点滅
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            PlayerPackets.title(player, "§6§lPIT EVENT!", "§5§lSPIRE", 22, 80, 0);
        }
        for (int i = 0; i < 6; i++) {
            String color = i % 2 == 0 ? "§5" : "§6";
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    PlayerPackets.title(player, color + "§lPIT EVENT!", "§5§lSPIRE", 0, 40, 10);
                }
            }, 50L + i * 4);
        }
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (!plugin.getBotManager().isBot(player)) {
                plugin.getKillListener().sendToSpawn(player);
            }
        }
        // Spire の間は Bot を全部消す (終わったらまた出る)
        plugin.getBotManager().setPaused(true);
    }

    // ------------------------------------------------------------------ 入る・出る

    /** 通路の床に着いたら 1 階へ。 */
    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (phase != Phase.RUNNING) {
            return;
        }
        Location to = event.getTo();
        Player player = event.getPlayer();
        if (to.getY() < TUNNEL_FLOOR_Y && Math.hypot(to.getX(), to.getZ()) <= TUNNEL_RADIUS
                && !participants.containsKey(player.getUniqueId()) && to.getWorld().equals(world())) {
            enter(player);
        }
    }

    /**
     * Bot が通路 (スポーンの真ん中の筒) に落ちたら 1 階へ (BotManager から毎 tick)。Bot は PlayerMoveEvent が出ないので、
     * 床のエンドポータルに触る前 (筒の途中) で拾う。触るとジ・エンドへ送られようとして Bot が落ちる。
     */
    public void catchFallingBot(Player bot) {
        if (phase != Phase.RUNNING || participants.containsKey(bot.getUniqueId())) {
            return;
        }
        Location at = bot.getLocation();
        if (at.getWorld().equals(world()) && at.getY() < BOT_TUNNEL_Y && Math.hypot(at.getX(), at.getZ()) <= TUNNEL_RADIUS) {
            enter(bot);
        }
    }

    /** 筒はスポーンの床 (y114) の下から y82 まで。Bot はこれより下に来たら拾う。 */
    private static final double BOT_TUNNEL_Y = 112;

    private void enter(Player player) {
        boolean bot = plugin.getBotManager().isBot(player);
        Participant p = new Participant(player.getUniqueId());
        p.bot = bot;
        p.name = plugin.getStatsManager().chatTag(player.getUniqueId()) + " " + plugin.getRankManager().nameColor(player.getUniqueId())
                + player.getName();
        participants.put(player.getUniqueId(), p);
        if (!bot) {
            // 持ち物を預かる前に、はいているパンツの Paparazzi を見ておく
            p.paparazzi = paparazziLevel(player.getInventory().getLeggings());
            stash(player);
        }
        teleport(player, 1);
        giveKit(player, 1);
        if (!bot) {
            player.sendMessage("");
            player.sendMessage(" §5§lSPIRE: " + SpireRules.name(1) + "!");
            player.sendMessage(SpireRules.color(1) + " " + SpireRules.color(1) + "Rewards: " + SpireRules.rewardText(1));
            player.sendMessage("");
        }
    }

    /** 持ち物を返して出す (終了・抜けたとき)。toSpawn ならスポーンへ。 */
    private void leave(Player player, boolean toSpawn) {
        Participant p = participants.remove(player.getUniqueId());
        if (p == null) {
            return;
        }
        if (p.bot) {
            if (toSpawn) {
                plugin.getBotManager().respawn(player);
            }
            return;
        }
        removeEventItems(player);
        restoreStash(player);
        if (toSpawn) {
            plugin.getKillListener().sendToSpawn(player);
        }
    }

    private void teleport(Player player, int floor) {
        double angle = ThreadLocalRandom.current().nextDouble(Math.PI * 2);
        double r = SpireRules.radius(floor);
        double cx = SpireRules.CENTER_X + dx();
        double cz = SpireRules.CENTER_Z + dz();
        Location at = new Location(world(), cx + Math.cos(angle) * r, SpireRules.floorY(floor) + dy(), cz + Math.sin(angle) * r);
        // 塔の真ん中を向く
        at.setYaw((float) Math.toDegrees(Math.atan2(-(cx - at.getX()), cz - at.getZ())));
        player.setFallDistance(0);
        player.setVelocity(new org.bukkit.util.Vector());
        player.teleport(at);
    }

    // ------------------------------------------------------------------ 持ち物

    /** パークのアイテム以外を預かる (plugins/Pit/spire-stash に保存してから消す)。 */
    private void stash(Player player) {
        PlayerInventory inventory = player.getInventory();
        List<ItemStack> contents = new ArrayList<>();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack item = inventory.getItem(slot);
            if (item != null && item.getType() != Material.AIR && PerkItems.of(item) == null) {
                contents.add(item);
                inventory.setItem(slot, null);
            } else {
                contents.add(null);
            }
        }
        ItemStack[] armor = inventory.getArmorContents();
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("contents", contents);
        yaml.set("armor", Arrays.asList(armor));
        if (!stashFolder.isDirectory() && !stashFolder.mkdirs()) {
            plugin.getLogger().warning("Could not create " + stashFolder);
        }
        try {
            yaml.save(new File(stashFolder, player.getUniqueId() + ".yml"));
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save the spire stash of " + player.getName(), e);
        }
        inventory.setArmorContents(new ItemStack[4]);
        player.updateInventory();
    }

    /** 預かっていた持ち物を返す (ファイルがあれば。サーバーが途中で止まったときも次に入ったときに返す)。 */
    private void restoreStash(Player player) {
        File file = new File(stashFolder, player.getUniqueId() + ".yml");
        if (!file.exists() || participants.containsKey(player.getUniqueId())) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        PlayerInventory inventory = player.getInventory();
        removeEventItems(player);
        List<?> contents = yaml.getList("contents", new ArrayList<>());
        for (int slot = 0; slot < contents.size() && slot < inventory.getSize(); slot++) {
            Object item = contents.get(slot);
            if (item instanceof ItemStack) {
                ItemStack old = inventory.getItem(slot);
                inventory.setItem(slot, (ItemStack) item);
                if (old != null && old.getType() != Material.AIR) {
                    give(player, old);
                }
            }
        }
        List<?> armor = yaml.getList("armor", new ArrayList<>());
        ItemStack[] pieces = new ItemStack[4];
        for (int i = 0; i < 4 && i < armor.size(); i++) {
            pieces[i] = armor.get(i) instanceof ItemStack ? (ItemStack) armor.get(i) : null;
        }
        inventory.setArmorContents(pieces);
        player.updateInventory();
        if (!file.delete()) {
            plugin.getLogger().warning("Could not delete " + file);
        }
    }

    private static void give(Player player, ItemStack item) {
        for (ItemStack left : player.getInventory().addItem(item).values()) {
            player.getWorld().dropItem(player.getLocation(), left);
        }
    }

    private static boolean isEventItem(ItemStack item) {
        return item != null && item.getType() != Material.AIR && NbtUtil.has(item, EVENT_ITEM_KEY);
    }

    private static void removeEventItems(Player player) {
        PlayerInventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (isEventItem(inventory.getItem(slot))) {
                inventory.setItem(slot, null);
            }
        }
        ItemStack[] armor = inventory.getArmorContents();
        for (int i = 0; i < armor.length; i++) {
            if (isEventItem(armor[i])) {
                armor[i] = null;
            }
        }
        inventory.setArmorContents(armor);
    }

    // ------------------------------------------------------------------ 装備

    /**
     * 各階の装備 (pit.wiki): 1 階は Spireblade (木) だけ、2〜3 階 革 / チェーン / 革 + 石、4〜5 階 鉄 / チェーン / 鉄 + 鉄、
     * 6〜7 階 ダイヤ / 鉄 / ダイヤ + ダイヤ、8〜9 階 全部ダイヤ。前の階の装備は消して付け替える (Fractured Soul は残す)。
     */
    private void giveKit(Player player, int floor) {
        PlayerInventory inventory = player.getInventory();
        int soulSlotCount = 0;
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack item = inventory.getItem(slot);
            if (isEventItem(item) && item.getType() != Material.MAGMA_CREAM) {
                inventory.setItem(slot, null);
            } else if (isEventItem(item)) {
                soulSlotCount++;
            }
        }
        Material sword;
        Material[] armor;
        if (floor <= 1) {
            sword = Material.WOOD_SWORD;
            armor = new Material[]{null, null, null};
        } else if (floor <= 3) {
            sword = Material.STONE_SWORD;
            armor = new Material[]{Material.LEATHER_CHESTPLATE, Material.CHAINMAIL_LEGGINGS, Material.LEATHER_BOOTS};
        } else if (floor <= 5) {
            sword = Material.IRON_SWORD;
            armor = new Material[]{Material.IRON_CHESTPLATE, Material.CHAINMAIL_LEGGINGS, Material.IRON_BOOTS};
        } else if (floor <= 7) {
            sword = Material.DIAMOND_SWORD;
            armor = new Material[]{Material.DIAMOND_CHESTPLATE, Material.IRON_LEGGINGS, Material.DIAMOND_BOOTS};
        } else {
            sword = Material.DIAMOND_SWORD;
            armor = new Material[]{Material.DIAMOND_CHESTPLATE, Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS};
        }
        ItemStack blade = eventItem(new ItemStack(sword), "§5Spireblade", Arrays.asList("§7Event item", "",
                "§bSoulripper", "§7Consecutive hits on a player", "§7deal §c+15% damage §7per hit up", "§7to §c+75%§7."));
        ItemStack held = inventory.getItem(0);
        inventory.setItem(0, blade);
        if (held != null && held.getType() != Material.AIR && !isEventItem(held)) {
            give(player, held);
        }
        ItemStack[] current = inventory.getArmorContents();
        Material[] slots = {armor[2], armor[1], armor[0]};
        for (int i = 0; i < 3; i++) {
            // getArmorContents: 0 足 1 脚 2 胴 3 頭。パークの防具 (金のパンツなど) は残す
            if (current[i] != null && PerkItems.of(current[i]) != null) {
                continue;
            }
            current[i] = slots[i] == null ? null : eventItem(new ItemStack(slots[i]), null, Arrays.asList("§7Event item"));
        }
        inventory.setArmorContents(current);
        if (soulSlotCount == 0 && !plugin.getBotManager().isBot(player)) {
            // 入ったときに 1 つ
            give(player, fracturedSoul());
        }
        player.updateInventory();
    }

    private static ItemStack eventItem(ItemStack item, String name, List<String> lore) {
        ItemMeta meta = item.getItemMeta();
        if (name != null) {
            meta.setDisplayName(name);
        }
        meta.setLore(lore);
        meta.spigot().setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return NbtUtil.setInt(item, EVENT_ITEM_KEY, 1);
    }

    /** Fractured Soul (録画のアイテム): 右クリックで 4❤ 回復、3❤ 衝撃吸収、Resistance II (3 秒)。 */
    private static ItemStack fracturedSoul() {
        ItemStack item = new ItemStack(Material.MAGMA_CREAM);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§bFractured Soul");
        meta.setLore(Arrays.asList("§7Event item", "§9Resistance II (0:03)", "§74§c❤ §7heal", "§73§6❤ §7absorption"));
        item.setItemMeta(meta);
        return NbtUtil.setInt(item, EVENT_ITEM_KEY, 1);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.MAGMA_CREAM || !isEventItem(item)) {
            return;
        }
        event.setCancelled(true);
        Player player = event.getPlayer();
        player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + 8));
        net.minecraft.server.v1_8_R3.EntityPlayer handle = ((org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer) player).getHandle();
        handle.setAbsorptionHearts(Math.max(handle.getAbsorptionHearts(), 6));
        player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 60, 1), true);
        player.playSound(player.getLocation(), "random.eat", 1.0f, 1.0f);
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.setItemInHand(null);
        }
    }

    /** キルの回復アイテムの代わり (PerkManager から)。持てるのは 2 個まで (推測)。 */
    public boolean giveHealingItem(Player killer) {
        if (!inSpire(killer)) {
            return false;
        }
        int count = 0;
        for (ItemStack item : killer.getInventory().getContents()) {
            if (item != null && item.getType() == Material.MAGMA_CREAM && isEventItem(item)) {
                count += item.getAmount();
            }
        }
        if (count < FRACTURED_SOUL_LIMIT) {
            give(killer, fracturedSoul());
        }
        return true;
    }

    /** イベントのアイテムは捨てられない。 */
    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (isEventItem(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§c§lNOPE! §7You cannot drop this item!");
        }
    }

    /** Soulripper: 同じ相手に続けて当てると 1 回ごとに +15% (最大 +75%)。別の相手に当てると数え直し。 */
    @Override
    public void modify(DamageContext context) {
        if (!context.byPlayer() || !context.melee || !isEventItem(context.attacker.getItemInHand())
                || !context.attacker.getItemInHand().getType().name().endsWith("_SWORD")) {
            return;
        }
        UUID id = context.attacker.getUniqueId();
        Object[] stack = soulripper.get(id);
        int hits = stack != null && context.victim.getUniqueId().equals(stack[0]) ? (Integer) stack[1] : 0;
        context.increase(Math.min(SOULRIPPER_MAX, SOULRIPPER_PER_HIT * hits));
        if (!context.isExtraStrike()) {
            soulripper.put(id, new Object[]{context.victim.getUniqueId(), hits + 1});
        }
    }

    // ------------------------------------------------------------------ キル・死亡 (KillListener から)

    /** Paparazzi: 1 レベルあたりのボーナスの増え方と、1 回のイベントで減る Lives (pitpal の説明文)。 */
    private static final double PAPARAZZI_BONUS = 0.5;
    private static final int PAPARAZZI_LIVES = 2;

    private static int paparazziLevel(ItemStack leggings) {
        if (leggings == null || leggings.getType() == Material.AIR) {
            return 0;
        }
        com.pitremake.item.MysticData data = com.pitremake.item.MysticData.read(leggings);
        return data == null ? 0 : data.getEnchantLevel(com.pitremake.enchant.list.PantsEnchants.PAPARAZZI);
    }

    /** はいている Paparazzi のパンツの Lives を減らす (0 になったら壊れる)。 */
    private void consumePaparazziLives(Player player) {
        ItemStack leggings = player.getInventory().getLeggings();
        if (paparazziLevel(leggings) <= 0) {
            return;
        }
        com.pitremake.item.MysticData data = com.pitremake.item.MysticData.read(leggings);
        int lives = data.getLives() - PAPARAZZI_LIVES;
        if (lives <= 0) {
            player.getInventory().setLeggings(null);
            player.playSound(player.getLocation(), "random.break", 1.0f, 1.0f);
        } else {
            data.setLives(lives);
            player.getInventory().setLeggings(com.pitremake.item.MysticItems.applyDisplay(data.write(leggings),
                    plugin.getEnchantRegistry()));
        }
    }

    /** キルの XP・ゴールドに掛ける数 (塔の中の人だけ、階の倍率)。 */
    public double rewardMultiplier(Player killer) {
        Participant p = inSpire(killer) ? participants.get(killer.getUniqueId()) : null;
        return p == null ? 1 : 1 + SpireRules.rewardBonus(p.floor);
    }

    /** イベント中に稼いだ分 (終わりの Your rewards に足す)。 */
    public void recordReward(Player killer, double xp, double gold) {
        Participant p = inSpire(killer) ? participants.get(killer.getUniqueId()) : null;
        if (p != null) {
            p.xp += xp;
            p.gold += gold;
        }
    }

    public void onKill(Player killer) {
        Participant p = inSpire(killer) ? participants.get(killer.getUniqueId()) : null;
        if (p == null) {
            return;
        }
        int gained = SpireRules.killSouls(p.floor);
        p.souls += gained;
        p.kills++;
        if (p.floor < SpireRules.FLOORS) {
            p.floor++;
            p.maxFloor = Math.max(p.maxFloor, p.floor);
            teleport(killer, p.floor);
            giveKit(killer, p.floor);
        }
        if (!p.bot) {
            PlayerPackets.title(killer, "§aYOU KILLED", SpireRules.name(p.floor).replace("§l", ""), 0, 40, 20);
            floorChat(killer, p, "§7from kill", "§a+" + gained);
        }
    }

    /**
     * 塔の中で死んだら true (スポーンへは送らず、1 階下に出す)。now = HP 0 の仕組みで死んだとき (すぐ出す)。
     * 本当に死んだときはリスポーンしてから ({@link #afterRespawn})。
     */
    public boolean onDeath(Player victim, boolean now) {
        Participant p = inSpire(victim) ? participants.get(victim.getUniqueId()) : null;
        if (p == null) {
            return false;
        }
        int lost = Math.min(p.souls, SpireRules.deathSouls(p.floor));
        p.souls -= lost;
        boolean bottom = p.floor <= 1;
        if (!bottom) {
            p.floor--;
        }
        soulripper.remove(victim.getUniqueId());
        if (!p.bot) {
            PlayerPackets.title(victim, "§cYOU DIED", SpireRules.name(p.floor).replace("§l", ""), 0, 40, 20);
            floorChat(victim, p, p.floor <= 1 ? "§7(bottom floor)" : "§7from death", lost > 0 ? "§c-" + lost : "§c0");
        }
        if (now) {
            respawn(victim, p.floor);
        } else {
            pendingRespawn.add(victim.getUniqueId());
        }
        return true;
    }

    public void afterRespawn(Player player) {
        Participant p = participants.get(player.getUniqueId());
        if (pendingRespawn.remove(player.getUniqueId()) && p != null && inSpire(player)) {
            respawn(player, p.floor);
        }
    }

    /** 録画のチャット: 空行 / " §5§lSPIRE: §c§lFLOOR TWO! §7from kill" / " §cSouls: §b1 §7(§a+1§7)  §cRewards: §c-25%" / 空行。 */
    private static void floorChat(Player player, Participant p, String reason, String change) {
        String color = SpireRules.color(p.floor);
        player.sendMessage("");
        player.sendMessage(" §5§lSPIRE: " + SpireRules.name(p.floor) + "! " + reason);
        player.sendMessage(" " + color + "Souls: §b" + p.souls + " §7(" + change + "§7)  " + color + "Rewards: " + SpireRules.rewardText(p.floor));
        player.sendMessage("");
    }

    /** 体力などを戻して、その階のどこかに出す。 */
    private void respawn(Player player, int floor) {
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
        player.setHealth(player.getMaxHealth());
        ((org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer) player).getHandle().setAbsorptionHearts(0);
        player.setFoodLevel(20);
        player.setFireTicks(0);
        teleport(player, floor);
        giveKit(player, floor);
        plugin.getSimpleEnchants().onSpawn(player);
    }

    // ------------------------------------------------------------------ 終わり

    /** /pit spire end と時間切れ。 */
    public boolean end() {
        if (phase == Phase.IDLE) {
            return false;
        }
        plugin.getEventBossBar().hide();
        if (phase == Phase.WARNING) {
            phase = Phase.IDLE;
            structure.restore(world());
            scheduleNext();
            broadcast("§5§lSPIRE! §7The event was cancelled.");
            return true;
        }
        phase = Phase.IDLE;
        List<Participant> ranking = new ArrayList<>(participants.values());
        ranking.sort((x, y) -> y.souls != x.souls ? Integer.compare(y.souls, x.souls) : Integer.compare(y.kills, x.kills));
        List<String> top = new ArrayList<>();
        for (int i = 0; i < Math.min(3, ranking.size()); i++) {
            Participant p = ranking.get(i);
            top.add("§6§l  §e§l#" + (i + 1) + " " + p.name + " §ewith §b" + p.souls + " souls §7(§c" + p.kills + " kills§7)");
        }
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (plugin.getBotManager().isBot(player)) {
                continue;
            }
            Participant p = participants.get(player.getUniqueId());
            player.sendMessage("§6§m----------------------");
            sendClickable(player, "§6§lPIT EVENT ENDED: §5§lSPIRE §7[§e§lINFO§7]", "Click to learn more about this event!");
            if (p != null && !p.bot) {
                int rank = ranking.indexOf(p) + 1;
                int[] placement = SpireRules.placementReward(rank);
                boolean bonus = p.maxFloor >= SpireRules.BONUS_FLOOR;
                // Paparazzi: イベントのボーナス (順位の報酬と Floor Bonus の XP・ゴールド・Renown) が +50% / +100% / +150%。キルの報酬は対象外
                double paparazzi = 1 + PAPARAZZI_BONUS * p.paparazzi;
                long bonusXp = Math.round((bonus ? SpireRules.BONUS_XP : 0) * paparazzi);
                double bonusGold = (placement[0] + (bonus ? SpireRules.BONUS_GOLD : 0)) * paparazzi;
                long xp = Math.round(p.xp) + bonusXp;
                double gold = p.gold + bonusGold;
                // Renown も Paparazzi で増える (例: Paparazzi III で 2 → 5)
                int renown = (int) Math.round((placement[1] + (bonus ? SpireRules.BONUS_RENOWN : 0)) * paparazzi);
                if (bonusXp > 0) {
                    plugin.getKillListener().giveXp(player, bonusXp);
                }
                plugin.getGoldManager().add(player.getUniqueId(), bonusGold);
                if (renown > 0) {
                    plugin.getStatsManager().setRenown(player.getUniqueId(), plugin.getStatsManager().getRenown(player.getUniqueId()) + renown);
                }
                player.sendMessage("§6§lYour rewards: §b+" + String.format(java.util.Locale.US, "%,d", xp) + "XP §6+§6"
                        + String.format(java.util.Locale.US, "%,d", Math.round(gold)) + "g" + (renown > 0 ? " §e+" + renown + " Renown" : ""));
                player.sendMessage(bonus
                        ? "§6§lFloor Bonus: §a§lSUCCESS! §7you reached floor " + SpireRules.name(SpireRules.BONUS_FLOOR).replace("§l", "") + "§7!"
                        : "§6§lFloor Bonus: §c§lFAILED! §7you have to reach floor " + SpireRules.name(SpireRules.BONUS_FLOOR).replace("§l", "")
                        + " §7but only got to " + SpireRules.name(p.maxFloor).replace("§l", "") + "§7!");
                player.sendMessage("§6§lYou: §b" + p.souls + " souls §7(ranked #" + rank + ")");
                if (p.paparazzi > 0) {
                    player.sendMessage("§d§lPAPARAZZI! §7Event bonus rewards §d+" + Math.round(PAPARAZZI_BONUS * p.paparazzi * 100)
                            + "%§7! §c-" + PAPARAZZI_LIVES + " lives");
                }
            }
            player.sendMessage("§6§lTop players:");
            for (String line : top) {
                player.sendMessage(line);
            }
            player.sendMessage("§6§m----------------------");
        }
        for (Participant p : new ArrayList<>(participants.values())) {
            Player player = plugin.getServer().getPlayer(p.id);
            if (player != null) {
                leave(player, true);
                if (p.paparazzi > 0) {
                    // 持ち物を返した後で、Paparazzi のパンツの Lives を 2 減らす
                    consumePaparazziLives(player);
                }
            }
        }
        participants.clear();
        soulripper.clear();
        plugin.getBotManager().setPaused(false);
        structure.restore(world());
        scheduleNext();
        if (plugin.getSidebar() != null) {
            plugin.getSidebar().updateAll();
        }
        return true;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Participant p = participants.get(player.getUniqueId());
        if (p != null && !p.bot) {
            // Spire の途中で抜けた: 預かっていた持ち物は Item Stash へ (次に入ったときに ITEM STASH! → /pickupstash)。塔には戻らない
            participants.remove(player.getUniqueId());
            removeEventItems(player);
            toItemStash(player.getUniqueId());
            return;
        }
        leave(player, false);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!participants.containsKey(event.getPlayer().getUniqueId())) {
            toItemStash(event.getPlayer().getUniqueId());
        }
    }

    // ------------------------------------------------------------------ サイドバー・本

    /** サイドバーのイベントの行 (開催中だけ。録画: Event / Remaining / Floor / Souls (#順位))。 */
    public List<String> sidebarLines(Player player) {
        List<String> lines = new ArrayList<>();
        if (phase != Phase.RUNNING) {
            return lines;
        }
        long left = (phaseEnd - System.currentTimeMillis()) / 1000;
        lines.add("Event: §5§lSPIRE");
        lines.add("Remaining: §a" + SpireRules.time(left));
        Participant p = participants.get(player.getUniqueId());
        if (p != null) {
            lines.add("Floor: " + SpireRules.name(p.floor));
            int rank = 1;
            for (Participant other : participants.values()) {
                if (other.souls > p.souls) {
                    rank++;
                }
            }
            lines.add("Souls: §b" + p.souls + (p.souls > 0 ? " §7(§e#" + rank + "§7)" : ""));
        } else {
            lines.add("Souls: §b0");
        }
        return lines;
    }

    /** ボスバーの中身。filling = 予告中 (時間が経つほど伸びる)。 */
    private com.pitremake.util.EventBossBar.Content bar(String label, long from, int seconds, boolean filling) {
        return new com.pitremake.util.EventBossBar.Content() {
            @Override
            public String text(int tick) {
                long left = Math.max(0, (phaseEnd - System.currentTimeMillis() + 999) / 1000);
                return com.pitremake.util.EventBossBar.shimmer("MAJOR EVENT!", "§5", "§d", tick) + " §5§lSPIRE! " + label
                        + SpireRules.time(left) + "§7";
            }

            @Override
            public double fraction() {
                double done = (System.currentTimeMillis() - from) / (seconds * 1000.0);
                return filling ? done : 1 - done;
            }
        };
    }

    private void broadcast(String message) {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            player.sendMessage(message);
        }
        plugin.getServer().getConsoleSender().sendMessage(message);
    }

    private void broadcastClickable(String text, String hover) {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            sendClickable(player, text, hover);
        }
        plugin.getServer().getConsoleSender().sendMessage(text);
    }

    private static void sendClickable(Player player, String text, String hover) {
        TextComponent message = new TextComponent(TextComponent.fromLegacyText(text));
        message.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/eventbook spire"));
        message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(hover).color(ChatColor.YELLOW).create()));
        player.spigot().sendMessage(message);
    }

    /** /eventbook spire: ルールの本 (録画の本) を開く。 */
    public void openBook(Player player) {
        EventBook.open(player);
    }
}
