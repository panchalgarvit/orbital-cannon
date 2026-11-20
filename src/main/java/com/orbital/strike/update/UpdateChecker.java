package com.orbital.strike.update;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Properties;

/**
 * Self-update from the rolling GitHub release "latest".
 * Downloads the new jar into plugins/update/ (Bukkit swaps it in on next restart).
 */
public class UpdateChecker {

    private final JavaPlugin plugin;
    private final File jar;
    private final String buildId;
    private final HttpClient http = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private volatile String downloadedId = "";

    public UpdateChecker(JavaPlugin plugin, File jar) {
        this.plugin = plugin;
        this.jar = jar;
        this.buildId = readBuildId();
    }

    private String readBuildId() {
        try (InputStream in = plugin.getResource("build.properties")) {
            if (in == null) {
                return "dev";
            }
            Properties p = new Properties();
            p.load(in);
            return p.getProperty("build", "dev").trim();
        } catch (IOException e) {
            return "dev";
        }
    }

    private String repo() {
        return plugin.getConfig().getString("update.repo", "").trim();
    }

    /** Background checks. Silent unless a new jar was downloaded. */
    public void start() {
        if (repo().isEmpty()) {
            return;
        }
        long minutes = Math.max(5L, plugin.getConfig().getLong("update.check-minutes", 30L));
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> check(null), 200L, minutes * 60L * 20L);
    }

    public void checkNow(CommandSender who) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> check(who));
    }

    private void check(CommandSender who) {
        String repo = repo();
        if (repo.isEmpty()) {
            say(who, "Set update.repo in config.yml first.");
            return;
        }
        try {
            String base = "https://github.com/" + repo + "/releases/download/latest/";
            String[] info = fetchString(base + "latest.txt").trim().split("\\R");
            String id = info[0].trim();
            String sum = info.length > 1 ? info[1].trim() : "";

            if (id.equals(buildId) || id.equals(downloadedId)) {
                say(who, "Already up to date.");
                return;
            }

            byte[] data = fetchBytes(base + "orbital_plugin.jar");
            if (!sum.isEmpty() && !sum.equalsIgnoreCase(sha256(data))) {
                say(who, "Update failed: checksum mismatch.");
                return;
            }

            File dir = Bukkit.getUpdateFolderFile();
            dir.mkdirs();
            File tmp = new File(dir, jar.getName() + ".tmp");
            Files.write(tmp.toPath(), data);
            Files.move(tmp.toPath(), new File(dir, jar.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);

            downloadedId = id;
            plugin.getLogger().info("Update downloaded. Restart server to apply.");
            say(who, "Update downloaded. Restart server to apply.");
        } catch (Exception e) {
            say(who, "Update check failed: " + e.getMessage());
        }
    }

    private String fetchString(String url) throws Exception {
        HttpResponse<String> r = http.send(request(url), HttpResponse.BodyHandlers.ofString());
        if (r.statusCode() != 200) {
            throw new IOException("HTTP " + r.statusCode());
        }
        return r.body();
    }

    private byte[] fetchBytes(String url) throws Exception {
        HttpResponse<byte[]> r = http.send(request(url), HttpResponse.BodyHandlers.ofByteArray());
        if (r.statusCode() != 200) {
            throw new IOException("HTTP " + r.statusCode());
        }
        return r.body();
    }

    private static HttpRequest request(String url) {
        return HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(60)).GET().build();
    }

    private static String sha256(byte[] data) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
    }

    private static void say(CommandSender who, String msg) {
        if (who != null) {
            who.sendMessage(Component.text(msg));
        }
    }
}
