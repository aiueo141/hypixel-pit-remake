package com.pitremake.well;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Mystic Well の効果音。録画 (MysticWell.mcrc) のタイミング・音程・音量をそのまま再現する。
 * <pre>
 * 開く            : random.pop (1.206)
 * アイテムを置く   : random.pop (1.397)
 * アイテムを取る   : random.pop (0.746)
 * 強化できない     : mob.endermen.hit
 *
 * 抽選 (クリックからの tick):
 *   0 / 6     : random.pop の和音、3 / 9 : random.click
 *   15〜      : random.pop でアルペジオ (長三和音 + オクターブを半音ずつ上げていく)。音量は徐々に大きくなる
 *               43 tick 目からは毎 tick 鳴らす。Tier II 以上は 4 半音上の note.harp を重ねる
 *   最後の数 tick : random.successful_hit の和音 (Tier III は和音が上がっていき、harp がグリッサンド)
 *   終了      : fireworks.twinkle + fireworks.largeBlast
 *   終了 tick : Tier I = 60, Tier II = 70, Tier III = 82
 * </pre>
 */
final class WellSounds {

    private static final String POP = "random.pop";
    private static final String CLICK = "random.click";
    private static final String HARP = "note.harp";
    private static final String HIT = "random.successful_hit";
    private static final String TWINKLE = "fireworks.twinkle";
    private static final String BLAST = "fireworks.largeBlast";
    private static final String DENY = "mob.endermen.hit";

    /** アルペジオが始まる tick。 */
    private static final int SPIN_START = 15;
    /** この tick からアルペジオを毎 tick 鳴らす。 */
    private static final int DOUBLE_FROM = 43;
    /** アルペジオの 1 周 (長三和音 + オクターブ, 音符番号)。周ごとに半音上がる。 */
    private static final int[] CHORD = {2, 6, 9, 14};

    private WellSounds() {
    }

    static void open(Player player) {
        play(player, POP, 1.0f, pitchByte(76));
    }

    static void place(Player player) {
        play(player, POP, 1.0f, pitchByte(88));
    }

    static void remove(Player player) {
        play(player, POP, 1.0f, pitchByte(47));
    }

    static void deny(Player player) {
        play(player, DENY, 1.0f, 1.0f);
    }

    /** 抽選の効果音が鳴り終わる tick (この tick で花火)。 */
    static int endTick(int tier) {
        switch (tier) {
            case 1:
                return 60;
            case 2:
                return 70;
            default:
                return 82;
        }
    }

    /** successful_hit の和音が始まる tick。 */
    private static int finaleStart(int tier) {
        switch (tier) {
            case 1:
                return 52;
            case 2:
                return 62;
            default:
                return 60;
        }
    }

    /** 抽選中、クリックから tick 目の効果音を鳴らす。 */
    static void roll(Player player, int tier, int tick) {
        for (Sound sound : rollSounds(tier, tick)) {
            play(player, sound.name, sound.volume, sound.pitch);
        }
    }

    /** 抽選中、クリックから tick 目に鳴る効果音。 */
    static List<Sound> rollSounds(int tier, int tick) {
        List<Sound> sounds = new ArrayList<>();
        if (tick == 0) {
            sounds.add(new Sound(POP, 0.8f, pitchByte(35)));
            sounds.add(new Sound(POP, 0.8f, pitchByte(70)));
        } else if (tick == 3 || tick == 9) {
            sounds.add(new Sound(CLICK, 0.4f, pitchByte(52)));
        } else if (tick == 6) {
            sounds.add(new Sound(POP, 0.9f, pitchByte(44)));
            sounds.add(new Sound(POP, 0.9f, pitchByte(89)));
        }

        int end = endTick(tier);
        if (tick >= SPIN_START && tick < end) {
            int spin = tick - SPIN_START;
            if (tick >= DOUBLE_FROM || spin % 2 == 0) {
                int index = spin / 2;
                int note = CHORD[index % CHORD.length] + index / CHORD.length + (tier >= 3 ? 1 : 0);
                sounds.add(new Sound(POP, 1.16f + 0.02f * spin, note(note)));
                if (tier >= 2) {
                    sounds.add(harp(tier, tick, note));
                }
            }
            if (tick >= finaleStart(tier)) {
                int[] chord = finaleChord(tier, tick - finaleStart(tier));
                sounds.add(new Sound(HIT, 0.42f, note(chord[0])));
                sounds.add(new Sound(HIT, 0.42f, note(chord[1])));
            }
        }
        if (tick == end) {
            sounds.add(new Sound(TWINKLE, 0.9f, pitchByte(113)));
            sounds.add(new Sound(BLAST, 0.9f, pitchByte(75)));
        }
        return sounds;
    }

    /** Tier II 以上: 4 半音上の harp。Tier III の終盤は下から上へのグリッサンド。 */
    private static Sound harp(int tier, int tick, int note) {
        int glissandoStart = finaleStart(tier) + 1;
        if (tier >= 3 && tick >= glissandoStart) {
            boolean last = tick == endTick(tier) - 1;
            return new Sound(HARP, 0.76f, last ? pitchByte(189) : pitchByte(25 + 5 * (tick - glissandoStart)));
        }
        return new Sound(HARP, 0.85f, note(note + 4));
    }

    /** 1 つの効果音。 */
    static final class Sound {
        final String name;
        final float volume;
        final float pitch;

        Sound(String name, float volume, float pitch) {
            this.name = name;
            this.volume = volume;
            this.pitch = pitch;
        }

        /** パケットに載る音程 (0〜255)。 */
        int pitchByte() {
            return Math.min(255, (int) (pitch * 63));
        }
    }

    /** 終盤の successful_hit の 2 音。Tier III は 7 tick ごとに上がる。 */
    private static int[] finaleChord(int tier, int elapsed) {
        if (tier < 3 || elapsed < 7) {
            return new int[]{15, 19};
        }
        return elapsed < 14 ? new int[]{18, 21} : new int[]{21, 24};
    }

    /** 音符ブロックと同じ音階 (音符番号 12 = 音程 1.0)。 */
    private static float note(int note) {
        return (float) Math.pow(2, (note - 12) / 12.0);
    }

    /** パケットの音程 (0〜255, 63 = 1.0) を Bukkit の音程にする。 */
    private static float pitchByte(int value) {
        return value / 63f;
    }

    private static void play(Player player, String sound, float volume, float pitch) {
        player.playSound(player.getLocation(), sound, volume, pitch);
    }
}
