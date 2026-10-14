package com.pitremake.special;

import com.pitremake.PitPlugin;
import com.pitremake.gem.GemItem;
import com.pitremake.item.MysticData;
import com.pitremake.item.MysticItems;
import com.pitremake.util.NbtUtil;
import net.minecraft.server.v1_8_R3.NBTTagCompound;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Uberdrop (Uberstreak で 400 キル以上して死んだときの景品、pit.wiki の Uberstreak)。
 * 中身が決まった箱のアイテム (見た目はエンダーチェスト。置けない) で、持ってクリックすると開く (ユーザーのスクリーンショット):
 * <pre>
 * §dUberdrop
 * §7Kept on death
 * §7Contains: §aTotally Legit Gem
 *
 * §eHold and click to open!
 * </pre>
 * 中身: Philosopher's Cactus 5 / 10 / 15 / 20 (§a) / Funky Feather 1〜3 (アイテムと同じ §3) /
 * Hidden Jewel の Mystic Sword (§d) / Totally Legit Gem (§a) / Mystic のドロップ率 +1% ずっと (§d、10 回 = +10% まで)。
 * 確率は wiki に無いので均等。スタックしない (1 つずつ別の ID)。
 */
public final class Uberdrop {

    public enum Reward {
        CACTUS, FEATHER, HIDDEN_JEWEL_SWORD, GEM, PERMA_MYSTIC_CHANCE
    }

    private static final String KEY = "Uberdrop";
    private static final String AMOUNT_KEY = "UberdropAmount";
    private static final String ID_KEY = "UberdropId";
    private static final int[] CACTUS_AMOUNTS = {5, 10, 15, 20};
    /** Mystic のドロップ率 +1% は 10 回 (+10%) まで。 */
    public static final int MAX_PERMA_MYSTIC = 10;
    public static final double PERMA_MYSTIC_CHANCE = 0.01;

    private Uberdrop() {
    }

    /** 中身を引いた Uberdrop (Mystic のドロップ率が上限の人には出さない)。 */
    public static ItemStack roll(PitPlugin plugin, Player player) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        List<Reward> rewards = new ArrayList<>(Arrays.asList(Reward.values()));
        if (plugin.getUpgradeManager().getPermaMystic(player.getUniqueId()) >= MAX_PERMA_MYSTIC) {
            rewards.remove(Reward.PERMA_MYSTIC_CHANCE);
        }
        Reward reward = rewards.get(random.nextInt(rewards.size()));
        int amount = 1;
        if (reward == Reward.CACTUS) {
            amount = CACTUS_AMOUNTS[random.nextInt(CACTUS_AMOUNTS.length)];
        } else if (reward == Reward.FEATHER) {
            amount = 1 + random.nextInt(3);
        }
        return create(reward, amount);
    }

    public static ItemStack create(Reward reward, int amount) {
        ItemStack item = new ItemStack(Material.ENDER_CHEST);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§dUberdrop");
        meta.setLore(Arrays.asList(
                "§7Kept on death",
                "§7Contains: " + contentsName(reward, amount),
                "",
                "§eHold and click to open!"));
        item.setItemMeta(meta);
        NBTTagCompound extra = NbtUtil.readExtraAttributes(item);
        extra.setInt(KEY, reward.ordinal() + 1);
        extra.setInt(AMOUNT_KEY, amount);
        // スタックしないように 1 つずつ別の ID
        extra.setInt(ID_KEY, ThreadLocalRandom.current().nextInt());
        return NbtUtil.writeExtraAttributes(item, extra);
    }

    /** 中身の表示 (Hidden Jewel の剣・ずっと続くドロップ率はピンク、Cactus / Gem は黄緑、Feather はアイテムと同じ色)。 */
    static String contentsName(Reward reward, int amount) {
        switch (reward) {
            case CACTUS:
                return "§a" + amount + "x Philosopher's Cactus";
            case FEATHER:
                return "§3" + amount + "x Funky Feather";
            case HIDDEN_JEWEL_SWORD:
                return "§dHidden Jewel Sword";
            case GEM:
                return "§aTotally Legit Gem";
            default:
                return "§d+1% Perma Mystic Chance";
        }
    }

    public static boolean is(ItemStack item) {
        return item != null && item.getType() == Material.ENDER_CHEST && NbtUtil.has(item, KEY);
    }

    /** 手に持っている Uberdrop を開ける。 */
    public static void open(PitPlugin plugin, Player player) {
        ItemStack held = player.getItemInHand();
        int index = NbtUtil.getInt(held, KEY) - 1;
        if (index < 0 || index >= Reward.values().length) {
            return;
        }
        Reward reward = Reward.values()[index];
        int amount = Math.max(1, NbtUtil.getInt(held, AMOUNT_KEY));
        if (held.getAmount() > 1) {
            held.setAmount(held.getAmount() - 1);
            player.setItemInHand(held);
        } else {
            player.setItemInHand(null);
        }
        ItemStack give = null;
        switch (reward) {
            case CACTUS:
                give = SpecialItems.cactus(amount);
                break;
            case FEATHER:
                give = SpecialItems.feather(amount);
                break;
            case HIDDEN_JEWEL_SWORD:
                ItemStack sword = MysticItems.freshSword();
                MysticData data = MysticData.read(sword);
                data.setEnchant("hidden_jewel", 1);
                plugin.getWellRoller().raiseTier(data);
                give = MysticItems.applyDisplay(data.write(sword), plugin.getEnchantRegistry());
                break;
            case GEM:
                give = GemItem.create(1);
                break;
            default:
                plugin.getUpgradeManager().addPermaMystic(player.getUniqueId(), MAX_PERMA_MYSTIC);
                break;
        }
        if (give != null) {
            for (ItemStack leftover : player.getInventory().addItem(give).values()) {
                player.getWorld().dropItem(player.getLocation(), leftover);
            }
        }
        player.sendMessage("§d§lUBERDROP! §7You found " + contentsName(reward, amount) + "§7!");
        player.playSound(player.getLocation(), "random.levelup", 1.0f, 1.5f);
    }
}
