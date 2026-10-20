package me.tzahi.tcpack.harness;

import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
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
        check("2.1.0".equals(plugin.getDescription().getVersion()), "release version 2.1.0");
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
        compression(command("compress", plugin), player, console);
        PluginCommand help = command("tcpack", plugin);
        for (State sender : new State[] {player, console}) {
            execute(help, sender);
            check(sender.message().contains("}---- TCPack help ----{")
                    && sender.message().contains("/fly")
                    && sender.message().contains("/compress")
                    && sender.message().contains("/ptime")
                    && !sender.message().contains("/fake"), "TCPack aggregate help");
            sender.denied.add("tcpack.list");
            execute(help, sender);
            check(sender.message().contains("permission: tcpack.list"), "TCPack help permission");
            sender.denied.clear();
        }
    }

    private void compression(PluginCommand command, State player, State console) {
        Map<String, Material> materials = new LinkedHashMap<String, Material>();
        for (Material material : Material.values()) {
            if (!material.name().startsWith("LEGACY_")) {
                materials.put(material.name(), material);
            }
        }
        execute(command, player);
        check(player.message().contains("}---- Compress help ----{")
                && player.message().contains("/tcpack:compress wood"), "compression original-style help");
        execute(command, console, "wood");
        check(console.message().contains("only be used by a player"), "compression console");
        for (String group : new String[] {"wood", "bonemeal", "ores"}) {
            player.denied.add("compress." + group);
            execute(command, player, group);
            check(player.message().contains("permission"), "compression permission " + group);
            player.denied.clear();
        }
        execute(command, player, "wood", "extra");
        check(player.message().contains("Too many"), "compression arity");
        execute(command, player, "unknown");
        check(player.message().contains("Unknown"), "compression unknown group");
        if (materials.containsKey("OAK_PLANKS")) {
            for (String name : materials.keySet()) {
                if (name.endsWith("_PLANKS")) {
                    String species = name.substring(0, name.length() - "_PLANKS".length());
                    Material log = materials.get(species + "_LOG");
                    if (log == null) {
                        log = materials.get(species + "_STEM");
                    }
                    if (log != null) {
                        conversion(command, player, "wood", materials.get(name), 0, log, 0, 4);
                    } else {
                        player.storage.clear();
                        player.storage.setItem(0, new ItemStack(materials.get(name), 8));
                        execute(command, player, "wood");
                        check(count(player.storage, materials.get(name), 0) == 8,
                                "wood without base log/stem is unchanged: " + name);
                    }
                }
            }
        } else {
            for (int data = 0; data < 6; data++) {
                conversion(command, player, "wood", materials.get("WOOD"), data,
                        materials.get(data < 4 ? "LOG" : "LOG_2"), data < 4 ? data : data - 4, 4);
            }
        }
        boolean boneBlock = materials.containsKey("BONE_BLOCK");
        conversion(command, player, "bonemeal",
                materials.get(materials.containsKey("BONE_MEAL") ? "BONE_MEAL" : "INK_SACK"),
                materials.containsKey("BONE_MEAL") ? 0 : 15,
                materials.get(boneBlock ? "BONE_BLOCK" : "BONE"), 0, boneBlock ? 9 : 3);
        for (String name : materials.keySet()) {
            String output = name.endsWith("_INGOT")
                    ? name.substring(0, name.length() - 6) + "_BLOCK"
                    : name.startsWith("RAW_") && !name.endsWith("_BLOCK") ? name + "_BLOCK" : "";
            if (materials.containsKey(output)) {
                conversion(command, player, "ores", materials.get(name), 0, materials.get(output), 0, 9);
            }
        }
        conversion(command, player, "ores", Material.DIAMOND, 0, Material.DIAMOND_BLOCK, 0, 9);
        conversion(command, player, "ores", Material.REDSTONE, 0, Material.REDSTONE_BLOCK, 0, 9);
        conversion(command, player, "ores",
                materials.get(materials.containsKey("LAPIS_LAZULI") ? "LAPIS_LAZULI" : "INK_SACK"),
                materials.containsKey("LAPIS_LAZULI") ? 0 : 4, Material.LAPIS_BLOCK, 0, 9);
        for (String metal : new String[] {"IRON", "GOLD"}) {
            Material nugget = materials.get(metal + "_NUGGET");
            if (nugget == null) {
                continue;
            }
            Material ingot = materials.get(metal + "_INGOT");
            Material block = materials.get(metal + "_BLOCK");
            conversion(command, player, "ores", nugget, 0, ingot, 0, 9);
            for (int ingots : new int[] {0, 8, 9, 17}) {
                player.storage.clear();
                if (ingots > 0) {
                    player.storage.setItem(0, new ItemStack(ingot, ingots));
                }
                player.storage.setItem(1, new ItemStack(nugget, 64));
                player.storage.setItem(2, new ItemStack(nugget, 18));
                execute(command, player, "ores");
                check(count(player.storage, block, 0) == (ingots + 9) / 9,
                        metal + " chained nugget block");
                check(count(player.storage, ingot, 0) == ingots % 9,
                        metal + " remaining ingots");
                check(count(player.storage, nugget, 0) == 1, metal + " remaining nuggets");
                if (ingots == 8) {
                    check(player.storage.getItem(0).getAmount() == 8
                            && player.storage.getItem(0).getType() == ingot,
                            metal + " nugget-derived ingots consumed first");
                }
            }
            player.storage.clear();
            for (int slot = 0; slot < 36; slot++) {
                player.storage.setItem(slot, new ItemStack(Material.COBBLESTONE, 64));
            }
            player.storage.setItem(0, new ItemStack(ingot, 8));
            player.storage.setItem(1, new ItemStack(nugget, 10));
            execute(command, player, "ores");
            check(count(player.storage, block, 0) == 1
                    && count(player.storage, ingot, 0) == 0
                    && count(player.storage, nugget, 0) == 1,
                    metal + " no intermediate inventory space needed");
            player.storage.setItem(0, new ItemStack(Material.COBBLESTONE, 64));
            player.storage.setItem(1, new ItemStack(nugget, 10));
            ItemStack[] nuggetBefore = player.storage.getContents();
            execute(command, player, "ores");
            check(player.message().contains("No items were changed")
                    && Arrays.equals(nuggetBefore, player.storage.getContents()),
                    metal + " chained no-space rollback");
            player.storage.clear();
            ItemStack namedNuggets = new ItemStack(nugget, 9);
            ItemMeta nuggetMeta = namedNuggets.getItemMeta();
            nuggetMeta.setDisplayName("Preserve custom nuggets");
            namedNuggets.setItemMeta(nuggetMeta);
            player.storage.setItem(0, namedNuggets);
            execute(command, player, "ores");
            check(namedNuggets.equals(player.storage.getItem(0))
                    && count(player.storage, ingot, 0) == 0, metal + " custom nuggets preserved");
        }

        player.storage.clear();
        ItemStack custom = new ItemStack(Material.IRON_INGOT, 18);
        ItemMeta meta = custom.getItemMeta();
        meta.setDisplayName("Keep this custom item");
        meta.setLore(Collections.singletonList("Not a compression input"));
        custom.setItemMeta(meta);
        player.storage.setItem(0, custom);
        execute(command, player, "ores");
        check(custom.equals(player.storage.getItem(0)), "custom metadata input preserved");
        check(count(player.storage, Material.IRON_BLOCK, 0) == 0, "custom input not converted");

        player.storage.clear();
        for (int slot = 0; slot < 36; slot++) {
            player.storage.setItem(slot, new ItemStack(Material.COBBLESTONE, 64));
        }
        player.storage.setItem(0, new ItemStack(Material.IRON_INGOT, 10));
        player.storage.setItem(1, new ItemStack(Material.GOLD_INGOT, 10));
        ItemStack[] before = player.storage.getContents();
        execute(command, player, "ores");
        check(player.message().contains("No items were changed"), "no-space explicit rejection");
        check(Arrays.equals(before, player.storage.getContents()), "no-space transaction rollback");
        player.storage.clear();
        player.reserved[0] = new ItemStack(Material.IRON_INGOT, 18);
        player.reserved[4] = new ItemStack(Material.IRON_INGOT, 18);
        execute(command, player, "ores");
        check(player.reserved[0].getAmount() == 18 && player.reserved[4].getAmount() == 18,
                "armor/offhand ignored");
        check(count(player.storage, Material.IRON_BLOCK, 0) == 0, "reserved items not converted");
        getLogger().info("TCPACK_RUNTIME_EVIDENCE compression=real-server-ItemStack-and-inventory"
                + " wood=runtime-species boneBlocks=" + boneBlock);
    }

    private void conversion(PluginCommand command, State player, String group,
            Material input, int inputData, Material output, int outputData, int ratio) {
        player.storage.clear();
        player.storage.setItem(0, new ItemStack(input, ratio - 1, (short) inputData));
        player.storage.setItem(1, new ItemStack(input, ratio * 2 + 2, (short) inputData));
        execute(command, player, group);
        check(count(player.storage, input, inputData) == 1, "remainder " + input + ":" + inputData);
        check(count(player.storage, output, outputData) == 3, "base output " + output + ":" + outputData);
        check(player.message().contains("successfully"), "conversion feedback " + input);
    }

    private int count(Inventory inventory, Material material, int data) {
        int total = 0;
        for (ItemStack stack : inventory.getContents()) {
            if (stack != null && stack.getType() == material && stack.getDurability() == data) {
                total += stack.getAmount();
            }
        }
        return total;
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
        private final Inventory storage = getServer().createInventory(null, 36);
        private final ItemStack[] reserved = new ItemStack[5];
        private final PlayerInventory inventory = (PlayerInventory) Proxy.newProxyInstance(
                PlayerInventory.class.getClassLoader(), new Class<?>[] {PlayerInventory.class},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) {
                        if ("getSize".equals(method.getName())) { return 41; }
                        if ("getMaxStackSize".equals(method.getName())) { return storage.getMaxStackSize(); }
                        if ("getItem".equals(method.getName())) {
                            int slot = (Integer) args[0];
                            return slot < 36 ? storage.getItem(slot) : reserved[slot - 36];
                        }
                        if ("setItem".equals(method.getName())) {
                            int slot = (Integer) args[0];
                            check(slot < 36, "compression writes storage only");
                            storage.setItem(slot, (ItemStack) args[1]);
                            return null;
                        }
                        throw new AssertionError("Unexpected inventory API: " + method);
                    }
                });

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
            if ("getInventory".equals(name)) { return inventory; }
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
