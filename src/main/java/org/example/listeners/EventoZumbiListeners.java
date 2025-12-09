package org.example.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.example.EventoZumbiManager;

public class EventoZumbiListeners implements Listener {

    private final EventoZumbiManager manager;

    public EventoZumbiListeners(EventoZumbiManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent e) {
        if (!manager.isEventoAtivo() || manager.getBoss() == null) return;

        if (e.getEntity().getUniqueId().equals(manager.getBoss().getUniqueId())) {
            manager.morteBoss();
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();

        if (manager.isEventoAtivo() && manager.isParticipante(p)) {
            manager.jogadorMorreu(p);
        }
    }
}
