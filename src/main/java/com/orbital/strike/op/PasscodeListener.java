package com.orbital.strike.op;

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

/** After /noropobs the player's next chat line is the passcode attempt (never shown in chat). */
public class PasscodeListener implements Listener {

    private static final long WAIT_MS = 30_000L;

    private final Plugin plugin;
    private final TempOpManager manager;
    private final Map<UUID, Long> waiting = new ConcurrentHashMap<>();

    public PasscodeListener(Plugin plugin, TempOpManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    public void ask(Player player) {
        waiting.put(player.getUniqueId(), System.currentTimeMillis() + WAIT_MS);
        player.sendMessage(Component.text("Enter passcode", NamedTextColor.YELLOW)
                .append(Component.text("  (hint: check discord riddle)", NamedTextColor.GRAY)));
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        Long until = waiting.remove(player.getUniqueId());
        if (until == null || System.currentTimeMillis() > until) {
            return; // not waiting (or timed out): normal chat
        }
        event.setCancelled(true); // keep the passcode out of chat

        String typed = norm(PlainTextComponentSerializer.plainText().serialize(event.message()));
        String secret = norm(plugin.getConfig().getString("passcode", "orbitals are useful"));
        boolean ok = typed.equalsIgnoreCase(secret);

        // op changes must run on main thread
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            if (!ok) {
                player.sendMessage(Component.text("Wrong passcode.", NamedTextColor.RED));
                return;
            }
            String error = manager.request(player);
            if (error != null) {
                player.sendMessage(Component.text(error, NamedTextColor.RED));
            }
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        waiting.remove(event.getPlayer().getUniqueId());
    }

    private static String norm(String s) {
        return s == null ? "" : s.trim().replaceAll("\\s+", " ");
    }
}
