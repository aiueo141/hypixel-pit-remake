package com.pitremake.spire;

import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Spire のブロック (resources/spire/&lt;マップ名&gt;.txt の build / tunnel / open)。
 * Pit の地形を壊さないよう、置く前のブロックを覚えておき (plugins/Pit/spire-restore.yml にも保存)、終わったら全部戻す。
 * サーバーが途中で止まっても、次に起動したときにファイルから戻す。KOTH の丘も同じ仕組み (resources/koth, koth-restore.yml)。
 */
public final class SpireStructure {

    private static final String RESTORE_FILE = "spire-restore.yml";

    private final JavaPlugin plugin;
    private final String folder;
    /** フェーズ → {x, y, z, id, data} (y の小さい順)。 */
    private final Map<String, List<int[]>> phases = new HashMap<>();
    /** 位置 "x,y,z" → 置く前の {id, data} (最初に置いたときのもの)。 */
    private final Map<String, int[]> originals = new LinkedHashMap<>();
    /** フェーズ → 録画の場所からのずらし {dx, dy, dz} (/pit spire addpos などで足した場所に出すとき)。 */
    private final Map<String, int[]> offsets = new HashMap<>();
    private final File restoreFile;

    SpireStructure(JavaPlugin plugin) {
        this(plugin, "spire", RESTORE_FILE);
    }

    /** folder = resources の下のフォルダ、restoreFile = plugins/Pit に置く元のブロックのファイル。 */
    public SpireStructure(JavaPlugin plugin, String folder, String restoreFile) {
        this.plugin = plugin;
        this.folder = folder;
        this.restoreFile = new File(plugin.getDataFolder(), restoreFile);
    }

    /** そのマップのデータを読む。無ければ false。 */
    public boolean load(String map) {
        InputStream in = plugin.getResource(folder + "/" + map.toLowerCase(java.util.Locale.ROOT) + ".txt");
        if (in == null) {
            return false;
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            List<int[]> current = null;
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                if (line.startsWith("@")) {
                    current = new ArrayList<>();
                    phases.put(line.substring(1).trim(), current);
                    continue;
                }
                String[] p = line.trim().split(" ");
                if (current != null && p.length == 5) {
                    current.add(new int[]{Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2]),
                            Integer.parseInt(p[3]), Integer.parseInt(p[4])});
                }
            }
        } catch (IOException | NumberFormatException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to load spire data for " + map, e);
            return false;
        }
        return !phases.isEmpty();
    }

    /** データにあるフェーズの名前 (KOTH は丘の場所の名前)。 */
    public java.util.Set<String> phaseNames() {
        return phases.keySet();
    }

    /** そのフェーズのブロック {x, y, z, id, data} (ずらした後の位置)。 */
    public List<int[]> blocks(String phase) {
        List<int[]> blocks = phases.get(phase);
        if (blocks == null) {
            return new ArrayList<>();
        }
        int[] offset = offsets.get(phase);
        if (offset == null) {
            return blocks;
        }
        List<int[]> shifted = new ArrayList<>(blocks.size());
        for (int[] b : blocks) {
            shifted.add(new int[]{b[0] + offset[0], b[1] + offset[1], b[2] + offset[2], b[3], b[4]});
        }
        return shifted;
    }

    /** これから置くそのフェーズを録画の場所から (dx, dy, dz) ずらす。 */
    public void setOffset(String phase, int dx, int dy, int dz) {
        offsets.put(phase, new int[]{dx, dy, dz});
    }

    /** フェーズのブロックを全部すぐ置く。 */
    public void place(World world, String phase) {
        if (!phases.containsKey(phase)) {
            return;
        }
        List<int[]> blocks = blocks(phase);
        for (int[] b : blocks) {
            set(world, b);
        }
        saveOriginals();
    }

    /** フェーズのブロックを下から順に ticks かけて置く (録画: 塔は 70〜75 秒の約 5 秒で下から建つ)。 */
    public void placeAnimated(World world, String phase, int ticks) {
        List<int[]> all = blocks(phase);
        if (all.isEmpty()) {
            return;
        }
        // 先に全部の元のブロックを覚えて保存しておく (途中で止まっても戻せるよう)。変える必要のある所だけ置く
        List<int[]> blocks = new ArrayList<>();
        for (int[] b : all) {
            if (remember(world, b)) {
                blocks.add(b);
            }
        }
        saveOriginals();
        if (blocks.isEmpty()) {
            return;
        }
        int perTick = Math.max(1, (int) Math.ceil(blocks.size() / (double) Math.max(1, ticks)));
        for (int i = 0; i * perTick < blocks.size(); i++) {
            int from = i * perTick;
            int to = Math.min(blocks.size(), from + perTick);
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                for (int j = from; j < to; j++) {
                    int[] b = blocks.get(j);
                    world.getBlockAt(b[0], b[1], b[2]).setTypeIdAndData(b[3], (byte) b[4], false);
                }
            }, i);
        }
    }

    /**
     * 置く前のブロックを覚える。置く必要があれば true。
     * もう同じブロックなら置かない (覚えもしない)。看板・頭・チェストなど中身のあるブロックは戻せないので触らない。
     */
    private boolean remember(World world, int[] b) {
        String key = b[0] + "," + b[1] + "," + b[2];
        if (originals.containsKey(key)) {
            return true;
        }
        Block block = world.getBlockAt(b[0], b[1], b[2]);
        if (block.getTypeId() == b[3] && block.getData() == b[4]) {
            return false;
        }
        if (((org.bukkit.craftbukkit.v1_8_R3.CraftWorld) world).getHandle()
                .getTileEntity(new net.minecraft.server.v1_8_R3.BlockPosition(b[0], b[1], b[2])) != null) {
            return false;
        }
        originals.put(key, new int[]{block.getTypeId(), block.getData()});
        return true;
    }

    private void set(World world, int[] b) {
        if (remember(world, b)) {
            world.getBlockAt(b[0], b[1], b[2]).setTypeIdAndData(b[3], (byte) b[4], false);
        }
    }

    /**
     * 建物の中の空気もちゃんと空気で置く (別の場所に出したとき地形が中に残らないように)。
     * 建物のブロックがある縦の列ごとに、一番下から一番上 + clearance の、建物のブロックが無い所を空気にする
     * (塔の各階の部屋、丘の上の立つ所)。録画の場所ではもともと空気なので何も変わらない。
     */
    public void fillAir(String phase, int clearance) {
        List<int[]> blocks = phases.get(phase);
        if (blocks == null) {
            return;
        }
        Map<String, int[]> columns = new HashMap<>();
        java.util.Set<String> taken = new java.util.HashSet<>();
        for (int[] b : blocks) {
            taken.add(b[0] + "," + b[1] + "," + b[2]);
            if (b[3] == 0) {
                continue;
            }
            int[] range = columns.computeIfAbsent(b[0] + "," + b[2], k -> new int[]{b[0], b[2], b[1], b[1]});
            range[2] = Math.min(range[2], b[1]);
            range[3] = Math.max(range[3], b[1]);
        }
        List<int[]> filled = new ArrayList<>(blocks);
        for (int[] range : columns.values()) {
            for (int y = range[2]; y <= range[3] + clearance; y++) {
                if (taken.add(range[0] + "," + y + "," + range[1])) {
                    filled.add(new int[]{range[0], y, range[1], 0, 0});
                }
            }
        }
        // 下から順 (塔が下から建つ)
        filled.sort((a, b) -> Integer.compare(a[1], b[1]));
        phases.put(phase, filled);
    }

    public boolean hasPlaced() {
        return !originals.isEmpty();
    }

    /** 置いたブロックを全部元に戻す (後から置いたものから)。 */
    public void restore(World world) {
        List<Map.Entry<String, int[]>> entries = new ArrayList<>(originals.entrySet());
        for (int i = entries.size() - 1; i >= 0; i--) {
            String[] p = entries.get(i).getKey().split(",");
            int[] original = entries.get(i).getValue();
            world.getBlockAt(Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2]))
                    .setTypeIdAndData(original[0], (byte) original[1], false);
        }
        originals.clear();
        // 戻した地形をすぐ保存してから記録を消す (保存前に落ちても記録が残るように)
        world.save();
        if (restoreFile.exists() && !restoreFile.delete()) {
            plugin.getLogger().warning("Could not delete " + restoreFile);
        }
    }

    private void saveOriginals() {
        YamlConfiguration yaml = new YamlConfiguration();
        List<String> list = new ArrayList<>();
        for (Map.Entry<String, int[]> entry : originals.entrySet()) {
            list.add(entry.getKey() + "," + entry.getValue()[0] + "," + entry.getValue()[1]);
        }
        yaml.set("world", plugin.getServer().getWorlds().get(0).getName());
        yaml.set("blocks", list);
        try {
            yaml.save(restoreFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save " + restoreFile, e);
        }
    }

    /** 前回のサーバーが Spire の途中で止まっていたら、ファイルから地形を戻す。 */
    public void recover() {
        if (!restoreFile.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(restoreFile);
        World world = plugin.getServer().getWorld(yaml.getString("world", ""));
        if (world == null) {
            world = plugin.getServer().getWorlds().get(0);
        }
        int count = 0;
        for (String raw : yaml.getStringList("blocks")) {
            String[] p = raw.split(",");
            if (p.length == 5) {
                originals.put(p[0] + "," + p[1] + "," + p[2], new int[]{Integer.parseInt(p[3]), Integer.parseInt(p[4])});
                count++;
            }
        }
        restore(world);
        plugin.getLogger().info("Restored " + count + " blocks left by an unfinished Spire event.");
    }
}
