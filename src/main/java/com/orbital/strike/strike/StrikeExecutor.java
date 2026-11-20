package com.orbital.strike.strike;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class StrikeExecutor {

    /** Safety timeout (ticks). TNT is normally detonated by the watcher, this only stops stuck ones. */
    private static final int MAX_FUSE = 600;
    private static final double GOLDEN_ANGLE = Math.PI * (3.0 - Math.sqrt(5.0));
    private static final double LANDING_ONLY = Double.NEGATIVE_INFINITY;

    private static final class Falling {
        final TNTPrimed tnt;
        final double detonateY; // stab: depth this TNT blows at. LANDING_ONLY = blow on landing
        final double speed;     // >0: forced constant downward speed (blocks/tick)
        int groundTicks;

        Falling(TNTPrimed tnt, double detonateY, double speed) {
            this.tnt = tnt;
            this.detonateY = detonateY;
            this.speed = speed;
        }
    }

    private final Plugin plugin;
    private final List<Falling> falling = new ArrayList<>();
    private BukkitTask watcher;

    public StrikeExecutor(Plugin plugin) {
        this.plugin = plugin;
    }

    private void dropTnt(World world, double x, double y, double z, float power, double speed, double detonateY) {
        int bx = (int) Math.floor(x);
        int bz = (int) Math.floor(z);
        if (!world.isChunkLoaded(bx >> 4, bz >> 4)) {
            return;
        }
        // never spawn inside terrain
        double minY = world.getHighestBlockYAt(bx, bz) + 2;
        TNTPrimed tnt = world.spawn(new Location(world, x, Math.max(y, minY), z), TNTPrimed.class);
        tnt.setFuseTicks(MAX_FUSE);
        tnt.setYield(power);
        if (speed > 0) {
            tnt.setVelocity(new Vector(0, -speed, 0));
        }
        falling.add(new Falling(tnt, detonateY, speed));
        startWatcher();
    }

    private void startWatcher() {
        if (watcher != null) {
            return;
        }
        watcher = new BukkitRunnable() {
            @Override
            public void run() {
                Iterator<Falling> it = falling.iterator();
                while (it.hasNext()) {
                    Falling f = it.next();
                    TNTPrimed t = f.tnt;
                    if (!t.isValid()) {
                        it.remove();
                        continue;
                    }
                    boolean ground = t.isOnGround();
                    if (f.detonateY == LANDING_ONLY) {
                        if (ground) {
                            t.setFuseTicks(1);
                            it.remove();
                        }
                        continue;
                    }
                    // stab: blow at planned depth, or if stuck on ground (bedrock / waiting at bottom)
                    f.groundTicks = ground ? f.groundTicks + 1 : 0;
                    if (t.getLocation().getY() <= f.detonateY + 0.5 || f.groundTicks >= 5) {
                        t.setVelocity(new Vector(0, 0, 0));
                        t.setFuseTicks(1);
                        it.remove();
                        continue;
                    }
                    t.setVelocity(new Vector(0, -f.speed, 0));
                }
                if (falling.isEmpty()) {
                    cancel();
                    watcher = null;
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    /** All TNT at once, spread over a wide disc, each blows on landing. */
    public void executeNuke(Location target) {
        World world = target.getWorld();
        if (world == null) {
            return;
        }
        FileConfiguration cfg = plugin.getConfig();
        int count = Math.max(1, cfg.getInt("nuke.count", 270));
        double radius = cfg.getDouble("nuke.radius", 50.0);
        float power = (float) cfg.getDouble("nuke.power", 4.0);

        world.playSound(target, Sound.ENTITY_ENDER_DRAGON_GROWL, 2f, 0.5f);
        world.playSound(target, Sound.ENTITY_WITHER_SPAWN, 1f, 1.2f);

        double startY = Math.min(world.getMaxHeight() - 10, target.getY() + 40.0);
        for (int i = 0; i < count; i++) {
            double r = radius * Math.sqrt((i + 0.5) / count); // even spread over the disc
            double angle = i * GOLDEN_ANGLE;
            dropTnt(world,
                    target.getX() + r * Math.cos(angle),
                    startY,
                    target.getZ() + r * Math.sin(angle),
                    power, 0, LANDING_ONLY);
        }
    }

    /** Beam of TNT from world height to bedrock at high speed. Each TNT blows deeper than the last. */
    public void executeStab(Location target) {
        World world = target.getWorld();
        if (world == null) {
            return;
        }
        FileConfiguration cfg = plugin.getConfig();
        int count = Math.max(1, cfg.getInt("stab.count", 270));
        double speed = Math.max(0.5, cfg.getDouble("stab.speed", 6.0));
        double spread = cfg.getDouble("stab.spread", 1.5);
        float power = (float) cfg.getDouble("stab.power", 6.0);

        world.playSound(target, Sound.ENTITY_WARDEN_SONIC_BOOM, 2f, 1f);
        world.playSound(target, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1f, 2f);
        world.spawnParticle(Particle.CRIT, target, 200, 0.5, 8.0, 0.5, 0.2);

        double startY = world.getMaxHeight() - 1;
        double top = world.getHighestBlockYAt(target.getBlockX(), target.getBlockZ()) + 1;
        double bottom = world.getMinHeight();

        for (int i = 0; i < count; i++) {
            double frac = count == 1 ? 0 : i / (double) (count - 1);
            double detonateY = top - (top - bottom) * frac;
            double r = spread * Math.sqrt((i + 0.5) / count);
            double angle = i * GOLDEN_ANGLE;
            dropTnt(world,
                    target.getX() + r * Math.cos(angle),
                    startY,
                    target.getZ() + r * Math.sin(angle),
                    power, speed, detonateY);
        }
    }
}
