package com.pitremake.command;

import com.pitremake.PitPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** /pitstreak <amount> : 自分のキルストリークを設定する (メガストリークの確認用。49 にして 1 キルすると発動する)。 */
public final class PitStreakCommand implements TabExecutor {

    private final PitPlugin plugin;

    public PitStreakCommand(PitPlugin plugin) {
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
        int streak;
        try {
            streak = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Not a number: " + args[0]);
            return true;
        }
        Player player = (Player) sender;
        plugin.getStatsManager().setStreak(player.getUniqueId(), streak);
        plugin.getMegastreakManager().refresh(player);
        plugin.getSidebar().updateAll();
        sender.sendMessage(ChatColor.GREEN + "Your streak is now " + Math.max(0, streak) + ".");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return args.length == 1 ? Arrays.asList("49", "99", "199", "399") : Collections.<String>emptyList();
    }
}
