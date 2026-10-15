package com.pitremake.scoreboard;

import com.pitremake.PitPlugin;
import com.pitremake.bounty.BountyRules;
import com.pitremake.gold.GoldManager;
import com.pitremake.stats.PitLevels;
import com.pitremake.stats.PrestigeRules;
import com.pitremake.util.RomanNumerals;
import com.pitremake.stats.StatsManager;
import com.pitremake.util.PlayerPackets;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * 実験的な Pit のサイドバー。録画の Hypixel と同じく 1 秒ごとに更新し、行はチームの prefix / suffix で書き換える。
 * <pre>
 * THE HYPIXEL PIT
 * 10/13/26  M6C
 *
 * Level: [2]
 * Needed XP: 1
 *
 * Gold: 23.70g
 *
 * Status: Idling       (戦闘中は Fighting (10) と残りの秒数。メガストリークの発動中はその名前)
 * Bounty: 1,800g       (バウンティがあるときだけ)
 * Streak: 3            (キルストリーク中だけ)
 * Strength: V (7)      (Strength-Chaining のレベルと残りの秒数)
 *
 * www.hypixel.net
 * </pre>
 * 同じスコアボードで tab と頭の上の名前 ({@link PitNametags}) も更新し、参加時に tab の上下の文字を送る。
 */
public final class PitSidebar implements Listener {

    private static final String OBJECTIVE = "pit";
    /** サイドバーは 15 行まで (行の名前には色コード §0〜§e を使う)。 */
    private static final int MAX_LINES = 15;
    private static final DecimalFormat NUMBER = new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(Locale.US));
    /** ストリークは Assistant (to the) Streaker で小数になる。 */
    private static final DecimalFormat STREAK = new DecimalFormat("#,##0.##", DecimalFormatSymbols.getInstance(Locale.US));

    private final PitPlugin plugin;
    private final Map<UUID, Scoreboard> boards = new HashMap<>();
    private final PitNametags nametags = new PitNametags();
    private BukkitTask task;

    public PitSidebar(PitPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            show(player);
        }
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::updateAll, 20, 20);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
        }
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (boards.containsKey(player.getUniqueId())) {
                player.setScoreboard(plugin.getServer().getScoreboardManager().getMainScoreboard());
            }
        }
        boards.clear();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        show(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        boards.remove(event.getPlayer().getUniqueId());
    }

    /** スコアボード (tab の名前のチームは常に、サイドバーは設定で有効なときだけ) と tab の上下の文字。 */
    private void show(Player player) {
        Scoreboard board = plugin.getServer().getScoreboardManager().getNewScoreboard();
        if (plugin.getConfig().getBoolean("scoreboard.enabled", true)) {
            Objective objective = board.registerNewObjective(OBJECTIVE, "dummy");
            objective.setDisplayName(color(plugin.getConfig().getString("scoreboard.title", "&f&lTHE &e&lHYPIXEL PIT")));
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        }
        boards.put(player.getUniqueId(), board);
        player.setScoreboard(board);
        PlayerPackets.tabHeaderFooter(player,
                color(plugin.getConfig().getString("tab.header", "&bYou are playing on &e&lMC.HYPIXEL.NET")),
                color(plugin.getConfig().getString("tab.footer", "&aRanks, Boosters & MORE! &c&lSTORE.HYPIXEL.NET")));
        update(player);
    }

    /** 全員のサイドバーと tab を書き直す (キルやバウンティの変化の直後にも呼ぶ)。 */
    public void updateAll() {
        List<PitNametags.Entry> entries = tabEntries();
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            update(player, entries);
        }
    }

    public void update(Player player) {
        update(player, tabEntries());
    }

    private void update(Player player, List<PitNametags.Entry> entries) {
        Scoreboard board = boards.get(player.getUniqueId());
        if (board == null) {
            return;
        }
        nametags.apply(board, entries);
        if (board.getObjective(OBJECTIVE) == null) {
            return;
        }
        StatsManager stats = plugin.getStatsManager();
        UUID id = player.getUniqueId();
        int bounty = plugin.getBountyManager().get(id);
        String status = plugin.getMegastreakManager().statusName(player);
        if (status == null && plugin.getSpireEvent() != null && plugin.getSpireEvent().isRunning()
                && stats.combatSecondsLeft(id, BountyRules.combatExtensionMillis(bounty)) <= 0) {
            status = "§6Event";
        }
        if (status == null) {
            // 戦闘中は残りの秒数も (Strength と同じ形: Fighting (10))
            int left = stats.combatSecondsLeft(id, BountyRules.combatExtensionMillis(bounty));
            status = left > 0 ? "§cFighting §7(" + left + ")" : "§aIdling";
        }
        List<String> lines = lines(
                new SimpleDateFormat("MM/dd/yy", Locale.US).format(new Date()),
                plugin.getConfig().getString("scoreboard.server", "M1A"),
                stats.getPrestige(id),
                stats.getXp(id),
                plugin.getGoldManager().get(id),
                status,
                bounty,
                stats.getExactStreak(id),
                plugin.getPerkManager().strengthLevel(player),
                plugin.getPerkManager().strengthSeconds(player),
                color(plugin.getConfig().getString("scoreboard.footer", "&ewww.hypixel.net")));
        // Spire の開催中はフッターの前の空行の後ろに Event / Remaining / Floor / Souls (録画)
        if (plugin.getSpireEvent() != null) {
            List<String> spire = plugin.getSpireEvent().sidebarLines(player);
            if (!spire.isEmpty()) {
                lines.addAll(lines.size() - 1, spire);
                lines.add(lines.size() - 1, "");
                // 15 行に収まらなければ途中の空行から詰める (フッターは残す)
                for (int i = 2; lines.size() > MAX_LINES && i < lines.size() - 2; i++) {
                    if (lines.get(i).isEmpty()) {
                        lines.remove(i--);
                    }
                }
            }
        }
        render(board, lines);
    }

    /** tab に出すプレイヤーと Bot。 */
    private List<PitNametags.Entry> tabEntries() {
        List<PitNametags.Entry> entries = new ArrayList<>();
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            UUID id = player.getUniqueId();
            entries.add(new PitNametags.Entry(player.getName(), plugin.getStatsManager().getPrestige(id),
                    plugin.getStatsManager().getLevel(id), plugin.getBountyManager().get(id),
                    plugin.getMegastreakManager().tabTag(player), plugin.getRankManager().nameColor(id)));
        }
        Map<String, Integer> botPrestiges = plugin.getBotManager().prestiges();
        for (Map.Entry<String, Integer> bot : plugin.getBotManager().levels().entrySet()) {
            Integer prestige = botPrestiges.get(bot.getKey());
            entries.add(new PitNametags.Entry(bot.getKey(), prestige == null ? 0 : prestige, bot.getValue(), 0, null, "§7"));
        }
        return entries;
    }

    static List<String> lines(String date, String server, int prestige, long xp, double gold, boolean fighting, int bounty,
                              double streak, String footer) {
        return lines(date, server, prestige, xp, gold, fighting ? "§cFighting" : "§aIdling", bounty, streak, 0, 0, footer);
    }

    /**
     * 上から順の行。空行は "" (表示するときに見えない色コードで区別する)。
     * status は "Status: " の後ろ (Idling / Fighting / 発動中のメガストリーク)。strength は Strength-Chaining のレベル。
     */
    static List<String> lines(String date, String server, int prestige, long xp, double gold, String status, int bounty,
                              double streak, int strength, int strengthSeconds, String footer) {
        List<String> lines = new ArrayList<>();
        lines.add("§7" + date + "  §8" + server);
        lines.add("");
        if (prestige > 0) {
            lines.add("Prestige: §e" + RomanNumerals.of(prestige));
        }
        lines.add("Level: " + PrestigeRules.levelTag(prestige, PitLevels.levelOf(xp, prestige)));
        long needed = PitLevels.neededXp(xp, prestige);
        lines.add("Needed XP: §b" + (needed > 0 ? NUMBER.format(needed) : "MAXED!"));
        lines.add("");
        lines.add("Gold: §6" + GoldManager.formatExact(gold));
        lines.add("");
        lines.add("Status: " + status);
        if (bounty > 0) {
            lines.add("Bounty: §6" + NUMBER.format(bounty) + "g");
        }
        if (streak > 0) {
            lines.add("Streak: §a" + STREAK.format(streak));
        }
        if (strength > 0) {
            lines.add("Strength: §c" + RomanNumerals.of(strength) + " §7(" + strengthSeconds + ")");
        }
        lines.add("");
        lines.add(footer);
        return lines;
    }

    private static void render(Scoreboard board, List<String> lines) {
        Objective objective = board.getObjective(OBJECTIVE);
        ChatColor[] colors = ChatColor.values();
        for (int i = 0; i < MAX_LINES; i++) {
            // 行ごとに見えない固有の名前 (§0§r, §1§r, ...) を使い、中身はチームの prefix / suffix に入れる
            String entry = colors[i].toString() + ChatColor.RESET;
            Team team = board.getTeam("line" + i);
            if (i >= lines.size()) {
                if (team != null) {
                    board.resetScores(entry);
                    team.unregister();
                }
                continue;
            }
            if (team == null) {
                team = board.registerNewTeam("line" + i);
                team.addEntry(entry);
            }
            SidebarLine line = SidebarLine.split(lines.get(i));
            if (!line.prefix.equals(team.getPrefix())) {
                team.setPrefix(line.prefix);
            }
            if (!line.suffix.equals(team.getSuffix())) {
                team.setSuffix(line.suffix);
            }
            objective.getScore(entry).setScore(lines.size() - i);
        }
    }

    private static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
