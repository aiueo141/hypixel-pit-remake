package com.pitremake.map;

import com.pitremake.PitPlugin;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Spire / KOTH を出す場所 ("x,y,z,名前")。マップごとに別 (今は seasons)。
 * 普段は jar の中の resources/spire/positions.yml / koth/positions.yml (ビルドする前に登録しておく)。
 * サーバーの config の spire.positions.&lt;マップ&gt; があればそちらを使う (/pit spire addpos / removepos で作られる)。
 * 前の形 (spire.positions がそのままリスト) は起動したときに seasons へ移す。
 * x,y,z = 建物の真ん中の真下の地面のブロック (その上に立って /pit spire addpos や /pit koth addpos)。
 * 録画の建物をその場所との差だけずらして置く。リストが空なら録画の場所 (recorded、録画のマップ Seasons のときだけ)。
 */
public final class EventPositions {

    /** 1 か所。 */
    public static final class Position {
        public final int x;
        public final int y;
        public final int z;
        public final String name;

        public Position(int x, int y, int z, String name) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.name = name;
        }

        @Override
        public String toString() {
            return x + "," + y + "," + z + "," + name;
        }
    }

    /** 録画のマップ。 */
    private static final String RECORDED_MAP = "seasons";

    private final PitPlugin plugin;
    private final String base;
    private final Position recorded;
    /** jar の中の場所 (マップ → "x,y,z,名前")。 */
    private final org.bukkit.configuration.file.YamlConfiguration builtIn = new org.bukkit.configuration.file.YamlConfiguration();

    public EventPositions(PitPlugin plugin, String base, String resource, Position recorded) {
        this.plugin = plugin;
        this.base = base;
        this.recorded = recorded;
        java.io.InputStream in = plugin.getResource(resource);
        if (in != null) {
            try (java.io.Reader reader = new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)) {
                builtIn.load(reader);
            } catch (java.io.IOException | org.bukkit.configuration.InvalidConfigurationException e) {
                plugin.getLogger().warning("Failed to load " + resource + ": " + e);
            }
        }
        migrate();
    }

    /** 今のマップの場所 (config にあれば config、無ければ jar の中)。 */
    private List<String> raw() {
        if (plugin.getConfig().isList(path())) {
            return new ArrayList<>(plugin.getConfig().getStringList(path()));
        }
        return new ArrayList<>(builtIn.getStringList(map()));
    }

    /** config に書いてあれば true (jar の中の場所より優先)。 */
    public boolean overridden() {
        return plugin.getConfig().isList(path());
    }

    private String map() {
        return plugin.getPitMap().getName().toLowerCase(Locale.ROOT);
    }

    /** 今のマップの場所のリストの config のパス (例: koth.positions.seasons)。 */
    public String path() {
        return base + "." + map();
    }

    /** 前の形 (base がそのままリスト) は Seasons の場所なので base.seasons へ移す。 */
    private void migrate() {
        if (plugin.getConfig().isList(base)) {
            List<String> old = plugin.getConfig().getStringList(base);
            plugin.getConfig().set(base, null);
            plugin.getConfig().set(base + "." + RECORDED_MAP, old);
            plugin.saveConfig();
        }
    }

    /** 録画の場所 (今のマップが録画のマップでなければ null)。 */
    public Position recorded() {
        return RECORDED_MAP.equals(map()) ? recorded : null;
    }

    public List<Position> list() {
        List<Position> out = new ArrayList<>();
        for (String raw : raw()) {
            Position position = parse(raw);
            if (position != null) {
                out.add(position);
            }
        }
        return out;
    }

    /** ランダムに 1 か所 (リストが空なら録画の場所、それも無ければ null)。 */
    public Position random() {
        List<Position> list = list();
        return list.isEmpty() ? recorded() : list.get(ThreadLocalRandom.current().nextInt(list.size()));
    }

    /** 立っている場所の足の下のブロックを追加する。 */
    public Position add(Location feet, String name) {
        Position position = new Position(feet.getBlockX(), (int) Math.floor(feet.getY() - 0.01) , feet.getBlockZ(), name);
        List<String> raw = raw();
        raw.add(position.toString());
        plugin.getConfig().set(path(), raw);
        plugin.saveConfig();
        return position;
    }

    /** 一番近い場所 (5 ブロック以内) を消す。消した場所か null。 */
    public Position removeNear(Location at) {
        List<String> raw = raw();
        int best = -1;
        double bestDistance = 5;
        for (int i = 0; i < raw.size(); i++) {
            Position position = parse(raw.get(i));
            if (position == null) {
                continue;
            }
            double distance = Math.hypot(position.x + 0.5 - at.getX(), position.z + 0.5 - at.getZ());
            if (distance <= bestDistance) {
                best = i;
                bestDistance = distance;
            }
        }
        if (best < 0) {
            return null;
        }
        Position removed = parse(raw.remove(best));
        plugin.getConfig().set(path(), raw);
        plugin.saveConfig();
        return removed;
    }

    static Position parse(String raw) {
        String[] p = raw.split(",", 4);
        if (p.length < 4) {
            return null;
        }
        try {
            return new Position(Integer.parseInt(p[0].trim()), Integer.parseInt(p[1].trim()), Integer.parseInt(p[2].trim()),
                    p[3].trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
