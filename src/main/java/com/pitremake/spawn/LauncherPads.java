package com.pitremake.spawn;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * マップごとの Launcher Pad のデータ (resources/launcher/&lt;マップ名&gt;.yml)。
 * <pre>
 * pads      : スポーンの角のパッド。中心から見た向き atan2(z, x) が angle-min〜angle-max で、radius を外へ越えたら打ち上げる
 * targets   : パッドごとの着地点 "x,y,z,重み"。打ち上げごとに重みでランダムに選ぶ
 * templates : 録画の打ち上げの 0 tick からの "水平距離,高さ" (tick ごと)。着地点までの距離が一番近いものを伸縮して使う
 * </pre>
 */
final class LauncherPads {

    static final class Pad {
        final String name;
        final double angleMin;
        final double angleMax;
        final double radius;
        /** {x, y, z, 重み} */
        final List<double[]> targets;
        final double totalWeight;

        Pad(String name, double angleMin, double angleMax, double radius, List<double[]> targets) {
            this.name = name;
            this.angleMin = angleMin;
            this.angleMax = angleMax;
            this.radius = radius;
            this.targets = targets;
            double total = 0;
            for (double[] target : targets) {
                total += target[3];
            }
            this.totalWeight = total;
        }

        boolean contains(double x, double z) {
            double angle = Math.toDegrees(Math.atan2(z, x));
            return angle >= angleMin && angle <= angleMax;
        }
    }

    static final class Template {
        /** tick ごとの {水平距離, 高さ}。 */
        final double[][] path;

        Template(double[][] path) {
            this.path = path;
        }

        double distance() {
            return path[path.length - 1][0];
        }
    }

    final List<Pad> pads;
    final List<Template> templates;

    private LauncherPads(List<Pad> pads, List<Template> templates) {
        this.pads = pads;
        this.templates = templates;
    }

    /** in が null (そのマップのデータが無い) なら null。 */
    static LauncherPads load(InputStream in) throws IOException, InvalidConfigurationException {
        if (in == null) {
            return null;
        }
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) > 0) {
                out.write(buffer, 0, read);
            }
            return parse(new String(out.toByteArray(), StandardCharsets.UTF_8));
        } finally {
            in.close();
        }
    }

    static LauncherPads parse(String text) throws InvalidConfigurationException {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.loadFromString(text);
        List<Pad> pads = new ArrayList<>();
        ConfigurationSection section = yaml.getConfigurationSection("pads");
        if (section != null) {
            for (String name : section.getKeys(false)) {
                ConfigurationSection pad = section.getConfigurationSection(name);
                List<double[]> targets = new ArrayList<>();
                for (String target : pad.getStringList("targets")) {
                    targets.add(numbers(target, ","));
                }
                pads.add(new Pad(name, pad.getDouble("angle-min"), pad.getDouble("angle-max"), pad.getDouble("radius"),
                        Collections.unmodifiableList(targets)));
            }
        }
        List<Template> templates = new ArrayList<>();
        for (String template : yaml.getStringList("templates")) {
            String[] points = template.split(";");
            double[][] path = new double[points.length][];
            for (int i = 0; i < points.length; i++) {
                path[i] = numbers(points[i], ",");
            }
            templates.add(new Template(path));
        }
        return new LauncherPads(Collections.unmodifiableList(pads), Collections.unmodifiableList(templates));
    }

    private static double[] numbers(String text, String separator) {
        String[] parts = text.split(separator);
        double[] values = new double[parts.length];
        for (int i = 0; i < parts.length; i++) {
            values[i] = Double.parseDouble(parts[i].trim());
        }
        return values;
    }

    /** (fromX, fromZ) → (toX, toZ) の移動でパッドの radius を外へ越えたら、そのパッド。越えていなければ null。 */
    Pad crossed(double fromX, double fromZ, double toX, double toZ) {
        double from = Math.hypot(fromX, fromZ);
        double to = Math.hypot(toX, toZ);
        for (Pad pad : pads) {
            if (pad.contains(toX, toZ) && from < pad.radius && to >= pad.radius) {
                return pad;
            }
        }
        return null;
    }

    /** 中心から見た向き (度) が一番近いパッド (KOTH の丘に一番近いパッド)。 */
    Pad facing(double angle) {
        Pad best = null;
        double bestDiff = Double.MAX_VALUE;
        for (Pad pad : pads) {
            double diff = Math.abs(((pad.angleMin + pad.angleMax) / 2 - angle + 540) % 360 - 180);
            if (diff < bestDiff) {
                best = pad;
                bestDiff = diff;
            }
        }
        return best;
    }

    /** 重みでランダムに着地点 {x, y, z} を選ぶ。 */
    static double[] pickTarget(Pad pad, Random random) {
        double roll = random.nextDouble() * pad.totalWeight;
        for (double[] target : pad.targets) {
            roll -= target[3];
            if (roll < 0) {
                return target;
            }
        }
        return pad.targets.get(pad.targets.size() - 1);
    }

    /** 着地点までの水平距離が一番近いテンプレート。 */
    Template nearest(double distance) {
        Template best = null;
        for (Template template : templates) {
            if (best == null || Math.abs(template.distance() - distance) < Math.abs(best.distance() - distance)) {
                best = template;
            }
        }
        return best;
    }
}
