package com.pitremake.scoreboard;

import com.pitremake.stats.PitLevels;
import com.pitremake.stats.PrestigeRules;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * tab と頭の上の名前 (録画 2026-10-14_00-39-15.mcrc の Hypixel と同じ形式)。プレイヤーごとにチームを作る。
 * <pre>
 * チーム名  "aaps12"   先頭 2 文字がレベル順 (120 = aa, 119 = ab, … 1 = jl)。tab はチーム名順に並ぶので高レベルが上
 * prefix   "§7[§b§l120§7]§7 "
 *          メガストリーク発動中は "§d§lUBER100§7 " のようにレベルの代わりに名前が出る
 * suffix   " "  /  " §6§l1800g" (バウンティ)
 * </pre>
 * レベルが変わったら新しい名前のチームに移す (Hypixel も同じ)。
 */
final class PitNametags {

    /** このクラスが作ったチームの名前。サイドバーの行のチーム (line0 など) とは被らない。 */
    private static final Pattern TEAM_NAME = Pattern.compile("[a-j][a-l]ps\\d+");
    private static final int LEVELS_PER_LETTER = 12;

    private final Map<String, Integer> ids = new HashMap<>();
    private int nextId = 1;

    /** tab に出す 1 人分。 */
    static final class Entry {
        final String name;
        final int prestige;
        final int level;
        final int bounty;
        /** メガストリーク発動中にレベルの代わりに出す文字 (例: §d§lUBER100)。無ければ null。 */
        final String tag;
        /** 名前の色 (ランクの色。録画の tab: §6[§f§l112§6]§b )。 */
        final String nameColor;

        Entry(String name, int prestige, int level, int bounty, String tag, String nameColor) {
            this.name = name;
            this.prestige = prestige;
            this.level = level;
            this.bounty = bounty;
            this.tag = tag;
            this.nameColor = nameColor;
        }
    }

    static String sortKey(int level) {
        int index = PitLevels.MAX_LEVEL - Math.max(1, Math.min(level, PitLevels.MAX_LEVEL));
        return "" + (char) ('a' + index / LEVELS_PER_LETTER) + (char) ('a' + index % LEVELS_PER_LETTER);
    }

    static String prefix(int level) {
        return prefix(0, level, "§7");
    }

    /** 括弧の色はプレステージの色、最後は名前の色 (録画の tab: §6[§f§l112§6]§b )。 */
    static String prefix(int prestige, int level, String nameColor) {
        return PrestigeRules.levelTag(prestige, level) + nameColor + " ";
    }

    static String suffix(int bounty) {
        return bounty > 0 ? " §6§l" + bounty + "g" : " ";
    }

    /** board のチームを entries に合わせる (居なくなったプレイヤーのチームは消す)。 */
    void apply(Scoreboard board, List<Entry> entries) {
        Set<String> wanted = new HashSet<>();
        for (Entry entry : entries) {
            Integer id = ids.get(entry.name);
            if (id == null) {
                id = nextId++;
                ids.put(entry.name, id);
            }
            String teamName = sortKey(entry.level) + "ps" + id;
            wanted.add(teamName);
            Team team = board.getTeam(teamName);
            if (team == null) {
                Team old = board.getEntryTeam(entry.name);
                if (old != null && TEAM_NAME.matcher(old.getName()).matches()) {
                    old.unregister();
                }
                team = board.registerNewTeam(teamName);
                team.addEntry(entry.name);
            }
            String prefix = entry.tag != null ? entry.tag + entry.nameColor + " " : prefix(entry.prestige, entry.level, entry.nameColor);
            String suffix = suffix(entry.bounty);
            if (!prefix.equals(team.getPrefix())) {
                team.setPrefix(prefix);
            }
            if (!suffix.equals(team.getSuffix())) {
                team.setSuffix(suffix);
            }
        }
        for (Team team : new HashSet<>(board.getTeams())) {
            if (TEAM_NAME.matcher(team.getName()).matches() && !wanted.contains(team.getName())) {
                team.unregister();
            }
        }
    }
}
