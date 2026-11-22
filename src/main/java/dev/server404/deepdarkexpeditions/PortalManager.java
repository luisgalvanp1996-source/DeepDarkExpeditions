
package dev.server404.deepdarkexpeditions;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PortalManager implements Listener {

    private final DeepDarkExpeditions plugin;

    private final Map<UUID, Long> cooldowns =
            new ConcurrentHashMap<>();

    private boolean particleWarningShown = false;

    public PortalManager(DeepDarkExpeditions plugin) {
        this.plugin = plugin;
    }

    public void register() {

        if (!plugin.getConfigManager()
                .getBoolean("portals.enabled")) {

            plugin.getLogger().info(
                    "Sistema de portales desactivado."
            );
            return;
        }

        Bukkit.getPluginManager().registerEvents(this, plugin);

        plugin.getLogger().info(
                "PortalManager registrado correctamente."
        );
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {

        Location from = event.getFrom();
        Location to = event.getTo();

        if (to == null || from.getWorld() == null
                || to.getWorld() == null) {
            return;
        }

        // Solo comprobar movimientos entre bloques.
        if (from.getWorld().equals(to.getWorld())
                && from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();

        // Solo activar al entrar desde fuera de la región.
        boolean enteredEntry =
                !isEntryPortal(from) && isEntryPortal(to);

        boolean enteredReturn =
                !isReturnPortal(from) && isReturnPortal(to);

        if (!enteredEntry && !enteredReturn) {
            return;
        }

        if (isOnCooldown(player)) {
            return;
        }

        if (enteredEntry) {
            handleEntry(player);
        } else {
            handleReturn(player);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        cooldowns.remove(event.getPlayer().getUniqueId());
    }

    private boolean isEntryPortal(Location location) {

        String worldName = plugin.getConfigManager()
                .getString("portals.entry.world");

        if (worldName == null
                || location.getWorld() == null
                || !location.getWorld().getName().equals(worldName)) {
            return false;
        }

        return isInsideRegion(
                location,
                "portals.entry.region"
        );
    }

    private boolean isReturnPortal(Location location) {

        if (location.getWorld() == null) {
            return false;
        }

        List<String> worlds = plugin.getConfigManager()
                .getConfig()
                .getStringList("portals.return.worlds");

        if (!worlds.contains(location.getWorld().getName())) {
            return false;
        }

        return isInsideRegion(
                location,
                "portals.return.region"
        );
    }

    private boolean isInsideRegion(
            Location location,
            String path
    ) {

        int minX = plugin.getConfigManager()
                .getInt(path + ".min.x");

        int minY = plugin.getConfigManager()
                .getInt(path + ".min.y");

        int minZ = plugin.getConfigManager()
                .getInt(path + ".min.z");

        int maxX = plugin.getConfigManager()
                .getInt(path + ".max.x");

        int maxY = plugin.getConfigManager()
                .getInt(path + ".max.y");

        int maxZ = plugin.getConfigManager()
                .getInt(path + ".max.z");

        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();

        return x >= minX && x <= maxX
                && y >= minY && y <= maxY
                && z >= minZ && z <= maxZ;
    }

    private void handleEntry(Player player) {

        if (!plugin.getExpeditionManager().isState(
                ExpeditionManager.State.ACTIVE
        )) {
            player.sendMessage(
                    "§cLa expedición no está disponible actualmente."
            );
            return;
        }

        String activeWorldName = plugin.getConfigManager()
                .getString("worlds.active");

        if (activeWorldName == null || activeWorldName.isBlank()) {
            player.sendMessage(
                    "§cNo hay un mundo de expedición activo."
            );
            return;
        }

        World activeWorld = Bukkit.getWorld(activeWorldName);

        if (activeWorld == null) {
            activeWorld = plugin.getWorldManager()
                    .loadExistingWorld(activeWorldName);
        }

        if (activeWorld == null) {
            player.sendMessage(
                    "§cNo se pudo cargar el mundo de expedición."
            );
            return;
        }

        Location destination;

        boolean useSpawn = plugin.getConfigManager()
                .getBoolean(
                        "portals.entry.destination.active-world-spawn"
                );

        if (useSpawn) {

            destination = activeWorld.getSpawnLocation();

        } else {

            String path = "portals.entry.destination.fallback";

            destination = new Location(
                    activeWorld,
                    plugin.getConfigManager().getConfig()
                            .getDouble(path + ".x"),
                    plugin.getConfigManager().getConfig()
                            .getDouble(path + ".y"),
                    plugin.getConfigManager().getConfig()
                            .getDouble(path + ".z"),
                    (float) plugin.getConfigManager().getConfig()
                            .getDouble(path + ".yaw"),
                    (float) plugin.getConfigManager().getConfig()
                            .getDouble(path + ".pitch")
            );
        }

        teleportPlayer(player, destination);
    }

    private void handleReturn(Player player) {

        String worldName = plugin.getConfigManager()
                .getString("portals.return.destination.world");

        if (worldName == null || worldName.isBlank()) {
            player.sendMessage(
                    "§cNo está configurado el mundo de regreso."
            );
            return;
        }

        World world = Bukkit.getWorld(worldName);

        if (world == null) {
            world = plugin.getWorldManager()
                    .loadExistingWorld(worldName);
        }

        if (world == null) {
            player.sendMessage(
                    "§cNo se pudo cargar el mundo de regreso."
            );
            return;
        }

        String path = "portals.return.destination";

        Location destination = new Location(
                world,
                plugin.getConfigManager().getConfig()
                        .getDouble(path + ".x"),
                plugin.getConfigManager().getConfig()
                        .getDouble(path + ".y"),
                plugin.getConfigManager().getConfig()
                        .getDouble(path + ".z"),
                (float) plugin.getConfigManager().getConfig()
                        .getDouble(path + ".yaw"),
                (float) plugin.getConfigManager().getConfig()
                        .getDouble(path + ".pitch")
        );

        teleportPlayer(player, destination);
    }

    private void teleportPlayer(
            Player player,
            Location destination
    ) {

        if (destination.getWorld() == null) {
            player.sendMessage(
                    "§cEl destino del portal no es válido."
            );
            return;
        }

        Location origin = player.getLocation();

        boolean teleported = player.teleport(destination);

        if (!teleported) {
            player.sendMessage(
                    "§cNo se pudo completar el teletransporte."
            );
            return;
        }

        playPortalParticles(origin);
        applyTransitionEffect(player);
        applyCooldown(player);
    }

    private void playPortalParticles(Location location) {

        if (location.getWorld() == null) {
            return;
        }

        int particleCount = plugin.getConfigManager()
                .getInt("portals.appearance.particle-count");

        String particleName = plugin.getConfigManager()
                .getString("portals.appearance.particle");

        if (particleCount <= 0 || particleName == null) {
            return;
        }

        try {

            Particle particle = Particle.valueOf(particleName);

            location.getWorld().spawnParticle(
                    particle,
                    location,
                    particleCount,
                    0.35,
                    0.5,
                    0.35,
                    0.02
            );

        } catch (IllegalArgumentException exception) {

            if (!particleWarningShown) {
                plugin.getLogger().warning(
                        "Partícula no válida: " + particleName
                );
                particleWarningShown = true;
            }
        }
    }

    private void applyTransitionEffect(Player player) {

        int duration = plugin.getConfigManager()
                .getInt(
                        "portals.transition.nausea-duration-ticks"
                );

        int amplifier = plugin.getConfigManager()
                .getInt(
                        "portals.transition.nausea-amplifier"
                );

        if (duration > 0) {
            player.addPotionEffect(
                    new PotionEffect(
                            PotionEffectType.NAUSEA,
                            duration,
                            Math.max(0, amplifier)
                    )
            );
        }
    }

    private boolean isOnCooldown(Player player) {

        Long expiration = cooldowns.get(player.getUniqueId());

        if (expiration == null) {
            return false;
        }

        if (System.currentTimeMillis() >= expiration) {
            cooldowns.remove(player.getUniqueId());
            return false;
        }

        return true;
    }

    private void applyCooldown(Player player) {

        int cooldownTicks = plugin.getConfigManager()
                .getInt("portals.cooldown-ticks");

        long cooldownMillis = Math.max(0, cooldownTicks) * 50L;

        cooldowns.put(
                player.getUniqueId(),
                System.currentTimeMillis() + cooldownMillis
        );
}
}
