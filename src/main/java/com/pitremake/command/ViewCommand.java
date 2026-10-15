package com.pitremake.command;

import com.pitremake.PitPlugin;
import com.pitremake.data.PlayerProfiles;
import com.pitremake.view.ProfileViewer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * /view &lt;player&gt; : そのプレイヤーの Profile Viewer を開く。オフラインなら記録の写し ({@link PlayerProfiles}) を見る。
 * Hypixel と同じくレベル 70 で解放 (管理者はいつでも使える)。
 */
public final class ViewCommand implements TabExecutor {

    static final int REQUIRED_LEVEL = 70;

    private final PitPlugin plugin;

    public ViewCommand(PitPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can do that.");
            return true;
        }
        Player player = (Player) sender;
        if (args.length != 1) {
            player.sendMessage("§cUsage: /" + label + " <player>");
            return true;
        }
        if (!player.hasPermission("pit.admin") && plugin.getStatsManager().getLevel(player.getUniqueId()) < REQUIRED_LEVEL) {
            player.sendMessage("§cYou need to be level " + REQUIRED_LEVEL + " to use /view!");
            return true;
        }
        // オンラインの完全一致 → オフラインの完全一致 → オンラインの前方一致 (オフラインの Bob より先にオンラインの Bobby を開かない)
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target != null) {
            new ProfileViewer(plugin, player, target).open();
            return true;
        }
        PlayerProfiles.Profile offline = plugin.getPlayerProfiles().find(args[0]);
        if (offline != null && !offline.name.isEmpty()) {
            new ProfileViewer(plugin, player, offline).open();
            return true;
        }
        target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            player.sendMessage("§cCouldn't find a player named " + args[0] + "!");
            return true;
        }
        new ProfileViewer(plugin, player, target).open();
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return Collections.emptyList();
        }
        // オンラインの人と、記録のあるオフラインの人
        Set<String> candidates = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (Player player : Bukkit.getOnlinePlayers()) {
            candidates.add(player.getName());
        }
        candidates.addAll(plugin.getPlayerProfiles().knownNames().values());
        List<String> names = new ArrayList<>();
        for (String name : candidates) {
            if (!name.isEmpty() && name.toLowerCase().startsWith(args[0].toLowerCase())) {
                names.add(name);
            }
        }
        return names;
    }
}
