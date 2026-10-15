package com.pitremake.command;

import com.pitremake.PitPlugin;
import com.pitremake.item.MysticData;
import com.pitremake.item.MysticItems;
import com.pitremake.item.MysticPrefix;
import com.pitremake.item.MysticType;
import com.pitremake.well.MysticWellRoller;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 管理者用: インベントリにあるミスティックアイテムをすべて、Mystic Well と同じ抽選で強化する。
 * ゴールド・生贄・Mysticism (Renown) の条件は無視し、全体アナウンスもしない。
 * <pre>
 * /instantupgrade [tiers]
 *   tiers: 何 Tier 強化するか (デフォルト 1)。そのアイテムの最大 Tier で止まる
 * </pre>
 */
public final class InstantUpgradeCommand implements CommandExecutor, TabCompleter {

    private final PitPlugin plugin;

    public InstantUpgradeCommand(PitPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }
        int tiers = 1;
        if (args.length >= 1) {
            try {
                tiers = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid tiers: " + args[0]);
                return true;
            }
        }
        if (tiers < 1 || tiers > MysticWellRoller.maxTier()) {
            sender.sendMessage(ChatColor.RED + "Tiers must be between 1 and " + MysticWellRoller.maxTier() + ".");
            return true;
        }

        Player player = (Player) sender;
        PlayerInventory inventory = player.getInventory();
        int upgraded = 0;
        int alreadyMax = 0;
        Map<MysticPrefix, Integer> prefixes = new EnumMap<>(MysticPrefix.class);
        ItemStack[] contents = inventory.getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack stack = contents[slot];
            if (!MysticItems.isMystic(stack)) {
                continue;
            }
            MysticData data = MysticData.read(stack);
            int maxTier = MysticWellRoller.maxTier(data);
            if (data.getTier() >= maxTier) {
                alreadyMax++;
                continue;
            }
            MysticType type = MysticType.of(stack);
            for (int i = 0; i < tiers && data.getTier() < maxTier; i++) {
                plugin.getWellRoller().upgrade(data, type);
            }
            inventory.setItem(slot, MysticItems.applyDisplay(data.write(stack), plugin.getEnchantRegistry()));
            upgraded++;
            MysticPrefix prefix = MysticPrefix.of(data, plugin.getEnchantRegistry());
            if (prefix != null) {
                prefixes.merge(prefix, 1, Integer::sum);
            }
        }

        if (upgraded == 0) {
            player.sendMessage(ChatColor.RED + (alreadyMax > 0
                    ? "All mystic items in your inventory are already max tier." : "No mystic items in your inventory."));
            return true;
        }
        StringBuilder message = new StringBuilder(ChatColor.GREEN + "Upgraded " + upgraded + " mystic items");
        if (alreadyMax > 0) {
            message.append(ChatColor.GRAY).append(" (").append(alreadyMax).append(" already max tier)");
        }
        message.append(ChatColor.GREEN).append(".");
        player.sendMessage(message.toString());
        for (Map.Entry<MysticPrefix, Integer> entry : prefixes.entrySet()) {
            player.sendMessage(ChatColor.GOLD + "  " + entry.getKey().getDisplayName() + ChatColor.GRAY + " x" + entry.getValue());
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return args.length == 1 ? Arrays.asList("1", "2", "3") : Collections.<String>emptyList();
    }
}
