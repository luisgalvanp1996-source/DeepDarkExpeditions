package dev.server404.deepdarkexpeditions;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class WorldManager {

    private final DeepDarkExpeditions plugin;

    public WorldManager(DeepDarkExpeditions plugin) {
        this.plugin = plugin;
    }

    /**
     * Obtener los mundos configurados.
     */
    public List<String> getConfiguredWorlds() {
        return plugin.getConfigManager()
                .getConfig()
                .getStringList("worlds.available");
    }

    /**
     * Detectar el estado de los mundos configurados.
     */
    public void detectWorlds() {

        plugin.getLogger().info("Comprobando mundos de expedición...");

        for (String name : getConfiguredWorlds()) {

            if (!isValidWorldName(name)) {
                plugin.getLogger().warning(
                        "[NOMBRE INVALIDO] " + name
                );
                continue;
            }

            World world = Bukkit.getWorld(name);
            File folder = new File(Bukkit.getWorldContainer(), name);

            if (world != null) {

                plugin.getLogger().info("[CARGADO] " + name);

            } else if (folder.isDirectory()) {

                plugin.getLogger().info(
                        "[DISPONIBLE] " + name + " - No cargado"
                );

            } else {

                plugin.getLogger().warning(
                        "[NO EXISTE] " + name
                );
            }
        }
    }

    /**
     * Cargar un mundo existente.
     * No genera mundos nuevos.
     */
    public World loadExistingWorld(String name) {

        if (!isValidWorldName(name)) {
            plugin.getLogger().warning(
                    "Nombre de mundo inválido: " + name
            );
            return null;
        }

        World loadedWorld = Bukkit.getWorld(name);

        if (loadedWorld != null) {
            return loadedWorld;
        }

        File folder = new File(Bukkit.getWorldContainer(), name);

        if (!folder.isDirectory()) {
            plugin.getLogger().warning(
                    "No se puede cargar " + name
                            + ": el mundo no existe."
            );
            return null;
        }

        plugin.getLogger().info(
                "Cargando mundo existente: " + name
        );

        World world = Bukkit.createWorld(
                new WorldCreator(name)
                        .environment(World.Environment.NORMAL)
        );

        if (world == null) {
            plugin.getLogger().severe(
                    "Error al cargar el mundo: " + name
            );
        }

        return world;
    }

    /**
     * Descargar un mundo.
     */
    public boolean unloadWorld(String name, boolean save) {

        World world = Bukkit.getWorld(name);

        if (world == null) {
            return false;
        }

        if (!world.getPlayers().isEmpty()) {
            plugin.getLogger().warning(
                    "No se puede descargar " + name
                            + ": todavía tiene jugadores."
            );
            return false;
        }

        boolean unloaded = Bukkit.unloadWorld(world, save);

        if (unloaded) {
            plugin.getLogger().info(
                    "Mundo descargado: " + name
            );
        }

        return unloaded;
    }

    /**
     * Obtener mundos actualmente cargados.
     */
    public List<String> getLoadedWorlds() {

        List<String> loaded = new ArrayList<>();

        for (World world : Bukkit.getWorlds()) {
            loaded.add(world.getName());
        }

        return loaded;
    }

    /**
     * Validar nombres para evitar rutas no permitidas.
     */
    private boolean isValidWorldName(String name) {
        return name != null
                && name.matches("[a-zA-Z0-9_-]+");
    }
}
