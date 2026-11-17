package com.orbital.strike.listener;

import com.orbital.strike.item.CustomItemManager;
import com.orbital.strike.strike.StrikeExecutor;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.RayTraceResult;

public class StrikeListener implements Listener {

    private final CustomItemManager itemManager;
    private final StrikeExecutor strikeExecutor;

    public StrikeListener(Plugin plugin, CustomItemManager itemManager) {
        this.itemManager = itemManager;
        this.strikeExecutor = new StrikeExecutor(plugin);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!event.getAction().isRightClick()) {
            return;
        }
        EquipmentSlot hand = event.getHand();
        if (hand != EquipmentSlot.HAND && hand != EquipmentSlot.OFF_HAND) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItem(hand);
        String type = itemManager.getStrikeType(item);
        if (type == null) {
            return;
        }

        event.setCancelled(true);
        item.setAmount(item.getAmount() - 1);
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 1f);

        Location eye = player.getEyeLocation();
        RayTraceResult hit = player.getWorld().rayTraceBlocks(
                eye, eye.getDirection(), 120.0, FluidCollisionMode.NEVER, true);

        Location targetLoc;
        if (hit != null && hit.getHitBlock() != null) {
            targetLoc = hit.getHitBlock().getLocation().add(0.5, 1, 0.5);
        } else {
            targetLoc = eye.clone().add(eye.getDirection().multiply(60)).getBlock().getLocation().add(0.5, 1, 0.5);
        }

        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 1.5f);

        if (type.equals(CustomItemManager.TYPE_NUKE)) {
            strikeExecutor.executeNuke(targetLoc);
        } else if (type.equals(CustomItemManager.TYPE_STAB)) {
            strikeExecutor.executeStab(targetLoc);
        }
    }
}
