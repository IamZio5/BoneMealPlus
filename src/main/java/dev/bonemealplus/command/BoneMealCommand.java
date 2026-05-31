package dev.bonemealplus.command;

import dev.bonemealplus.BoneMealPlus;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.List;

public class BoneMealCommand implements CommandExecutor, TabCompleter {

    private final BoneMealPlus plugin;

    public BoneMealCommand(BoneMealPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("bonemealplus.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfiguration();
            sender.sendMessage(ChatColor.GREEN + "[BoneMealPlus] " + ChatColor.WHITE + "Configuration reloaded successfully.");
            return true;
        }

        sender.sendMessage(ChatColor.GOLD + "=== BoneMealPlus ===" + ChatColor.RESET);
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " reload" + ChatColor.WHITE + " – reload config.yml");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("reload");
        }
        return List.of();
    }
}
