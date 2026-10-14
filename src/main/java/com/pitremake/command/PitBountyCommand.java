package com.pitremake.command;

import com.pitremake.PitPlugin;
import com.pitremake.bounty.BountyRules;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** /pitbounty <amount> [player] : バウンティを設定する (確認用)。 */
public final class PitBountyCommand implements TabExecutor {

    private final PitPlugin plugin;

    public PitBountyCommand(PitPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1 || args.length > 2) {
            return false;
        }
        int amount;
        try {
            amount = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Not a number: " + args[0]);
            return true;
        }
        Player target = args.length == 2 ? Bukkit.getPlayerExact(args[1]) : sender instanceof Player ? (Player) sender : null;
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found.");
            return true;
        }
        int bounty = Math.max(0, Math.min(amount / BountyRules.STEP * BountyRules.STEP, BountyRules.MAX_BOUNTY));
        plugin.getBountyManager().set(target.getUniqueId(), bounty);
        plugin.getSidebar().updateAll();
        sender.sendMessage(ChatColor.GREEN + target.getName() + "'s bounty is now " + ChatColor.GOLD + bounty + "g");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Collections.singletonList("1000");
        }
        if (args.length == 2) {
            List<String> names = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                    names.add(player.getName());
                }
            }
            return names;
        }
        return Collections.emptyList();
    }
}
