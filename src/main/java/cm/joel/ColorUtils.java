package cm.joel;

import org.bukkit.ChatColor;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorUtils {
    private static final Map<ChatColor, Color> CHAT_COLOR_TO_COLOR = new HashMap<>();

    static {
        CHAT_COLOR_TO_COLOR.put(ChatColor.BLACK, Color.BLACK);
        CHAT_COLOR_TO_COLOR.put(ChatColor.DARK_BLUE, new Color(0, 0, 170));
        CHAT_COLOR_TO_COLOR.put(ChatColor.DARK_GREEN, new Color(0, 170, 0));
        CHAT_COLOR_TO_COLOR.put(ChatColor.DARK_AQUA, new Color(0, 170, 170));
        CHAT_COLOR_TO_COLOR.put(ChatColor.DARK_RED, new Color(170, 0, 0));
        CHAT_COLOR_TO_COLOR.put(ChatColor.DARK_PURPLE, new Color(170, 0, 170));
        CHAT_COLOR_TO_COLOR.put(ChatColor.GOLD, new Color(255, 170, 0));
        CHAT_COLOR_TO_COLOR.put(ChatColor.GRAY, new Color(170, 170, 170));
        CHAT_COLOR_TO_COLOR.put(ChatColor.DARK_GRAY, new Color(85, 85, 85));
        CHAT_COLOR_TO_COLOR.put(ChatColor.BLUE, new Color(85, 85, 255));
        CHAT_COLOR_TO_COLOR.put(ChatColor.GREEN, new Color(85, 255, 85));
        CHAT_COLOR_TO_COLOR.put(ChatColor.AQUA, new Color(85, 255, 255));
        CHAT_COLOR_TO_COLOR.put(ChatColor.RED, new Color(255, 85, 85));
        CHAT_COLOR_TO_COLOR.put(ChatColor.LIGHT_PURPLE, new Color(255, 85, 255));
        CHAT_COLOR_TO_COLOR.put(ChatColor.YELLOW, new Color(255, 255, 85));
        CHAT_COLOR_TO_COLOR.put(ChatColor.WHITE, Color.WHITE);
    }

    public static String translateColors(String message) {
        if (message == null) {
            return null;
        }

        message = message.replaceAll("§x(§r\\w{2})(§g\\w{2})(§b\\w{2})", "§x§$1§$2§$3");

        message = ChatColor.translateAlternateColorCodes('&', message);
        return message;
    }
}
