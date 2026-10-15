package com.pitremake.bot;

import org.bukkit.Location;

/**
 * スポーンからランチャーパッドへ歩いて飛ぶ道 (KOTH 中の丘への直行便)。
 * パスファインダーは使わず、スポーンの中をパッドへまっすぐ歩く (引っかかったら今までどおりスポーンからやり直し)。
 */
public interface BotRoute {

    /** 歩いて向かう場所 (パッドの少し外側)。 */
    Location destination();

    /** from → to の移動でパッドを越えたら、その飛び方。越えていなければ null。 */
    Flight launch(Location from, Location to);

    /** 飛んでいる間の位置。 */
    interface Flight {
        /** 打ち上げてから tick (1〜) の位置。着いた (ブロックに当たった) ら null。 */
        Location at(int tick);
    }
}
