package com.orbital.strike.item;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.List;

public class CustomItemManager {

    public static final String TYPE_NUKE = "nuke";
    public static final String TYPE_STAB = "stab";

    private final NamespacedKey strikeKey;

    public CustomItemManager(Plugin plugin) {
        this.strikeKey = new NamespacedKey(plugin, "orbital_type");
    }

    public ItemStack createNukeRod() {
        return build(TYPE_NUKE, "Orbital Strike: Nuke",
                "Right-click to designate a target area.",
                "Deploys a devastating wide-area TNT barrage.",
                "Consumes on use.",
                NamedTextColor.RED, NamedTextColor.RED, NamedTextColor.DARK_RED);
    }

    public ItemStack createStabRod() {
        return build(TYPE_STAB, "Orbital Strike: Stab",
                "Right-click to designate a target coordinate.",
                "Fires a concentrated vertical piercing laser beam.",
                "Consumes on use.",
                NamedTextColor.AQUA, NamedTextColor.AQUA, NamedTextColor.DARK_AQUA);
    }

    private ItemStack build(String type, String name, String l1, String l2, String l3,
                            TextColor nameColor, TextColor l2Color, TextColor l3Color) {
        ItemStack rod = new ItemStack(Material.FISHING_ROD);
        ItemMeta meta = rod.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name, nameColor, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text(l1, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text(l2, l2Color).decoration(TextDecoration.ITALIC, false),
                    Component.text(l3, l3Color).decoration(TextDecoration.ITALIC, false)));
            meta.getPersistentDataContainer().set(strikeKey, PersistentDataType.STRING, type);
            meta.setUnbreakable(false);
            if (meta instanceof Damageable damageable) {
                damageable.setDamage(Material.FISHING_ROD.getMaxDurability() - 1);
            }
            rod.setItemMeta(meta);
        }
        return rod;
    }

    public String getStrikeType(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer().get(strikeKey, PersistentDataType.STRING);
    }
}
