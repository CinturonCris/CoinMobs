package cm.joel.utils;

import cm.joel.utils.HeadConfiguration;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

public class HeadUtils {
    private static final Map<EntityType, HeadConfiguration> headConfigurations = new HashMap<>();

    public static void setHeadConfiguration(EntityType entityType, HeadConfiguration headConfig) {
        headConfigurations.put(entityType, headConfig);
    }

    public static HeadConfiguration getHeadConfiguration(EntityType entityType) {
        return headConfigurations.get(entityType);
    }

    public static ItemStack getHead(EntityType entityType, Logger logger) {
        if (entityType == EntityType.PLAYER) {
            return new ItemStack(Material.PLAYER_HEAD);
        } else {
            return createHead(entityType);
        }
    }

    private static ItemStack createHead(EntityType entityType) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD, 1);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            HeadConfiguration headConfig = getHeadConfiguration(entityType);
            if (headConfig != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', headConfig.getHeadName()));
                GameProfile profile = new GameProfile(UUID.randomUUID(), headConfig.getHeadName());
                profile.getProperties().put("textures", new Property("textures", headConfig.getTexture()));
                try {
                    Field profileField = meta.getClass().getDeclaredField("profile");
                    profileField.setAccessible(true);
                    profileField.set(meta, profile);
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            } else {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', entityType.name()));
            }
            head.setItemMeta(meta);
        }
        return head;
    }

    public static ItemStack getPlayerHead(Player player, Logger logger) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD, 1);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            String playerName = player.getName();
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', playerName));

            GameProfile profile = new GameProfile(player.getUniqueId(), playerName);
            Property textureProperty = getTextureProperty(player, logger);
            if (textureProperty != null) {
                profile.getProperties().put("textures", textureProperty);
            }

            try {
                Field profileField = meta.getClass().getDeclaredField("profile");
                profileField.setAccessible(true);
                profileField.set(meta, profile);
            } catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException e) {
                logger.warning("Error setting player head profile: " + e.getMessage());
                e.printStackTrace();
            }

            head.setItemMeta(meta);
        }
        return head;
    }

    private static Property getTextureProperty(Player player, Logger logger) {
        try {
            URL url = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + player.getUniqueId() + "?unsigned=false");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                InputStreamReader reader = new InputStreamReader(connection.getInputStream());
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                JsonObject textures = json.getAsJsonArray("properties").get(0).getAsJsonObject();
                String value = textures.get("value").getAsString();
                String signature = textures.get("signature").getAsString();
                return new Property("textures", value, signature);
            } else {
                logger.warning("Failed to retrieve texture property for player: " + player.getName() + ". Response code: " + responseCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.warning("Error while retrieving texture property for player: " + player.getName() + ". Error message: " + e.getMessage());
        }
        return null;
    }

}
