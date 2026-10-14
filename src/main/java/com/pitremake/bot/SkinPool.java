package com.pitremake.bot;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

/**
 * Bot のスキン。MineSkin (https://api.mineskin.org、API キー無しで使える公開スキン) から署名付きのスキンを取ってきて
 * plugins/Pit/skins.yml に保存する。次からはファイルから読むので、足りないときだけ取りに行く。
 * <pre>
 * GET /v2/skins?size=N        : 公開スキンの一覧 (uuid だけ)
 * GET /v2/skins/{uuid}        : texture.data.value / signature (GameProfile の textures にそのまま入れる)
 * </pre>
 * 取りに行くのは別スレッドで、少し間を空けて 1 つずつ (API キー無しの回数制限に引っかからないように)。
 */
public final class SkinPool {

    /** これだけ集める。 */
    private static final int TARGET = 60;
    private static final String API = "https://api.mineskin.org/v2/skins";
    private static final long REQUEST_INTERVAL_MILLIS = 400;

    public static final class Skin {
        final String value;
        final String signature;

        Skin(String value, String signature) {
            this.value = value;
            this.signature = signature;
        }
    }

    private final JavaPlugin plugin;
    private final File file;
    private final List<Skin> skins = new CopyOnWriteArrayList<>();

    SkinPool(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "skins.yml");
    }

    /** ファイルから読み、足りなければ別スレッドで取りに行く。 */
    void load() {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (Map<?, ?> entry : yaml.getMapList("skins")) {
            Object value = entry.get("value");
            Object signature = entry.get("signature");
            if (value != null && signature != null) {
                skins.add(new Skin(value.toString(), signature.toString()));
            }
        }
        if (skins.size() < TARGET) {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, this::fetch);
        }
    }

    /** ランダムなスキン (まだ 1 つも無ければ null = いつもの Steve / Alex)。 */
    Skin random() {
        return skins.isEmpty() ? null : skins.get(ThreadLocalRandom.current().nextInt(skins.size()));
    }

    private void fetch() {
        try {
            JsonObject list = get(API + "?size=" + (TARGET * 2)).getAsJsonObject();
            JsonArray entries = list.getAsJsonArray("skins");
            int added = 0;
            for (JsonElement element : entries) {
                if (skins.size() >= TARGET) {
                    break;
                }
                String uuid = element.getAsJsonObject().get("uuid").getAsString();
                Thread.sleep(REQUEST_INTERVAL_MILLIS);
                try {
                    JsonObject data = get(API + "/" + uuid).getAsJsonObject()
                            .getAsJsonObject("skin").getAsJsonObject("texture").getAsJsonObject("data");
                    skins.add(new Skin(data.get("value").getAsString(), data.get("signature").getAsString()));
                    added++;
                } catch (IOException | RuntimeException e) {
                    // 1 つ取れなくても続ける
                }
            }
            plugin.getLogger().info("Fetched " + added + " bot skins from MineSkin (" + skins.size() + " total).");
            save();
        } catch (IOException | RuntimeException e) {
            plugin.getLogger().log(Level.WARNING, "Could not fetch bot skins from MineSkin: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private synchronized void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        List<Map<String, String>> list = new ArrayList<>();
        for (Skin skin : skins) {
            Map<String, String> entry = new HashMap<>();
            entry.put("value", skin.value);
            entry.put("signature", skin.signature);
            list.add(entry);
        }
        yaml.set("skins", list);
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save skins.yml", e);
        }
    }

    private static JsonElement get(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestProperty("User-Agent", "PitRemake/1.0");
        connection.setConnectTimeout(10_000);
        connection.setReadTimeout(10_000);
        if (connection.getResponseCode() != 200) {
            throw new IOException("HTTP " + connection.getResponseCode() + " for " + url);
        }
        try (InputStream in = connection.getInputStream()) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) > 0) {
                out.write(buffer, 0, read);
            }
            return new JsonParser().parse(new String(out.toByteArray(), StandardCharsets.UTF_8));
        }
    }
}
