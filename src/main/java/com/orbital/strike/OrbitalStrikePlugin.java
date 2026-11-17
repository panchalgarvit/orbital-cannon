package com.orbital.strike;

import com.orbital.strike.command.OrbitalCommand;
import com.orbital.strike.command.TempOpCommand;
import com.orbital.strike.item.CustomItemManager;
import com.orbital.strike.listener.StrikeListener;
import com.orbital.strike.op.TempOpManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class OrbitalStrikePlugin extends JavaPlugin {

    private TempOpManager tempOp;

    @Override
    public void onEnable() {
        CustomItemManager items = new CustomItemManager(this);
        getServer().getPluginManager().registerEvents(new StrikeListener(this, items), this);

        OrbitalCommand give = new OrbitalCommand(items);
        PluginCommand norobs = getCommand("norobs");
        if (norobs != null) {
            norobs.setExecutor(give);
            norobs.setTabCompleter(give);
        }

        tempOp = new TempOpManager(this);
        tempOp.start();
        PluginCommand noropobs = getCommand("noropobs");
        if (noropobs != null) {
            noropobs.setExecutor(new TempOpCommand(tempOp));
        }
    }

    @Override
    public void onDisable() {
        if (tempOp != null) {
            tempOp.save();
        }
    }
}
