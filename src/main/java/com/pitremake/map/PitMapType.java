package com.pitremake.map;

import org.bukkit.Location;
import org.bukkit.Material;

/**
 * Pit のマップごとの数値 (ユーザーから: スポーンの高さ・半径、下の床の高さ・真ん中の円の半径、打ち上げの半径、
 * マップを見分けるブロック)。今のマップは Seasons (将来ほかのマップも足す)。
 * <p>
 * 真ん中の円 (floorRadius) の中にはブロックを置けない。円の判定は元のコードと同じく
 * ブロック座標 (x, z) について (x - 0.5)² + (z - 0.5)² ≤ r²。置けない範囲は円より 1 ブロック小さい (ユーザーの指定)。
 */
public enum PitMapType {
    ABYSS("Abyss", 114, 30, 82, 9.9, 22, -14, 115, 0, Material.LOG),
    GENESIS("Genesis", 86, 30, 43, 10.9, 28, -19, 87, 0, Material.STONE),
    SEASONS("Seasons", 114, 30, 82, 9.9, 25, -17, 115, 0, Material.WOOD),
    ELEMENTS("Elements", 114, 30, 82, 9.9, 22, -14, 118, -1, Material.STAINED_GLASS),
    CASTLE("Castle", 95, 30, 71, 9.9, 22, -16, 96, 0, Material.SMOOTH_BRICK);

    private final String name;
    private final int spawnY;
    private final int spawnRadius;
    private final int floorY;
    private final double floorRadius;
    private final double launcherRadius;
    private final int identifierX;
    private final int identifierY;
    private final int identifierZ;
    private final Material identifierBlock;

    PitMapType(String name, int spawnY, int spawnRadius, int floorY, double floorRadius, double launcherRadius,
               int identifierX, int identifierY, int identifierZ, Material identifierBlock) {
        this.name = name;
        this.spawnY = spawnY;
        this.spawnRadius = spawnRadius;
        this.floorY = floorY;
        this.floorRadius = floorRadius;
        this.launcherRadius = launcherRadius;
        this.identifierX = identifierX;
        this.identifierY = identifierY;
        this.identifierZ = identifierZ;
        this.identifierBlock = identifierBlock;
    }

    public String getName() {
        return name;
    }

    public int getSpawnY() {
        return spawnY;
    }

    public int getSpawnRadius() {
        return spawnRadius;
    }

    public int getFloorY() {
        return floorY;
    }

    public double getFloorRadius() {
        return floorRadius;
    }

    public double getLauncherRadius() {
        return launcherRadius;
    }

    public int getIdentifierX() {
        return identifierX;
    }

    public int getIdentifierY() {
        return identifierY;
    }

    public int getIdentifierZ() {
        return identifierZ;
    }

    public Material getIdentifierBlock() {
        return identifierBlock;
    }

    /** ブロックを置けない範囲を床の円より小さくする量。 */
    private static final double NO_BUILD_SHRINK = 1;

    /** そのブロックの位置が真ん中の置けない範囲の中 (床の高さ以上) か。 */
    public boolean isCenter(int blockX, int blockY, int blockZ) {
        double cx = blockX - 0.5;
        double cz = blockZ - 0.5;
        double radius = floorRadius - NO_BUILD_SHRINK;
        return blockY >= floorY && cx * cx + cz * cz <= radius * radius;
    }

    public boolean isCenter(Location location) {
        return isCenter(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public static PitMapType fromName(String name) {
        for (PitMapType type : values()) {
            if (type.getName().equalsIgnoreCase(name)) {
                return type;
            }
        }
        return null;
    }
}
