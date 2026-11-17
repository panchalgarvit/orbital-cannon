package com.orbital.strike.op;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Temp op: 30 min of op, then 20 min cooldown (counted from the moment op ends).
 * Timestamps are wall-clock and saved to data.yml, so logout / restart / crash
 * can not keep op alive past expiry.
 */
public class TempOpManager {

    private static final long OP_MS = 30L * 60_000L;
    private static final long COOLDOWN_MS = 20L * 60_000L;

    private final Plugin plugin;
    private final File file;
    private final Map<UUID, Long> active = new HashMap<>();   // uuid -> op ends at
    private final Map<UUID, Long> cooldown = new HashMap<>(); // uuid -> cooldown ends at

    public TempOpManager(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");
        load();
    }

    /** Start expiry check. Runs once right away to clean up after restart. */
    public void start() {
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 0L, 20L);
    }

    /** @return error text for the player, or null on success. */
    public String request(Player player) {
        UUID id = player.getUniqueId();
        long now = System.currentTimeMillis();

        Long endsAt = active.get(id);
        if (endsAt != null) {
            return "Temp op active: " + fmt(endsAt - now) + " left.";
        }
        if (player.isOp()) {
            return "You are already op.";
        }
        Long cdEnds = cooldown.get(id);
        if (cdEnds != null && cdEnds > now) {
            return "Cooldown: " + fmt(cdEnds - now) + " left.";
        }

        cooldown.remove(id);
        active.put(id, now + OP_MS);
        save(); // save BEFORE granting, so a crash can never leave a stuck op
        player.setOp(true);
        player.updateCommands();
        return null;
    }

    private void tick() {
        long now = System.currentTimeMillis();
        boolean changed = false;

        for (UUID id : new ArrayList<>(active.keySet())) {
            if (active.get(id) <= now) {
                revoke(id);
                active.remove(id);
                cooldown.put(id, now + COOLDOWN_MS);
                changed = true;
            }
        }
        if (cooldown.values().removeIf(t -> t <= now)) {
            changed = true;
        }
        if (changed) {
            save();
        }
    }

    private void revoke(UUID id) {
        Player online = Bukkit.getPlayer(id);
        if (online != null) {
            online.setOp(false);
            online.updateCommands();
        } else {
            OfflinePlayer off = Bukkit.getOfflinePlayer(id);
            off.setOp(false);
        }
    }

    private void load() {
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        read(yml.getConfigurationSection("active"), active);
        read(yml.getConfigurationSection("cooldown"), cooldown);
    }

    private static void read(ConfigurationSection sec, Map<UUID, Long> into) {
        if (sec == null) {
            return;
        }
        for (String key : sec.getKeys(false)) {
            try {
                into.put(UUID.fromString(key), sec.getLong(key));
            } catch (IllegalArgumentException ignored) {
                // bad key, skip
            }
        }
    }

    public void save() {
        YamlConfiguration yml = new YamlConfiguration();
        active.forEach((id, t) -> yml.set("active." + id, t));
        cooldown.forEach((id, t) -> yml.set("cooldown." + id, t));
        try {
            plugin.getDataFolder().mkdirs();
            yml.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save data.yml: " + e.getMessage());
        }
    }

    private static String fmt(long ms) {
        long s = Math.max(0, (ms + 999) / 1000);
        return (s / 60) + "m " + (s % 60) + "s";
    }
}
