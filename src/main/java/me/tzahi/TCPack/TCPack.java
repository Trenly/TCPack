package me.tzahi.TCPack;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import me.tzahi.TCPack.command.CompressCommand;
import me.tzahi.TCPack.command.FlyCommand;
import me.tzahi.TCPack.command.PtimeCommand;
import me.tzahi.TCPack.command.TCPackCommand;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * TCPack by tzahi12345 (Tzahi) and TacoID; 2.0 refactor by Trenly.
 * Adapted under CC BY-SA 3.0 Unported; see LICENSE and NOTICE.
 */
public final class TCPack extends JavaPlugin {
    @Override
    public void onEnable() {
        saveDefaultConfig();
        final PluginSettings settings;
        try {
            settings = PluginSettings.load(new File(getDataFolder(), "config.yml"));
        } catch (IOException | InvalidConfigurationException exception) {
            getLogger().log(Level.SEVERE, "Cannot load config.yml; TCPack is disabled.", exception);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        register("ptime", new PtimeCommand(settings.isPtimeEnabled()));
        register("fly", new FlyCommand(settings.isFlightEnabled(), getServer()));
        register("compress", new CompressCommand(settings.isCompressEnabled()));
        register("tcpack", new TCPackCommand());
    }

    private void register(String name, CommandExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            throw new IllegalStateException("Missing command in plugin.yml: " + name);
        }
        command.setExecutor(executor);
    }
}
