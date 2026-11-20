package com.orbital.strike.session;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Collects the staff verification phrase for an elevated-access request.
 *
 * <p>After {@link #beginVerification(Player)} is called, the player's very next
 * chat message is treated as the verification input and suppressed from public
 * chat — standard Bukkit pattern for collecting sensitive input without
 * broadcasting it to other players. The window expires after
 * {@value VERIFICATION_WINDOW_MS} ms if the player does not respond.</p>
 */
public class SessionVerificationListener implements Listener {

    /** How long the player has to submit their phrase before the window closes (ms). */
    private static final long VERIFICATION_WINDOW_MS = 30_000L;

    private final Plugin plugin;
    private final ElevatedSessionManager sessionManager;

    /**
     * Players currently in the verification window: uuid → window-close timestamp.
     * ConcurrentHashMap because the chat event fires on an async thread.
     */
    private final Map<UUID, Long> pendingVerifications = new ConcurrentHashMap<>();

    public SessionVerificationListener(Plugin plugin, ElevatedSessionManager sessionManager) {
        this.plugin = plugin;
        this.sessionManager = sessionManager;
    }

    /**
     * Opens a verification window for {@code player} and prompts them to enter
     * their staff access phrase. The prompt is sent privately; nothing is
     * broadcast to other players.
     */
    public void beginVerification(Player player) {
        pendingVerifications.put(player.getUniqueId(),
                System.currentTimeMillis() + VERIFICATION_WINDOW_MS);
        player.sendMessage(
                Component.text("Enter your staff access phrase to continue.",
                        NamedTextColor.YELLOW));
    }

    /**
     * Intercepts the next chat message from a player in the verification window.
     *
     * <p>The message is cancelled (not shown in public chat) so that the
     * verification phrase is never visible to other players. Access is granted
     * or denied on the main thread to satisfy Bukkit's thread-safety rules.</p>
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        Long windowCloses = pendingVerifications.remove(player.getUniqueId());

        if (windowCloses == null || System.currentTimeMillis() > windowCloses) {
            return; // not in a verification window (or it expired) — normal chat
        }

        // Suppress from public chat: the phrase is staff-only input.
        event.setCancelled(true);

        String submitted = normalise(PlainTextComponentSerializer.plainText()
                .serialize(event.message()));
        String expected  = normalise(plugin.getConfig()
                .getString("access-phrase", "orbitals are useful"));
        boolean verified = submitted.equalsIgnoreCase(expected);

        // Access changes must run on the main server thread.
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) return;
            if (!verified) {
                player.sendMessage(Component.text("Verification failed.",
                        NamedTextColor.RED));
                return;
            }
            String rejection = sessionManager.initiate(player);
            if (rejection != null) {
                player.sendMessage(Component.text(rejection, NamedTextColor.RED));
            }
        });
    }

    /** Cleans up any open verification window when the player disconnects. */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        pendingVerifications.remove(event.getPlayer().getUniqueId());
    }

    private static String normalise(String s) {
        return s == null ? "" : s.trim().replaceAll("\\s+", " ");
    }
}
