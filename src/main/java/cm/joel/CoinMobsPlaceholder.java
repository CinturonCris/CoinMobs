package cm.joel;

import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;

public class CoinMobsPlaceholder extends PlaceholderExpansion {

    private final CoinMobs plugin;
    private double moneyEarned = 0.0;

    public CoinMobsPlaceholder(CoinMobs plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "CoinMobs";
    }

    @Override
    public String getAuthor() {
        return "CinturonCris";
    }

    @Override
    public String getVersion() {
        return "2.0-ALPHA-1.2";
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (player == null) {
            return "";
        }

        if (identifier.equals("coins")) {
            if (moneyEarned > 0) {
                String moneyMessage = ChatColor.GREEN + "+ " + String.format("%.2f$", moneyEarned);
                moneyEarned = 0.0;
                return moneyMessage;
            } else {
                return "";
            }
        }

        return null;
    }

    public void updateMoneyEarned(double amount) {
        moneyEarned += amount;
        Bukkit.getScheduler().runTaskLater(plugin, () -> moneyEarned = 0.0, 60L);
    }
}