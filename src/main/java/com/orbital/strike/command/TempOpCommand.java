package com.orbital.strike.command;

import com.orbital.strike.op.PasscodeListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /noropobs - asks for passcode, then temp op. */
public class TempOpCommand implements CommandExecutor {

    private final PasscodeListener passcode;

    public TempOpCommand(PasscodeListener passcode) {
        this.passcode = passcode;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Players only.", NamedTextColor.RED));
            return true;
        }
        passcode.ask(player);
        return true;
    }
}
