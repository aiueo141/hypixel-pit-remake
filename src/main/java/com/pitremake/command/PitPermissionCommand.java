package com.pitremake.command;

import com.pitremake.permission.PitPermissions;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * /pitpermission add|remove &lt;player&gt; &lt;name|all&gt; / list [player] : op だけ (コンソールも可)。
 * 名前は PitPermissions.GRANTS (renown, gold, event など)。
 */
public final class PitPermissionCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = Arrays.asList("add", "remove", "list");

    private final PitPermissions permissions;

    public PitPermissionCommand(PitPermissions permissions) {
        this.permissions = permissions;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage("§cOnly operators can use this command.");
            return true;
        }
        String sub = args.length == 0 ? "" : args[0].toLowerCase();
        if ((sub.equals("add") || sub.equals("remove")) && args.length >= 3) {
            OfflinePlayer target = find(args[1]);
            if (target == null) {
                sender.sendMessage("§cThat player has never joined: " + args[1]);
                return true;
            }
            List<String> grants = new ArrayList<>();
            for (int i = 2; i < args.length; i++) {
                String name = args[i].toLowerCase();
                if (name.equals("all")) {
                    grants.addAll(PitPermissions.GRANTS.keySet());
                } else if (PitPermissions.GRANTS.containsKey(name)) {
                    grants.add(name);
                } else {
                    sender.sendMessage("§cUnknown permission: " + args[i]);
                    sender.sendMessage("§7Permissions: §f" + String.join(", ", PitPermissions.GRANTS.keySet()) + ", all");
                    return true;
                }
            }
            boolean add = sub.equals("add");
            List<String> changed = permissions.change(target.getUniqueId(), target.getName(), grants, add);
            String name = target.getName();
            if (changed.isEmpty()) {
                sender.sendMessage("§7Nothing changed for " + name + ".");
            } else {
                sender.sendMessage((add ? "§aGave " : "§aRemoved ") + String.join(", ", changed) + (add ? " to " : " from ") + name + ".");
                Player online = target.getPlayer();
                if (online != null && online != sender) {
                    online.sendMessage((add ? "§aYou were given Pit permissions: §f" : "§cYour Pit permissions were removed: §f")
                            + String.join(", ", changed));
                }
            }
            show(sender, target.getUniqueId(), name);
            return true;
        }
        if (sub.equals("list")) {
            if (args.length >= 2) {
                OfflinePlayer target = find(args[1]);
                if (target == null) {
                    sender.sendMessage("§cThat player has never joined: " + args[1]);
                } else {
                    show(sender, target.getUniqueId(), target.getName());
                }
                return true;
            }
            Map<UUID, String> players = permissions.players();
            sender.sendMessage("§e§lPIT PERMISSIONS §7(" + players.size() + " players)");
            for (Map.Entry<UUID, String> entry : players.entrySet()) {
                show(sender, entry.getKey(), entry.getValue());
            }
            sender.sendMessage("§7Available: §f" + String.join(", ", PitPermissions.GRANTS.keySet()));
            return true;
        }
        sender.sendMessage("§e/" + label + " add <player> <permission...|all>");
        sender.sendMessage("§e/" + label + " remove <player> <permission...|all>");
        sender.sendMessage("§e/" + label + " list [player]");
        sender.sendMessage("§7Permissions: §f" + String.join(", ", PitPermissions.GRANTS.keySet()));
        return true;
    }

    private void show(CommandSender sender, UUID id, String name) {
        Set<String> set = permissions.get(id);
        sender.sendMessage(" §f" + name + "§7: " + (set.isEmpty() ? "§8none" : "§a" + String.join(", ", set)));
    }

    /** 今いるプレイヤー、権限を持っているプレイヤー、入ったことのあるプレイヤーの順で探す。 */
    @SuppressWarnings("deprecation")
    private OfflinePlayer find(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return online;
        }
        for (Map.Entry<UUID, String> entry : permissions.players().entrySet()) {
            if (entry.getValue().equalsIgnoreCase(name)) {
                return Bukkit.getOfflinePlayer(entry.getKey());
            }
        }
        OfflinePlayer offline = Bukkit.getOfflinePlayer(name);
        return offline.hasPlayedBefore() ? offline : null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.isOp()) {
            return Collections.emptyList();
        }
        List<String> options = new ArrayList<>();
        if (args.length == 1) {
            options.addAll(SUBCOMMANDS);
        } else if (args.length == 2) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                options.add(player.getName());
            }
            for (String name : permissions.players().values()) {
                if (!options.contains(name)) {
                    options.add(name);
                }
            }
        } else if (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove")) {
            options.addAll(PitPermissions.GRANTS.keySet());
            options.add("all");
        }
        String last = args[args.length - 1].toLowerCase();
        List<String> result = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase().startsWith(last)) {
                result.add(option);
            }
        }
        return result;
    }
}
