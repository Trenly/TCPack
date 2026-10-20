package me.tzahi.TCPack.command;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression tests for Trenly's flight adaptation of TCPack by
 * tzahi12345 (Tzahi) and TacoID; CC BY-SA 3.0, see LICENSE and NOTICE.
 */
final class FlyCommandTest {
    @Test
    void helpAllowsPlayersAndConsoleWithOnlyListPermission() {
        for (boolean player : new boolean[] {true, false}) {
            Fixture fixture = new Fixture(player);
            fixture.sender.permissions.add("fly.list");
            fixture.run();
            fixture.messageContains("/fly", "toggle", "on", "off", "[player]");
            assertEquals(Arrays.asList("fly.list"), fixture.sender.permissionChecks);
            fixture.unchanged();
        }
    }

    @Test
    void helpDenialDoesNotLookUpOrMutatePlayers() {
        Fixture fixture = new Fixture(false);
        fixture.run();
        fixture.messageContains("permission");
        fixture.unchanged();
    }

    @Test
    void allSelfActionsAndToggleAliasesUseTheirOwnPermissions() {
        for (String action : new String[] {"t", "toggle", "on", "off", "TOGGLE", "ON", "OFF"}) {
            Fixture fixture = new Fixture(true);
            boolean disabling = "off".equalsIgnoreCase(action);
            fixture.sender.allowed = disabling;
            fixture.sender.flying = disabling;
            fixture.sender.permissions.add(permission(action, false));
            fixture.run(action);
            assertEquals(!disabling, fixture.sender.allowed);
            assertEquals(!disabling, fixture.sender.flying);
            assertEquals(order(!disabling), fixture.sender.mutations);
            assertEquals(Arrays.asList(permission(action, false)), fixture.sender.permissionChecks);
            assertTrue(fixture.lookups.isEmpty());
            assertTrue(fixture.target.mutations.isEmpty());
            fixture.messageContains(disabling ? "disabled" : "enabled", "Sender");
        }
    }

    @Test
    void explicitTargetsWorkForBothPlayersAndConsoleWithOnlyOtherPermission() {
        for (boolean player : new boolean[] {true, false}) {
            for (String action : new String[] {"t", "toggle", "on", "off"}) {
                Fixture fixture = new Fixture(player);
                boolean disabling = "off".equals(action);
                fixture.target.allowed = disabling;
                fixture.target.flying = disabling;
                fixture.sender.permissions.add(permission(action, true));
                fixture.run(action, "Target");
                assertEquals(Arrays.asList("Target"), fixture.lookups);
                assertEquals(Arrays.asList(permission(action, true)), fixture.sender.permissionChecks);
                assertEquals(!disabling, fixture.target.allowed);
                assertEquals(!disabling, fixture.target.flying);
                assertEquals(order(!disabling), fixture.target.mutations);
                assertTrue(fixture.sender.mutations.isEmpty());
                fixture.messageContains(disabling ? "disabled" : "enabled", "Target");
            }
        }
    }

    @Test
    void explicitSelfNameStillRequiresOtherPermission() {
        Fixture fixture = new Fixture(true);
        fixture.sender.permissions.add("fly.enable");
        fixture.online.put("Sender", (Player) fixture.sender.proxy);
        fixture.run("on", "Sender");
        fixture.messageContains("permission");
        fixture.unchanged();
        fixture.sender.permissions.add("fly.enable.other");
        fixture.run("on", "Sender");
        assertEquals(Arrays.asList("Sender"), fixture.lookups);
        assertEquals(order(true), fixture.sender.mutations);
    }

    @Test
    void toggleUsesAllowanceRatherThanCurrentFlyingState() {
        Fixture fixture = new Fixture(true);
        fixture.sender.permissions.add("fly.toggle");
        fixture.sender.allowed = true;
        fixture.sender.flying = false;
        fixture.run("toggle");
        assertFalse(fixture.sender.allowed);
        assertFalse(fixture.sender.flying);
        assertEquals(order(false), fixture.sender.mutations);
    }

    @Test
    void consoleMustSpecifyTargetForEveryAction() {
        for (String action : new String[] {"t", "toggle", "on", "off"}) {
            Fixture fixture = new Fixture(false);
            fixture.sender.permissions.add(permission(action, false));
            fixture.run(action);
            fixture.messageContains("Console", "specify");
            fixture.unchanged();
        }
    }

    @Test
    void targetMustBeExactAndOnlineWithoutPartialLookup() {
        for (String name : new String[] {"Missing", "Tar"}) {
            Fixture fixture = new Fixture(true);
            fixture.sender.permissions.add("fly.enable.other");
            fixture.run("on", name);
            fixture.messageContains("exact", "not online");
            assertEquals(Arrays.asList(name), fixture.lookups);
            assertTrue(fixture.sender.mutations.isEmpty());
            assertTrue(fixture.target.mutations.isEmpty());
        }
    }

    @Test
    void deniedActionsNeverLookUpOrMutateTargetEvenWhenSelfPermissionExists() {
        for (boolean player : new boolean[] {true, false}) {
            for (String action : new String[] {"t", "toggle", "on", "off"}) {
                Fixture self = new Fixture(player);
                self.run(action);
                self.messageContains("permission");
                self.unchanged();

                Fixture other = new Fixture(player);
                other.sender.permissions.add(permission(action, false));
                other.run(action, "Target");
                other.messageContains("permission");
                other.unchanged();
            }
        }
    }

    @Test
    void invalidInputIsRejectedBeforePermissionsOrTargetLookup() {
        for (String[] args : new String[][] {
                {"unknown"}, {"unknown", "Target"}, {"on", "Target", "extra"},
                {"off", "Target", "extra"}, {"toggle", "Target", "extra"},
                {"on", ""}, {"on", "   "}, {""}
        }) {
            Fixture fixture = new Fixture(true);
            fixture.run(args);
            fixture.messageContains(args[0].equals("on") || args[0].equals("off")
                    || args[0].equals("toggle") ? "Usage" : "Unknown");
            assertTrue(fixture.sender.permissionChecks.isEmpty());
            fixture.unchanged();
        }
    }

    @Test
    void disabledFeatureBlocksHelpAndAllActionsBeforeAnythingElse() {
        for (boolean player : new boolean[] {true, false}) {
            for (String[] args : new String[][] {
                    {}, {"t"}, {"toggle", "Target"}, {"on"}, {"off", "Target"}, {"unknown"}
            }) {
                Fixture fixture = new Fixture(player);
                fixture.enabled = false;
                fixture.run(args);
                fixture.messageContains("disabled");
                assertTrue(fixture.sender.permissionChecks.isEmpty());
                fixture.unchanged();
            }
        }
    }

    @Test
    void survivalAndAdventureSupportBothDirections() {
        for (GameMode mode : new GameMode[] {GameMode.SURVIVAL, GameMode.ADVENTURE}) {
            for (boolean explicitTarget : new boolean[] {true, false}) {
                Fixture fixture = new Fixture(true);
                Stub changed = explicitTarget ? fixture.target : fixture.sender;
                changed.mode = mode;
                fixture.sender.permissions.add(permission("on", explicitTarget));
                fixture.sender.permissions.add(permission("off", explicitTarget));
                fixture.run(explicitTarget ? new String[] {"on", "Target"} : new String[] {"on"});
                fixture.run(explicitTarget ? new String[] {"off", "Target"} : new String[] {"off"});
                assertEquals(Arrays.asList("allow:true", "flying:true", "flying:false", "allow:false"),
                        changed.mutations);
                assertEquals(mode, changed.mode);
            }
        }
    }

    @Test
    void creativeAndSpectatorAreUnchangedForEveryActionAndAllowanceState() {
        for (GameMode mode : new GameMode[] {GameMode.CREATIVE, GameMode.SPECTATOR}) {
            for (boolean allowed : new boolean[] {true, false}) {
                for (boolean explicitTarget : new boolean[] {true, false}) {
                    for (String action : new String[] {"t", "toggle", "on", "off"}) {
                        Fixture fixture = new Fixture(true);
                        Stub changed = explicitTarget ? fixture.target : fixture.sender;
                        changed.mode = mode;
                        changed.allowed = allowed;
                        changed.flying = allowed;
                        fixture.sender.permissions.add(permission(action, explicitTarget));
                        fixture.run(explicitTarget
                                ? new String[] {action, "Target"} : new String[] {action});
                        fixture.messageContains("survival", "adventure");
                        assertEquals(allowed, changed.allowed);
                        assertEquals(allowed, changed.flying);
                        assertEquals(mode, changed.mode);
                        assertTrue(changed.mutations.isEmpty());
                    }
                }
            }
        }
    }

    @Test
    void redundantOnAndOffDoNotChangeEitherFlightFlag() {
        for (boolean explicitTarget : new boolean[] {true, false}) {
            for (boolean allowed : new boolean[] {true, false}) {
                for (boolean flying : new boolean[] {true, false}) {
                    Fixture fixture = new Fixture(true);
                    Stub changed = explicitTarget ? fixture.target : fixture.sender;
                    changed.allowed = allowed;
                    changed.flying = flying;
                    String action = allowed ? "on" : "off";
                    fixture.sender.permissions.add(permission(action, explicitTarget));
                    fixture.run(explicitTarget
                            ? new String[] {action, "Target"} : new String[] {action});
                    fixture.messageContains("already", allowed ? "enabled" : "disabled");
                    assertEquals(allowed, changed.allowed);
                    assertEquals(flying, changed.flying);
                    assertTrue(changed.mutations.isEmpty());
                }
            }
        }
    }

    private static String permission(String action, boolean other) {
        String base = "off".equalsIgnoreCase(action) ? "fly.disable"
                : "on".equalsIgnoreCase(action) ? "fly.enable" : "fly.toggle";
        return base + (other ? ".other" : "");
    }

    private static List<String> order(boolean enable) {
        return enable ? Arrays.asList("allow:true", "flying:true")
                : Arrays.asList("flying:false", "allow:false");
    }

    private static final class Fixture {
        final Stub sender;
        final Stub target = new Stub("Target", Player.class);
        final Map<String, Player> online = new HashMap<>();
        final List<String> lookups = new ArrayList<>();
        final Server server;
        boolean enabled = true;

        Fixture(boolean player) {
            sender = new Stub("Sender", player ? Player.class : ConsoleCommandSender.class);
            online.put("Target", (Player) target.proxy);
            server = (Server) Proxy.newProxyInstance(Server.class.getClassLoader(),
                    new Class<?>[] {Server.class}, (proxy, method, args) -> {
                        if ("getPlayerExact".equals(method.getName())) {
                            String name = (String) args[0];
                            lookups.add(name);
                            return online.get(name);
                        }
                        throw new AssertionError("Unexpected Server call: " + method);
                    });
        }

        void run(String... args) {
            assertTrue(new FlyCommand(enabled, server).onCommand(sender.proxy, null, "fly", args));
        }

        void messageContains(String... parts) {
            assertFalse(sender.messages.isEmpty());
            String message = ChatColor.stripColor(sender.messages.get(sender.messages.size() - 1));
            for (String part : parts) {
                assertTrue(message.contains(part), message + " should contain " + part);
            }
        }

        void unchanged() {
            assertTrue(lookups.isEmpty());
            assertTrue(sender.mutations.isEmpty());
            assertTrue(target.mutations.isEmpty());
        }
    }

    private static final class Stub implements InvocationHandler {
        final CommandSender proxy;
        final String name;
        final Set<String> permissions = new HashSet<>();
        final List<String> permissionChecks = new ArrayList<>();
        final List<String> messages = new ArrayList<>();
        final List<String> mutations = new ArrayList<>();
        GameMode mode = GameMode.SURVIVAL;
        boolean allowed;
        boolean flying;

        Stub(String name, Class<? extends CommandSender> type) {
            this.name = name;
            proxy = (CommandSender) Proxy.newProxyInstance(type.getClassLoader(),
                    new Class<?>[] {type}, this);
        }

        @Override
        public Object invoke(Object instance, Method method, Object[] args) {
            switch (method.getName()) {
                case "hasPermission":
                    permissionChecks.add((String) args[0]);
                    return permissions.contains(args[0]);
                case "sendMessage":
                    messages.add((String) args[0]);
                    return null;
                case "getName":
                    return name;
                case "getGameMode":
                    return mode;
                case "getAllowFlight":
                    return allowed;
                case "setAllowFlight":
                    boolean nextAllowed = (Boolean) args[0];
                    assertFalse(!nextAllowed && flying, "Clear flying before revoking allowance");
                    allowed = nextAllowed;
                    mutations.add("allow:" + allowed);
                    return null;
                case "setFlying":
                    boolean nextFlying = (Boolean) args[0];
                    assertFalse(nextFlying && !allowed, "Grant allowance before enabling flight");
                    flying = nextFlying;
                    mutations.add("flying:" + flying);
                    return null;
                default:
                    throw new AssertionError("Unexpected sender call or side effect: " + method);
            }
        }
    }
}
