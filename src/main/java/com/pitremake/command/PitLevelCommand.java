package com.pitremake.command;

import com.pitremake.PitPlugin;
import com.pitremake.stats.PitLevels;
import com.pitremake.stats.StatsManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** /pitlevel <level> : 自分のレベルを設定する (プレステージの確認用。XP をそのレベルの最初の値にする)。 */
public final class PitLevelCommand implements TabExecutor {

    private final PitPlugin plugin;

    public PitLevelCommand(PitPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can do that.");
            return true;
        }
        if (args.length != 1) {
            return false;
        }
        int level;
        try {
            level = Math.max(1, Math.min(Integer.parseInt(args[0]), PitLevels.MAX_LEVEL));
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Not a number: " + args[0]);
            return true;
        }
        Player player = (Player) sender;
        StatsManager stats = plugin.getStatsManager();
        stats.setXp(player.getUniqueId(), PitLevels.totalXpFor(level, stats.getPrestige(player.getUniqueId())));
        player.setLevel(stats.getLevel(player.getUniqueId()));
        player.setExp(stats.getProgress(player.getUniqueId()));
        plugin.getSidebar().updateAll();
        sender.sendMessage(ChatColor.GREEN + "Your level is now " + level + ".");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return args.length == 1 ? Arrays.asList("1", "60", "120") : Collections.<String>emptyList();
    }
}
