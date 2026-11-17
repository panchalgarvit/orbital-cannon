package com.orbital.strike.strike;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class StrikeExecutor {

    private final Plugin plugin;

    public StrikeExecutor(Plugin plugin) {
        this.plugin = plugin;
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
                        Location spawnLoc = new Location(world,
                                target.getX() + r * Math.cos(angle),
                                center.getY() + wave * 2,
                                target.getZ() + r * Math.sin(angle));
                        TNTPrimed tnt = world.spawn(spawnLoc, TNTPrimed.class);
                        tnt.setFuseTicks(40 + wave * 10);
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
                Location spawnLoc = new Location(world, target.getX(), startY, target.getZ());
                TNTPrimed tnt = world.spawn(spawnLoc, TNTPrimed.class);
                tnt.setFuseTicks(25);
                world.spawnParticle(Particle.CRIT, target, 25, 0.5, 5.0, 0.5, 0.1);
                world.playSound(target, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.5f, 2f);
                count++;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }
}
