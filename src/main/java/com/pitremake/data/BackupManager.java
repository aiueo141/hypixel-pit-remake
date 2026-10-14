package com.pitremake.data;

import com.pitremake.PitPlugin;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * バックアップ (/pit backup と、config の backup.interval-minutes ごと)。
 * <pre>
 * 1. メインスレッドで: Pit のデータを全部ファイルに書き、ワールドを保存する
 * 2. 別スレッドで: ワールドのフォルダ (地形・置かれたブロック) と plugins/Pit を
 *    サーバーのフォルダの backups/pit-日付-時刻.zip にまとめる
 * 3. 古いものは backup.keep 個だけ残す
 * </pre>
 * 戻すときはサーバーを止めて zip を展開し、ワールドと plugins/Pit を置き換える (docs/DATA_STORAGE.md)。
 */
public final class BackupManager {

    private final PitPlugin plugin;
    private volatile boolean running;

    public BackupManager(PitPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        int minutes = plugin.getConfig().getInt("backup.interval-minutes", 60);
        if (minutes > 0) {
            long ticks = minutes * 60L * 20L;
            plugin.getServer().getScheduler().runTaskTimer(plugin, () -> backup(null), ticks, ticks);
        }
    }

    /** バックアップを取る (sender が null なら自動)。 */
    public void backup(CommandSender sender) {
        if (running) {
            if (sender != null) {
                sender.sendMessage("§cA backup is already running.");
            }
            return;
        }
        running = true;
        plugin.saveAllData();
        List<File> worlds = new ArrayList<>();
        for (World world : plugin.getServer().getWorlds()) {
            world.save();
            worlds.add(world.getWorldFolder());
        }
        File dataFolder = plugin.getDataFolder();
        File backups = new File(plugin.getServer().getWorldContainer(), "backups");
        String name = "pit-" + new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new Date()) + ".zip";
        int keep = plugin.getConfig().getInt("backup.keep", 10);
        if (sender != null) {
            sender.sendMessage("§eBacking up worlds and Pit data...");
        }
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            File target = new File(backups, name);
            try {
                if (!backups.isDirectory() && !backups.mkdirs()) {
                    throw new IOException("Could not create " + backups);
                }
                try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(target))) {
                    for (File world : worlds) {
                        add(zip, world, "worlds/" + world.getName());
                    }
                    add(zip, dataFolder, "plugins/" + dataFolder.getName());
                }
                prune(backups, keep);
                long size = target.length() / 1024;
                plugin.getLogger().info("Backup saved: " + target.getPath() + " (" + size + " KB)");
                tell(sender, "§aBackup saved: §f" + target.getName() + " §7(" + size + " KB)");
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "Backup failed", e);
                tell(sender, "§cBackup failed: " + e.getMessage());
            } finally {
                running = false;
            }
        });
    }

    private void tell(CommandSender sender, String message) {
        if (sender != null) {
            plugin.getServer().getScheduler().runTask(plugin, () -> sender.sendMessage(message));
        }
    }

    private static void add(ZipOutputStream zip, File file, String path) throws IOException {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children == null) {
                return;
            }
            for (File child : children) {
                add(zip, child, path + "/" + child.getName());
            }
            return;
        }
        // 他のサーバーが使っている印のファイルは入れない
        if (file.getName().equals("session.lock")) {
            return;
        }
        zip.putNextEntry(new ZipEntry(path));
        try (InputStream in = new FileInputStream(file)) {
            byte[] buffer = new byte[16 * 1024];
            int read;
            while ((read = in.read(buffer)) > 0) {
                zip.write(buffer, 0, read);
            }
        } catch (IOException e) {
            // 書き込み中などで読めないファイルは飛ばす (ほかは続ける)
        }
        zip.closeEntry();
    }

    private static void prune(File folder, int keep) {
        File[] files = folder.listFiles((dir, name) -> name.startsWith("pit-") && name.endsWith(".zip"));
        if (files == null || files.length <= keep) {
            return;
        }
        Arrays.sort(files, Comparator.comparing(File::getName));
        for (int i = 0; i < files.length - keep; i++) {
            if (!files[i].delete()) {
                files[i].deleteOnExit();
            }
        }
    }
}
