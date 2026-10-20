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
        player.sendMessage(ChatColor.GOLD + "Personal time commands:");
        player.sendMessage(ChatColor.YELLOW + "/" + label + " day" + ChatColor.WHITE
                + " — Set personal time to day (6000 ticks; ptime.day).");
        player.sendMessage(ChatColor.YELLOW + "/" + label + " night" + ChatColor.WHITE
                + " — Set personal time to night (18000 ticks; ptime.night).");
        player.sendMessage(ChatColor.YELLOW + "/" + label + " morning" + ChatColor.WHITE
                + " — Set personal time to morning (0 ticks; ptime.morning).");
        player.sendMessage(ChatColor.YELLOW + "/" + label + " dawn" + ChatColor.WHITE
                + " — Set personal time to sunrise (23000 ticks; ptime.dawn).");
        player.sendMessage(ChatColor.YELLOW + "/" + label + " dusk" + ChatColor.WHITE
                + " — Set personal time to sunset (12000 ticks; ptime.dusk).");
        player.sendMessage(ChatColor.YELLOW + "/" + label + " reset" + ChatColor.WHITE
                + " — Reset personal time to the world's time (ptime.reset).");
        player.sendMessage(ChatColor.YELLOW + "/" + label + " info" + ChatColor.WHITE
                + " — Show personal time and offset in ticks (ptime.info).");
        player.sendMessage(ChatColor.GRAY + "Help permission: ptime.list.");
    }
}
