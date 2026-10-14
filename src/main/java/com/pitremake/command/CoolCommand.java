package com.pitremake.command;

import com.pitremake.PitPlugin;
import com.pitremake.renown.RenownUpgrade;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/** /cool : Renown の Cool Conclave で使える。プレステージ IV 以上のプレイヤーの一覧 (表示の形は推測)。 */
public final class CoolCommand implements CommandExecutor {

    private static final int MIN_PRESTIGE = 4;

    private final PitPlugin plugin;

    public CoolCommand(PitPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can do that.");
            return true;
        }
        Player player = (Player) sender;
        if (!plugin.getUpgradeManager().hasRenown(player.getUniqueId(), RenownUpgrade.COOL_CONCLAVE)) {
            player.sendMessage("§cUnlock Cool Conclave in the Renown shop to use this command!");
            return true;
        }
        List<String> names = new ArrayList<>();
        for (Player online : plugin.getServer().getOnlinePlayers()) {
            if (plugin.getStatsManager().getPrestige(online.getUniqueId()) >= MIN_PRESTIGE) {
                names.add(plugin.getStatsManager().chatTag(online.getUniqueId()) + " "
                        + plugin.getRankManager().nameColor(online.getUniqueId()) + online.getName());
            }
        }
        player.sendMessage("§b§lCOOL PLAYERS §7(" + names.size() + ")");
        for (String name : names) {
            player.sendMessage("§8- " + name);
        }
        return true;
    }
}
