package org.example;

import org.bukkit.plugin.java.JavaPlugin;
import org.example.commands.EventoZumbiCommand;
import org.example.listeners.EventoZumbiListeners;

public class Main extends JavaPlugin {

    private EventoZumbiManager manager;

    @Override
    public void onEnable() {
        inicializarPlugin();
    }

    @Override
    public void onDisable() {
        finalizarPlugin();
    }

    // ==================== MÉTODOS DE INICIALIZAÇÃO ====================

    private void inicializarPlugin() {
        salvarConfiguracaoPadrao();
        inicializarManager();
        registrarComandos();
        registrarListeners();

        getLogger().info(" EventoZumbi ativado com sucesso!");
    }

    private void salvarConfiguracaoPadrao() {
        saveDefaultConfig();
        getLogger().info(" Configuração padrão carregada.");
    }

    private void inicializarManager() {
        manager = new EventoZumbiManager(this);
        getLogger().info(" Manager inicializado.");
    }

    private void registrarComandos() {
        getCommand("eventozumbi").setExecutor(new EventoZumbiCommand(manager, this));
        getLogger().info(" Comandos registrados.");
    }

    private void registrarListeners() {
        getServer().getPluginManager().registerEvents(new EventoZumbiListeners(manager), this);
        getLogger().info(" Listeners registrados.");
    }

    // ==================== MÉTODOS DE FINALIZAÇÃO ====================

    private void finalizarPlugin() {
        if (manager != null) {
            finalizarEventoAtivo();
        }
        getLogger().info(" EventoZumbi desativado.");
    }

    private void finalizarEventoAtivo() {
        try {
            manager.finalizarEvento(false, "&c[EVENTO] &ePlugin desativado. Evento cancelado.");
        } catch (Exception e) {
            getLogger().warning(" Erro ao finalizar evento: " + e.getMessage());
        }
    }

    // ==================== GETTER PÚBLICO ====================

    public EventoZumbiManager getManager() {
        return manager;
    }
}