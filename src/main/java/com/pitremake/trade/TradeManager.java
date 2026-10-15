package com.pitremake.trade;

import com.pitremake.PitPlugin;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * /trade (録画 2026-10-15_12-11-57.mcrc = 申し込んだ側、12-11-56.mcrc = 受けた側)。
 * <pre>
 * /trade 名前   : §6§lTRADE REQUEST! §7Sent to 名前§7! と mob.villager.haggle (1.0 / 63)
 *                相手に §6§lTRADE REQUEST! §7From 名前§7! §7[§e§lCLICK§7] (クリックで /tradeaccept &lt;ID&gt;、ホバー Click to accept!)
 * /tradeaccept : 2 人とも画面を閉じて haggle (0.66 / 66)、トレードの画面 ({@link TradeSession}) を開く
 * </pre>
 * レベル 60 から (スポーンの UNLOCKED FEATURES)。申し込みの期限 (60 秒)・断られたときなどのチャットは推測。
 * ゴールドの受け渡しは 1 日 50,000g まで (渡した量。サーバーを再起動すると数え直し)。
 */
public final class TradeManager implements Listener {

    static final int REQUIRED_LEVEL = 60;
    static final long DAILY_GOLD_LIMIT = 50_000;
    private static final long REQUEST_MILLIS = 60_000;

    private static final class Request {
        final UUID from;
        final UUID to;
        final long time;

        Request(UUID from, UUID to, long time) {
            this.from = from;
            this.to = to;
            this.time = time;
        }
    }

    private final PitPlugin plugin;
    private final Map<UUID, Request> requests = new HashMap<>();
    private final Map<UUID, TradeSession> sessions = new HashMap<>();
    /** 今日渡したゴールド (日付が変わったら 0)。 */
    private final Map<UUID, Long> dailyGold = new HashMap<>();
    /** 死んでキャンセルになったときに、リスポーンしてから返すアイテム。 */
    private final Map<UUID, List<ItemStack>> pendingReturns = new HashMap<>();
    private LocalDate day = LocalDate.now();

    public TradeManager(PitPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void stop() {
        for (TradeSession session : new HashSet<>(sessions.values())) {
            session.cancel(null);
        }
    }

    PitPlugin plugin() {
        return plugin;
    }

    /** チャットの名前 (録画: §7[§6§l68§7] §7SFlashCookie)。 */
    String name(Player player) {
        UUID id = player.getUniqueId();
        return plugin.getStatsManager().chatTag(id) + " " + plugin.getRankManager().nameColor(id) + player.getName();
    }

    private boolean levelOk(Player player) {
        return player.hasPermission("pit.admin") || plugin.getStatsManager().getLevel(player.getUniqueId()) >= REQUIRED_LEVEL;
    }

    /** /trade 名前 */
    public void request(Player from, Player to) {
        if (to == null || !to.isOnline() || plugin.getBotManager().isBot(to)) {
            from.sendMessage("§cCouldn't find that player!");
            return;
        }
        if (to.equals(from)) {
            from.sendMessage("§cYou can't trade with yourself!");
            return;
        }
        if (!levelOk(from)) {
            from.sendMessage("§cYou need to be level " + REQUIRED_LEVEL + " to trade!");
            return;
        }
        if (!levelOk(to)) {
            from.sendMessage("§cThat player needs to be level " + REQUIRED_LEVEL + " to trade!");
            return;
        }
        if (sessions.containsKey(from.getUniqueId()) || sessions.containsKey(to.getUniqueId())) {
            from.sendMessage("§cOne of you is already trading!");
            return;
        }
        // 同じ相手への前の申し込みは消す
        requests.values().removeIf(r -> r.from.equals(from.getUniqueId()) && r.to.equals(to.getUniqueId()));
        UUID id = UUID.randomUUID();
        requests.put(id, new Request(from.getUniqueId(), to.getUniqueId(), System.currentTimeMillis()));
        from.sendMessage("§6§lTRADE REQUEST! §7Sent to " + name(to) + "§7!");
        from.playSound(from.getLocation(), "mob.villager.haggle", 1.0f, 1.0f);
        TextComponent message = new TextComponent(TextComponent.fromLegacyText(
                "§6§lTRADE REQUEST! §7From " + name(from) + "§7! §7[§e§lCLICK§7]"));
        message.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tradeaccept " + id));
        message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder("Click to accept!").color(ChatColor.YELLOW).create()));
        to.spigot().sendMessage(message);
    }

    /** /tradeaccept ID */
    public void accept(Player to, String rawId) {
        Request request = null;
        try {
            request = requests.get(UUID.fromString(rawId));
        } catch (IllegalArgumentException ignored) {
            // 違う形の ID
        }
        long now = System.currentTimeMillis();
        for (Iterator<Request> it = requests.values().iterator(); it.hasNext(); ) {
            if (now - it.next().time > REQUEST_MILLIS) {
                it.remove();
            }
        }
        if (request == null || !request.to.equals(to.getUniqueId()) || now - request.time > REQUEST_MILLIS) {
            to.sendMessage("§cThat trade request has expired!");
            return;
        }
        Player from = plugin.getServer().getPlayer(request.from);
        requests.values().remove(request);
        if (from == null || !from.isOnline()) {
            to.sendMessage("§cThat player is no longer online!");
            return;
        }
        if (sessions.containsKey(from.getUniqueId()) || sessions.containsKey(to.getUniqueId())) {
            to.sendMessage("§cOne of you is already trading!");
            return;
        }
        TradeSession session = new TradeSession(this, from, to);
        sessions.put(from.getUniqueId(), session);
        sessions.put(to.getUniqueId(), session);
        session.open();
    }

    void ended(TradeSession session) {
        sessions.values().removeIf(s -> s == session);
    }

    long dailyGold(UUID player) {
        rollDay();
        Long used = dailyGold.get(player);
        return used == null ? 0 : used;
    }

    void addDailyGold(UUID player, long amount) {
        rollDay();
        dailyGold.merge(player, amount, Long::sum);
    }

    private void rollDay() {
        LocalDate today = LocalDate.now();
        if (!today.equals(day)) {
            day = today;
            dailyGold.clear();
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        TradeSession session = sessions.get(event.getPlayer().getUniqueId());
        if (session != null) {
            session.cancel(event.getPlayer());
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        TradeSession session = sessions.get(event.getEntity().getUniqueId());
        if (session != null) {
            session.cancel(event.getEntity(), event.getEntity());
        }
    }

    void returnAfterRespawn(UUID player, List<ItemStack> items) {
        if (!items.isEmpty()) {
            pendingReturns.computeIfAbsent(player, k -> new ArrayList<>()).addAll(items);
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        List<ItemStack> items = pendingReturns.remove(player.getUniqueId());
        if (items != null) {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                for (ItemStack item : items) {
                    TradeSession.give(player, item);
                }
            });
        }
    }
}
