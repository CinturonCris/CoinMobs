package cm.joel.utils;

import cm.joel.main.CoinMobs;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import org.bukkit.ChatColor;

public class Hologram {
    public static void showHologram(Player player, double money) {
        CoinMobs plugin = CoinMobs.getPlugin(CoinMobs.class);
        boolean hologramEnabled = plugin.getConfig().getBoolean("hologram-enabled", true);
        String hologramColor = plugin.getConfig().getString("hologram-color", "&a");

        if (!hologramEnabled) {
            return;
        }

        String text = hologramColor + "+ $" + String.format("%.2f", money);
        Location playerLocation = player.getLocation();
        Location hologramLocation = playerLocation.clone().add(player.getLocation().getDirection().multiply(2));
        ArmorStand hologram = (ArmorStand) player.getWorld().spawnEntity(hologramLocation, EntityType.ARMOR_STAND);

        hologram.setGravity(false);
        hologram.setCustomName(ChatColor.translateAlternateColorCodes('&', text));
        hologram.setCustomNameVisible(true);
        hologram.setVisible(false);

        new BukkitRunnable() {
            @Override
            public void run() {
                hologram.remove();
            }
        }.runTaskLater(plugin, 45L);
    }
}
