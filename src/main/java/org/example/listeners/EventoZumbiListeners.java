package org.example.listeners;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.example.EventoZumbiManager;

public class EventoZumbiListeners implements Listener {

    private final EventoZumbiManager manager;

    public EventoZumbiListeners(EventoZumbiManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageByEntityEvent e) {
        if (!manager.isEventoAtivo()) return;

        if (e.getDamager() instanceof Player) {
            handlePlayerAttacker(e, (Player) e.getDamager());
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent e) {
        if (!manager.isEventoAtivo()) return;

        Entity ent = e.getEntity();
        if (ent.equals(manager.getBoss())) {
            manager.morteBoss();
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();

        if (manager.isEventoAtivo() && manager.isParticipante(p)) {
            manager.jogadorMorreu(p);
            updateDeathMessage(e, p);
        }
    }

    // ==================== MÉTODOS AUXILIARES ====================

    private void handlePlayerAttacker(EntityDamageByEntityEvent e, Player attacker) {
        if (e.getEntity() instanceof Player) {
            handlePvP(e, attacker, (Player) e.getEntity());
        } else if (e.getEntity().equals(manager.getBoss())) {
            handleBossDamage(e, attacker);
        }
    }

    private void handlePvP(EntityDamageByEntityEvent e, Player attacker, Player target) {
        if (!manager.isParticipante(attacker) || !manager.isParticipante(target)) return;

        if (!manager.isPvpPermitido()) {
            e.setCancelled(true);
            attacker.sendMessage("§cPvP está desativado durante o evento!");
        }
    }

    private void handleBossDamage(EntityDamageByEntityEvent e, Player player) {
        if (manager.isParticipante(player)) {
            manager.registrarDano(player, e.getDamage());
        }
    }

    private void updateDeathMessage(PlayerDeathEvent e, Player victim) {
        EntityDamageEvent lastDamage = victim.getLastDamageCause();

        if (lastDamage instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent damageEvent = (EntityDamageByEntityEvent) lastDamage;

            if (damageEvent.getDamager() instanceof Player) {
                Player killer = (Player) damageEvent.getDamager();
                e.setDeathMessage("§c" + victim.getName() + " §7foi morto por §c" + killer.getName() + " §7no evento!");
            }
        }
    }
}