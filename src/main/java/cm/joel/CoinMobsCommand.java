package cm.joel;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;

public class CoinMobsCommand implements CommandExecutor {
    private final CoinMobs plugin;
    private final FileConfiguration headNamesConfig;

    public CoinMobsCommand(CoinMobs plugin) {
        this.plugin = plugin;
        this.headNamesConfig = loadHeadNamesConfig();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (player.hasPermission("mobcoins.reload")) {
                plugin.reloadConfig();
                plugin.loadMessagesConfig();
                plugin.loadHeadConfigurations();
                plugin.loadHeadNamesConfig();
                String reloadMessage = plugin.getMessagesConfig().getString("reloadMessage");
                player.sendMessage(ColorUtils.translateColors(reloadMessage));
            } else {
                String noPermissionMessage = plugin.getMessagesConfig().getString("noPermissionMessage");
                player.sendMessage(ColorUtils.translateColors(noPermissionMessage));
            }
        } else {
            String noPermissionMessage = plugin.getMessagesConfig().getString("noPermissionMessage");
            sender.sendMessage(ColorUtils.translateColors(noPermissionMessage));
        }
        return true;
    }

    public FileConfiguration loadHeadNamesConfig() {
        File headNamesFile = new File(plugin.getDataFolder(), "headnames.yml");
        if (!headNamesFile.exists()) {
            plugin.saveResource("headnames.yml", false);
        }
        return YamlConfiguration.loadConfiguration(headNamesFile);
    }
}

