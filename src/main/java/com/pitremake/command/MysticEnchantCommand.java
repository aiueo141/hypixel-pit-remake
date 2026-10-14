package com.pitremake.command;

import com.pitremake.PitPlugin;
import com.pitremake.enchant.PitEnchant;
import com.pitremake.item.MysticData;
import com.pitremake.item.MysticItems;
import com.pitremake.item.MysticType;
import com.pitremake.util.RomanNumerals;
import com.pitremake.well.MysticWellRoller;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * 管理者用: 手に持っているミスティックアイテムに好きなエンチャントを付ける。
 * 付けるたびに通常の強化と同じく Tier が 1 上がり、Lives もその Tier のルールで決まる (Tier III ならそのまま)。
 * <pre>
 * /menchant &lt;enchant&gt; [level]
 *   enchant: キー (melee_literally_p2w) か、名前を小文字・英数字だけにしたもの (billionaire)
 * </pre>
 */
public final class MysticEnchantCommand implements CommandExecutor, TabCompleter {

    private final PitPlugin plugin;

    public MysticEnchantCommand(PitPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "/" + label + " <enchant> [level]");
            return true;
        }

        Player player = (Player) sender;
        ItemStack held = player.getItemInHand();
        MysticType type = MysticType.of(held);
        if (!MysticItems.isMystic(held)) {
            player.sendMessage(ChatColor.RED + "Hold a mystic item.");
            return true;
        }

        PitEnchant enchant = find(args[0]);
        if (enchant == null) {
            player.sendMessage(ChatColor.RED + "Unknown enchant: " + args[0]);
            return true;
        }
        if (!enchant.canApplyTo(type)) {
            player.sendMessage(ChatColor.RED + enchant.getName() + " cannot be applied to this item.");
            return true;
        }

        int level = 1;
        if (args.length >= 2) {
            try {
                level = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Invalid level: " + args[1]);
                return true;
            }
        }
        if (level < 1 || level > enchant.getMaxLevel()) {
            player.sendMessage(ChatColor.RED + "Level must be between 1 and " + enchant.getMaxLevel() + ".");
            return true;
        }

        MysticData data = MysticData.read(held);
        // 同じエンチャントが既に付いていればレベルを上書きする (重複はしない)
        data.setEnchant(enchant.getKey(), level);
        boolean tierUp = data.getTier() < MysticWellRoller.maxTier(data);
        if (tierUp) {
            plugin.getWellRoller().raiseTier(data);
        }
        player.setItemInHand(MysticItems.applyDisplay(data.write(held), plugin.getEnchantRegistry()));

        player.sendMessage(ChatColor.GREEN + "Added " + enchant.formatName(level) + ChatColor.GREEN
                + (tierUp ? " and upgraded to Tier " + RomanNumerals.of(data.getTier()) + "." : " (already max tier)."));
        return true;
    }

    private PitEnchant find(String input) {
        PitEnchant byKey = plugin.getEnchantRegistry().get(input);
        if (byKey != null) {
            return byKey;
        }
        String wanted = simplify(input);
        for (PitEnchant enchant : plugin.getEnchantRegistry().getAll()) {
            if (simplify(enchant.getName()).equals(wanted)) {
                return enchant;
            }
        }
        return null;
    }

    /** "Combo: XP" → "comboxp" */
    private static String simplify(String name) {
        return name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1 || !(sender instanceof Player)) {
            return Collections.emptyList();
        }
        MysticType type = MysticType.of(((Player) sender).getItemInHand());
        String prefix = simplify(args[0]);
        List<String> result = new ArrayList<>();
        for (PitEnchant enchant : plugin.getEnchantRegistry().getAll()) {
            String name = simplify(enchant.getName());
            if ((type == null || enchant.canApplyTo(type)) && name.startsWith(prefix)) {
                result.add(name);
            }
        }
        Collections.sort(result);
        return result;
    }
}
