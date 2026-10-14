package com.pitremake.command;

import com.pitremake.gem.GemItem;
import com.pitremake.item.MysticItems;
import com.pitremake.item.RewardItems;
import com.pitremake.special.SpecialItems;
import com.pitremake.item.PantsColor;
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
import java.util.function.Supplier;

public final class FreshCommand implements CommandExecutor, TabCompleter {

    private static final int MAX_AMOUNT = 64;
    private static final List<String> TYPES = Arrays.asList("sword", "bow", "pants", "gem", "cactus", "feather",
            "archangel", "armageddon", "goldenhelmet", "vile");

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }
        if (args.length == 0) {
            sendUsage(sender, label);
            return true;
        }

        Supplier<ItemStack> factory;
        int amountIndex;
        switch (args[0].toLowerCase()) {
            case "sword":
                factory = MysticItems::freshSword;
                amountIndex = 1;
                break;
            case "bow":
                factory = MysticItems::freshBow;
                amountIndex = 1;
                break;
            case "gem":
                factory = () -> GemItem.create(1);
                amountIndex = 1;
                break;
            case "cactus":
                factory = () -> SpecialItems.cactus(1);
                amountIndex = 1;
                break;
            case "feather":
                factory = () -> SpecialItems.feather(1);
                amountIndex = 1;
                break;
            case "archangel":
                factory = RewardItems::archangelChestplate;
                amountIndex = 1;
                break;
            case "armageddon":
                factory = RewardItems::armageddonBoots;
                amountIndex = 1;
                break;
            case "vile":
                factory = () -> SpecialItems.vile(1);
                amountIndex = 1;
                break;
            case "goldenhelmet":
                factory = RewardItems::goldenHelmet;
                amountIndex = 1;
                break;
            case "pants":
                if (args.length < 2) {
                    sendUsage(sender, label);
                    return true;
                }
                PantsColor color = PantsColor.fromName(args[1]);
                if (color == null) {
                    sender.sendMessage(ChatColor.RED + "Unknown color: " + args[1]);
                    return true;
                }
                if (color == PantsColor.RAGE) {
                    factory = MysticItems::freshRagePants;
                } else if (color == PantsColor.DARK) {
                    factory = MysticItems::freshDarkPants;
                } else if (color == PantsColor.SEWER) {
                    factory = MysticItems::freshSewerPants;
                } else if (color == PantsColor.AQUA) {
                    factory = MysticItems::freshAquaPants;
                } else {
                    factory = () -> MysticItems.freshPants(color);
                }
                amountIndex = 2;
                break;
            default:
                sendUsage(sender, label);
                return true;
        }

        int amount = 1;
        if (args.length > amountIndex) {
            try {
                amount = Integer.parseInt(args[amountIndex]);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid amount: " + args[amountIndex]);
                return true;
            }
            if (amount < 1 || amount > MAX_AMOUNT) {
                sender.sendMessage(ChatColor.RED + "Amount must be between 1 and " + MAX_AMOUNT + ".");
                return true;
            }
        }

        Player player = (Player) sender;
        ItemStack item = null;
        for (int i = 0; i < amount; i++) {
            // スタックしないので 1 つずつ作り、それぞれ別の Nonce を持たせる
            item = factory.get();
            for (ItemStack leftover : player.getInventory().addItem(item).values()) {
                player.getWorld().dropItem(player.getLocation(), leftover);
            }
        }
        player.sendMessage(ChatColor.GREEN + "Gave " + amount + "x " + item.getItemMeta().getDisplayName());
        return true;
    }

    private static void sendUsage(CommandSender sender, String label) {
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " sword [amount]");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " bow [amount]");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " pants <red|orange|yellow|green|blue|rage|dark|sewer|aqua> [amount]");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " <gem|cactus|feather> [amount]");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " <archangel|armageddon|goldenhelmet|vile> [amount]");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(TYPES, args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("pants")) {
            List<String> colors = new ArrayList<>();
            for (PantsColor color : PantsColor.values()) {
                colors.add(color.name().toLowerCase());
            }
            return filter(colors, args[1]);
        }
        return Collections.emptyList();
    }

    private static List<String> filter(List<String> options, String prefix) {
        List<String> result = new ArrayList<>();
        for (String option : options) {
            if (option.startsWith(prefix.toLowerCase())) {
                result.add(option);
            }
        }
        return result;
    }
}
