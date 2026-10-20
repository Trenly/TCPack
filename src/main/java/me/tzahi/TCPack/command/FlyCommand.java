package me.tzahi.TCPack.command;

import java.util.Locale;
import java.util.Objects;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * TCPack by tzahi12345 (Tzahi) and TacoID; flight adaptation by Trenly.
 * Adapted under CC BY-SA 3.0 Unported; see LICENSE and NOTICE.
 */
public final class FlyCommand implements CommandExecutor {
    private final boolean enabled;
    private final Server server;

    public FlyCommand(boolean enabled, Server server) {
        this.enabled = enabled;
        this.server = Objects.requireNonNull(server, "server");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!enabled) {
            return error(sender, "Flight commands are disabled.");
        }
        if (args.length == 0) {
            if (!sender.hasPermission("fly.list")) {
                return error(sender, "You do not have permission: fly.list");
            }
            sender.sendMessage(ChatColor.YELLOW + "/fly <t|toggle|on|off> [player]");
            return true;
        }

        String action = args[0].toLowerCase(Locale.ROOT);
        String permission;
        switch (action) {
            case "t":
            case "toggle":
                permission = "fly.toggle";
                break;
            case "on":
                permission = "fly.enable";
                break;
            case "off":
                permission = "fly.disable";
                break;
            default:
                return error(sender, "Unknown flight action. Use /fly for help.");
        }
        if (args.length > 2 || (args.length == 2 && args[1].trim().isEmpty())) {
            return error(sender, "Usage: /fly <t|toggle|on|off> [player]");
        }

        boolean explicitTarget = args.length == 2;
        if (explicitTarget) {
            permission += ".other";
        }
        if (!sender.hasPermission(permission)) {
            return error(sender, "You do not have permission: " + permission);
        }

        Player target;
        if (explicitTarget) {
            target = server.getPlayerExact(args[1]);
            if (target == null) {
                return error(sender, "That exact player is not online.");
            }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            return error(sender, "Console must specify an online player.");
        }

        GameMode mode = target.getGameMode();
        if (mode != GameMode.SURVIVAL && mode != GameMode.ADVENTURE) {
            return error(sender, "Flight can only be changed in survival or adventure mode.");
        }
        boolean allowed = target.getAllowFlight();
        boolean enable = "on".equals(action) || (!"off".equals(action) && !allowed);
        if (enable == allowed) {
            return error(sender, "Flight is already " + (allowed ? "enabled." : "disabled."));
        }
        if (enable) {
            target.setAllowFlight(true);
            target.setFlying(true);
        } else {
            target.setFlying(false);
            target.setAllowFlight(false);
        }
        sender.sendMessage(ChatColor.GREEN + "Flight " + (enable ? "enabled" : "disabled")
                + " for " + target.getName() + ".");
        return true;
    }

    private static boolean error(CommandSender sender, String message) {
        sender.sendMessage(ChatColor.RED + message);
        return true;
    }
}
