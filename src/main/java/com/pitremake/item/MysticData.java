package com.pitremake.item;

import com.pitremake.util.NbtUtil;
import net.minecraft.server.v1_8_R3.NBTTagCompound;
import net.minecraft.server.v1_8_R3.NBTTagList;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ミスティックアイテムの状態。Hypixel と同じ形式で tag.ExtraAttributes に保存する。
 * <pre>
 * ExtraAttributes: {
 *   Nonce: 9496665,
 *   Lives: 87, MaxLives: 100,
 *   UpgradeTier: 3,              // 0 = Fresh
 *   UpgradeGemsUses: 1,          // ジェム済み
 *   RagePants: 1,                // 特殊なパンツ (RagePants / DarkPants / SewerPants / AquaPants のどれか 1 つ)
 *   CustomEnchants: [{Key: "billionaire", Level: 3}, ...]
 * }
 * </pre>
 */
public final class MysticData {

    private static final String NONCE = "Nonce";
    static final String LIVES = "Lives";
    static final String MAX_LIVES = "MaxLives";
    private static final String TIER = "UpgradeTier";
    private static final String GEMS = "UpgradeGemsUses";
    private static final String JEWEL_KILLS = "HiddenJewelKills";
    private static final String ENCHANTS = "CustomEnchants";
    private static final String ENCHANT_KEY = "Key";
    private static final String ENCHANT_LEVEL = "Level";
    private static final int TAG_COMPOUND = 10;

    private int nonce;
    private int lives;
    private int maxLives;
    private int tier;
    private boolean gemmed;
    /** 特殊なパンツの種類。通常のアイテムは null。 */
    private SpecialPants specialPants;
    /** Hidden Jewel のキル数 (表示のみ。数える処理はまだ無い)。 */
    private int hiddenJewelKills;
    /** エンチャントキー → レベル。付いた順を保持する。 */
    private final Map<String, Integer> enchants = new LinkedHashMap<>();

    public static MysticData read(ItemStack item) {
        NBTTagCompound extra = NbtUtil.readExtraAttributes(item);
        MysticData data = new MysticData();
        data.nonce = extra.getInt(NONCE);
        data.lives = extra.getInt(LIVES);
        data.maxLives = extra.getInt(MAX_LIVES);
        data.tier = extra.getInt(TIER);
        data.gemmed = extra.getInt(GEMS) > 0;
        data.hiddenJewelKills = extra.getInt(JEWEL_KILLS);
        for (SpecialPants special : SpecialPants.values()) {
            if (extra.getInt(special.getNbtKey()) > 0) {
                data.specialPants = special;
            }
        }
        NBTTagList list = extra.getList(ENCHANTS, TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            NBTTagCompound entry = list.get(i);
            // 万一同じキーが重複していたら高い方のレベルにまとめる (書き込み時は Map なので重複しない)
            data.enchants.merge(entry.getString(ENCHANT_KEY), entry.getInt(ENCHANT_LEVEL), Math::max);
        }
        return data;
    }

    /** このデータを書き込んだアイテムのコピーを返す。ExtraAttributes の他のキーは保持する。 */
    public ItemStack write(ItemStack item) {
        NBTTagCompound extra = NbtUtil.readExtraAttributes(item);
        extra.setInt(NONCE, nonce);
        if (maxLives > 0) {
            extra.setInt(LIVES, lives);
            extra.setInt(MAX_LIVES, maxLives);
        } else {
            extra.remove(LIVES);
            extra.remove(MAX_LIVES);
        }
        setOrRemove(extra, TIER, tier);
        setOrRemove(extra, GEMS, gemmed ? 1 : 0);
        for (SpecialPants special : SpecialPants.values()) {
            setOrRemove(extra, special.getNbtKey(), special == specialPants ? 1 : 0);
        }
        setOrRemove(extra, JEWEL_KILLS, hiddenJewelKills);
        if (enchants.isEmpty()) {
            extra.remove(ENCHANTS);
        } else {
            NBTTagList list = new NBTTagList();
            for (Map.Entry<String, Integer> enchant : enchants.entrySet()) {
                NBTTagCompound entry = new NBTTagCompound();
                entry.setString(ENCHANT_KEY, enchant.getKey());
                entry.setInt(ENCHANT_LEVEL, enchant.getValue());
                list.add(entry);
            }
            extra.set(ENCHANTS, list);
        }
        return NbtUtil.writeExtraAttributes(item, extra);
    }

    private static void setOrRemove(NBTTagCompound extra, String key, int value) {
        if (value > 0) {
            extra.setInt(key, value);
        } else {
            extra.remove(key);
        }
    }

    public boolean isFresh() {
        return tier == 0 && enchants.isEmpty();
    }

    public int getNonce() {
        return nonce;
    }

    public void setNonce(int nonce) {
        this.nonce = nonce;
    }

    public int getLives() {
        return lives;
    }

    public void setLives(int lives) {
        this.lives = lives;
    }

    public int getMaxLives() {
        return maxLives;
    }

    public void setMaxLives(int maxLives) {
        this.maxLives = maxLives;
    }

    public int getTier() {
        return tier;
    }

    public void setTier(int tier) {
        this.tier = tier;
    }

    public boolean isGemmed() {
        return gemmed;
    }

    public void setGemmed(boolean gemmed) {
        this.gemmed = gemmed;
    }

    /** 特殊なパンツの種類。通常のアイテムは null。 */
    public SpecialPants getSpecialPants() {
        return specialPants;
    }

    public void setSpecialPants(SpecialPants specialPants) {
        this.specialPants = specialPants;
    }

    public boolean isRagePants() {
        return specialPants == SpecialPants.RAGE;
    }

    public void setRagePants(boolean ragePants) {
        setSpecial(SpecialPants.RAGE, ragePants);
    }

    public boolean isDarkPants() {
        return specialPants == SpecialPants.DARK;
    }

    public void setDarkPants(boolean darkPants) {
        setSpecial(SpecialPants.DARK, darkPants);
    }

    public boolean isSewerPants() {
        return specialPants == SpecialPants.SEWER;
    }

    public boolean isAquaPants() {
        return specialPants == SpecialPants.AQUA;
    }

    public int getHiddenJewelKills() {
        return hiddenJewelKills;
    }

    public void setHiddenJewelKills(int hiddenJewelKills) {
        this.hiddenJewelKills = hiddenJewelKills;
    }

    private void setSpecial(SpecialPants special, boolean value) {
        if (value) {
            specialPants = special;
        } else if (specialPants == special) {
            specialPants = null;
        }
    }

    public Map<String, Integer> getEnchants() {
        return Collections.unmodifiableMap(enchants);
    }

    public int getEnchantLevel(String key) {
        Integer level = enchants.get(key);
        return level == null ? 0 : level;
    }

    public void setEnchant(String key, int level) {
        enchants.put(key, level);
    }

    public void removeEnchant(String key) {
        enchants.remove(key);
    }
}
