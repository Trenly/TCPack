package me.tzahi.TCPack;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.bukkit.configuration.InvalidConfigurationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PluginSettingsTest {
    @TempDir
    Path directory;

    @Test
    void missingFlagsDefaultToEnabled() throws Exception {
        PluginSettings settings = PluginSettings.load(config("# Existing configuration\n"));
        assertTrue(settings.isPtimeEnabled());
        assertTrue(settings.isFlightEnabled());
        assertTrue(settings.isCompressEnabled());
    }

    @Test
    void readsBothBooleanFlags() throws Exception {
        PluginSettings settings = PluginSettings.load(config(
                "EnablePtime: false\nEnableFlight: false\nEnableCompress: false\n"));
        assertFalse(settings.isPtimeEnabled());
        assertFalse(settings.isFlightEnabled());
        assertFalse(settings.isCompressEnabled());
    }

    @Test
    void missingFlightFlagStillDefaultsToEnabled() throws Exception {
        PluginSettings settings = PluginSettings.load(config("EnablePtime: false\n"));
        assertFalse(settings.isPtimeEnabled());
        assertTrue(settings.isFlightEnabled());
    }

    @Test
    void loadingDoesNotRewriteExistingOrObsoleteSettings() throws Exception {
        String original = "# Preserve me\nEnablePtime: true\nEnableFlight: false\n"
                + "EnableFake: true\nBanMessage: 'Original custom message'\n";
        File file = config(original);
        PluginSettings settings = PluginSettings.load(file);
        assertTrue(settings.isPtimeEnabled());
        assertFalse(settings.isFlightEnabled());
        assertTrue(original.equals(new String(Files.readAllBytes(file.toPath()),
                StandardCharsets.UTF_8)));
    }

    @Test
    void invalidFlagTypesFailExplicitly() throws Exception {
        for (String key : new String[] {"EnablePtime", "EnableFlight", "EnableCompress"}) {
            for (String value : new String[] {"'true'", "1", "[]", "{}"}) {
                File file = config(key + ": " + value + "\n");
                InvalidConfigurationException error = assertThrows(
                        InvalidConfigurationException.class, () -> PluginSettings.load(file));
                assertTrue(error.getMessage().contains(key));
            }
        }
    }

    @Test
    void malformedYamlFailsInsteadOfSilentlyLoadingDefaults() throws Exception {
        File file = config("EnablePtime: [broken\n");
        assertThrows(InvalidConfigurationException.class, () -> PluginSettings.load(file));
    }

    @Test
    void unreadableFileFailsInsteadOfSilentlyLoadingDefaults() {
        assertThrows(IOException.class,
                () -> PluginSettings.load(directory.resolve("missing.yml").toFile()));
    }

    private File config(String text) throws IOException {
        Path file = directory.resolve("config.yml");
        Files.write(file, text.getBytes(StandardCharsets.UTF_8));
        return file.toFile();
    }
}
