package me.tzahi.TCPack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginDescriptionFile;
import org.junit.jupiter.api.Test;

class PluginMetadataTest {
    @Test
    void descriptorLoadsWithLegacyApiAndRegistersOnlyRetainedCommands() throws Exception {
        PluginDescriptionFile descriptor;
        try (InputStream stream = getClass().getResourceAsStream("/plugin.yml")) {
            assertNotNull(stream);
            descriptor = new PluginDescriptionFile(stream);
        }
        assertEquals("TCPack", descriptor.getName());
        assertEquals("2.0.1", descriptor.getVersion());
        assertEquals("me.tzahi.TCPack.TCPack", descriptor.getMain());
        assertEquals(new HashSet<>(Arrays.asList("ptime", "fly")),
                descriptor.getCommands().keySet());
        assertTrue(descriptor.getAuthors().containsAll(Arrays.asList("Tzahi", "TacoID", "Trenly")));
        HashSet<String> expectedPermissions = new HashSet<>(Arrays.asList(
                "ptime.list", "ptime.day", "ptime.night", "ptime.morning", "ptime.dawn",
                "ptime.dusk", "ptime.reset", "ptime.info", "fly.list", "fly.toggle",
                "fly.enable", "fly.disable", "fly.toggle.other", "fly.enable.other",
                "fly.disable.other"));
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.options().pathSeparator('/');
        try (InputStream stream = getClass().getResourceAsStream("/plugin.yml")) {
            assertNotNull(stream);
            configuration.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
        }
        ConfigurationSection permissions = configuration.getConfigurationSection("permissions");
        assertNotNull(permissions);
        assertEquals(expectedPermissions, permissions.getKeys(false));
        for (String permission : expectedPermissions) {
            assertEquals("op", permissions.getConfigurationSection(permission).getString("default"),
                    permission);
        }
        assertEquals("1.13", configuration.getString("api-version"));
    }
}
