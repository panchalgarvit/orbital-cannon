package com.orbital.strike.command;

import com.orbital.strike.op.TempOpManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /noropobs - temp op. */
public class TempOpCommand implements CommandExecutor {

    private final TempOpManager manager;

    public TempOpCommand(TempOpManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Players only.", NamedTextColor.RED));
            return true;
        }
        String error = manager.request(player);
        if (error != null) {
            player.sendMessage(Component.text(error, NamedTextColor.RED));
        }
        return true;
    }
}
