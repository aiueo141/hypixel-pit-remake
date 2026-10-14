package com.pitremake.command;

import com.pitremake.gold.GoldManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class PitGoldCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = Arrays.asList("help", "set", "inc");

    private final GoldManager gold;

    public PitGoldCommand(GoldManager gold) {
        this.gold = gold;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }
        if (args.length < 2 || !(args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("inc"))) {
            sendHelp(sender, label);
            return true;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid amount: " + args[1]);
            return true;
        }

        Player player = (Player) sender;
        if (args[0].equalsIgnoreCase("set")) {
            gold.set(player.getUniqueId(), amount);
        } else {
            gold.add(player.getUniqueId(), amount);
        }
        player.sendMessage(ChatColor.GRAY + "Gold: " + ChatColor.GOLD + GoldManager.format(gold.get(player.getUniqueId())));
        return true;
    }

    private static void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(ChatColor.AQUA + "=== Showing help for /" + label + " ===");
        sender.sendMessage(ChatColor.AQUA + "/" + label + " help [page]");
        sender.sendMessage(ChatColor.AQUA + "/" + label + " set <amount>" + ChatColor.YELLOW + " - Set gold of player");
        sender.sendMessage(ChatColor.AQUA + "/" + label + " inc <amount>" + ChatColor.YELLOW + " - Increase gold of player");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        for (String sub : SUBCOMMANDS) {
            if (sub.startsWith(args[0].toLowerCase())) {
                result.add(sub);
            }
        }
        return result;
    }
}
