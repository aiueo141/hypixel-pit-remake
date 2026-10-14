package com.pitremake.command;

import com.pitremake.bot.BotManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * /pitbot spawn [count] [player|spawn] : player = 自分の場所 (プレイヤーの初期値)、spawn = Pit の復活地点からランダム (コンソールの初期値)
 * /pitbot remove | count | list
 */
public final class BotCommand implements CommandExecutor, TabCompleter {

    private static final int MAX_SPAWN = 100;
    private static final List<String> SUBCOMMANDS = Arrays.asList("spawn", "remove", "count", "list");

    private final BotManager bots;
    /** Pit の復活地点 (呼ぶたびにランダム)。 */
    private final Supplier<Location> pitSpawn;

    public BotCommand(BotManager bots, Supplier<Location> pitSpawn) {
        this.bots = bots;
        this.pitSpawn = pitSpawn;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub = args.length == 0 ? "" : args[0].toLowerCase();
        switch (sub) {
            case "spawn":
                // 数と場所 (player = 自分の場所 / spawn = Pit の復活地点からランダム) はどちらの順でもよい
                int count = 1;
                String where = sender instanceof Player ? "player" : "spawn";
                for (int i = 1; i < args.length; i++) {
                    String arg = args[i].toLowerCase();
                    if (arg.equals("player") || arg.equals("spawn")) {
                        where = arg;
                        continue;
                    }
                    try {
                        count = Integer.parseInt(arg);
                    } catch (NumberFormatException e) {
                        sender.sendMessage(ChatColor.RED + "Invalid argument: " + args[i]);
                        return true;
                    }
                }
                if (count < 1 || count > MAX_SPAWN) {
                    sender.sendMessage(ChatColor.RED + "Count must be between 1 and " + MAX_SPAWN + ".");
                    return true;
                }
                if (where.equals("player") && !(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "Only players can spawn bots at their location.");
                    return true;
                }
                if (where.equals("player")) {
                    bots.spawn(((Player) sender).getLocation(), count);
                } else {
                    bots.spawn(pitSpawn, count);
                }
                sender.sendMessage(ChatColor.GREEN + "Spawned " + count + " bot(s) at " + (where.equals("player") ? "your location." : "the Pit spawn."));
                return true;
            case "remove":
                sender.sendMessage(ChatColor.GREEN + "Removed " + bots.removeAll() + " bot(s).");
                return true;
            case "count":
                sender.sendMessage(ChatColor.GRAY + "Bots: " + ChatColor.WHITE + bots.count());
                return true;
            case "list":
                sender.sendMessage(ChatColor.GRAY + "Bots: " + ChatColor.WHITE + bots.count());
                for (String line : bots.describe()) {
                    sender.sendMessage(ChatColor.GRAY + " - " + line);
                }
                return true;
            default:
                sender.sendMessage(ChatColor.YELLOW + "/" + label + " spawn [count] [player|spawn]");
                sender.sendMessage(ChatColor.YELLOW + "/" + label + " remove");
                sender.sendMessage(ChatColor.YELLOW + "/" + label + " count");
                sender.sendMessage(ChatColor.YELLOW + "/" + label + " list");
                return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> options;
        if (args.length == 1) {
            options = SUBCOMMANDS;
        } else if (args[0].equalsIgnoreCase("spawn") && args.length <= 3) {
            options = Arrays.asList("player", "spawn", "10");
        } else {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        for (String sub : options) {
            if (sub.startsWith(args[args.length - 1].toLowerCase())) {
                result.add(sub);
            }
        }
        return result;
    }
}
