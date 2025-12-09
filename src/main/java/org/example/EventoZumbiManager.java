package org.example;

import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Giant;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;

import java.util.*;

public class EventoZumbiManager {

    private final Main plugin;
    private Giant boss = null;

    // Estados do evento
    public boolean inscricoesAbertas = false;
    public boolean eventoAtivo = false;
    private boolean pvpPermitido;

    // Listas de jogadores
    private final Set<UUID> participantes = new HashSet<>();
    private final Set<UUID> mortos = new HashSet<>();
    private final Map<UUID, Double> danoPlayers = new HashMap<>();

    // Tasks
    private BukkitTask countdownTask;
    private BukkitTask bossAttackTask;
    private BukkitTask tempoEventoTask;

    public EventoZumbiManager(Main plugin) {
        this.plugin = plugin;
        this.pvpPermitido = plugin.getConfig().getBoolean("evento.pvp-habilitado", false);
    }

    // ==================== MÉTODOS PÚBLICOS PRINCIPAIS ====================

    public void iniciarInscricoes() {
        if (inscricoesAbertas || eventoAtivo) return;

        resetarEvento();
        inscricoesAbertas = true;

        int tempo = plugin.getConfig().getInt("tempo-entrada", 60);
        broadcastConfig("mensagens.evento-iniciado");
        broadcastConfig("mensagens.como-entrar", "%tempo%", String.valueOf(tempo));

        iniciarCountdown(tempo);
    }

    public boolean entrar(Player p) {
        if (!inscricoesAbertas) {
            enviarMensagem(p, "mensagens.sem-evento");
            return false;
        }

        UUID uuid = p.getUniqueId();
        if (participantes.contains(uuid)) {
            enviarMensagem(p, "mensagens.ja-esta");
            return false;
        }

        if (mortos.contains(uuid)) {
            enviarMensagem(p, "mensagens.morto-no-evento");
            return false;
        }

        participantes.add(uuid);
        enviarMensagem(p, "mensagens.entrou");
        return true;
    }

    public void jogadorMorreu(Player p) {
        UUID uuid = p.getUniqueId();
        mortos.add(uuid);
        participantes.remove(uuid);
        danoPlayers.remove(uuid);

        enviarMensagem(p, "mensagens.jogador-morreu");

        if (participantes.isEmpty()) {
            finalizarEvento(false, "mensagens.todos-morreram");
        }
    }

    public void morteBoss() {
        broadcastConfig("mensagens.boss-morto");
        distribuirPremiosRanking(true);
        finalizarEvento(true, "mensagens.evento-finalizado");
    }

    public void finalizarEvento(boolean bossMorto, String mensagem) {
        eventoAtivo = false;
        inscricoesAbertas = false;

        cancelarTasks();

        if (!bossMorto && !participantes.isEmpty()) {
            distribuirPremiosRanking(false);
        }

        if (boss != null && !boss.isDead()) {
            boss.remove();
        }

        resetarListas();
        broadcastMensagem(mensagem);
    }

    public void registrarDano(Player player, double dano) {
        if (!eventoAtivo || !participantes.contains(player.getUniqueId())) return;

        UUID uuid = player.getUniqueId();
        danoPlayers.put(uuid, danoPlayers.getOrDefault(uuid, 0.0) + dano);
    }

    // ==================== MÉTODOS PRIVADOS ====================

    private void iniciarEvento() {
        inscricoesAbertas = false;
        eventoAtivo = true;

        if (participantes.isEmpty()) {
            broadcastConfig("mensagens.cancelado-sem-jogadores");
            return;
        }

        broadcastConfig("mensagens.teleportando");
        teleportarParaArena();
        spawnBoss();
        iniciarTimerEvento();

        if (!pvpPermitido) {
            broadcastMensagem("&e[EVENTO] &fPvP está &cDESATIVADO &fdurante o evento!");
        }
    }

    private void iniciarCountdown(int tempo) {
        countdownTask = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            int t = tempo;

            @Override
            public void run() {
                if (t <= 0) {
                    countdownTask.cancel();
                    iniciarEvento();
                    return;
                }
                if (t == 60 || t == 30 || t == 10 || t <= 5) {
                    Bukkit.broadcastMessage(color("&e[EVENTO] Começa em " + t + " segundos!"));
                }
                t--;
            }
        }, 20L, 20L);
    }

    private void iniciarTimerEvento() {
        int tempoMaximo = plugin.getConfig().getInt("evento.tempo-maximo", 600);

        tempoEventoTask = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            int tempoRestante = tempoMaximo;

            @Override
            public void run() {
                if (!eventoAtivo) {
                    tempoEventoTask.cancel();
                    return;
                }

                if (tempoRestante <= 0) {
                    broadcastMensagem("&c[EVENTO] &eTempo esgotado! Evento finalizado.");
                    finalizarEventoPorTempo();
                    return;
                }

                if (tempoRestante == 300 || tempoRestante == 180 || tempoRestante == 60 ||
                        tempoRestante == 30 || tempoRestante <= 10) {
                    Bukkit.broadcastMessage(color("&e[EVENTO] &fTempo restante: &6" + formatarTempo(tempoRestante)));
                }

                tempoRestante--;
            }
        }, 20L, 20L);
    }

    private void finalizarEventoPorTempo() {
        finalizarEvento(false, "&e[EVENTO] &fEvento finalizado por tempo!");
    }

    private void teleportarParaArena() {
        Location arena = getArenaLocation();
        for (UUID uuid : participantes) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.teleport(arena);
                danoPlayers.put(uuid, 0.0);
            }
        }
    }

    private void spawnBoss() {
        Location loc = getArenaLocation();
        boss = (Giant) loc.getWorld().spawnEntity(loc, EntityType.GIANT);

        boss.setCustomName(color(getConfigString("boss.nome")));
        boss.setCustomNameVisible(true);

        double vida = getConfigDouble("boss.vida");
        boss.setMaxHealth(vida);
        boss.setHealth(vida);

        if (getConfigBoolean("habilitar-som-aparicao-boss")) {
            try {
                Sound sound = Sound.valueOf(getConfigString("som-boss"));
                loc.getWorld().playSound(loc, sound, 1.0f, 1.0f);
            } catch (IllegalArgumentException ignored) {}
        }

        broadcastConfig("mensagens.boss-apareceu");
        iniciarAtaquesBoss();
    }

    private void iniciarAtaquesBoss() {
        int intervalo = getConfigInt("boss.ataque-intervalo");
        int alcance = getConfigInt("boss.alcance-ataque");
        double dano = getConfigDouble("boss.dano");

        bossAttackTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (boss == null || boss.isDead()) return;

            for (UUID uuid : participantes) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null && p.getLocation().distance(boss.getLocation()) <= alcance) {
                    p.damage(dano);
                }
            }
        }, 40L, intervalo);
    }

    private void distribuirPremiosRanking(boolean bossMorto) {
        if (!getConfigBoolean("evento.ranking-habilitado") || danoPlayers.isEmpty()) return;

        List<Map.Entry<UUID, Double>> ranking = getRankingOrdenado();
        mostrarRanking(ranking);
        distribuirPremios(ranking, bossMorto);
    }

    private List<Map.Entry<UUID, Double>> getRankingOrdenado() {
        List<Map.Entry<UUID, Double>> ranking = new ArrayList<>(danoPlayers.entrySet());
        ranking.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        return ranking;
    }

    private void mostrarRanking(List<Map.Entry<UUID, Double>> ranking) {
        broadcastMensagem("&a════════════════════════════");
        broadcastMensagem("&6 &e&lRANKING DO EVENTO &6");
        broadcastMensagem("&a════════════════════════════");

        String[] icones = {"🥇", "🥈", "🥉"};
        for (int i = 0; i < Math.min(ranking.size(), 10); i++) {
            Map.Entry<UUID, Double> entry = ranking.get(i);
            Player p = Bukkit.getPlayer(entry.getKey());
            if (p != null) {
                String icone = i < 3 ? icones[i] : "&7" + (i + 1) + "º";
                String msg = String.format("%s &f%s &7(%.1f dano)",
                        i < 3 ? "&6" + icone : icone, p.getName(), entry.getValue());
                broadcastMensagem(msg);
            }
        }
        broadcastMensagem("&a════════════════════════════");
    }

    private void distribuirPremios(List<Map.Entry<UUID, Double>> ranking, boolean bossMorto) {
        String[] posicoes = {"primeiro", "segundo", "terceiro"};

        for (int i = 0; i < Math.min(ranking.size(), 3); i++) {
            Player p = Bukkit.getPlayer(ranking.get(i).getKey());
            if (p != null) {
                darPremio(p, posicoes[i], bossMorto);
                if (bossMorto) darDropsBoss(p, i + 1);
            }
        }

        // Premiação para participantes restantes
        if (bossMorto) {
            for (int i = 3; i < ranking.size(); i++) {
                Player p = Bukkit.getPlayer(ranking.get(i).getKey());
                if (p != null) {
                    darPremio(p, "participantes", true);
                }
            }
        }
    }

    private void darPremio(Player player, String tipoPremio, boolean bossMorto) {
        if (!bossMorto && "participantes".equals(tipoPremio)) return;

        String premiosConfig = getConfigString("evento.premios-ranking." + tipoPremio);
        if (premiosConfig.isEmpty()) return;

        for (String premio : premiosConfig.split(", ")) {
            try {
                String[] parts = premio.split(":");
                Material material = Material.getMaterial(parts[0]);
                int quantidade = Integer.parseInt(parts[1]);

                if (material != null) {
                    player.getInventory().addItem(new ItemStack(material, quantidade));
                    enviarMensagemDireta(player, "&a &fVocê recebeu: &e" + quantidade + "x " + material.name());
                }
            } catch (Exception ignored) {}
        }
    }

    private void darDropsBoss(Player player, int posicao) {
        if (!getConfigBoolean("boss.drop-customizado")) return;

        List<String> drops = plugin.getConfig().getStringList("boss.drops");
        for (String drop : drops) {
            try {
                String[] parts = drop.split(":");
                Material material = Material.getMaterial(parts[0]);
                int quantidadeTotal = Integer.parseInt(parts[1]);

                int porcentagem = 100;
                if (posicao == 2) porcentagem = 50;
                if (posicao == 3) porcentagem = 33;

                int quantidade = Math.max(1, (quantidadeTotal * porcentagem) / 100);
                if (material != null) {
                    player.getInventory().addItem(new ItemStack(material, quantidade));
                }
            } catch (Exception ignored) {}
        }
    }

    // ==================== MÉTODOS UTILITÁRIOS ====================

    private Location getArenaLocation() {
        World world = Bukkit.getWorld(getConfigString("arena.mundo"));
        return new Location(world,
                getConfigDouble("arena.x"),
                getConfigDouble("arena.y"),
                getConfigDouble("arena.z"),
                (float) getConfigDouble("arena.yaw"),
                (float) getConfigDouble("arena.pitch")
        );
    }

    private void cancelarTasks() {
        if (countdownTask != null) countdownTask.cancel();
        if (bossAttackTask != null) bossAttackTask.cancel();
        if (tempoEventoTask != null) tempoEventoTask.cancel();
    }

    private void resetarListas() {
        participantes.clear();
        mortos.clear();
        danoPlayers.clear();
        boss = null;
    }

    private void resetarEvento() {
        resetarListas();
        cancelarTasks();
    }

    private String formatarTempo(int segundos) {
        return String.format("%02d:%02d", segundos / 60, segundos % 60);
    }

    // ==================== MÉTODOS DE CONFIGURAÇÃO ====================

    private String getConfigString(String path) {
        return plugin.getConfig().getString(path, "");
    }

    private int getConfigInt(String path) {
        return plugin.getConfig().getInt(path);
    }

    private double getConfigDouble(String path) {
        return plugin.getConfig().getDouble(path);
    }

    private boolean getConfigBoolean(String path) {
        return plugin.getConfig().getBoolean(path, false);
    }

    // ==================== MÉTODOS DE MENSAGEM ====================

    private String color(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    private void broadcastConfig(String path) {
        String msg = getConfigString(path);
        if (!msg.isEmpty()) {
            Bukkit.broadcastMessage(color(msg));
        }
    }

    private void broadcastConfig(String path, String placeholder, String value) {
        String msg = getConfigString(path).replace(placeholder, value);
        if (!msg.isEmpty()) {
            Bukkit.broadcastMessage(color(msg));
        }
    }

    private void broadcastMensagem(String message) {
        if (!message.isEmpty()) {
            Bukkit.broadcastMessage(color(message));
        }
    }

    private void enviarMensagem(Player p, String path) {
        String msg = getConfigString(path);
        if (!msg.isEmpty()) {
            p.sendMessage(color(msg));
        }
    }

    private void enviarMensagemDireta(Player p, String message) {
        if (!message.isEmpty()) {
            p.sendMessage(color(message));
        }
    }

    // ==================== GETTERS E SETTERS ====================

    public boolean isEventoAtivo() { return eventoAtivo; }
    public boolean isInscricoesAbertas() { return inscricoesAbertas; }
    public boolean isPvpPermitido() { return pvpPermitido; }
    public Giant getBoss() { return boss; }
    public Set<UUID> getParticipantes() { return new HashSet<>(participantes); }
    public Map<UUID, Double> getDanoPlayers() { return new HashMap<>(danoPlayers); }
    public boolean isParticipante(Player p) { return participantes.contains(p.getUniqueId()); }

    public void setPvpPermitido(boolean permitido) {
        this.pvpPermitido = permitido;
        plugin.getConfig().set("evento.pvp-habilitado", permitido);
        plugin.saveConfig();
    }
}