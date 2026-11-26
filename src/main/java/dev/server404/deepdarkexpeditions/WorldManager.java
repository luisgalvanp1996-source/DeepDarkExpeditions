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
     * Estados posibles de un mundo administrado.
     */
    public enum WorldStatus {
        LOADED,
        AVAILABLE,
        NOT_FOUND,
        INVALID_NAME,
        NOT_MANAGED,
        INVALID_DATA
    }

    /**
     * Obtener los mundos configurados.
     */
    public List<String> getConfiguredWorlds() {
        return new ArrayList<>(
                plugin.getConfigManager()
                        .getConfig()
                        .getStringList("worlds.available")
        );
    }

    /**
     * Comprobar si un mundo pertenece al sistema.
     */
    public boolean isManagedWorld(String name) {

        return isValidWorldName(name)
                && getConfiguredWorlds().contains(name);
    }

    /**
     * Obtener el nombre del mundo activo.
     */
    public String getActiveWorldName() {

        String name = plugin.getConfigManager()
                .getConfig()
                .getString("worlds.active", "");

        if (name == null || name.isBlank()) {
            return null;
        }

        return isManagedWorld(name) ? name : null;
    }

    /**
     * Obtener el nombre del mundo standby.
     */
    public String getStandbyWorldName() {

        String name = plugin.getConfigManager()
                .getConfig()
                .getString("worlds.standby", "");

        if (name == null || name.isBlank()) {
            return null;
        }

        return isManagedWorld(name) ? name : null;
    }

    /**
     * Obtener el mundo activo si está cargado.
     */
    public World getActiveWorld() {

        String name = getActiveWorldName();

        return name == null ? null : Bukkit.getWorld(name);
    }

    /**
     * Obtener el mundo standby si está cargado.
     */
    public World getStandbyWorld() {

        String name = getStandbyWorldName();

        return name == null ? null : Bukkit.getWorld(name);
    }

    /**
     * Detectar el estado de los mundos configurados.
     */
    public void detectWorlds() {

        plugin.getLogger().info(
                "Comprobando mundos de expedición..."
        );

        for (String name : getConfiguredWorlds()) {

            WorldStatus status = getWorldStatus(name);

            switch (status) {

                case LOADED -> plugin.getLogger().info(
                        "[CARGADO] " + name
                );

                case AVAILABLE -> plugin.getLogger().info(
                        "[DISPONIBLE] " + name + " - No cargado"
                );

                case NOT_FOUND -> plugin.getLogger().warning(
                        "[NO EXISTE] " + name
                );

                case INVALID_DATA -> plugin.getLogger().warning(
                        "[DATOS INVALIDOS] " + name
                );

                case INVALID_NAME -> plugin.getLogger().warning(
                        "[NOMBRE INVALIDO] " + name
                );

                case NOT_MANAGED -> plugin.getLogger().warning(
                        "[NO ADMINISTRADO] " + name
                );
            }
        }
    }

    /**
     * Consultar el estado de un mundo.
     */
    public WorldStatus getWorldStatus(String name) {

        if (!isValidWorldName(name)) {
            return WorldStatus.INVALID_NAME;
        }

        if (!isManagedWorld(name)) {
            return WorldStatus.NOT_MANAGED;
        }

        if (Bukkit.getWorld(name) != null) {
            return WorldStatus.LOADED;
        }

        File folder = getWorldFolder(name);

        if (!folder.exists()) {
            return WorldStatus.NOT_FOUND;
        }

        if (!folder.isDirectory()) {
            return WorldStatus.INVALID_DATA;
        }

        File levelData = new File(folder, "level.dat");

        if (!levelData.isFile()) {
            return WorldStatus.INVALID_DATA;
        }

        return WorldStatus.AVAILABLE;
    }

    /**
     * Crear un mundo nuevo.
     *
     * Generación:
     * - Chunks vacíos.
     * - Bioma DEEP_DARK.
     * - Sin estructuras vanilla.
     * - Sin terreno natural.
     *
     * Nunca sobrescribe mundos existentes.
     */
    public World createWorld(String name) {

        if (!isValidWorldName(name)) {
            plugin.getLogger().warning(
                    "Nombre de mundo inválido: " + name
            );
            return null;
        }

        if (!isManagedWorld(name)) {
            plugin.getLogger().warning(
                    "No se puede crear " + name
                            + ": no está en worlds.available."
            );
            return null;
        }

        if (Bukkit.getWorld(name) != null) {
            plugin.getLogger().warning(
                    "No se puede crear " + name
                            + ": ya está cargado."
            );
            return null;
        }

        File folder = getWorldFolder(name);

        if (folder.exists()) {
            plugin.getLogger().warning(
                    "No se puede crear " + name
                            + ": ya existe una carpeta con ese nombre."
            );
            return null;
        }

        plugin.getLogger().info(
                "Creando mundo vacío Deep Dark: " + name
        );

        WorldCreator creator = new WorldCreator(name)
                .environment(World.Environment.NORMAL)
                .generator(new EmptyChunkGenerator())
                .biomeProvider(new DeepDarkBiomeProvider());

        World world = Bukkit.createWorld(creator);

        if (world == null) {
            plugin.getLogger().severe(
                    "No se pudo crear el mundo: " + name
            );
            return null;
        }

        plugin.getLogger().info(
                "Mundo Deep Dark creado correctamente: " + name
        );

        return world;
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

        if (!isManagedWorld(name)) {
            plugin.getLogger().warning(
                    "No se puede cargar " + name
                            + ": no está administrado por el plugin."
            );
            return null;
        }

        World loadedWorld = Bukkit.getWorld(name);

        if (loadedWorld != null) {
            return loadedWorld;
        }

        WorldStatus status = getWorldStatus(name);

        if (status != WorldStatus.AVAILABLE) {
            plugin.getLogger().warning(
                    "No se puede cargar " + name
                            + ". Estado: " + status
            );
            return null;
        }

        plugin.getLogger().info(
                "Cargando mundo existente: " + name
        );

        WorldCreator creator = new WorldCreator(name)
                .environment(World.Environment.NORMAL)
                .generator(new EmptyChunkGenerator())
                .biomeProvider(new DeepDarkBiomeProvider());

        World world = Bukkit.createWorld(creator);

        if (world == null) {
            plugin.getLogger().severe(
                    "Error al cargar el mundo: " + name
            );
            return null;
        }

        plugin.getLogger().info(
                "Mundo cargado correctamente: " + name
        );

        return world;
    }

    /**
     * Descargar un mundo.
     */
    public boolean unloadWorld(String name, boolean save) {

        if (!isManagedWorld(name)) {
            plugin.getLogger().warning(
                    "No se puede descargar " + name
                            + ": no está administrado por el plugin."
            );
            return false;
        }

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
        } else {
            plugin.getLogger().warning(
                    "No se pudo descargar el mundo: " + name
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
     * Obtener mundos administrados que están cargados.
     */
    public List<String> getManagedLoadedWorlds() {

        List<String> loaded = new ArrayList<>();

        for (String name : getConfiguredWorlds()) {

            if (Bukkit.getWorld(name) != null) {
                loaded.add(name);
            }
        }

        return loaded;
    }

    /**
     * Obtener la carpeta de un mundo.
     */
    private File getWorldFolder(String name) {

        return new File(
                Bukkit.getWorldContainer(),
                name
        );
    }

    /**
     * Validar nombres para evitar rutas no permitidas.
     */
    private boolean isValidWorldName(String name) {

        return name != null
                && name.matches("[a-zA-Z0-9_-]+");
    }
}