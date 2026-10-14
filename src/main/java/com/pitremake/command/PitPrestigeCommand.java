package com.pitremake.command;

import com.pitremake.PitPlugin;
import com.pitremake.stats.PrestigeRules;
import com.pitremake.stats.StatsManager;
import com.pitremake.util.RomanNumerals;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** /pitprestige <0〜50> [player] : プレステージを設定する (Renown Shop などの確認用。レベルはそのまま)。 */
public final class PitPrestigeCommand implements TabExecutor {

    private final PitPlugin plugin;

    public PitPrestigeCommand(PitPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1 || args.length > 2) {
            return false;
        }
        int prestige;
        try {
            prestige = Math.max(0, Math.min(Integer.parseInt(args[0]), PrestigeRules.MAX_PRESTIGE));
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Not a number: " + args[0]);
            return true;
        }
        Player target;
        if (args.length == 2) {
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found: " + args[1]);
                return true;
            }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            sender.sendMessage(ChatColor.RED + "Usage: /pitprestige <prestige> <player>");
            return true;
        }
        StatsManager stats = plugin.getStatsManager();
        stats.setPrestige(target.getUniqueId(), prestige);
        target.setLevel(stats.getLevel(target.getUniqueId()));
        target.setExp(stats.getProgress(target.getUniqueId()));
        plugin.getSidebar().updateAll();
        sender.sendMessage(ChatColor.GREEN + target.getName() + "'s prestige is now "
                + (prestige == 0 ? "0" : RomanNumerals.of(prestige)) + ".");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("0", "1", "5", "10", "25", "50");
        }
        if (args.length == 2) {
            List<String> names = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                names.add(player.getName());
            }
            return names;
        }
        return Collections.emptyList();
    }
}
