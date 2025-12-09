package org.example.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.example.EventoZumbiManager;

public class EventoZumbiCommand implements CommandExecutor {

    private final EventoZumbiManager manager;

    public EventoZumbiCommand(EventoZumbiManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] args) {

        if (args.length == 0) {
            s.sendMessage("§e/eventozumbi start §7- abre inscrições");
            s.sendMessage("§e/eventozumbi entrar §7- entrar no evento");
            s.sendMessage("§e/eventozumbi setarena §7- define arena");
            s.sendMessage("§e/eventozumbi reload §7- recarrega config");
            s.sendMessage("§e/eventozumbi status §7- status do evento");
            return true;
        }

        switch (args[0].toLowerCase()) {

            case "iniciar":
                if (!s.hasPermission("eventozumbi.admin")) {
                    s.sendMessage("§cSem permissão.");
                    return true;
                }
                manager.iniciarEvento();
                return true;

            case "start":
                if (!s.hasPermission("eventozumbi.admin")) {
                    s.sendMessage("§cSem permissão.");
                    return true;
                }
                manager.iniciarInscricoes();
                return true;

            case "entrar":
                if (!(s instanceof Player)) {
                    s.sendMessage("Apenas jogadores.");
                    return true;
                }
                manager.entrar((Player) s);
                return true;

            case "setarena":
                if (!(s instanceof Player)) {
                    s.sendMessage("Apenas jogadores.");
                    return true;
                }
                if (!s.hasPermission("eventozumbi.admin")) {
                    s.sendMessage("§cSem permissão.");
                    return true;
                }
                manager.setArena((Player) s);
                return true;

            case "reload":
                if (!s.hasPermission("eventozumbi.admin")) {
                    s.sendMessage("§cSem permissão.");
                    return true;
                }
                manager.reload();
                s.sendMessage("§aConfig recarregada.");
                return true;

            case "status":
                s.sendMessage("§eInscrições abertas: " + (manager.isInscricoesAbertas() ? "§aSim" : "§cNão"));
                s.sendMessage("§eEvento ativo: " + (manager.isEventoAtivo() ? "§aSim" : "§cNão"));
                s.sendMessage("§eParticipantes: §b" + manager.getParticipantes().size());
                return true;

            default:
                s.sendMessage("§cSubcomando inválido.");
                return true;
        }
    }
}
