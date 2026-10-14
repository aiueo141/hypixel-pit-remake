package com.pitremake.command;

import com.pitremake.npc.NpcManager;
import com.pitremake.npc.PitNpcType;
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
 * /pitnpc spawn <upgrades|prestige> : 立っているブロックの中央に、自分の方を向いた NPC を置く
 * /pitnpc remove                    : 5 ブロック以内で一番近い NPC を消す
 * /pitnpc list                      : 置いてある NPC の一覧
 */
public final class PitNpcCommand implements TabExecutor {

    private static final double REMOVE_RADIUS = 5;

    private final NpcManager npcs;

    public PitNpcCommand(NpcManager npcs) {
        this.npcs = npcs;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            return false;
        }
        if (args[0].equalsIgnoreCase("list")) {
            List<String> lines = npcs.describe();
            sender.sendMessage(ChatColor.YELLOW + "NPCs: " + lines.size());
            for (String line : lines) {
                sender.sendMessage(ChatColor.GRAY + "- " + line);
            }
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can do that.");
            return true;
        }
        Player player = (Player) sender;
        if (args[0].equalsIgnoreCase("spawn") && args.length == 2) {
            PitNpcType type = PitNpcType.fromName(args[1]);
            if (type == null) {
                sender.sendMessage(ChatColor.RED + "Unknown NPC: " + args[1]);
                return true;
            }
            npcs.create(type, placement(player.getLocation()));
            sender.sendMessage(ChatColor.GREEN + "Spawned " + type.name() + " NPC.");
            return true;
        }
        if (args[0].equalsIgnoreCase("remove")) {
            PitNpcType removed = npcs.removeNearest(player.getLocation(), REMOVE_RADIUS);
            sender.sendMessage(removed == null ? ChatColor.RED + "No NPC within " + (int) REMOVE_RADIUS + " blocks."
                    : ChatColor.GREEN + "Removed " + removed.name() + " NPC.");
            return true;
        }
        return false;
    }

    /** ブロックの中央 (Hypixel の NPC も x.5 / z.5)。向きはプレイヤーの方を向くよう 90° 単位に揃える。 */
    static Location placement(Location player) {
        float facing = Math.round((player.getYaw() + 180) / 90f) * 90f;
        facing = ((facing % 360) + 360) % 360;
        if (facing > 180) {
            facing -= 360;
        }
        return new Location(player.getWorld(), player.getBlockX() + 0.5, player.getBlockY(), player.getBlockZ() + 0.5, facing, 0);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> options;
        if (args.length == 1) {
            options = Arrays.asList("spawn", "remove", "list");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("spawn")) {
            options = new ArrayList<>();
            for (PitNpcType type : PitNpcType.values()) {
                options.add(type.name().toLowerCase());
            }
        } else {
            return Collections.emptyList();
        }
        List<String> matches = new ArrayList<>();
        for (String option : options) {
            if (option.startsWith(args[args.length - 1].toLowerCase())) {
                matches.add(option);
            }
        }
        return matches;
    }
}
