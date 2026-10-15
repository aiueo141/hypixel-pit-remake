package com.pitremake.npc;

import org.bukkit.entity.Villager;

/**
 * Pit の村人 NPC (録画 2026-10-14_01-08-56.mcrc / 01-09-29.mcrc の Hypixel と同じ見た目)。
 * 村人はファーマー、頭の上に 2 行の文字 (足元から 2.156 / 1.844 ブロック上の透明なアーマースタンド)。
 */
public enum PitNpcType {
    UPGRADES("§a§lUPGRADES", "§7Permanent", Villager.Profession.FARMER),
    /** 録画 2026-10-14_08-03-51.mcrc のマップでは鍛冶屋。 */
    ITEMS("§6§lITEMS", "§7Non-permanent", Villager.Profession.BLACKSMITH),
    /** 見た目だけの Hypixel のランクを付ける (録画の THE KEEPER の位置に置く)。 */
    RANKS("§6§lRANKS", "§7Cosmetic ranks", Villager.Profession.LIBRARIAN),
    /** 録画 2026-10-14_08-03-51.mcrc のマップでは司祭 (紫)。 */
    PRESTIGE("§e§lPRESTIGE", "§7Resets & Renown", Villager.Profession.PRIEST),
    /** 録画 2026-10-15_10-32-13.mcrc: (13.5, 114, 4.5) のファーマー。右クリックで My pit stats。 */
    STATS("§3§lSTATS", "§7View your stats", Villager.Profession.FARMER),
    /** 録画 2026-10-15_10-32-13.mcrc: (12.5, 114, -4.5) のライブラリアン。右クリックで Quests &amp; Contracts。 */
    QUEST_MASTER("§b§lQUEST MASTER", "§7Quests & Contracts", Villager.Profession.LIBRARIAN);

    /** 文字の高さ (村人の足元から)。 */
    static final double TITLE_HEIGHT = 2.156;
    static final double SUBTITLE_HEIGHT = 1.844;

    private final String title;
    private final String subtitle;
    private final Villager.Profession profession;

    PitNpcType(String title, String subtitle, Villager.Profession profession) {
        this.title = title;
        this.subtitle = subtitle;
        this.profession = profession;
    }

    public Villager.Profession getProfession() {
        return profession;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    /** 名前 (大文字小文字は区別しない)。該当しなければ null。 */
    public static PitNpcType fromName(String name) {
        for (PitNpcType type : values()) {
            if (type.name().equalsIgnoreCase(name)) {
                return type;
            }
        }
        return null;
    }
}
