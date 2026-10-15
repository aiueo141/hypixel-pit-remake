package com.pitremake.listener;

import com.pitremake.item.MysticItems;
import com.pitremake.item.RewardItems;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageModifier;
import org.bukkit.inventory.ItemStack;

/**
 * 1.8 では防御ポイントがアイテムの種類で固定されているため、
 * Fresh Pants (革) を鉄のレギンス相当、Golden Helmet をダイヤのヘルメット相当にするよう防具による軽減量を再計算する。
 * (Guardian などの増減は {@code combat.EnchantEffects} で掛け算する)
 */
public final class ArmorListener implements Listener {

    private static final int LEATHER_LEGGINGS_ARMOR_POINTS = 2;
    /** 1 防御ポイントあたりの軽減率 (1.8 は 4%)。 */
    private static final double REDUCTION_PER_POINT = 0.04;

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity)) {
            return;
        }
        LivingEntity entity = (LivingEntity) event.getEntity();
        int extraPoints = 0;
        ItemStack leggings = entity.getEquipment().getLeggings();
        if (MysticItems.isMysticPants(leggings)) {
            // Pit のパンツは Fresh・Mystic・Dark のどれも鉄のレギンスと同じ 5 ポイント。Dark Pants も説明文 (As strong as leather)
            // と違い 5 (逆算レポート-1: Somber を着た人への被弾 627 件が、2 ポイントでは 1 件も一致せず 5 ポイントで揃う)
            extraPoints += MysticItems.PANTS_ARMOR_POINTS - LEATHER_LEGGINGS_ARMOR_POINTS;
        }
        // Golden Helmet はダイヤのヘルメットと同じ強さ
        if (RewardItems.is(entity.getEquipment().getHelmet(), RewardItems.Type.GOLDEN_HELMET)) {
            extraPoints += RewardItems.GOLDEN_HELMET_EXTRA_ARMOR;
        }
        if (extraPoints == 0 || !event.isApplicable(DamageModifier.ARMOR)) {
            return;
        }
        double oldArmor = event.getDamage(DamageModifier.ARMOR);
        if (oldArmor == 0) {
            // 防具を無視するダメージ (落下・奈落など)
            return;
        }

        // 防具軽減は BASE + HARD_HAT + BLOCKING 適用後のダメージに掛かる
        double incoming = event.getDamage(DamageModifier.BASE)
                + damageIfApplicable(event, DamageModifier.HARD_HAT)
                + damageIfApplicable(event, DamageModifier.BLOCKING);
        double newArmor = Math.max(oldArmor - incoming * extraPoints * REDUCTION_PER_POINT, -incoming);

        // 後段の軽減 (耐性・ダメージ軽減エンチャント) は防具適用後のダメージに比例するので合わせて縮める
        double before = incoming + oldArmor;
        double after = incoming + newArmor;
        double scale = before > 0 ? after / before : 0;
        event.setDamage(DamageModifier.ARMOR, newArmor);
        double remaining = after;
        for (DamageModifier modifier : new DamageModifier[]{DamageModifier.RESISTANCE, DamageModifier.MAGIC}) {
            if (event.isApplicable(modifier)) {
                double scaled = event.getDamage(modifier) * scale;
                event.setDamage(modifier, scaled);
                remaining += scaled;
            }
        }
        if (event.isApplicable(DamageModifier.ABSORPTION)) {
            event.setDamage(DamageModifier.ABSORPTION,
                    Math.max(event.getDamage(DamageModifier.ABSORPTION), -remaining));
        }
    }

    private static double damageIfApplicable(EntityDamageEvent event, DamageModifier modifier) {
        return event.isApplicable(modifier) ? event.getDamage(modifier) : 0;
    }
}
