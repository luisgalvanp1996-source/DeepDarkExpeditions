
package dev.server404.deepdarkexpeditions;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.List;

public final class DeepDarkExpeditions extends JavaPlugin {

    private ConfigManager configManager;
    private WorldManager worldManager;
    private ExpeditionManager expeditionManager;
    private PortalManager portalManager;
    private MapManager mapManager;

    private static final List<String> DEPENDENCIES = List.of(
            "Denizen",
            "WorldEdit",
            "WorldGuard",
            "Multiverse-Core",
            "Chunky"
    );

    @Override
    public void onEnable() {

        configManager = new ConfigManager(this);
        configManager.load();

        File mapsFile = new File(getDataFolder(), "maps.yml");

        if (!mapsFile.exists()) {
            saveResource("maps.yml", false);
        }

        mapManager = new MapManager(this);
        mapManager.load();

        worldManager = new WorldManager(this);
        worldManager.detectWorlds();

        expeditionManager = new ExpeditionManager(this);

        portalManager = new PortalManager(this);
        portalManager.register();

        getLogger().info("Configuración de expedición:");
        getLogger().info("Mundos configurados: "
                + configManager.getConfig().getStringList("worlds.available"));
        getLogger().info("Mapas registrados: "
                + mapManager.getMapCount());
        getLogger().info("Duración: "
                + configManager.getInt("expedition.duration") + " minutos");
        getLogger().info("Rotación automática: "
                + configManager.getBoolean("rotation.enabled"));

        getLogger().info("------------------------------------");
        getLogger().info(" DeepDarkExpeditions v" + getDescription().getVersion());
        getLogger().info(" Plugin iniciado correctamente.");
        getLogger().info("------------------------------------");

        if (getCommand("dde") != null) {
            getCommand("dde").setExecutor(this);
        }

        checkDependencies();
    }

    @Override
    public void onDisable() {
        getLogger().info("DeepDarkExpeditions desactivado.");
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public ExpeditionManager getExpeditionManager() {
        return expeditionManager;
    }

    public WorldManager getWorldManager() {
        return worldManager;
    }

    public PortalManager getPortalManager() {
        return portalManager;
    }

    public MapManager getMapManager() {
        return mapManager;
    }

    private void checkDependencies() {

        getLogger().info("Comprobando plugins instalados...");

        for (String name : DEPENDENCIES) {

            Plugin dependency = getServer()
                    .getPluginManager()
                    .getPlugin(name);

            if (dependency != null && dependency.isEnabled()) {
                getLogger().info("[OK] " + name + " - "
                        + dependency.getDescription().getVersion());
            } else {
                getLogger().warning("[NO DISPONIBLE] " + name);
            }
        }
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {

            case "version" -> sender.sendMessage(
                    "§8[§5DeepDark§8] §fVersión: §a"
                            + getDescription().getVersion()
            );

            case "status" -> handleStatus(sender);

            case "transition" -> handleTransition(sender, args);

            case "worlds" -> handleWorlds(sender);

            case "world" -> handleWorldCommand(sender, args);

            default -> sendHelp(sender);
        }

        return true;
    }

    /**
     * Mostrar estado general del sistema.
     */
    private void handleStatus(CommandSender sender) {

        sender.sendMessage("§8========== §5DeepDark Expeditions §8==========");
        sender.sendMessage("§fEstado de expedición: §e"
                + expeditionManager.getStateName());
        sender.sendMessage("§fVersión: §a" + getDescription().getVersion());
        sender.sendMessage("§fServidor: §a" + getServer().getBukkitVersion());
        sender.sendMessage("§fMapas registrados: §a"
                + mapManager.getMapCount());

        sender.sendMessage("§fDependencias:");

        for (String name : DEPENDENCIES) {

            Plugin dependency = getServer()
                    .getPluginManager()
                    .getPlugin(name);

            boolean enabled = dependency != null && dependency.isEnabled();

            sender.sendMessage(
                    (enabled ? "§a[OK] " : "§c[FALTA] ")
                            + "§f" + name
            );
        }
    }

    /**
     * Mostrar estado de los mundos administrados.
     */
    private void handleWorlds(CommandSender sender) {

        if (!checkAdminPermission(sender)) {
            return;
        }

        sender.sendMessage("§8========== §5Mundos de expedición §8==========");

        String active = worldManager.getActiveWorldName();
        String standby = worldManager.getStandbyWorldName();

        for (String name : worldManager.getConfiguredWorlds()) {

            WorldManager.WorldStatus status =
                    worldManager.getWorldStatus(name);

            String role = "§7Sin asignar";

            if (name.equals(active)) {
                role = "§aACTIVO";
            } else if (name.equals(standby)) {
                role = "§bSTANDBY";
            }

            String statusText = switch (status) {
                case LOADED -> "§aCARGADO";
                case AVAILABLE -> "§eDISPONIBLE";
                case NOT_FOUND -> "§cNO EXISTE";
                case INVALID_DATA -> "§cDATOS INVÁLIDOS";
                case INVALID_NAME -> "§cNOMBRE INVÁLIDO";
                case NOT_MANAGED -> "§cNO ADMINISTRADO";
            };

            sender.sendMessage(
                    "§f" + name + " §8| " + role
                            + " §8| " + statusText
            );

            World world = Bukkit.getWorld(name);

            if (world != null) {
                sender.sendMessage(
                        "  §7Jugadores: §f" + world.getPlayers().size()
                );
            }
        }
    }

    /**
     * Procesar comandos de administración de mundos.
     */
    private void handleWorldCommand(CommandSender sender, String[] args) {

        if (!checkAdminPermission(sender)) {
            return;
        }

        if (args.length < 3) {
            sendWorldHelp(sender);
            return;
        }

        String action = args[1].toLowerCase();
        String worldName = args[2];

        if (!worldManager.isManagedWorld(worldName)) {
            sender.sendMessage(
                    "§cEl mundo '" + worldName
                            + "' no está registrado en worlds.available."
            );
            return;
        }

        switch (action) {

            case "create" -> createManagedWorld(sender, worldName);

            case "load" -> loadManagedWorld(sender, worldName);

            case "unload" -> unloadManagedWorld(sender, worldName);

            default -> sendWorldHelp(sender);
        }
    }

    /**
     * Crear un mundo vacío Deep Dark.
     */
    private void createManagedWorld(
            CommandSender sender,
            String worldName
    ) {

        WorldManager.WorldStatus status =
                worldManager.getWorldStatus(worldName);

        if (status != WorldManager.WorldStatus.NOT_FOUND) {
            sender.sendMessage(
                    "§cNo se puede crear " + worldName
                            + ". Estado actual: " + status
            );
            return;
        }

        World world = worldManager.createWorld(worldName);

        if (world != null) {
            sender.sendMessage(
                    "§aMundo creado correctamente: §f" + worldName
            );
            sender.sendMessage(
                    "§7Generación: vacío | Bioma: DEEP_DARK"
            );
        } else {
            sender.sendMessage(
                    "§cNo se pudo crear el mundo: " + worldName
            );
        }
    }

    /**
     * Cargar un mundo existente.
     */
    private void loadManagedWorld(
            CommandSender sender,
            String worldName
    ) {

        World existing = Bukkit.getWorld(worldName);

        if (existing != null) {
            sender.sendMessage(
                    "§eEl mundo ya está cargado: " + worldName
            );
            return;
        }

        World world = worldManager.loadExistingWorld(worldName);

        if (world != null) {
            sender.sendMessage(
                    "§aMundo cargado correctamente: §f" + worldName
            );
        } else {
            sender.sendMessage(
                    "§cNo se pudo cargar el mundo: " + worldName
            );
        }
    }

    /**
     * Descargar un mundo de forma segura.
     */
    private void unloadManagedWorld(
            CommandSender sender,
            String worldName
    ) {

        String active = worldManager.getActiveWorldName();

        if (worldName.equals(active)) {
            sender.sendMessage(
                    "§cNo puedes descargar el mundo activo."
            );
            return;
        }

        World world = Bukkit.getWorld(worldName);

        if (world == null) {
            sender.sendMessage(
                    "§eEl mundo no está cargado: " + worldName
            );
            return;
        }

        if (!world.getPlayers().isEmpty()) {
            sender.sendMessage(
                    "§cNo se puede descargar el mundo: todavía tiene jugadores."
            );
            return;
        }

        boolean success = worldManager.unloadWorld(worldName, true);

        if (success) {
            sender.sendMessage(
                    "§aMundo descargado correctamente: §f" + worldName
            );
        } else {
            sender.sendMessage(
                    "§cNo se pudo descargar el mundo: " + worldName
            );
        }
    }

    /**
     * Procesar una transición manual de estado.
     */
    private void handleTransition(CommandSender sender, String[] args) {

        if (!checkAdminPermission(sender)) {
            return;
        }

        if (args.length < 2) {
            sender.sendMessage("§cUso: /dde transition <estado>");
            return;
        }

        ExpeditionManager.State targetState;

        try {
            targetState = ExpeditionManager.State.valueOf(
                    args[1].toUpperCase()
            );
        } catch (IllegalArgumentException exception) {
            sender.sendMessage("§cEstado desconocido: " + args[1]);
            sender.sendMessage(
                    "§7Estados: PREPARING, ACTIVE, EXTRACTION, EVACUATING, RESETTING"
            );
            return;
        }

        ExpeditionManager.State previousState =
                expeditionManager.getState();

        boolean success = expeditionManager.transitionTo(targetState);

        if (success) {
            sender.sendMessage(
                    "§8[§5DeepDark§8] §aEstado cambiado: §f"
                            + previousState + " §7→ §a" + targetState
            );
        } else {
            sender.sendMessage(
                    "§cTransición no permitida: "
                            + previousState + " → " + targetState
            );
        }
    }

    /**
     * Comprobar permiso administrativo.
     */
    private boolean checkAdminPermission(CommandSender sender) {

        if (!sender.hasPermission("deepdarkexpeditions.admin")) {
            sender.sendMessage(
                    "§cNo tienes permiso para utilizar este comando."
            );
            return false;
        }

        return true;
    }

    /**
     * Mostrar ayuda general.
     */
    private void sendHelp(CommandSender sender) {

        sender.sendMessage("§8[§5DeepDark§8] §fComandos disponibles:");
        sender.sendMessage("§d/dde version §7- Versión del plugin");
        sender.sendMessage("§d/dde status §7- Estado del sistema");
        sender.sendMessage("§d/dde transition <estado> §7- Cambiar estado");
        sender.sendMessage("§d/dde worlds §7- Mostrar mundos administrados");
        sender.sendMessage("§d/dde world §7- Administración de mundos");
    }

    /**
     * Mostrar ayuda de administración de mundos.
     */
    private void sendWorldHelp(CommandSender sender) {

        sender.sendMessage("§8[§5DeepDark§8] §fAdministración de mundos:");
        sender.sendMessage("§d/dde worlds §7- Consultar mundos");
        sender.sendMessage("§d/dde world create <nombre> §7- Crear mundo");
        sender.sendMessage("§d/dde world load <nombre> §7- Cargar mundo");
        sender.sendMessage("§d/dde world unload <nombre> §7- Descargar mundo");
    }
}
