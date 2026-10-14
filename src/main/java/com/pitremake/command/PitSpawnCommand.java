package com.pitremake.command;

import com.pitremake.spawn.PitSpawn;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * /pitspawn add                     : 今いる場所 (向きも) を復活地点に足す
 * /pitspawn list                    : 復活地点と攻撃不可の範囲
 * /pitspawn clear                   : 復活地点を全部消す (ワールドのスポーンになる)
 * /pitspawn area <radius> <min-y>   : 今いる場所を中心に攻撃不可の範囲を設定する
 * /pitspawn tp                      : ランダムな復活地点へ移動する
 */
public final class PitSpawnCommand implements TabExecutor {

    private final PitSpawn spawn;

    public PitSpawnCommand(PitSpawn spawn) {
        this.spawn = spawn;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            return false;
        }
        switch (args[0].toLowerCase()) {
            case "list":
                List<Location> points = spawn.points();
                sender.sendMessage(ChatColor.YELLOW + "Spawn points: " + points.size());
                for (Location point : points) {
                    sender.sendMessage(String.format("%s- (%.2f, %.2f, %.2f) yaw %.1f", ChatColor.GRAY,
                            point.getX(), point.getY(), point.getZ(), point.getYaw()));
                }
                sender.sendMessage(ChatColor.YELLOW + "No-PvP area: " + ChatColor.GRAY + spawn.describeProtection());
                return true;
            case "clear":
                spawn.clearPoints();
                sender.sendMessage(ChatColor.GREEN + "Cleared spawn points. The world spawn is used now.");
                return true;
            default:
                break;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can do that.");
            return true;
        }
        Player player = (Player) sender;
        switch (args[0].toLowerCase()) {
            case "add":
                spawn.addPoint(player.getLocation());
                sender.sendMessage(ChatColor.GREEN + "Added a spawn point here.");
                return true;
            case "tp":
                player.teleport(spawn.randomPoint());
                return true;
            case "area":
                if (args.length != 3) {
                    return false;
                }
                try {
                    spawn.setProtection(player.getLocation(), Double.parseDouble(args[1]), Double.parseDouble(args[2]));
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Usage: /" + label + " area <radius> <min-y>");
                    return true;
                }
                sender.sendMessage(ChatColor.GREEN + "No-PvP area: " + spawn.describeProtection());
                return true;
            default:
                return false;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return Collections.emptyList();
        }
        List<String> matches = new ArrayList<>();
        for (String option : Arrays.asList("add", "list", "clear", "area", "tp")) {
            if (option.startsWith(args[0].toLowerCase())) {
                matches.add(option);
            }
        }
        return matches;
    }
}
