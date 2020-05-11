package eisenwave.elytra;

import eisenwave.elytra.command.ElytraModeCommand;
import eisenwave.elytra.command.ElytraToggleCommand;
import eisenwave.elytra.command.ReloadCommand;
import eisenwave.elytra.data.PlayerPreferences;
import eisenwave.elytra.data.SuperElytraConfig;
import eisenwave.elytra.messages.Messageable;
import eisenwave.elytra.messages.Messages;
import eisenwave.elytra.messages.Messenger;
import java.io.File;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class SuperElytraPlugin extends JavaPlugin implements Listener, Messageable {
    
    private SuperElytraListener eventHandler;
    private SuperElytraConfig config;
    private Messenger messenger;
    private Messages messages;

    public SuperElytraConfig config() {
        return config;
    }

    public void reload() {
        if(this.getConfig().contains("config")) {
            this.config = (SuperElytraConfig) this.getConfig().get("config");
        }
        this.config = new SuperElytraConfig(
            this.getConfig().getInt("chargeup_time", 60),
            this.getConfig().getDouble("speed_multiplier", 1.0d),
            this.getConfig().getDouble("launch_multiplier", 1.0d),
            this.getConfig().getBoolean("default", true),
            Sound.valueOf(this.getConfig().getString("charge-sound", "FUSE")),
            Sound.valueOf(this.getConfig().getString("ready-sound", "BAT_TAKEOFF")),
            Sound.valueOf(this.getConfig().getString("launch-sound", "ENDERDRAGON_WINGS")),
            600
        );
        this.getConfig().set("chargeup_time", null);
        this.getConfig().set("speed_multiplier", null);
        this.getConfig().set("launch_multiplier", null);
        this.getConfig().set("default", null);
        this.getConfig().set("charge-sound", null);
        this.getConfig().set("ready-sound", null);
        this.getConfig().set("launch-sound", null);
        this.getConfig().set("config", this.config);
        try{
            this.getConfig().save(this.getDataFolder() + File.separator + "config.yml");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        loadMessages();
    }

    public void autosave() {
        for(SuperElytraPlayer player : PlayerManager.getInstance()) {
            player.preferences.save(player.getPlayer());
        }
    }

    @Override
    public void onLoad() {
        ConfigurationSerialization.registerClass(PlayerPreferences.class);
        ConfigurationSerialization.registerClass(SuperElytraConfig.class);
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reload();

        initListeners();
        initCommands();
    }

    private void loadMessages() {
        if (!getDataFolder().exists()) {
            //noinspection ResultOfMethodCallIgnored
            getDataFolder().mkdirs();
        }
        File messagesFile = new File(getDataFolder(), "messages.yml");

        if (!messagesFile.exists()) {
            saveResource("messages.yml", false);
        }
        Messages messages = new Messages(this, YamlConfiguration.loadConfiguration(messagesFile));
        messenger = new Messenger(this, messages);
    }

    private void initListeners() {
        this.eventHandler = new SuperElytraListener(this);
    
        getServer().getPluginManager().registerEvents(eventHandler, this);
    
        getServer().getScheduler().runTaskTimer(this, () -> eventHandler.onTick(), 0, 1);

        getServer().getScheduler().scheduleSyncRepeatingTask(this, this::autosave, 0, 20 * config.autosaveInterval);
    }
    
    private void initCommands() {
        getCommand("elytramode").setExecutor(new ElytraModeCommand(this));
        getCommand("elytrareload").setExecutor(new ReloadCommand(this));
        getCommand("elytraprefs").setExecutor(new ElytraToggleCommand(this));
    }
    
    public SuperElytraListener getEventHandler() {
        return eventHandler;
    }

    @Override
    public Messenger getMessenger() {
        return messenger;
    }
}
