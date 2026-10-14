package com.pitremake.command;

import com.pitremake.PitPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /oof : /kill の代わり。HP 0 になったときと同じく死んだ扱いにしてスポーンへ戻す
 * (録画 Death.mcrc: §c§lDEATH! §e§lVIEW RECAP)。少し前に殴っていた人がいればその人のキルになる。
 */
public final class OofCommand implements CommandExecutor {

    private final PitPlugin plugin;

    public OofCommand(PitPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can do that.");
            return true;
        }
        Player player = (Player) sender;
        if (plugin.getPitSpawn().isProtected(player.getLocation())) {
            // スポーンの中では使えない (ユーザーの指定。メッセージは推測)
            player.sendMessage("§cYou cannot do this in spawn!");
            return true;
        }
        if (!player.isDead()) {
            plugin.getKillListener().oof(player);
        }
        return true;
    }
}
