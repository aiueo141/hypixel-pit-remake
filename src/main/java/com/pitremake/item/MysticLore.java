package com.pitremake.item;

import com.pitremake.enchant.EnchantRegistry;
import com.pitremake.enchant.PitEnchant;
import com.pitremake.util.RomanNumerals;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * エンチャント済みミスティックアイテムの Lore を {@link MysticData} から組み立てる。
 * <pre>
 * Lives: 87/100 ♦             (♦ は Totally Legit Gem 使用済み)
 *
 * RARE! Billionaire III
 * Hits with this swords deal ...
 *
 * Sharp II
 * Deal +7% melee damage
 *
 * As strong as iron          (パンツのみ, パンツの色)
 * </pre>
 * Dark Pants の最後は "Enchants require heresy" / "As strong as leather"。Hidden Jewel の説明の下には "Kills: 0"。
 */
public final class MysticLore {

    /** Dark Pants の名前が Evil Pants になる Lives。 */
    public static final int EVIL_PANTS_LIVES = 100;
    /**
     * この Lives 以下は赤 (録画 2026-10-14_09-15-41.mcrc: Tier I Dark Pants の 1/5 は §c、5/10 や 10/100 は §a。
     * 3 以下で赤になるのは推測)。
     */
    public static final int LOW_LIVES = 3;
    /** 説明文の下にキル数を表示するエンチャント (Sewer Pants の Hidden Jewel)。 */
    private static final String HIDDEN_JEWEL = "hidden_jewel";

    private MysticLore() {
    }

    /** Tier の色。I = 緑, II = 黄, III = 赤。 */
    public static ChatColor tierColor(int tier) {
        switch (tier) {
            case 1:
                return ChatColor.GREEN;
            case 2:
                return ChatColor.YELLOW;
            default:
                return ChatColor.RED;
        }
    }

    /**
     * 例: "Tier I Sword", "Legendary Tier III Sword", "Tier III Red Pants"。
     * 剣と弓は Tier の色、パンツはパンツの色で表示する。称号 ({@link MysticPrefix}) は名前と同じ色で先頭に付ける。
     */
    public static String name(MysticData data, MysticType type, PantsColor pantsColor, EnchantRegistry registry) {
        String tier = "Tier " + RomanNumerals.of(data.getTier());
        ChatColor color = tierColor(data.getTier());
        String text;
        switch (type) {
            case SWORD:
                text = tier + " Sword";
                break;
            case BOW:
                text = tier + " Bow";
                break;
            default:
                if (pantsColor == null) {
                    text = tier + " Pants";
                } else {
                    color = pantsColor.getChatColor();
                    text = tier + " " + pantsColor.getDisplayName() + " Pants";
                }
                if (data.isDarkPants()) {
                    // Dark Pants は Lives が 100 以上で Evil Pants になる。称号は付かない
                    String name = data.getMaxLives() >= EVIL_PANTS_LIVES ? "Evil" : "Dark";
                    return PantsColor.DARK.getChatColor() + tier + " " + name + " Pants";
                }
        }
        MysticPrefix prefix = MysticPrefix.of(data, registry);
        return color + (prefix == null ? "" : prefix.getDisplayName() + " ") + text;
    }

    /** Lives の数字の色 (少ないと赤)。 */
    public static ChatColor livesColor(int lives) {
        return lives <= LOW_LIVES ? ChatColor.RED : ChatColor.GREEN;
    }

    /** @param pantsColor パンツの色 ("As strong as iron" の色)。パンツ以外や不明なら null */
    public static List<String> build(MysticData data, MysticType type, PantsColor pantsColor, EnchantRegistry registry) {
        List<String> lore = new ArrayList<>();
        if (data.getMaxLives() > 0) {
            // Totally Legit Gem を使ったアイテムは Lives の横に緑の ♦
            lore.add(ChatColor.GRAY + "Lives: " + livesColor(data.getLives()) + data.getLives()
                    + ChatColor.GRAY + "/" + data.getMaxLives()
                    + (data.isGemmed() ? " " + ChatColor.GREEN + "♦" : ""));
        }
        for (Map.Entry<String, Integer> entry : data.getEnchants().entrySet()) {
            PitEnchant enchant = registry.get(entry.getKey());
            if (enchant == null) {
                continue;
            }
            addSeparator(lore);
            lore.add(enchant.formatName(entry.getValue()));
            lore.addAll(enchant.getDescription(entry.getValue()));
            if (entry.getKey().equals(HIDDEN_JEWEL)) {
                lore.add(ChatColor.GRAY + "Kills: " + ChatColor.GREEN + data.getHiddenJewelKills());
            }
        }
        if (type == MysticType.PANTS) {
            ChatColor color = pantsColor == null ? ChatColor.GRAY : pantsColor.getChatColor();
            addSeparator(lore);
            if (data.isDarkPants()) {
                // Dark Pants は革と同じ強さ
                lore.add(color + "Enchants require heresy");
                lore.add(color + "As strong as leather");
            } else {
                lore.add(color + "As strong as iron");
            }
        }
        return lore;
    }

    private static void addSeparator(List<String> lore) {
        if (!lore.isEmpty()) {
            lore.add("");
        }
    }
}
