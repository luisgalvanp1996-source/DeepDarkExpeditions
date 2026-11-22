
package dev.server404.deepdarkexpeditions;

import org.bukkit.configuration.file.FileConfiguration;

public final class ConfigManager {

    private final DeepDarkExpeditions plugin;

    private FileConfiguration config;

    public ConfigManager(DeepDarkExpeditions plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();

        config = plugin.getConfig();

        plugin.getLogger().info(
                "Configuracion cargada correctamente."
        );
    }

    public void reload() {
        plugin.reloadConfig();
        config = plugin.getConfig();

        plugin.getLogger().info(
                "Configuracion recargada correctamente."
        );
    }

    public void save() {
        plugin.saveConfig();
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public String getString(String path) {
        return config.getString(path);
    }

    public int getInt(String path) {
        return config.getInt(path);
    }

    public boolean getBoolean(String path) {
        return config.getBoolean(path);
    }

    public boolean contains(String path) {
        return config.contains(path);
    }
}
