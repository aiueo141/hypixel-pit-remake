package com.pitremake.command;

import com.pitremake.PitPlugin;
import com.pitremake.bounty.BountyRules;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /spawn : Status が Idling で、スポーンにいないときだけスポーンへ戻る (メッセージは推測)。 */
public final class SpawnCommand implements CommandExecutor {

    private final PitPlugin plugin;

    public SpawnCommand(PitPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can do that.");
            return true;
        }
        Player player = (Player) sender;
        long extension = BountyRules.combatExtensionMillis(plugin.getBountyManager().get(player.getUniqueId()));
        if (plugin.getStatsManager().isFighting(player.getUniqueId(), extension)) {
            player.sendMessage("§cYou cannot do this while in combat!");
            return true;
        }
        if (plugin.getPitSpawn().isProtected(player.getLocation())) {
            player.sendMessage("§cYou are already in spawn!");
            return true;
        }
        plugin.getLauncher().cancel(player);
        player.setFallDistance(0);
        player.teleport(plugin.getPitSpawn().randomPoint());
        // 衝撃吸収 (Golden Head など) を 0 にする
        player.removePotionEffect(org.bukkit.potion.PotionEffectType.ABSORPTION);
        ((org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer) player).getHandle().setAbsorptionHearts(0);
        // Eggs / Creative / TNT で増えたアイテムは元に戻り、Excess で増えた回復アイテムは消える
        plugin.getSimpleEnchants().onSpawnCommand(player);
        return true;
    }
}
