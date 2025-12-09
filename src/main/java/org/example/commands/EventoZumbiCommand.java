package org.example.commands;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.example.EventoZumbiManager;
import org.example.Main;

import java.util.*;

public class EventoZumbiCommand implements CommandExecutor {

    private final Main plugin;
    private final EventoZumbiManager manager;

    public EventoZumbiCommand(EventoZumbiManager manager, Main plugin) {
        this.plugin = plugin;
        this.manager = manager;
    }

    private String color(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            mostrarAjuda(sender);
            return true;
        }

        String comando = args[0].toLowerCase();

        switch (comando) {
            case "start":
                return processarStart(sender);
            case "entrar":
                return processarEntrar(sender);
            case "setarena":
                return processarSetArena(sender);
            case "reload":
                return processarReload(sender);
            case "status":
                return processarStatus(sender);
            case "pvp":
                return processarPvp(sender, args);
            case "ranking":
                return processarRanking(sender);
            default:
                return false;
        }
    }

    // ==================== PROCESSAMENTO DE COMANDOS ====================

    private boolean processarStart(CommandSender sender) {
        if (!temPermissaoAdmin(sender)) {
            return true;
        }

        manager.iniciarInscricoes();
        int tempo = plugin.getConfig().getInt("tempo-entrada", 60);

        sender.sendMessage(color("&a Inscrições abertas! Tempo: " + tempo + "s"));
        return true;
    }

    private boolean processarEntrar(CommandSender sender) {
        if (!eJogador(sender)) {
            return true;
        }

        Player p = (Player) sender;
        manager.entrar(p);
        return true;
    }

    private boolean processarSetArena(CommandSender sender) {
        if (!eJogador(sender) || !temPermissaoAdmin(sender)) {
            return true;
        }

        Player p = (Player) sender;
        Location loc = p.getLocation();

        plugin.getConfig().set("arena.mundo", loc.getWorld().getName());
        plugin.getConfig().set("arena.x", loc.getX());
        plugin.getConfig().set("arena.y", loc.getY());
        plugin.getConfig().set("arena.z", loc.getZ());
        plugin.getConfig().set("arena.yaw", loc.getYaw());
        plugin.getConfig().set("arena.pitch", loc.getPitch());
        plugin.saveConfig();

        sender.sendMessage(color("&a Arena definida com sucesso!"));
        return true;
    }

    private boolean processarReload(CommandSender sender) {
        if (!temPermissaoAdmin(sender)) {
            return true;
        }

        plugin.reloadConfig();
        sender.sendMessage(color("&a Configuração recarregada!"));
        return true;
    }

    private boolean processarStatus(CommandSender sender) {
        sender.sendMessage(color("&6===== &eSTATUS DO EVENTO &6====="));
        sender.sendMessage(color("&eInscrições abertas: &f" + manager.isInscricoesAbertas()));
        sender.sendMessage(color("&eEvento ativo: &f" + manager.isEventoAtivo()));
        sender.sendMessage(color("&ePvP permitido: &f" + manager.isPvpPermitido()));
        sender.sendMessage(color("&eParticipantes: &f" + manager.getParticipantes().size()));

        boolean bossVivo = manager.getBoss() != null && !manager.getBoss().isDead();
        sender.sendMessage(color("&eBoss vivo: &f" + bossVivo));

        return true;
    }

    private boolean processarPvp(CommandSender sender, String[] args) {
        if (!temPermissaoAdmin(sender)) {
            return true;
        }

        if (args.length < 2) {
            enviarStatusPvp(sender);
            return true;
        }

        String acao = args[1].toLowerCase();

        switch (acao) {
            case "on":
                manager.setPvpPermitido(true);
                sender.sendMessage(color("&a PvP ativado para o próximo evento!"));
                break;
            case "off":
                manager.setPvpPermitido(false);
                sender.sendMessage(color("&c PvP desativado para o próximo evento!"));
                break;
            case "status":
                enviarStatusPvp(sender);
                break;
            default:
                sender.sendMessage(color("&cUso: /eventozumbi pvp <on|off|status>"));
        }

        return true;
    }

    private boolean processarRanking(CommandSender sender) {
        Map<UUID, Double> ranking = manager.getDanoPlayers();

        if (ranking == null || ranking.isEmpty()) {
            sender.sendMessage(color("&eNenhum dano registrado no momento."));
            return true;
        }

        sender.sendMessage(color("&6===== &eRANKING ATUAL &6====="));

        // Criar e ordenar lista do ranking
        List<Map.Entry<UUID, Double>> entradas = new ArrayList<>(ranking.entrySet());
        entradas.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        int contador = 0;
        for (Map.Entry<UUID, Double> entrada : entradas) {
            if (contador >= 10) break;

            Player jogador = plugin.getServer().getPlayer(entrada.getKey());
            if (jogador != null) {
                String nome = jogador.getName();
                double dano = entrada.getValue();
                sender.sendMessage(color(String.format("&7- &f%s &8(&c%.1f dano&8)", nome, dano)));
                contador++;
            }
        }

        return true;
    }

    // ==================== MÉTODOS AUXILIARES ====================

    private void mostrarAjuda(CommandSender sender) {
        sender.sendMessage(color("&6===== &eEVENTO ZUMBI &6====="));
        sender.sendMessage(color("&e/eventozumbi start &7- Inicia evento (Admin)"));
        sender.sendMessage(color("&e/eventozumbi entrar &7- Entra no evento"));
        sender.sendMessage(color("&e/eventozumbi setarena &7- Define arena (Admin)"));
        sender.sendMessage(color("&e/eventozumbi status &7- Mostra status"));
        sender.sendMessage(color("&e/eventozumbi ranking &7- Mostra ranking"));
        sender.sendMessage(color("&e/eventozumbi pvp <on|off> &7- Controla PvP (Admin)"));
        sender.sendMessage(color("&e/eventozumbi reload &7- Recarrega config (Admin)"));
        sender.sendMessage(color("&6Aliases: &7/ez, /zumbi"));
    }

    private void enviarStatusPvp(CommandSender sender) {
        sender.sendMessage(color("&eUso: &7/eventozumbi pvp <on|off|status>"));

        String status = manager.isPvpPermitido() ? "&aON" : "&cOFF";
        sender.sendMessage(color("&eStatus atual: " + status));
    }

    private boolean temPermissaoAdmin(CommandSender sender) {
        if (sender.hasPermission("eventozumbi.admin")) {
            return true;
        }

        sender.sendMessage(color("&c Você não tem permissão para isso."));
        return false;
    }

    private boolean eJogador(CommandSender sender) {
        if (sender instanceof Player) {
            return true;
        }

        sender.sendMessage(color("&c Apenas jogadores podem usar este comando."));
        return false;
    }
}