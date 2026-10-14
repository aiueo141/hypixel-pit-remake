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

/** /pitrenown <amount> : 自分の Renown を設定する (Renown shop の確認用)。 */
public final class PitRenownCommand implements TabExecutor {

    private final PitPlugin plugin;

    public PitRenownCommand(PitPlugin plugin) {
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
        int amount;
        try {
            amount = Math.max(0, Integer.parseInt(args[0]));
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Not a number: " + args[0]);
            return true;
        }
        plugin.getStatsManager().setRenown(((Player) sender).getUniqueId(), amount);
        sender.sendMessage(ChatColor.GREEN + "Your renown is now " + amount + ".");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return args.length == 1 ? Arrays.asList("0", "100", "1000") : Collections.<String>emptyList();
    }
}
