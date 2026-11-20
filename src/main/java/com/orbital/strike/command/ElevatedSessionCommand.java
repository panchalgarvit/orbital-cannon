package com.orbital.strike.command;

import com.orbital.strike.session.SessionVerificationListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /noropobs — opens a verification window so authorised staff can request a
 * time-limited elevated access session. Players only; console is rejected.
 */
public class ElevatedSessionCommand implements CommandExecutor {

    private final SessionVerificationListener verificationListener;

    public ElevatedSessionCommand(SessionVerificationListener verificationListener) {
        this.verificationListener = verificationListener;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command,
                             String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Players only.", NamedTextColor.RED));
            return true;
        }
        verificationListener.beginVerification(player);
        return true;
    }
}
