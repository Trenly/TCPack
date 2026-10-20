package me.tzahi.TCPack.command;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Personal-time command for TCPack, originally by tzahi12345 (Tzahi) and TacoID.
 * Adapted by Trenly for TCPack 2.0 under CC BY-SA 3.0 Unported; see LICENSE and NOTICE.
 */
public final class PtimeCommand implements CommandExecutor {
    private final boolean enabled;

    public PtimeCommand(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Personal time can only be used by a player.");
            return true;
        }

        Player player = (Player) sender;
        if (!enabled) {
            player.sendMessage(ChatColor.RED + "Personal time is disabled.");
            return true;
        }

        if (args.length == 0) {
            if (!requirePermission(player, "list")) {
                return true;
            }
            sendHelp(player, label);
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(ChatColor.RED + "Too many arguments. Use /" + label + " for help.");
            return true;
        }

        String action = args[0].toLowerCase(java.util.Locale.ROOT);
        if ("info".equals(action)) {
            if (!requirePermission(player, action)) {
                return true;
            }
            player.sendMessage(ChatColor.YELLOW + "Personal time: " + ChatColor.WHITE
                    + player.getPlayerTime() + " ticks; offset: " + player.getPlayerTimeOffset() + " ticks.");
            return true;
        }
        if ("reset".equals(action)) {
            if (!requirePermission(player, action)) {
                return true;
            }
            player.resetPlayerTime();
            player.sendMessage(ChatColor.GREEN + "Personal time reset to the world's time.");
            return true;
        }

        Long ticks = timeFor(action);
        if (ticks == null) {
            player.sendMessage(ChatColor.RED + "Unknown personal-time option. Use /" + label + " for help.");
            return true;
        }
        if (!requirePermission(player, action)) {
            return true;
        }

        player.setPlayerTime(ticks, false);
        player.sendMessage(ChatColor.GREEN + "Personal time set to " + action + " (" + ticks + " ticks).");
        return true;
    }

    private boolean requirePermission(Player player, String subcommand) {
        String permission = "ptime." + subcommand;
        if (!player.hasPermission(permission)) {
            player.sendMessage(ChatColor.RED + "You do not have permission: " + permission);
            return false;
        }
        return true;
    }

    private Long timeFor(String action) {
        if ("day".equals(action)) {
            return 6000L;
        }
        if ("night".equals(action)) {
            return 18000L;
        }
        if ("morning".equals(action)) {
            return 0L;
        }
        if ("dawn".equals(action)) {
            return 23000L;
        }
        if ("dusk".equals(action)) {
            return 12000L;
        }
        return null;
    }

    private void sendHelp(Player player, String label) {
        player.sendMessage(ChatColor.BLUE + "}---- PlayerTime help ----{");
        player.sendMessage(ChatColor.GOLD + "/" + label + " day" + ChatColor.BLUE
                + " - Sets your time to day.");
        player.sendMessage(ChatColor.GOLD + "/" + label + " night" + ChatColor.BLUE
                + " - Sets your time to night.");
        player.sendMessage(ChatColor.GOLD + "/" + label + " morning" + ChatColor.BLUE
                + " - Sets your time to morning.");
        player.sendMessage(ChatColor.GOLD + "/" + label + " dawn" + ChatColor.BLUE
                + " - Sets your time to sunrise.");
        player.sendMessage(ChatColor.GOLD + "/" + label + " dusk" + ChatColor.BLUE
                + " - Sets your time to sunset.");
        player.sendMessage(ChatColor.GOLD + "/" + label + " reset" + ChatColor.BLUE
                + " - Sets your time to the normal server-time.");
        player.sendMessage(ChatColor.GOLD + "/" + label + " info" + ChatColor.BLUE
                + " - Shows you information about the time.");
    }
}
