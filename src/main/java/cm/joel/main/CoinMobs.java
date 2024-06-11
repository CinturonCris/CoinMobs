package cm.joel.main;

import cm.joel.command.CoinMobsCommand;
import cm.joel.placeholder.CoinMobsPlaceholder;
import cm.joel.utils.HeadConfiguration;
import cm.joel.utils.HeadUtils;
import cm.joel.utils.Hologram;
import com.earth2me.essentials.libs.bstats.bukkit.Metrics;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
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
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Logger;

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
        dropHeadsEnabled = getConfig().getBoolean("PlayerHeadDrop.enabled", true);

        loadHeadConfigurations();

        getLogger().info("----------------------------------------------");
        getLogger().info("+==================+");
        getLogger().info("|     CoinMobs     |");
        getLogger().info("+==================+");
        getLogger().info("----------------------------------------------");
        getLogger().info("Version: v3.0");
        getLogger().info("Author: CinturonCris");
        getLogger().info("This is the latest version!");
        if (setupPlaceholderAPI()) {
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

            if (dropHeadsEnabled) {
                try {
                    dropHead(player.getLocation(), entityType, event.getEntity(), getLogger());
                } catch (Exception e) {
                    getLogger().severe("Error dropping head: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    private void dropHead(Location location, EntityType entityType, Entity entity, Logger logger) {
        double dropProbability = 0.0;

        if (entityType == EntityType.PLAYER) {
            dropProbability = config.getDouble("PlayerHeadDrop.PLAYER.probability", 0) / 100.0;
        } else {
            dropProbability = config.getDouble("dropheads.entities." + entityType + ".probability", 0.0) / 100.0;
        }

        Random random = new Random();
        double randomValue = random.nextDouble();

        if (randomValue <= dropProbability) {
            ItemStack head = null;

            if (entityType == EntityType.PLAYER) {
                head = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
                if (skullMeta != null) {
                    skullMeta.setOwningPlayer((Player) entity);
                    head.setItemMeta(skullMeta);
                }
            } else {
                head = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
                if (skullMeta != null) {
                    String entityName = getHeadName(entityType);
                    skullMeta.setDisplayName(entityName);

                    String texture = config.getString("dropheads.entities." + entityType + ".texture", "");
                    GameProfile profile = new GameProfile(UUID.randomUUID(), entityName);
                    profile.getProperties().put("textures", new Property("textures", texture));

                    try {
                        Field profileField = skullMeta.getClass().getDeclaredField("profile");
                        profileField.setAccessible(true);
                        profileField.set(skullMeta, profile);
                    } catch (NoSuchFieldException | IllegalAccessException e) {
                        e.printStackTrace();
                    }
                    head.setItemMeta(skullMeta);
                }
            }

            if (head != null && !head.getType().equals(Material.AIR)) {
                if (entityType != EntityType.PLAYER && entity instanceof Player && !((Player) entity).hasPermission("mobcoins.head.drop")) {
                    return;
                }
                location.getWorld().dropItem(location, head);
            } else {
                logger.info("Failed to create head item.");
            }
        } else {
            logger.info("Head not dropped, probability check failed.");
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
        if (headNamesConfig == null) {
            loadHeadNamesConfig();
        }
        return headNamesConfig;
    }

    public void loadHeadConfigurations() {
        ConfigurationSection dropHeadsSection = config.getConfigurationSection("dropheads");
        if (dropHeadsSection != null && dropHeadsSection.getBoolean("enabled", false)) {
            ConfigurationSection entitiesSection = dropHeadsSection.getConfigurationSection("entities");
            if (entitiesSection != null) {
                for (String entityTypeName : entitiesSection.getKeys(false)) {
                    try {
                        EntityType entityType = EntityType.valueOf(entityTypeName.toUpperCase());
                        ConfigurationSection entitySection = entitiesSection.getConfigurationSection(entityTypeName);
                        if (entitySection != null) {
                            double probability = entitySection.getDouble("probability", 0.0) / 100.0;
                            String headName = getHeadName(entityType);
                            if (headName != null) {
                                String headTexture = entitySection.getString("texture", "");
                                HeadConfiguration headConfig = new HeadConfiguration(probability, headTexture, headName);
                                HeadUtils.setHeadConfiguration(entityType, headConfig);
                            } else {
                                getLogger().warning("Skipping head configuration for entity type: " + entityType);
                            }
                        }
                    } catch (IllegalArgumentException e) {
                        getLogger().warning("Invalid entity type in configuration: " + entityTypeName);
                    }
                }
            }
        } else {
            getLogger().warning("dropheads configuration not found or disabled!");
        }
    }

    public String getHeadName(EntityType entityType) {
        if (headNamesConfig != null && headNamesConfig.contains("headnames." + entityType.toString())) {
            return ChatColor.translateAlternateColorCodes('&', headNamesConfig.getString("headnames." + entityType.toString()));
        } else {
            getLogger().warning("Head name not found for entity type: " + entityType);
            return entityType.toString();
        }
    }
}