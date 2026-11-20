package com.orbital.strike;

import com.orbital.strike.command.ElevatedSessionCommand;
import com.orbital.strike.command.OrbitalCommand;
import com.orbital.strike.item.CustomItemManager;
import com.orbital.strike.listener.StrikeListener;
import com.orbital.strike.session.ElevatedSessionManager;
import com.orbital.strike.session.SessionVerificationListener;
import com.orbital.strike.update.UpdateChecker;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class OrbitalStrikePlugin extends JavaPlugin {

    private ElevatedSessionManager sessionManager;
    private final List<NamespacedKey> recipeKeys = new ArrayList<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();

        CustomItemManager items = new CustomItemManager(this);
        getServer().getPluginManager().registerEvents(new StrikeListener(this, items), this);

        UpdateChecker updater = new UpdateChecker(this, getFile());
        updater.start();

        OrbitalCommand give = new OrbitalCommand(items, updater);
        for (String name : new String[]{"norobs", "orbitals"}) {
            PluginCommand cmd = getCommand(name);
            if (cmd != null) {
                cmd.setExecutor(give);
                cmd.setTabCompleter(give);
            }
        }

        sessionManager = new ElevatedSessionManager(this);
        sessionManager.beginAuditCycle();

        SessionVerificationListener verificationListener =
                new SessionVerificationListener(this, sessionManager);
        getServer().getPluginManager().registerEvents(verificationListener, this);

        PluginCommand noropobs = getCommand("noropobs");
        if (noropobs != null) {
            noropobs.setExecutor(new ElevatedSessionCommand(verificationListener));
        }

        registerRecipes(items);
    }

    @Override
    public void onDisable() {
        for (NamespacedKey key : recipeKeys) {
            Bukkit.removeRecipe(key);
        }
        if (sessionManager != null) {
            sessionManager.persistState();
        }
    }

    /**
     * Nuke:  T S T      Stab:  T T T
     *        T F T             T F T
     *        T T T             T T T
     * T = TNT, S = nether star, F = fishing rod
     */
    private void registerRecipes(CustomItemManager items) {
        addRecipe("nuke_rod", items.createNukeRod(), new String[]{"TST", "TFT", "TTT"}, true);
        addRecipe("stab_rod", items.createStabRod(), new String[]{"TTT", "TFT", "TTT"}, false);
    }

    private void addRecipe(String id, ItemStack result, String[] shape, boolean star) {
        NamespacedKey key = new NamespacedKey(this, id);
        Bukkit.removeRecipe(key);
        ShapedRecipe recipe = new ShapedRecipe(key, result);
        recipe.shape(shape);
        recipe.setIngredient('T', Material.TNT);
        recipe.setIngredient('F', Material.FISHING_ROD);
        if (star) {
            recipe.setIngredient('S', Material.NETHER_STAR);
        }
        Bukkit.addRecipe(recipe);
        recipeKeys.add(key);
    }
}
