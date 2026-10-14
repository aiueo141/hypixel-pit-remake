package com.pitremake.command;

import com.pitremake.PitPlugin;
import com.pitremake.combat.DamageContext;
import com.pitremake.item.MysticItems;
import com.pitremake.util.NbtUtil;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class PitCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = Arrays.asList("reload", "version", "nonce", "damage", "debug", "backup");

    private final PitPlugin plugin;

    public PitCommand(PitPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "/" + label + " <" + String.join("|", SUBCOMMANDS) + ">");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                if (!sender.hasPermission("pit.admin")) {
                    sender.sendMessage(ChatColor.RED + "You don't have permission.");
                    return true;
                }
                plugin.reloadConfig();
                plugin.loadDamageStacking();
                sender.sendMessage(ChatColor.GREEN + "Config reloaded.");
                return true;
            case "damage":
                return damage(sender, args);
            case "backup":
                // ワールド (地形・置かれたブロック) と plugins/Pit を backups/ に zip で保存
                if (!sender.hasPermission("pit.admin")) {
                    sender.sendMessage(ChatColor.RED + "You don't have permission.");
                    return true;
                }
                plugin.getBackupManager().backup(sender);
                return true;
            case "debug":
                if (!(sender instanceof Player) || !sender.hasPermission("pit.admin")) {
                    sender.sendMessage(ChatColor.RED + "You don't have permission.");
                    return true;
                }
                boolean on = plugin.getDamageCalculator().toggleDebug((Player) sender);
                sender.sendMessage(on ? ChatColor.GREEN + "Damage debug ON: every hit you deal or take is shown in chat."
                        : ChatColor.YELLOW + "Damage debug OFF.");
                return true;
            case "version":
                sender.sendMessage(ChatColor.GOLD + "Pit v" + plugin.getDescription().getVersion());
                return true;
            case "nonce":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "Only players can use this command.");
                    return true;
                }
                ItemStack held = ((Player) sender).getItemInHand();
                if (!NbtUtil.has(held, MysticItems.NONCE)) {
                    sender.sendMessage(ChatColor.RED + "This item has no nonce.");
                    return true;
                }
                sender.sendMessage(ChatColor.GRAY + "Nonce: " + ChatColor.WHITE + NbtUtil.getInt(held, MysticItems.NONCE));
                return true;
            default:
                sender.sendMessage(ChatColor.RED + "Unknown subcommand: " + args[0]);
                return true;
        }
    }

    /** /pit damage [additive|multiplicative] : ダメージの増減の重ね方を見る・変える (config にも保存)。 */
    private boolean damage(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.YELLOW + "Damage stacking: " + ChatColor.WHITE
                    + DamageContext.getStacking().name().toLowerCase() + ChatColor.GRAY + " (/pit damage <additive|multiplicative>)");
            return true;
        }
        if (!sender.hasPermission("pit.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission.");
            return true;
        }
        DamageContext.Stacking stacking = DamageContext.Stacking.fromName(args[1]);
        if (stacking == null) {
            sender.sendMessage(ChatColor.RED + "Use additive or multiplicative.");
            return true;
        }
        DamageContext.setStacking(stacking);
        plugin.getConfig().set("damage.stacking", stacking.name().toLowerCase());
        plugin.saveConfig();
        sender.sendMessage(ChatColor.GREEN + "Damage stacking is now " + stacking.name().toLowerCase() + ".");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 2 && args[0].equalsIgnoreCase("damage")) {
            return Arrays.asList("additive", "multiplicative");
        }
        if (args.length != 1) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        for (String sub : SUBCOMMANDS) {
            if (sub.startsWith(args[0].toLowerCase())) {
                result.add(sub);
            }
        }
        return result;
    }
}
