package com.orbital.strike.strike;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

public class StrikeExecutor {

    /** Safety timeout (ticks). TNT normally blows on landing, this only stops stuck ones. */
    private static final int MAX_FUSE = 400;

    private final Plugin plugin;
    private final List<TNTPrimed> falling = new ArrayList<>();
    private BukkitTask watcher;

    public StrikeExecutor(Plugin plugin) {
        this.plugin = plugin;
    }

    /** Spawn TNT that explodes when it touches the ground, not mid-air. */
    private void dropTnt(World world, double x, double y, double z) {
        // never spawn inside terrain
        double minY = world.getHighestBlockYAt((int) Math.floor(x), (int) Math.floor(z)) + 2;
        TNTPrimed tnt = world.spawn(new Location(world, x, Math.max(y, minY), z), TNTPrimed.class);
        tnt.setFuseTicks(MAX_FUSE);
        falling.add(tnt);
        startWatcher();
    }

    private void startWatcher() {
        if (watcher != null) {
            return;
        }
        watcher = new BukkitRunnable() {
            @Override
            public void run() {
                falling.removeIf(t -> {
                    if (!t.isValid()) {
                        return true;
                    }
                    if (t.isOnGround()) {
                        t.setFuseTicks(1);
                        return true;
                    }
                    return false;
                });
                if (falling.isEmpty()) {
                    cancel();
                    watcher = null;
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    public void executeNuke(Location target) {
        World world = target.getWorld();
        if (world == null) {
            return;
        }
        world.playSound(target, Sound.ENTITY_ENDER_DRAGON_GROWL, 2f, 0.5f);

        double startY = Math.min(world.getMaxHeight() - 10, target.getY() + 40.0);
        Location center = new Location(world, target.getX(), startY, target.getZ());

        new BukkitRunnable() {
            int wave = 0;

            @Override
            public void run() {
                if (wave >= 3) {
                    cancel();
                    return;
                }
                world.playSound(center, Sound.ENTITY_WITHER_SPAWN, 1f, 1.2f);
                for (double r = 2.0; r <= 10.0; r += 3.0) {
                    int points = (int) (r * 6.0);
                    for (int i = 0; i < points; i++) {
                        double angle = 2 * Math.PI * i / points;
                        dropTnt(world,
                                target.getX() + r * Math.cos(angle),
                                center.getY() + wave * 2,
                                target.getZ() + r * Math.sin(angle));
                    }
                }
                wave++;
            }
        }.runTaskTimer(plugin, 0L, 15L);
    }

    public void executeStab(Location target) {
        World world = target.getWorld();
        if (world == null) {
            return;
        }
        world.playSound(target, Sound.ENTITY_WARDEN_SONIC_BOOM, 2f, 1f);

        double startY = Math.min(world.getMaxHeight() - 5, target.getY() + 50.0);

        new BukkitRunnable() {
            int count = 0;

            @Override
            public void run() {
                if (count >= 12) {
                    cancel();
                    return;
                }
                dropTnt(world, target.getX(), startY, target.getZ());
                world.spawnParticle(Particle.CRIT, target, 25, 0.5, 5.0, 0.5, 0.1);
                world.playSound(target, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.5f, 2f);
                count++;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }
}
