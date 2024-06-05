package cm.joel;

import com.earth2me.essentials.libs.bstats.bukkit.Metrics;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class CoinMobs extends JavaPlugin implements Listener {
    private Economy economy;
    private boolean dropHeadsEnabled;
    private double dropHeadsProbability;
    private Map<EntityType, String> headNames = new HashMap<>();
    private CoinMobsPlaceholder coinMobsPlaceholder;
    private FileConfiguration config;
    private FileConfiguration messagesConfig;
    private FileConfiguration headNamesConfig;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = getConfig();
        loadMessagesConfig();
        loadHeadNamesConfig();

        getServer().getPluginManager().registerEvents(this, this);
        saveResource("messages.yml", false);

        new CoinMobsPlaceholder(this).register();
        if (!setupEconomy()) {
            getLogger().severe("Vault / EssentialsX is needed, please install the dependencies, disabling...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            coinMobsPlaceholder = new CoinMobsPlaceholder(this);
            coinMobsPlaceholder.register();
        } else {
            getLogger().warning("PlaceholderAPI not found...");
        }

        int pluginId = 20394;
        Metrics metrics = new Metrics(this, pluginId);

        getCommand("coinmobs").setExecutor(new CoinMobsCommand(this));

        dropHeadsProbability = getConfig().getDouble("PlayerHeadDrop.probability", 0) / 100;
        dropHeadsEnabled = getConfig().getBoolean("dropheads.enabled", true);

        loadHeadConfigurations();

        getLogger().info("----------------------------------------------");
        getLogger().info("+==================+");
        getLogger().info("|     CoinMobs     |");
        getLogger().info("+==================+");
        getLogger().info("----------------------------------------------");
        getLogger().info("Version: 2.0-ALPHA-1.2");
        getLogger().info("Author: CinturonCris");
        getLogger().info("This is the latest version!");
        if (setupPlaceholderAPI()) {
            getLogger().info("PlaceholderAPI found and hooked into CoinMobs.");
        } else {
            getLogger().warning("PlaceholderAPI not found! PlaceholderAPI is needed for some features to work properly.");
        }

        if (setupVault()) {
            getLogger().info("Vault found and hooked into CoinMobs.");
        } else {
            getLogger().warning("Vault not found! Vault is needed for economy integration.");
        }

        if (setupEssentials()) {
            getLogger().info("Essentials found and hooked into CoinMobs.");
        } else {
            getLogger().warning("Essentials not found! Essentials is needed for some features to work properly.");
        }
        getLogger().info("----------------------------------------------");
    }

    private boolean setupVault() {
        return getServer().getPluginManager().getPlugin("Vault") != null;
    }

    private boolean setupEssentials() {
        return getServer().getPluginManager().getPlugin("Essentials") != null;
    }

    private boolean setupPlaceholderAPI() {
        return getServer().getPluginManager().getPlugin("PlaceholderAPI") != null;
    }

    public void loadMessagesConfig() {
        File messagesFile = new File(getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public void loadHeadNamesConfig() {
        File headNamesFile = new File(getDataFolder(), "headnames.yml");
        if (!headNamesFile.exists()) {
            saveResource("headnames.yml", false);
        }
        headNamesConfig = YamlConfiguration.loadConfiguration(headNamesFile);
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getKiller() instanceof Player) {
            Player player = (Player) event.getEntity().getKiller();
            EntityType entityType = event.getEntityType();

            String mobKillMessage = getMessagesConfig().getString("mobKillMessage");

            if (getConfig().contains("mobs." + entityType.toString())) {
                String moneyString = getConfig().getString("mobs." + entityType.toString());
                try {
                    double money = Double.parseDouble(moneyString);
                    if (money > 0) {
                        economy.depositPlayer(player, money);
                        if (coinMobsPlaceholder != null) {
                            coinMobsPlaceholder.updateMoneyEarned(money);
                        }
                        mobKillMessage = mobKillMessage.replace("%money%", String.format("%.2f", money));
                        mobKillMessage = mobKillMessage.replace("%mob%", entityType.toString());
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', mobKillMessage));
                        Hologram.showHologram(player, money);
                    }
                } catch (NumberFormatException e) {
                    getLogger().warning(getMessagesConfig().getString("MobconversionError").replace("%mob%", entityType.toString()));
                }
            } else {
                getLogger().warning(getMessagesConfig().getString("NoMoneyConfigDefined").replace("%mob%", entityType.toString()));
            }

            if (entityType == EntityType.PLAYER) {
                Player victim = (Player) event.getEntity();
                dropHead(player.getLocation(), entityType, victim);
            } else {
                dropHead(player.getLocation(), entityType, player);
            }
        }
    }

    private ItemStack getPlayerHead(Player player) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD, 1);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(player);
            head.setItemMeta(meta);
        }
        return head;
    }

    private void dropHead(Location location, EntityType entityType, Player victim) {
        double dropProbability = dropHeadsProbability;
        if (entityType != EntityType.PLAYER) {
            dropProbability = HeadUtils.getDropProbability(entityType);
        }

        Random random = new Random();
        int randomValue = random.nextInt(100) + 1;

        getLogger().info("Checking head drop: entityType=" + entityType + ", probability=" + (dropProbability * 100) + "%, randomValue=" + randomValue);

        if (randomValue <= (dropProbability * 100)) {
            ItemStack head;
            if (entityType == EntityType.PLAYER) {
                head = getPlayerHead(victim);
            } else {
                head = HeadUtils.getHead(entityType);
            }

            if (!head.getType().equals(Material.AIR)) {
                if (entityType != EntityType.PLAYER && !victim.hasPermission("mobcoins.head.drop")) {
                    return;
                }
                location.getWorld().dropItem(location, head);
                getLogger().info("Dropped head at " + location + " for entityType=" + entityType);
            } else {
                getLogger().info("Head type is AIR, nothing to drop.");
            }
        } else {
            getLogger().info("Head not dropped, probability check failed.");
        }
    }

    private ItemStack createPlayerHead(Player player) {
        ItemStack headItem = new ItemStack(Material.PLAYER_HEAD, 1);
        SkullMeta meta = (SkullMeta) headItem.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(player);
            meta.setDisplayName(ChatColor.GOLD + player.getName() + " Head");
            headItem.setItemMeta(meta);
        }
        return headItem;
    }

    void loadHeadConfigurations() {
        ConfigurationSection playerHeadDropSection = config.getConfigurationSection("PlayerHeadDrop");
        if (playerHeadDropSection != null && playerHeadDropSection.getBoolean("enabled", false)) {
            ConfigurationSection playerSection = playerHeadDropSection.getConfigurationSection("PLAYER");
            if (playerSection != null) {
                double playerProbability = playerSection.getDouble("probability", 0) / 100;
                getLogger().info("Player Head Probability: " + playerProbability);
                HeadUtils.setHeadConfiguration(EntityType.PLAYER, playerProbability, "", "Player Head");
            }
        } else {
            getLogger().warning("Player head drop configuration not found or disabled!");
        }

        ConfigurationSection dropHeadsSection = config.getConfigurationSection("dropheads");
        if (dropHeadsSection != null && dropHeadsSection.getBoolean("enabled", true)) {
            for (String entityTypeName : dropHeadsSection.getKeys(false)) {
                EntityType entityType = EntityType.valueOf(entityTypeName);
                ConfigurationSection entitySection = dropHeadsSection.getConfigurationSection(entityTypeName);
                if (entitySection != null) {
                    double probability = entitySection.getDouble("probability", 0) / 100;
                    String headName = entitySection.getString("name", entityTypeName + " Head");
                    String headTexture = headNamesConfig.getString("headTextures." + entityTypeName, "");
                    HeadUtils.setHeadConfiguration(entityType, probability, headTexture, headName);
                }
            }
        } else {
            getLogger().warning("dropheads configuration not found or disabled!");
        }
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    public FileConfiguration getMessagesConfig() {
        return messagesConfig;
    }

    public FileConfiguration getHeadNamesConfig() {
        return headNamesConfig;
    }
}
