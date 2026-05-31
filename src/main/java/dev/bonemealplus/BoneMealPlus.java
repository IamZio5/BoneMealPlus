package dev.bonemealplus;

import dev.bonemealplus.listeners.BoneMealListener;
import dev.bonemealplus.command.BoneMealCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class BoneMealPlus extends JavaPlugin {

    private static BoneMealPlus instance;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        registerListeners();
        registerCommands();
        getLogger().info("BoneMealPlus enabled! All plants can now be grown with bone meal.");
    }

    @Override
    public void onDisable() {
        getLogger().info("BoneMealPlus disabled.");
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new BoneMealListener(this), this);
    }

    private void registerCommands() {
        BoneMealCommand cmd = new BoneMealCommand(this);
        getCommand("bonemealplus").setExecutor(cmd);
        getCommand("bonemealplus").setTabCompleter(cmd);
    }

    public static BoneMealPlus getInstance() {
        return instance;
    }

    public void reloadConfiguration() {
        reloadConfig();
        getLogger().info("Configuration reloaded.");
    }
}
