package com.pitremake.util;

public final class RomanNumerals {

    private static final int[] NUMBERS = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
    private static final String[] LETTERS = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};

    private RomanNumerals() {
    }

    /** 1〜3999 をローマ数字にする (プレステージ L = 50 まで使う)。範囲外はそのままの数字。 */
    public static String of(int value) {
        if (value <= 0 || value >= 4000) {
            return String.valueOf(value);
        }
        StringBuilder text = new StringBuilder();
        int rest = value;
        for (int i = 0; i < NUMBERS.length; i++) {
            while (rest >= NUMBERS[i]) {
                text.append(LETTERS[i]);
                rest -= NUMBERS[i];
            }
        }
        return text.toString();
    }
}
