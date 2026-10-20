package me.tzahi.TCPack.command;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.Test;

class CompressCommandTest {
    @Test
    void helpPreservesOriginalColorsLayoutAndNamespacedLabel() {
        Fixture fixture = new Fixture(true, true);
        fixture.run(true);
        assertEquals(Arrays.asList(
                ChatColor.BLUE + "}---- Compress help ----{",
                ChatColor.GOLD + "/tcpack:compress" + ChatColor.BLUE + " - Lists compress commands.",
                ChatColor.GOLD + "/tcpack:compress wood" + ChatColor.BLUE + " - Compresses wood.",
                ChatColor.GOLD + "/tcpack:compress bonemeal" + ChatColor.BLUE + " - Compresses bonemeal.",
                ChatColor.GOLD + "/tcpack:compress ores" + ChatColor.BLUE
                        + " - Compresses ingots, raw ores, and diamonds."), fixture.messages);
        assertEquals(Arrays.asList("compress.list"), fixture.permissions);
        assertEquals(0, fixture.inventoryAccesses);
    }

    @Test
    void eachActionChecksItsOwnPermissionAndReportsInsufficientItems() {
        for (String action : new String[] {"wood", "bonemeal", "ores"}) {
            Fixture fixture = new Fixture(true, true);
            fixture.run(true, action.toUpperCase(java.util.Locale.ROOT));
            assertEquals(Arrays.asList("compress." + action), fixture.permissions);
            assertEquals(1, fixture.inventoryAccesses);
            assertTrue(fixture.messages.toString().contains("No plain items"));
        }
    }

    @Test
    void denialAndInvalidInputNeverAccessInventory() {
        for (String[] args : new String[][] {{}, {"wood"}, {"bonemeal"}, {"ores"}}) {
            Fixture fixture = new Fixture(true, false);
            fixture.run(true, args);
            assertTrue(fixture.messages.toString().contains("permission"));
            assertEquals(0, fixture.inventoryAccesses);
        }
        for (String[] args : new String[][] {{"unknown"}, {"list"}, {"wood", "extra"}, {""}}) {
            Fixture fixture = new Fixture(true, true);
            fixture.run(true, args);
            assertTrue(fixture.messages.toString().contains("help"));
            assertEquals(0, fixture.permissions.size());
            assertEquals(0, fixture.inventoryAccesses);
        }
    }

    @Test
    void disabledAndConsoleRequestsReturnExplicitErrorsWithoutInventoryAccess() {
        Fixture disabled = new Fixture(true, true);
        disabled.run(false, "wood");
        assertTrue(disabled.messages.toString().contains("disabled"));
        assertEquals(0, disabled.inventoryAccesses);
        assertTrue(disabled.permissions.isEmpty());
        Fixture console = new Fixture(false, true);
        console.run(true);
        assertTrue(console.messages.toString().contains("only be used by a player"));
        assertEquals(0, console.inventoryAccesses);
    }

    private static final class Fixture {
        final List<String> messages = new ArrayList<>();
        final List<String> permissions = new ArrayList<>();
        int inventoryAccesses;
        final PlayerInventory inventory = (PlayerInventory) Proxy.newProxyInstance(
                PlayerInventory.class.getClassLoader(), new Class<?>[] {PlayerInventory.class},
                (proxy, method, args) -> {
                    if ("getSize".equals(method.getName())) {
                        return 36;
                    }
                    if ("getItem".equals(method.getName())) {
                        return null;
                    }
                    throw new AssertionError("Unexpected inventory call: " + method);
                });
        final CommandSender sender;

        Fixture(boolean player, boolean permitted) {
            sender = (CommandSender) Proxy.newProxyInstance(CommandSender.class.getClassLoader(),
                    new Class<?>[] {player ? Player.class : CommandSender.class},
                    (proxy, method, args) -> {
                        switch (method.getName()) {
                            case "sendMessage": messages.add((String) args[0]); return null;
                            case "hasPermission": permissions.add((String) args[0]); return permitted;
                            case "getInventory": inventoryAccesses++; return inventory;
                            default: throw new AssertionError("Unexpected sender call: " + method);
                        }
                    });
        }

        void run(boolean enabled, String... args) {
            assertTrue(new CompressCommand(enabled).onCommand(sender, null, "tcpack:compress", args));
        }
    }
}
