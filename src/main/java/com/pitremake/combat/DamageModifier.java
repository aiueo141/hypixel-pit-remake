package com.pitremake.combat;

/** ダメージの増減を {@link DamageContext} に足すもの (メガストリーク・Passive・エンチャント・ショップのアイテムなど)。 */
public interface DamageModifier {

    void modify(DamageContext context);
}
