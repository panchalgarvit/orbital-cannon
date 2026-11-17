package com.orbital.strike.command;

import com.orbital.strike.item.CustomItemManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** /norobs give <player> <nuke|stab> [amount]  - no permission needed. */
public class OrbitalCommand implements CommandExecutor, TabCompleter {

    private static final int MAX_AMOUNT = 64;

    private final CustomItemManager itemManager;

    public OrbitalCommand(CustomItemManager itemManager) {
        this.itemManager = itemManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 3 || !args[0].equalsIgnoreCase("give")) {
            sender.sendMessage(Component.text("Usage: /norobs give <player> <nuke|stab> [amount]", NamedTextColor.RED));
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
            return true;
        }

        String type = args[2].toLowerCase(Locale.ROOT);
        if (!type.equals(CustomItemManager.TYPE_NUKE) && !type.equals(CustomItemManager.TYPE_STAB)) {
            sender.sendMessage(Component.text("Invalid strike type. Use 'nuke' or 'stab'.", NamedTextColor.RED));
            return true;
        }

        int amount = 1;
        if (args.length >= 4) {
            try {
                amount = Integer.parseInt(args[3]);
            } catch (NumberFormatException ignored) {
                amount = 1;
            }
        }
        amount = Math.max(1, Math.min(MAX_AMOUNT, amount));

        for (int i = 0; i < amount; i++) {
            ItemStack rod = type.equals(CustomItemManager.TYPE_NUKE)
                    ? itemManager.createNukeRod()
                    : itemManager.createStabRod();
            // inventory full -> drop at feet instead of losing it
            target.getInventory().addItem(rod).values()
                    .forEach(left -> target.getWorld().dropItemNaturally(target.getLocation(), left));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        switch (args.length) {
            case 1 -> out.add("give");
            case 2 -> Bukkit.getOnlinePlayers().forEach(p -> out.add(p.getName()));
            case 3 -> {
                out.add(CustomItemManager.TYPE_NUKE);
                out.add(CustomItemManager.TYPE_STAB);
            }
            default -> {
                return Collections.emptyList();
            }
        }
        String prefix = args[args.length - 1].toLowerCase(Locale.ROOT);
        out.removeIf(s -> !s.toLowerCase(Locale.ROOT).startsWith(prefix));
        return out;
    }
}
