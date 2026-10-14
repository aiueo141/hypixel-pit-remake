package com.pitremake.bot;

import java.util.Collection;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Bot のプレイヤーっぽい名前 (3〜16 文字、英数字と _)。実在のプレイヤーの名前は使わず、単語と数字を組み合わせて作る。
 * 例: xXShadowSlayerXx / frost_fox27 / NekoPvP / TacoKing2011
 */
final class BotNames {

    private static final String[] FIRST = {
            "Shadow", "Dark", "Pro", "Ninja", "Frost", "Toxic", "Epic", "Sky", "Night", "Wolf", "Fire", "Ice", "Blaze",
            "Storm", "Pixel", "Cookie", "Panda", "Kitsune", "Neko", "Taco", "Mango", "Zero", "Void", "Ghost", "Lucky",
            "Crazy", "Silent", "Swift", "Jelly", "Mochi", "Sushi", "Lemon", "Toast", "Bean", "Nova", "Luna", "Astro",
            "Crimson", "Azure", "Venom", "Turbo", "Hyper", "Mega", "Tiny", "Sleepy", "Salty", "Spicy", "Golden"};
    private static final String[] SECOND = {
            "Killer", "Gamer", "Master", "Lord", "King", "Boy", "Girl", "Slayer", "Hunter", "Craft", "Pig", "Cat", "Fox",
            "Bear", "Dragon", "Sword", "Bow", "Pit", "Pants", "Gold", "PvP", "Clicker", "Bridger", "Sniper", "Main",
            "Tryhard", "God", "Noob", "Legend", "Warrior", "Knight", "Samurai", "Bunny", "Duck", "Frog", "Owl"};
    private static final String[] TAIL = {"", "", "", "YT", "TV", "PvP", "HD", "MC", "xd", "_", "__"};

    private BotNames() {
    }

    /** used に無い名前を 1 つ作る。 */
    static String random(Collection<String> used) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int attempt = 0; attempt < 100; attempt++) {
            String name = build(random);
            if (name.length() >= 3 && name.length() <= 16 && !used.contains(name)) {
                return name;
            }
        }
        return "Player" + random.nextInt(10_000);
    }

    private static String build(ThreadLocalRandom random) {
        String first = FIRST[random.nextInt(FIRST.length)];
        String second = random.nextInt(4) == 0 ? "" : SECOND[random.nextInt(SECOND.length)];
        String name;
        switch (random.nextInt(5)) {
            case 0:
                // xXShadowSlayerXx
                name = "xX" + first + second + "Xx";
                break;
            case 1:
                // frost_fox27
                name = first.toLowerCase() + (second.isEmpty() ? "" : "_" + second.toLowerCase());
                break;
            case 2:
                // TacoKing2011
                name = first + second;
                break;
            default:
                name = first + second + TAIL[random.nextInt(TAIL.length)];
                break;
        }
        int digits = random.nextInt(4);
        if (digits == 1) {
            name += random.nextInt(10);
        } else if (digits == 2) {
            name += random.nextInt(10, 100);
        } else if (digits == 3 && random.nextBoolean()) {
            name += 2000 + random.nextInt(15);
        }
        return name;
    }
}
