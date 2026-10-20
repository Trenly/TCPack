package me.tzahi.TCPack.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

class TCPackCommandTest {
    @Test
    void listsImplementedFamiliesInOriginalStyleForPlayersAndConsole() {
        for (Class<?> senderType : new Class<?>[] {Player.class, CommandSender.class}) {
            for (String label : new String[] {"tcpack", "tcpack:tcpack"}) {
                for (String[] args : new String[][] {{}, {"help"}}) {
                    List<String> messages = execute(senderType, true, label, args);
                    assertEquals(Arrays.asList(
                            ChatColor.BLUE + "}---- TCPack help ----{",
                            ChatColor.GOLD + "/fly" + ChatColor.BLUE + " - Get all fly commands.",
                            ChatColor.GOLD + "/compress" + ChatColor.BLUE + " - Get all compress commands.",
                            ChatColor.GOLD + "/ptime" + ChatColor.BLUE + " - Get all ptime commands."),
                            messages);
                }
            }
        }
    }

    @Test
    void requiresOriginalHelpPermissionWithoutListingCommandsOnDenial() {
        for (Class<?> senderType : new Class<?>[] {Player.class, CommandSender.class}) {
            assertEquals(Arrays.asList(ChatColor.RED + "You do not have permission: tcpack.list"),
                    execute(senderType, false, "tcpack", new String[0]));
        }
    }

    private List<String> execute(Class<?> senderType, boolean permitted, String label, String[] args) {
        List<String> messages = new ArrayList<>();
        CommandSender sender = (CommandSender) Proxy.newProxyInstance(
                senderType.getClassLoader(), new Class<?>[] {senderType},
                (proxy, method, arguments) -> {
                    if ("hasPermission".equals(method.getName())) {
                        assertEquals("tcpack.list", arguments[0]);
                        return permitted;
                    }
                    if ("sendMessage".equals(method.getName())) {
                        messages.add((String) arguments[0]);
                        return null;
                    }
                    throw new AssertionError("Unexpected help side effect: " + method);
                });
        assertTrue(new TCPackCommand().onCommand(sender, null, label, args));
        return messages;
    }
}
