package de.imhenne187.baltop;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class Baltop extends JavaPlugin {

    @Override
    public void onEnable() {
        DatabaseManager.init(this);
        getCommand("baltoptest").setExecutor(new BaltopCommand());
        getServer().getPluginManager().registerEvents(new BaltopGUIListener(this), this);
        getServer().getPluginManager().registerEvents(new SignSearchHandler(this), this);
        BaltopGUI.init(this);

        DatabaseManager.updateAllPlayers();

        getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            DatabaseManager.updateAllPlayers();
        }, 300L, 300L);

        String startMessage = """
                \n\u001B[32m---------------------------
                \u001B[0mVicentVanillaBaltop Loaded!
                \u001B[32m---------------------------\u001B[0m
                """;
        Bukkit.getLogger().info(startMessage);
    }

    @Override
    public void onDisable() {
        DatabaseManager.close();
    }
}


