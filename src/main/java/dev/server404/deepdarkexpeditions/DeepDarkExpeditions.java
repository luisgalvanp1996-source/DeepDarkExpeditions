package dev.server404.deepdarkexpeditions;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class DeepDarkExpeditions extends JavaPlugin {

    private ConfigManager configManager;
    private WorldManager worldManager;
    private ExpeditionManager expeditionManager;

    private static final List<String> DEPENDENCIES = List.of(
            "Denizen",
            "WorldEdit",
            "WorldGuard",
            "Multiverse-Core",
            "Multiverse-Portals",
            "Chunky"
    );

    @Override
    public void onEnable() {

        configManager = new ConfigManager(this);
        configManager.load();

        worldManager = new WorldManager(this);
        worldManager.detectWorlds();

        expeditionManager = new ExpeditionManager(this);

        getLogger().info("Configuración de expedición:");
        getLogger().info("Mapas: "
                + configManager.getConfig().getStringList("worlds.available"));
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

    private void checkDependencies() {

        getLogger().info("Comprobando plugins instalados...");

        for (String name : DEPENDENCIES) {

            Plugin plugin = getServer()
                    .getPluginManager()
                    .getPlugin(name);

            if (plugin != null && plugin.isEnabled()) {
                getLogger().info("[OK] " + name + " - "
                        + plugin.getDescription().getVersion());
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

            case "version" -> {
                sender.sendMessage(
                        "§8[§5DeepDark§8] §fVersión: §a"
                                + getDescription().getVersion()
                );
            }

            case "status" -> {

                sender.sendMessage("§8========== §5DeepDark Expeditions §8==========");
                sender.sendMessage("§fEstado de expedición: §e"
                        + expeditionManager.getStateName());
                sender.sendMessage("§fVersión: §a" + getDescription().getVersion());
                sender.sendMessage("§fServidor: §a" + getServer().getBukkitVersion());

                sender.sendMessage("§fDependencias:");

                for (String name : DEPENDENCIES) {

                    Plugin plugin = getServer()
                            .getPluginManager()
                            .getPlugin(name);

                    boolean enabled = plugin != null && plugin.isEnabled();

                    sender.sendMessage(
                            (enabled ? "§a[OK] " : "§c[FALTA] ")
                                    + "§f" + name
                    );
                }
            }

            case "transition" -> handleTransition(sender, args);

            default -> sendHelp(sender);
        }

        return true;
    }

    /**
     * Procesar una transición manual de estado.
     */
    private void handleTransition(CommandSender sender, String[] args) {

        if (!sender.hasPermission("deepdarkexpeditions.admin")) {
            sender.sendMessage("§cNo tienes permiso para utilizar este comando.");
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
     * Mostrar ayuda del comando.
     */
    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§8[§5DeepDark§8] §fComandos disponibles:");
        sender.sendMessage("§d/dde version §7- Versión del plugin");
        sender.sendMessage("§d/dde status §7- Estado del sistema");
        sender.sendMessage("§d/dde transition <estado> §7- Cambiar estado manualmente");
    }
}
