package eisenwave.elytra;

import eisenwave.elytra.command.ElytraModeCommand;
import eisenwave.elytra.command.ElytraToggleCommand;
import eisenwave.elytra.command.ReloadCommand;
import eisenwave.elytra.data.PlayerPreferences;
import eisenwave.elytra.data.SuperElytraConfig;
import eisenwave.elytra.errors.ErrorLogger;
import eisenwave.elytra.listeners.*;
import eisenwave.elytra.messages.Messageable;
import eisenwave.elytra.messages.Messages;
import eisenwave.elytra.messages.Messenger;
import io.sentry.Hub;
import io.sentry.SentryOptions;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.FileUtil;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.ArrayList;

public class SuperElytraPlugin extends JavaPlugin implements Messageable {

    private SuperElytraConfig config;
    private Messenger messenger;
    private Messages messages;
    private CooldownManager launchCooldownManager;
    private Hub hub;
    private ErrorLogger errorLogger;
    private PlayerManager playerManager;
    private TickListener tickListener;

    @Override
    public Messenger getMessenger() {
        return this.messenger;
    }

    public CooldownManager getLaunchCooldownManager() {
        return launchCooldownManager;
    }

    public SuperElytraConfig config() {
        return this.config;
    }

    public void reload() {
        if (getConfig().contains("config")) {
            reloadConfig();
            Object tmpConfig = getConfig().get("config");
            if (tmpConfig == null) {
                Throwable throwable = new IllegalStateException("Config loaded is null; please check your syntax or delete your config.");
                this.hub.captureException(throwable);
                this.printHelpMessage();
                throwable.printStackTrace();
                this.setEnabled(false);
            }
            if (!(tmpConfig instanceof SuperElytraConfig)) {
                Throwable throwable = new IllegalStateException("Config is not assignable to SuperElytraConfig; please check your syntax or delete your config.");
                this.hub.captureException(throwable);
                this.printHelpMessage();
                throwable.printStackTrace();
                this.setEnabled(false);
            }
            else {
                config = (SuperElytraConfig) tmpConfig;
            }
        } else {
            try {
                final File f = new File(getDataFolder(), "config.yml");
                final File backup = new File(getDataFolder(), "config.old.yml");
                FileUtil.copy(f, backup);
                config = new SuperElytraConfig(
                        getConfig().getInt("chargeup_time", 60),
                        getConfig().getDouble("speed_multiplier", 1.0d),
                        getConfig().getDouble("launch_multiplier", 1.0d),
                        getConfig().getBoolean("default", true),
                        Sound.valueOf(getConfig().getString("charge-sound", "FUSE")),
                        Sound.valueOf(getConfig().getString("ready-sound", "BAT_TAKEOFF")),
                        Sound.valueOf(getConfig().getString("launch-sound", "ENDERDRAGON_WINGS")),
                        600,
                        50,
                        new ArrayList<>(),
                        true,
                        false, 40, 0.0, -90, false);
                getConfig().set("chargeup_time", null);
                getConfig().set("speed_multiplier", null);
                getConfig().set("launch_multiplier", null);
                getConfig().set("default", null);
                getConfig().set("charge-sound", null);
                getConfig().set("ready-sound", null);
                getConfig().set("launch-sound", null);
                getConfig().set("config", config);
                getConfig().save(f);
            } catch (final Exception ex) {
                this.hub.captureException(ex);
                ex.printStackTrace();
                this.printHelpMessage();
                this.setEnabled(false);
                return;
            }
        }
        launchCooldownManager = new CooldownManager(config.cooldown);
        this.loadMessages();
    }

    @Override
    public void onDisable() {
        autosave();
    }

    public void autosave() {
        for (final SuperElytraPlayer player : playerManager) {
            if (player == null || player.getPlayer() == null) {
                continue;
            }
            player.preferences.save(player.getPlayer().getUniqueId());
        }
    }

    @Override
    public void onLoad() {
        ConfigurationSerialization.registerClass(PlayerPreferences.class);
        ConfigurationSerialization.registerClass(SuperElytraConfig.class);
    }

    @Override
    public void onEnable() {
        SentryOptions options = new SentryOptions();
        options.setDsn("https://d48172d870b54d749614e2711d6f377d@o244958.ingest.sentry.io/6339955");
        options.setTracesSampleRate(1.0);
        hub = new Hub(options);
        hub.setExtra("plugin_version", this.getDescription().getVersion());
        hub.setExtra("bukkit_version", this.getServer().getBukkitVersion());
        hub.setExtra("server_version", this.getServer().getVersion());

        this.errorLogger = new ErrorLogger(hub);
        this.playerManager = new PlayerManager();

        //noinspection unused
        final MetricsLite metrics = new MetricsLite(this, 7488);

        this.saveDefaultConfig();
        this.reload();

        this.initListeners();
        this.initCommands();
    }

    private void loadMessages() {
        if (!this.getDataFolder().exists()) {
            //noinspection ResultOfMethodCallIgnored
            this.getDataFolder().mkdirs();
        }
        final File messagesFile = new File(this.getDataFolder(), "messages.yml");

        if (!messagesFile.exists()) {
            this.saveResource("messages.yml", false);
        }
        final Messages messages = new Messages(this, YamlConfiguration.loadConfiguration(messagesFile));
        this.messenger = new Messenger(this, messages);
    }

    private void initListeners() {
        this.tickListener = new TickListener(this, errorLogger, playerManager);

        this.getServer().getPluginManager().registerEvents(new JoinListener(this, errorLogger, playerManager), this);
        this.getServer().getPluginManager().registerEvents(new QuitListener(this, errorLogger, playerManager), this);
        this.getServer().getPluginManager().registerEvents(new SneakListener(this, errorLogger, playerManager), this);
        this.getServer().getPluginManager().registerEvents(new MoveListener(this, errorLogger, playerManager), this);
        this.getServer().getPluginManager().registerEvents(new InteractListener(this, errorLogger, playerManager), this);

        this.getServer().getScheduler().runTaskTimer(this, () -> this.tickListener.onTick(), 0, 1);
        this.getServer().getScheduler().scheduleSyncRepeatingTask(this, this::autosave, 0, 20L * this.config.autosaveInterval);
    }

    private void initCommands() {
        this.getCommandOrThrow("elytramode").setExecutor(new ElytraModeCommand(this, playerManager));
        this.getCommandOrThrow("elytrareload").setExecutor(new ReloadCommand(this));
        this.getCommandOrThrow("elytraprefs").setExecutor(new ElytraToggleCommand(this, playerManager));
    }

    private @Nonnull PluginCommand getCommandOrThrow(String name) {
        PluginCommand command = this.getCommand(name);
        if (command == null) {
            throw new IllegalStateException("Failed to get command " + name + ". Is it declared in the plugin.yml?");
        }
        return command;
    }

    private void printHelpMessage() {
        getLogger().warning("");
        getLogger().warning("");
        getLogger().warning("An error has occurred; get help at https://discord.gg/zUbNX9t.");
        getLogger().warning("");
        getLogger().warning("");
    }
}
