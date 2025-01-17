package eisenwave.elytra.messages;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.InputStreamReader;
import java.util.HashMap;

/**
 * A class to store messages and load them from files, internal and external.
 *
 * @author UberPilot
 * @since 1.0.0
 */
public class Messages {

    private final HashMap<String, String> messages;

    /**
     * Creates a set of messages from a file, <code>defaultMessages.yml</code> included in a jar.
     *
     * @param instance The instance of JavaPlugin.
     * @param config   The config to load messages from.
     */
    public Messages(final JavaPlugin instance, final YamlConfiguration config) {
        this.messages = new HashMap<>();
        final YamlConfiguration defaults =
                YamlConfiguration.loadConfiguration(
                        new InputStreamReader(instance.getResource("messages.yml")));
        for (final String key : config.getKeys(false)) {
            this.messages.put(key, ChatColor.translateAlternateColorCodes('&', config.getString(key)));
        }
        for (final String key : defaults.getKeys(false)) {
            this.messages.putIfAbsent(
                    key, ChatColor.translateAlternateColorCodes('&', defaults.getString(key)));
        }
    }

    /**
     * Gets the message with the given key.
     *
     * @param key The key of the message to get.
     * @return The message, if it exists, or <code>null</code> otherwise.
     */
    public String getMessage(final String key) {
        return this.messages.get(key);
    }
}
