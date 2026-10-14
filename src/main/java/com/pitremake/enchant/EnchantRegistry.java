package com.pitremake.enchant;

import com.pitremake.item.MysticType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EnchantRegistry {

    private final Map<String, PitEnchant> enchants = new LinkedHashMap<>();

    public void register(PitEnchant enchant) {
        if (enchants.putIfAbsent(enchant.getKey(), enchant) != null) {
            throw new IllegalArgumentException("Duplicate enchant key: " + enchant.getKey());
        }
    }

    /** 見つからなければ null。 */
    public PitEnchant get(String key) {
        return enchants.get(key);
    }

    public Collection<PitEnchant> getAll() {
        return Collections.unmodifiableCollection(enchants.values());
    }

    /** その種類のアイテムに付けられる、指定レアリティのエンチャント (RESOURCE 含む)。 */
    public List<PitEnchant> getApplicable(MysticType type, EnchantRarity rarity) {
        List<PitEnchant> result = new ArrayList<>();
        for (PitEnchant enchant : enchants.values()) {
            if (enchant.getRarity() == rarity && enchant.canApplyTo(type)) {
                result.add(enchant);
            }
        }
        return result;
    }
}
