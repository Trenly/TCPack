package me.tzahi.TCPack.command;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PtimeCommandTest {
    @Test
    void setsEveryFixedPersonalTimeValueWithoutFollowingWorldTime() {
        Object[][] actions = {
                {"day", "ptime.day", 6000L},
                {"night", "ptime.night", 18000L},
                {"morning", "ptime.morning", 0L},
                {"dawn", "ptime.dawn", 23000L},
                {"dusk", "ptime.dusk", 12000L}
        };

        for (Object[] action : actions) {
            PlayerState state = new PlayerState(Collections.singleton((String) action[1]));
            boolean handled = execute(new PtimeCommand(true), state.player(), (String) action[0]);

            assertTrue(handled, (String) action[0]);
            assertEquals(action[2], state.setTicks, (String) action[0]);
            assertEquals(Boolean.FALSE, state.setRelative, (String) action[0]);
            assertEquals(0, state.resetCalls, (String) action[0]);
        }
    }

    @Test
    void resetsPersonalTimeAndReportsSuccess() {
        PlayerState state = new PlayerState(Collections.singleton("ptime.reset"));

        assertTrue(execute(new PtimeCommand(true), state.player(), "reset"));

        assertEquals(1, state.resetCalls);
        assertEquals(0, state.setCalls);
        assertTrue(state.messagesText().contains("reset to the world's time"));
    }

    @Test
    void displaysAccurateTimeAndOffsetWithInfoPermission() {
        PlayerState state = new PlayerState(Collections.singleton("ptime.info"));
        state.playerTime = 12345L;
        state.playerTimeOffset = -9876L;

        assertTrue(execute(new PtimeCommand(true), state.player(), "info"));

        assertTrue(state.messagesText().contains("12345 ticks; offset: -9876 ticks"));
        assertEquals(0, state.setCalls);
        assertEquals(0, state.resetCalls);
    }

    @Test
    void noArgumentsShowsCompleteHelpOnlyWithListPermission() {
        PlayerState allowed = new PlayerState(Collections.singleton("ptime.list"));

        assertTrue(execute(new PtimeCommand(true), allowed.player()));

        String help = allowed.messagesText();
        for (String entry : Arrays.asList(
                "/ptime day", "ptime.day", "/ptime night", "ptime.night",
                "/ptime morning", "ptime.morning", "/ptime dawn", "23000",
                "ptime.dawn", "/ptime dusk", "12000", "ptime.dusk",
                "/ptime reset", "ptime.reset", "/ptime info", "ptime.info")) {
            assertTrue(help.contains(entry), entry);
        }
        assertEquals(0, allowed.setCalls);
        assertEquals(0, allowed.resetCalls);

        PlayerState denied = new PlayerState(Collections.<String>emptySet());
        assertTrue(execute(new PtimeCommand(true), denied.player()));
        assertTrue(denied.messagesText().contains("ptime.list"));
        assertEquals(0, denied.setCalls);
        assertEquals(0, denied.resetCalls);
    }

    @Test
    void disabledCommandRejectsHelpAndActionsWithoutMutation() {
        for (String[] args : new String[][]{{}, {"day"}, {"reset"}, {"info"}}) {
            PlayerState state = new PlayerState(Collections.<String>emptySet());

            assertTrue(execute(new PtimeCommand(false), state.player(), args));

            assertTrue(state.messagesText().contains("Personal time is disabled."));
            assertEquals(0, state.setCalls);
            assertEquals(0, state.resetCalls);
            assertEquals(0, state.permissionChecks);
        }
    }

    @Test
    void rejectsConsoleSendersIncludingHelpAndDisabledCommands() {
        SenderState console = new SenderState();

        assertTrue(execute(new PtimeCommand(true), console.sender()));
        assertTrue(console.messagesText().contains("only be used by a player"));

        console.messages.clear();
        assertTrue(execute(new PtimeCommand(false), console.sender(), "day"));
        assertTrue(console.messagesText().contains("only be used by a player"));
    }

    @Test
    void rejectsUnknownAndExcessArgumentsWithoutMutation() {
        for (String[] args : new String[][]{{"unknown"}, {"day", "extra"}, {"info", "extra"}}) {
            PlayerState state = new PlayerState(Collections.<String>emptySet());

            assertTrue(execute(new PtimeCommand(true), state.player(), args));

            String output = state.messagesText();
            assertTrue(output.contains(args.length > 1 ? "Too many arguments" : "Unknown personal-time option"));
            assertEquals(0, state.setCalls);
            assertEquals(0, state.resetCalls);
        }
    }

    @Test
    void deniedActionsNeverChangePersonalTime() {
        String[][] actions = {
                {"day"}, {"night"}, {"morning"}, {"dawn"}, {"dusk"}, {"reset"}, {"info"}
        };
        for (String[] args : actions) {
            PlayerState state = new PlayerState(Collections.<String>emptySet());

            assertTrue(execute(new PtimeCommand(true), state.player(), args));

            assertTrue(state.messagesText().contains("You do not have permission"));
            assertEquals(0, state.setCalls, args[0]);
            assertEquals(0, state.resetCalls, args[0]);
        }
    }

    private boolean execute(PtimeCommand executor, CommandSender sender, String... args) {
        return executor.onCommand(sender, null, "ptime", args);
    }

    private static class PlayerState implements InvocationHandler {
        private final Set<String> permissions;
        private final List<String> messages = new ArrayList<String>();
        private long playerTime = 6000L;
        private long playerTimeOffset = 0L;
        private long setTicks;
        private Boolean setRelative;
        private int setCalls;
        private int resetCalls;
        private int permissionChecks;

        private PlayerState(Set<String> permissions) {
            this.permissions = new HashSet<String>(permissions);
        }

        private Player player() {
            return (Player) Proxy.newProxyInstance(
                    Player.class.getClassLoader(), new Class<?>[]{Player.class}, this);
        }

        private String messagesText() {
            return messages.toString();
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            String name = method.getName();
            if ("hasPermission".equals(name)) {
                permissionChecks++;
                return permissions.contains(args[0]);
            }
            if ("sendMessage".equals(name) && args != null && args.length == 1) {
                if (args[0] instanceof String) {
                    messages.add((String) args[0]);
                } else if (args[0] instanceof String[]) {
                    messages.addAll(Arrays.asList((String[]) args[0]));
                }
                return null;
            }
            if ("setPlayerTime".equals(name)) {
                setCalls++;
                setTicks = (Long) args[0];
                setRelative = (Boolean) args[1];
                return null;
            }
            if ("resetPlayerTime".equals(name)) {
                resetCalls++;
                return null;
            }
            if ("getPlayerTime".equals(name)) {
                return playerTime;
            }
            if ("getPlayerTimeOffset".equals(name)) {
                return playerTimeOffset;
            }
            return objectMethod(proxy, method, args);
        }
    }

    private static class SenderState implements InvocationHandler {
        private final List<String> messages = new ArrayList<String>();

        private CommandSender sender() {
            return (CommandSender) Proxy.newProxyInstance(
                    CommandSender.class.getClassLoader(), new Class<?>[]{CommandSender.class}, this);
        }

        private String messagesText() {
            return messages.toString();
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            if ("sendMessage".equals(method.getName()) && args != null && args.length == 1
                    && args[0] instanceof String) {
                messages.add((String) args[0]);
                return null;
            }
            return objectMethod(proxy, method, args);
        }
    }

    private static Object objectMethod(Object proxy, Method method, Object[] args) {
        String name = method.getName();
        if ("toString".equals(name)) {
            return proxy.getClass().getInterfaces()[0].getSimpleName() + " test proxy";
        }
        if ("hashCode".equals(name)) {
            return System.identityHashCode(proxy);
        }
        if ("equals".equals(name)) {
            return proxy == args[0];
        }
        Class<?> returnType = method.getReturnType();
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == char.class) {
            return '\0';
        }
        if (returnType == byte.class || returnType == short.class || returnType == int.class
                || returnType == long.class || returnType == float.class || returnType == double.class) {
            return 0;
        }
        return null;
    }
}
