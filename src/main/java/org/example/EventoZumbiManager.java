package org.example;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Giant;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;

public class EventoZumbiManager {

    private boolean inscricoesAbertas = false;
    private boolean eventoAtivo = false;

    private Giant boss = null;
    private final Set<Player> participantes = new HashSet<>();

    private Location arena;

    private final Main plugin;

    public EventoZumbiManager(Main plugin) {
        this.plugin = plugin;
        carregarArenaDoConfig();
    }

    //=========== CONFIG ===========//

    public void reload() {
        plugin.reloadConfig();
        carregarArenaDoConfig();
    }

    private void carregarArenaDoConfig() {
        FileConfiguration config = plugin.getConfig();

        String mundo = config.getString("arena.mundo", "world");
        double x = config.getDouble("arena.x");
        double y = config.getDouble("arena.y");
        double z = config.getDouble("arena.z");
        float yaw = (float) config.getDouble("arena.yaw");
        float pitch = (float) config.getDouble("arena.pitch");

        arena = new Location(Bukkit.getWorld(mundo), x, y, z, yaw, pitch);
    }

    public void salvarArenaNoConfig() {
        FileConfiguration config = plugin.getConfig();

        config.set("arena.mundo", arena.getWorld().getName());
        config.set("arena.x", arena.getX());
        config.set("arena.y", arena.getY());
        config.set("arena.z", arena.getZ());
        config.set("arena.yaw", arena.getYaw());
        config.set("arena.pitch", arena.getPitch());

        plugin.saveConfig();
    }

    //=========== INSCRIÇÕES ===========//

    public void iniciarInscricoes() {
        if (eventoAtivo) {
            Bukkit.broadcastMessage("§cO evento já está ativo.");
            return;
        }

        inscricoesAbertas = true;
        participantes.clear();

        Bukkit.broadcastMessage("§a[EVENTO] Inscrições abertas! Use §e/eventozumbi entrar§a.");
    }

    public boolean isInscricoesAbertas() {
        return inscricoesAbertas;
    }

    public void entrar(Player p) {
        if (!inscricoesAbertas) {
            p.sendMessage("§cAs inscrições não estão abertas.");
            return;
        }

        if (participantes.contains(p)) {
            p.sendMessage("§eVocê já está inscrito.");
            return;
        }

        participantes.add(p);
        p.sendMessage("§aVocê entrou no evento!");
    }

    //=========== ARENA ===========//

    public void setArena(Player p) {
        arena = p.getLocation();
        salvarArenaNoConfig();
        p.sendMessage("§aArena definida!");
    }

    public Location getArena() {
        return arena;
    }

    //=========== EVENTO ===========//

    public boolean isEventoAtivo() {
        return eventoAtivo;
    }

    public void iniciarEvento() {
        if (participantes.isEmpty()) {
            Bukkit.broadcastMessage("§cNenhum jogador inscrito.");
            inscricoesAbertas = false;
            return;
        }

        inscricoesAbertas = false;
        eventoAtivo = true;

        Bukkit.broadcastMessage("§eTeleportando jogadores...");

        for (Player p : participantes) {
            p.teleport(arena);
        }

        spawnBoss();
    }

    private void spawnBoss() {
        boss = arena.getWorld().spawn(arena, Giant.class);
        Bukkit.broadcastMessage("§cO Zumbi Gigante apareceu!");
    }

    public Giant getBoss() {
        return boss;
    }

    public boolean isParticipante(Player p) {
        return participantes.contains(p);
    }

    public void jogadorMorreu(Player p) {
        participantes.remove(p);
        p.sendMessage("§cVocê morreu no evento!");

        if (participantes.isEmpty()) {
            Bukkit.broadcastMessage("§cTodos morreram! Evento encerrado.");
            encerrarEvento();
        }
    }

    public void morteBoss() {
        Bukkit.broadcastMessage("§aO boss foi derrotado!");
        encerrarEvento();
    }

    public void encerrarEvento() {
        eventoAtivo = false;
        inscricoesAbertas = false;
        participantes.clear();

        if (boss != null && !boss.isDead()) boss.remove();

        Bukkit.broadcastMessage("§eEvento finalizado.");
    }

    public Set<Player> getParticipantes() {
        return participantes;
    }

}
