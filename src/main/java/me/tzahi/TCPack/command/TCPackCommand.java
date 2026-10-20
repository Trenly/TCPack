package me.tzahi.TCPack.command;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * TCPack by tzahi12345 (Tzahi) and TacoID; help adaptation by Trenly.
 * CC BY-SA 3.0 Unported; see LICENSE and NOTICE.
 */
public final class TCPackCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("tcpack.list")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission: tcpack.list");
            return true;
        }
        sender.sendMessage(ChatColor.BLUE + "}---- TCPack help ----{");
        sender.sendMessage(ChatColor.GOLD + "/fly" + ChatColor.BLUE + " - Get all fly commands.");
        sender.sendMessage(ChatColor.GOLD + "/compress" + ChatColor.BLUE + " - Get all compress commands.");
        sender.sendMessage(ChatColor.GOLD + "/ptime" + ChatColor.BLUE + " - Get all ptime commands.");
        return true;
    }
}
