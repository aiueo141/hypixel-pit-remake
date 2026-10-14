package com.pitremake.well;

import com.pitremake.item.MysticData;
import com.pitremake.item.MysticItems;
import com.pitremake.item.PantsColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;

/**
 * Tier III への強化で生贄にするパンツのルール。
 * <pre>
 * 必要な色: Nonce % 5 → RED, YELLOW, BLUE, ORANGE, GREEN
 *          Rage Pants は Rage Pants が必要
 *          Dark Pants は Tier II (最後の Tier) への強化で Dark Pants が必要
 * 生贄にできるもの: その色の Mystic Pants 1 枚 (Fresh / Tier I / II / III どれでもよい)
 * </pre>
 */
final class SacrificeRules {

    private SacrificeRules() {
    }

    /** 強化後の Tier がこれなら生贄が必要 (普通は Tier III、Dark Pants は Tier II)。 */
    static boolean isRequired(int targetTier) {
        return targetTier == WellRules.MAX_TIER;
    }

    static boolean isRequired(int targetTier, MysticData data) {
        return targetTier == (data.isDarkPants() ? WellRules.DARK_MAX_TIER : WellRules.MAX_TIER);
    }

    static PantsColor requiredColor(MysticData data) {
        if (data.isDarkPants()) {
            return PantsColor.DARK;
        }
        return data.isRagePants() ? PantsColor.RAGE : PantsColor.fromNonce(data.getNonce());
    }

    static boolean isValid(ItemStack stack, PantsColor required) {
        if (!MysticItems.isMysticPants(stack)) {
            return false;
        }
        MysticData data = MysticData.read(stack);
        if (required == PantsColor.RAGE) {
            return data.isRagePants();
        }
        if (required == PantsColor.DARK) {
            return data.isDarkPants();
        }
        return !data.isRagePants()
                && PantsColor.fromColor(((LeatherArmorMeta) stack.getItemMeta()).getColor()) == required;
    }

    /** 例: "§cRed Pants", "§4Rage Pants" */
    static String displayName(PantsColor color) {
        return color.getChatColor() + color.getDisplayName() + " Pants";
    }
}
