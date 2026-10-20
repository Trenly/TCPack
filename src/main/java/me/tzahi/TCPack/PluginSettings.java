package me.tzahi.TCPack;

import java.io.File;
import java.io.IOException;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

final class PluginSettings {
    private final boolean ptimeEnabled;
    private final boolean flightEnabled;
    private final boolean compressEnabled;

    private PluginSettings(boolean ptimeEnabled, boolean flightEnabled, boolean compressEnabled) {
        this.ptimeEnabled = ptimeEnabled;
        this.flightEnabled = flightEnabled;
        this.compressEnabled = compressEnabled;
    }

    static PluginSettings load(File file) throws IOException, InvalidConfigurationException {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.load(file);
        return new PluginSettings(flag(configuration, "EnablePtime"),
                flag(configuration, "EnableFlight"), flag(configuration, "EnableCompress"));
    }

    private static boolean flag(YamlConfiguration configuration, String name)
            throws InvalidConfigurationException {
        Object value = configuration.get(name);
        if (!configuration.contains(name)) {
            return true;
        }
        if (!(value instanceof Boolean)) {
            throw new InvalidConfigurationException(name + " must be true or false.");
        }
        return (Boolean) value;
    }

    boolean isPtimeEnabled() {
        return ptimeEnabled;
    }

    boolean isFlightEnabled() {
        return flightEnabled;
    }

    boolean isCompressEnabled() {
        return compressEnabled;
    }
}
