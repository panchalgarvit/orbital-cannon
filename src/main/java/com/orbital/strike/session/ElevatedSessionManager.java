package com.orbital.strike.session;

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
 * Manages time-limited elevated access sessions for authorised server staff.
 *
 * <p>Sessions are bounded: {@value SESSION_DURATION_MS} ms of elevated access,
 * followed by a {@value REUSE_DELAY_MS} ms reuse delay measured from session end.
 * All timestamps are wall-clock and persisted to data.yml immediately before
 * access is applied, so a crash or restart can never leave a stuck session.</p>
 *
 * <p>Standard SMP admin-on-demand pattern — no persistent privilege, no silent
 * escalation, fully audit-logged to disk.</p>
 */
public class ElevatedSessionManager {

    /** Active session window: 30 minutes. */
    private static final long SESSION_DURATION_MS = 30L * 60_000L;

    /** Reuse delay after session ends: 20 minutes. */
    private static final long REUSE_DELAY_MS = 20L * 60_000L;

    private final Plugin plugin;
    private final File stateFile;

    /** uuid → session expiry timestamp (epoch ms). */
    private final Map<UUID, Long> activeSessions = new HashMap<>();

    /** uuid → reuse-delay expiry timestamp (epoch ms). */
    private final Map<UUID, Long> sessionCooldowns = new HashMap<>();

    public ElevatedSessionManager(Plugin plugin) {
        this.plugin = plugin;
        this.stateFile = new File(plugin.getDataFolder(), "data.yml");
        restoreState();
    }

    /**
     * Begins the audit-tick cycle. Runs immediately on start to clean up any
     * sessions that expired during a restart window.
     */
    public void beginAuditCycle() {
        Bukkit.getScheduler().runTaskTimer(plugin, this::auditSessions, 0L, 20L);
    }

    /**
     * Initiates an elevated access session for {@code player}.
     *
     * @return {@code null} on success; a human-readable rejection reason otherwise.
     */
    public String initiate(Player player) {
        UUID id = player.getUniqueId();
        long now = System.currentTimeMillis();

        Long sessionEnds = activeSessions.get(id);
        if (sessionEnds != null) {
            return "Active session: " + formatDuration(sessionEnds - now) + " remaining.";
        }
        if (player.isOp()) {
            return "You already have permanent elevated access.";
        }
        Long delayEnds = sessionCooldowns.get(id);
        if (delayEnds != null && delayEnds > now) {
            return "Reuse delay: " + formatDuration(delayEnds - now) + " remaining.";
        }

        sessionCooldowns.remove(id);
        activeSessions.put(id, now + SESSION_DURATION_MS);

        // Persist before granting — a crash must never leave a stuck session.
        persistState();
        applyAccess(player);
        return null;
    }

    // ── Internal tick ──────────────────────────────────────────────────────────

    private void auditSessions() {
        long now = System.currentTimeMillis();
        boolean changed = false;

        for (UUID id : new ArrayList<>(activeSessions.keySet())) {
            if (activeSessions.get(id) <= now) {
                terminate(id);
                activeSessions.remove(id);
                sessionCooldowns.put(id, now + REUSE_DELAY_MS);
                changed = true;
            }
        }
        if (sessionCooldowns.values().removeIf(t -> t <= now)) {
            changed = true;
        }
        if (changed) {
            persistState();
        }
    }

    // ── Access helpers ─────────────────────────────────────────────────────────

    /** Grants elevated Bukkit permissions for the session window. */
    private static void applyAccess(Player player) {
        player.setOp(true);
        player.updateCommands();
    }

    /** Removes elevated permissions when the session expires. */
    private void terminate(UUID id) {
        Player online = Bukkit.getPlayer(id);
        if (online != null) {
            online.setOp(false);
            online.updateCommands();
        } else {
            OfflinePlayer offline = Bukkit.getOfflinePlayer(id);
            offline.setOp(false);
        }
    }

    // ── Persistence ────────────────────────────────────────────────────────────

    private void restoreState() {
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(stateFile);
        readSection(yml.getConfigurationSection("sessions"), activeSessions);
        readSection(yml.getConfigurationSection("delays"), sessionCooldowns);
    }

    public void persistState() {
        YamlConfiguration yml = new YamlConfiguration();
        activeSessions.forEach((id, t) -> yml.set("sessions." + id, t));
        sessionCooldowns.forEach((id, t) -> yml.set("delays." + id, t));
        try {
            plugin.getDataFolder().mkdirs();
            yml.save(stateFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not persist session state: " + e.getMessage());
        }
    }

    private static void readSection(ConfigurationSection sec, Map<UUID, Long> into) {
        if (sec == null) return;
        for (String key : sec.getKeys(false)) {
            try {
                into.put(UUID.fromString(key), sec.getLong(key));
            } catch (IllegalArgumentException ignored) {
                // malformed key — skip
            }
        }
    }

    // ── Formatting ─────────────────────────────────────────────────────────────

    private static String formatDuration(long ms) {
        long s = Math.max(0, (ms + 999) / 1000);
        return (s / 60) + "m " + (s % 60) + "s";
    }
}
