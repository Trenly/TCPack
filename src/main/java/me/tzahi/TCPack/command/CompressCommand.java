package me.tzahi.TCPack.command;

import java.util.Locale;
import me.tzahi.TCPack.compress.Compressor;
import me.tzahi.TCPack.compress.Recipes;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * TCPack by tzahi12345 (Tzahi) and TacoID; compression refactor by Trenly.
 * CC BY-SA 3.0 Unported; see LICENSE and NOTICE.
 */
public final class CompressCommand implements CommandExecutor {
    private final boolean enabled;
    private final Recipes recipes = new Recipes();
    private final Compressor compressor = new Compressor();

    public CompressCommand(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            return error(sender, "Compression can only be used by a player.");
        }
        if (!enabled) {
            return error(sender, "Compression is disabled.");
        }
        if (args.length > 1) {
            return error(sender, "Too many arguments. Use /" + label + " for help.");
        }
        String action = args.length == 0 ? "list" : args[0].toLowerCase(Locale.ROOT);
        if (!"list".equals(action) && !"wood".equals(action)
                && !"bonemeal".equals(action) && !"ores".equals(action)) {
            return error(sender, "Unknown compression option. Use /" + label + " for help.");
        }
        if (args.length == 1 && "list".equals(action)) {
            return error(sender, "Unknown compression option. Use /" + label + " for help.");
        }
        if (!sender.hasPermission("compress." + action)) {
            return error(sender, "You do not have permission: compress." + action);
        }
        if (args.length == 0) {
            sender.sendMessage(ChatColor.BLUE + "}---- Compress help ----{");
            sender.sendMessage(ChatColor.GOLD + "/" + label + ChatColor.BLUE + " - Lists compress commands.");
            sender.sendMessage(ChatColor.GOLD + "/" + label + " wood" + ChatColor.BLUE + " - Compresses wood.");
            sender.sendMessage(ChatColor.GOLD + "/" + label + " bonemeal" + ChatColor.BLUE + " - Compresses bonemeal.");
            sender.sendMessage(ChatColor.GOLD + "/" + label + " ores" + ChatColor.BLUE
                    + " - Compresses ingots, raw ores, and diamonds.");
            return true;
        }
        Compressor.Result result = compressor.compress(((Player) sender).getInventory(), recipes.forGroup(action));
        switch (result) {
            case CONVERTED:
                sender.sendMessage(ChatColor.DARK_GREEN + "[TCPack] Compression completed successfully.");
                return true;
            case NO_SPACE:
                return error(sender, "Not enough inventory space. No items were changed.");
            default:
                return error(sender, "No plain items in sufficient quantities to compress.");
        }
    }

    private boolean error(CommandSender sender, String message) {
        sender.sendMessage(ChatColor.RED + message);
        return true;
    }
}
