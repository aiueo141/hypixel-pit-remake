package com.pitremake.item;

import org.bukkit.ChatColor;
import org.bukkit.Color;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Mystic Pants の色。通常の 5 色は染料の色を Minecraft のチャットカラーに合わせる。
 * RAGE は特殊なパンツ (Rage Pants) で、染料は #780000、文字色は一番近い §4。
 * DARK は特殊なパンツ (Dark Pants) で、染料は #000000、文字色は §5。
 * SEWER (#7DC383, §3) と AQUA (#55FFFF, §b) も特殊なパンツ。
 */
public enum PantsColor {
    RED("Red", ChatColor.RED, 0xFF5555),
    ORANGE("Orange", ChatColor.GOLD, 0xFFAA00),
    YELLOW("Yellow", ChatColor.YELLOW, 0xFFFF55),
    GREEN("Green", ChatColor.GREEN, 0x55FF55),
    BLUE("Blue", ChatColor.BLUE, 0x5555FF),
    RAGE("Rage", ChatColor.DARK_RED, 0x780000),
    DARK("Dark", ChatColor.DARK_PURPLE, 0x000000),
    SEWER("Sewer", ChatColor.DARK_AQUA, 0x7DC383),
    AQUA("Aqua", ChatColor.AQUA, 0x55FFFF);

    private final String displayName;
    private final ChatColor chatColor;
    private final Color color;

    PantsColor(String displayName, ChatColor chatColor, int rgb) {
        this.displayName = displayName;
        this.chatColor = chatColor;
        this.color = Color.fromRGB(rgb);
    }

    public String getDisplayName() {
        return displayName;
    }

    public ChatColor getChatColor() {
        return chatColor;
    }

    public Color getColor() {
        return color;
    }

    /** 名前 (大文字小文字は区別しない) から取得する。見つからなければ null。 */
    public static PantsColor fromName(String name) {
        for (PantsColor value : values()) {
            if (value.name().equalsIgnoreCase(name)) {
                return value;
            }
        }
        return null;
    }

    /** Nonce % 5 で決まる色の順番 (Tier III への強化で生贄にするパンツの色)。RAGE は含まない。 */
    private static final PantsColor[] NONCE_ORDER = {RED, YELLOW, BLUE, ORANGE, GREEN};

    /** 通常の 5 色 (Philosopher's Cactus で選べる色)。Nonce % 5 の順番。 */
    public static List<PantsColor> standardColors() {
        return Collections.unmodifiableList(Arrays.asList(NONCE_ORDER));
    }

    /** Nonce から色を決める (Nonce % 5)。 */
    public static PantsColor fromNonce(int nonce) {
        return NONCE_ORDER[Math.floorMod(nonce, NONCE_ORDER.length)];
    }

    /** この色になる Nonce % 5 の値。RAGE は -1。 */
    public int nonceIndex() {
        return Arrays.asList(NONCE_ORDER).indexOf(this);
    }

    /** 染料の色から取得する。見つからなければ null。 */
    public static PantsColor fromColor(Color color) {
        for (PantsColor value : values()) {
            if (value.color.equals(color)) {
                return value;
            }
        }
        return null;
    }
}
