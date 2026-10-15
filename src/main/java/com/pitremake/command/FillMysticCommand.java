package com.pitremake.command;

import com.pitremake.item.MysticItems;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 * 管理者用: 手に持っているミスティックアイテムのコピーでインベントリの空きスロットをすべて埋める。
 * コピーは Nonce を振り直す (パンツの色は保つ)。
 * <pre>
 * /fillmystic
 * </pre>
 */
public final class FillMysticCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }
        Player player = (Player) sender;
        ItemStack held = player.getItemInHand();
        if (!MysticItems.isMystic(held)) {
            player.sendMessage(ChatColor.RED + "Hold a mystic item.");
            return true;
        }

        PlayerInventory inventory = player.getInventory();
        int filled = 0;
        // getContents() は防具を除くメインインベントリ (ホットバーを含む 36 スロット)
        ItemStack[] contents = inventory.getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack stack = contents[slot];
            if (stack == null || stack.getType() == Material.AIR) {
                inventory.setItem(slot, MysticItems.withNewNonce(held));
                filled++;
            }
        }
        player.sendMessage(filled == 0
                ? ChatColor.RED + "Your inventory has no empty slots."
                : ChatColor.GREEN + "Filled " + filled + " slots with copies of the held item.");
        return true;
    }
}
