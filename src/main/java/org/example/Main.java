package org.example;

import org.bukkit.plugin.java.JavaPlugin;
import org.example.commands.EventoZumbiCommand;
import org.example.listeners.EventoZumbiListeners;

public class Main extends JavaPlugin {

    private EventoZumbiManager manager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        manager = new EventoZumbiManager(this);

        // Registrar comando
        getCommand("eventozumbi").setExecutor(new EventoZumbiCommand(manager));

        // Registrar listeners
        getServer().getPluginManager().registerEvents(new EventoZumbiListeners(manager), this);

        getLogger().info("EventoZumbi ativado!");
    }

    @Override
    public void onDisable() {
        getLogger().info("EventoZumbi desativado!");
    }

    public EventoZumbiManager getManager() {
        return manager;
    }
}
