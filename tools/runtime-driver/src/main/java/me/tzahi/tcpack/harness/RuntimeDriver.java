package me.tzahi.tcpack.harness;

import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Isolated probes by Trenly for TCPack by tzahi12345 and TacoID.
 * CC BY-SA 3.0 Unported; see META-INF/LICENSE and META-INF/NOTICE.
 * Proxy state verifies executor/API interaction, not actual client behavior.
 */
public final class RuntimeDriver extends JavaPlugin {
    private int assertions;

    @Override
    public void onEnable() {
        try {
            String directory = System.getProperty("tcpack.harness.directory", "");
            String token = System.getProperty("tcpack.harness.token", "");
            File cwd = new File(".").getCanonicalFile();
            check(!directory.isEmpty() && cwd.equals(new File(directory).getCanonicalFile()),
                    "isolated working directory required");
            check(token.length() >= 32 && token.equals(new String(Files.readAllBytes(
                    new File(cwd, ".tcpack-harness").toPath()), StandardCharsets.UTF_8).trim()),
                    "runner ownership token required");
            check("127.0.0.1".equals(getServer().getIp()), "loopback binding required");
            check(getServer().getOnlinePlayers().isEmpty(), "no connected players allowed");
        } catch (Throwable failure) {
            getLogger().log(Level.SEVERE, "TCPACK_RUNTIME_FAIL safety gate", failure);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        getServer().getScheduler().runTaskLater(this, new Runnable() {
            @Override
            public void run() {
                try {
                    probe();
                    getLogger().info("TCPACK_RUNTIME_PASS assertions=" + assertions
                            + " scope=in-server-proxy NOT-connected-player-NOT-visual-E2E");
                } catch (Throwable failure) {
                    getLogger().log(Level.SEVERE, "TCPACK_RUNTIME_FAIL assertions=" + assertions, failure);
                }
            }
        }, 20L);
    }

    private void probe() {
        Plugin plugin = getServer().getPluginManager().getPlugin("TCPack");
        check(plugin != null && plugin.isEnabled(), "TCPack enabled");
        check("2.0.1".equals(plugin.getDescription().getVersion()), "release version 2.0.1");
        getLogger().info("TCPACK_RUNTIME_EVIDENCE server=" + getServer().getVersion()
                + " bukkit=" + getServer().getBukkitVersion()
                + " java=" + System.getProperty("java.runtime.version"));
        PluginCommand ptime = command("ptime", plugin);
        PluginCommand fly = command("fly", plugin);
        State player = new State(true);
        State console = new State(false);
        String[] names = {"day", "night", "morning", "dawn", "dusk"};
        long[] ticks = {6000, 18000, 0, 23000, 12000};
        for (int i = 0; i < names.length; i++) {
            execute(ptime, player, names[i]);
            check(player.time == ticks[i] && !player.relative, "ptime " + names[i] + " absolute ticks");
        }
        execute(ptime, player, "info");
        check(player.message().contains("Personal time: 12000 ticks; offset: 12000 ticks."), "ptime info");
        execute(ptime, player, "reset");
        check(player.resets == 1 && player.relative, "ptime reset API");
        execute(ptime, player);
        check(player.message().contains("}---- PlayerTime help ----{")
                && player.message().contains("/tcpack:ptime dawn")
                && player.message().contains("/tcpack:ptime dusk"), "ptime help");
        for (String action : new String[] {"day", "night", "morning", "dawn", "dusk", "info", "reset", ""}) {
            player.denied.add("ptime." + (action.isEmpty() ? "list" : action));
            int before = player.mutations;
            if (action.isEmpty()) {
                execute(ptime, player);
            } else {
                execute(ptime, player, action);
            }
            check(player.message().contains("permission: ptime.") && player.mutations == before,
                    "ptime permission " + action);
            player.denied.clear();
        }
        int before = player.mutations;
        execute(ptime, player, "invalid");
        check(player.message().contains("Unknown") && player.mutations == before, "ptime invalid");
        execute(ptime, player, "day", "extra");
        check(player.message().contains("Too many") && player.mutations == before, "ptime arity");
        execute(ptime, console, "day");
        check(console.message().contains("only be used by a player"), "ptime console");
        for (GameMode mode : GameMode.values()) {
            player.mode = mode;
            player.allowed = false;
            player.flying = false;
            player.events.clear();
            execute(fly, player, "on");
            if (mode != GameMode.SURVIVAL && mode != GameMode.ADVENTURE) {
                check(player.events.isEmpty() && player.message().contains("survival or adventure"),
                        "fly rejects " + mode);
                continue;
            }
            check(player.allowed && player.flying && player.events.toString().equals("[allow:true, flying:true]"),
                    "fly enable order " + mode);
            player.events.clear();
            execute(fly, player, "on");
            check(player.events.isEmpty() && player.message().contains("already enabled"), "fly redundant on");
            execute(fly, player, "off");
            check(!player.allowed && !player.flying
                    && player.events.toString().equals("[flying:false, allow:false]"), "fly disable order");
            player.events.clear();
            execute(fly, player, "off");
            check(player.events.isEmpty() && player.message().contains("already disabled"), "fly redundant off");
            execute(fly, player, "toggle");
            check(player.allowed && player.flying, "fly toggle");
            execute(fly, player, "t");
            check(!player.allowed && !player.flying, "fly t");
        }
        player.mode = GameMode.SURVIVAL;
        String[][] permissions = {{"on", "fly.enable"}, {"off", "fly.disable"},
                {"toggle", "fly.toggle"}, {"t", "fly.toggle"}, {"", "fly.list"}};
        for (String[] entry : permissions) {
            player.denied.add(entry[1]);
            player.events.clear();
            if (entry[0].isEmpty()) {
                execute(fly, player);
            } else {
                execute(fly, player, entry[0]);
            }
            check(player.events.isEmpty() && player.message().contains("permission: " + entry[1]),
                    "fly permission " + entry[1]);
            player.denied.clear();
        }
        execute(fly, player);
        check(player.message().contains("/fly"), "fly help");
        for (String action : new String[] {"on", "off", "toggle"}) {
            console.denied.add("fly." + ("on".equals(action) ? "enable" : "off".equals(action) ? "disable" : "toggle") + ".other");
            execute(fly, console, action, "TCPackMissing");
            check(console.message().contains("permission:"), "fly other permission " + action);
            console.denied.clear();
        }
        execute(fly, console, "on");
        check(console.message().contains("Console must specify"), "fly console self");
        execute(fly, console, "on", "TCPackMissing");
        check(console.message().contains("not online"), "fly console offline exact target");
        execute(fly, player, "on", "TCPackMissing");
        check(player.message().contains("not online"), "fly player offline exact target");
        player.events.clear();
        execute(fly, player, "invalid");
        check(player.message().contains("Unknown") && player.events.isEmpty(), "fly invalid");
        execute(fly, player, "on", "a", "b");
        check(player.message().contains("Usage:") && player.events.isEmpty(), "fly arity");
    }

    private PluginCommand command(String name, Plugin plugin) {
        PluginCommand command = getServer().getPluginCommand("tcpack:" + name);
        check(command != null && command.getPlugin() == plugin, "namespaced registry owner " + name);
        check(getServer().getPluginCommand(name) == command, "plain registry owner " + name);
        return command;
    }

    private void execute(PluginCommand command, State sender, String... args) {
        sender.messages.clear();
        check(command.execute(sender.proxy, "tcpack:" + command.getName(), args),
                "PluginCommand.execute " + command.getName());
        check(!sender.messages.isEmpty(), "command feedback " + command.getName());
    }

    private void check(boolean condition, String label) {
        if (!condition) {
            throw new AssertionError(label);
        }
        assertions++;
    }

    private final class State implements InvocationHandler {
        private final Set<String> denied = new HashSet<String>();
        private final List<String> messages = new ArrayList<String>();
        private final List<String> events = new ArrayList<String>();
        private final CommandSender proxy;
        private GameMode mode = GameMode.SURVIVAL;
        private long time;
        private boolean relative;
        private boolean allowed;
        private boolean flying;
        private int resets;
        private int mutations;

        private State(boolean player) {
            proxy = player ? PlayerStub.create(this) : (CommandSender) Proxy.newProxyInstance(
                    CommandSender.class.getClassLoader(), new Class<?>[] {CommandSender.class}, this);
        }

        private String message() {
            return messages.toString();
        }

        @Override
        public Object invoke(Object object, Method method, Object[] args) {
            String name = method.getName();
            if ("hasPermission".equals(name) || "isPermissionSet".equals(name)) {
                return !denied.contains(String.valueOf(args[0]));
            }
            if ("sendMessage".equals(name)) {
                for (Object argument : args) {
                    if (argument instanceof String) {
                        messages.add(ChatColor.stripColor((String) argument));
                    } else if (argument instanceof String[]) {
                        for (String message : (String[]) argument) {
                            messages.add(ChatColor.stripColor(message));
                        }
                    }
                }
                return null;
            }
            if ("getName".equals(name)) { return "TCPackProxy"; }
            if ("getUniqueId".equals(name)) { return new UUID(0, 1); }
            if ("getServer".equals(name)) { return getServer(); }
            if ("getGameMode".equals(name)) { return mode; }
            if ("getAllowFlight".equals(name)) { return allowed; }
            if ("isFlying".equals(name)) { return flying; }
            if ("getPlayerTime".equals(name) || "getPlayerTimeOffset".equals(name)) { return time; }
            if ("setPlayerTime".equals(name)) {
                time = (Long) args[0];
                relative = (Boolean) args[1];
                mutations++;
                return null;
            }
            if ("resetPlayerTime".equals(name)) {
                relative = true;
                resets++;
                mutations++;
                return null;
            }
            if ("setAllowFlight".equals(name)) {
                boolean value = (Boolean) args[0];
                check(value || !flying, "clear flying before allowance");
                events.add("allow:" + value);
                allowed = value;
                return null;
            }
            if ("setFlying".equals(name)) {
                boolean value = (Boolean) args[0];
                check(!value || allowed, "allowance before flying");
                events.add("flying:" + value);
                flying = value;
                return null;
            }
            if ("toString".equals(name)) { return "TCPackHarnessProxy"; }
            if ("hashCode".equals(name)) { return System.identityHashCode(object); }
            if ("equals".equals(name)) { return object == args[0]; }
            throw new AssertionError("Unexpected proxy API call: " + method);
        }
    }
}
