
package dev.server404.deepdarkexpeditions;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MapManager {

    private static final List<String> REQUIRED_ZONES = List.of(
            "outer_ruins",
            "deep_galleries",
            "forgotten_sanctuary",
            "ancestral_chambers",
            "abyss_heart"
    );

    private final DeepDarkExpeditions plugin;

    private final Map<String, MapDefinition> maps =
            new LinkedHashMap<>();

    private File mapsFile;
    private YamlConfiguration configuration;

    public MapManager(DeepDarkExpeditions plugin) {
        this.plugin = plugin;
    }

    public void load() {

        maps.clear();

        mapsFile = new File(
                plugin.getDataFolder(),
                "maps.yml"
        );

        if (!mapsFile.exists()) {
            plugin.getLogger().severe(
                    "No se encontró maps.yml. "
                            + "No se pueden registrar mapas."
            );
            return;
        }

        configuration = YamlConfiguration.loadConfiguration(
                mapsFile
        );

        ConfigurationSection mapsSection =
                configuration.getConfigurationSection("maps");

        if (mapsSection == null) {
            plugin.getLogger().warning(
                    "maps.yml no contiene la sección 'maps'."
            );
            return;
        }

        plugin.getLogger().info(
                "Validando manifiestos de mapas..."
        );

        for (String id : mapsSection.getKeys(false)) {

            ConfigurationSection mapSection =
                    mapsSection.getConfigurationSection(id);

            if (mapSection == null) {
                plugin.getLogger().warning(
                        "[MAPA INVALIDO] " + id
                                + ": configuración incorrecta."
                );
                continue;
            }

            MapDefinition definition =
                    validateMap(id, mapSection);

            if (definition != null) {
                maps.put(id, definition);

                plugin.getLogger().info(
                        "[MAPA VALIDO] " + id
                                + " - " + definition.displayName()
                );
            }
        }

        plugin.getLogger().info(
                "Registro de mapas finalizado: "
                        + maps.size() + " mapa(s) válido(s)."
        );
    }

    private MapDefinition validateMap(
            String id,
            ConfigurationSection section
    ) {

        List<String> errors = new ArrayList<>();

        if (id == null || !id.matches("[a-zA-Z0-9_-]+")) {
            errors.add("Identificador inválido.");
        }

        String displayName = section.getString("display-name");
        String schematic = section.getString("schematic");

        if (displayName == null || displayName.isBlank()) {
            errors.add("Falta display-name.");
        }

        if (schematic == null || schematic.isBlank()) {
            errors.add("Falta schematic.");
        }

        Bounds bounds = readBounds(
                section,
                "bounds",
                errors
        );

        Point entry = readPointAt(
                section,
                "entry",
                errors
        );

        Map<String, ZoneDefinition> zones =
                new LinkedHashMap<>();

        ConfigurationSection zonesSection =
                section.getConfigurationSection("zones");

        if (zonesSection == null) {

            errors.add("Falta la sección zones.");

        } else {

            for (String zoneId : REQUIRED_ZONES) {

                ConfigurationSection zoneSection =
                        zonesSection.getConfigurationSection(zoneId);

                if (zoneSection == null) {
                    errors.add(
                            "Falta la zona obligatoria: " + zoneId
                    );
                    continue;
                }

                Bounds zoneBounds = readBounds(
                        zoneSection,
                        "bounds",
                        errors,
                        "zones." + zoneId
                );

                if (zoneBounds != null) {
                    zones.put(
                            zoneId,
                            new ZoneDefinition(zoneId, zoneBounds)
                    );
                }
            }
        }

        List<Point> wardenSpawns = readPointCollection(
                section,
                "warden-spawns",
                errors
        );

        Point finalChest = readPointAt(
                section,
                "final-chest",
                errors
        );

        Point extraction = readPointAt(
                section,
                "extraction",
                errors
        );

        List<Point> safeSpawns = readPointCollection(
                section,
                "safe-spawns",
                errors
        );

        if (bounds != null && zones.size() == REQUIRED_ZONES.size()) {

            for (ZoneDefinition zone : zones.values()) {

                if (!bounds.contains(zone.bounds())) {
                    errors.add(
                            "La zona " + zone.id()
                                    + " está fuera de los límites del mapa."
                    );
                }
            }
        }

        if (!errors.isEmpty()) {

            plugin.getLogger().warning(
                    "[MAPA INVALIDO] " + id
            );

            for (String error : errors) {
                plugin.getLogger().warning(
                        "  - " + error
                );
            }

            return null;
        }

        return new MapDefinition(
                id,
                displayName,
                schematic,
                bounds,
                entry,
                zones,
                wardenSpawns,
                finalChest,
                extraction,
                safeSpawns
        );
    }

    private List<Point> readPointCollection(
            ConfigurationSection parent,
            String path,
            List<String> errors
    ) {

        ConfigurationSection section =
                parent.getConfigurationSection(path);

        List<Point> points = new ArrayList<>();

        if (section == null) {
            errors.add("Falta la sección " + path + ".");
            return points;
        }

        for (String key : section.getKeys(false)) {

            ConfigurationSection pointSection =
                    section.getConfigurationSection(key);

            if (pointSection == null) {
                errors.add(
                        path + "." + key + " no es un punto válido."
                );
                continue;
            }

            Point point = readPointSection(
                    pointSection,
                    path + "." + key,
                    errors
            );

            if (point != null) {
                points.add(point);
            }
        }

        if (points.isEmpty()) {
            errors.add(
                    "La sección " + path + " no contiene puntos."
            );
        }

        return points;
    }

    private Point readPointAt(
            ConfigurationSection parent,
            String path,
            List<String> errors
    ) {

        ConfigurationSection section =
                parent.getConfigurationSection(path);

        if (section == null) {
            errors.add("Falta el punto " + path + ".");
            return null;
        }

        return readPointSection(section, path, errors);
    }

    private Point readPointSection(
            ConfigurationSection section,
            String path,
            List<String> errors
    ) {

        if (!isNumber(section, "x")
                || !isNumber(section, "y")
                || !isNumber(section, "z")) {

            errors.add(
                    "Coordenadas x, y, z inválidas en " + path + "."
            );
            return null;
        }

        double x = section.getDouble("x");
        double y = section.getDouble("y");
        double z = section.getDouble("z");

        float yaw = (float) section.getDouble("yaw", 0.0);
        float pitch = (float) section.getDouble("pitch", 0.0);

        return new Point(x, y, z, yaw, pitch);
    }

    private Bounds readBounds(
            ConfigurationSection parent,
            String path,
            List<String> errors
    ) {

        ConfigurationSection section =
                parent.getConfigurationSection(path);

        if (section == null) {
            errors.add("Faltan los límites " + path + ".");
            return null;
        }

        return readBounds(section, "", errors, path);
    }

    private Bounds readBounds(
            ConfigurationSection section,
            String path,
            List<String> errors,
            String label
    ) {

        ConfigurationSection min =
                section.getConfigurationSection(
                        path.isEmpty() ? "min" : path + ".min"
                );

        ConfigurationSection max =
                section.getConfigurationSection(
                        path.isEmpty() ? "max" : path + ".max"
                );

        if (min == null || max == null) {
            errors.add(
                    "Faltan min/max en " + label + "."
            );
            return null;
        }

        if (!isNumber(min, "x") || !isNumber(min, "y")
                || !isNumber(min, "z")
                || !isNumber(max, "x") || !isNumber(max, "y")
                || !isNumber(max, "z")) {

            errors.add(
                    "Coordenadas min/max inválidas en " + label + "."
            );
            return null;
        }

        Bounds bounds = new Bounds(
                min.getInt("x"),
                min.getInt("y"),
                min.getInt("z"),
                max.getInt("x"),
                max.getInt("y"),
                max.getInt("z")
        );

        if (bounds.minX() > bounds.maxX()
                || bounds.minY() > bounds.maxY()
                || bounds.minZ() > bounds.maxZ()) {

            errors.add(
                    "Los límites min/max están invertidos en "
                            + label + "."
            );
            return null;
        }

        return bounds;
    }

    private boolean isNumber(
            ConfigurationSection section,
            String path
    ) {
        return section.get(path) instanceof Number;
    }

    public MapDefinition getMap(String id) {
        return maps.get(id);
    }

    public List<String> getMapIds() {
        return List.copyOf(maps.keySet());
    }

    public Map<String, MapDefinition> getMaps() {
        return Collections.unmodifiableMap(maps);
    }

    public boolean hasValidMaps() {
        return !maps.isEmpty();
    }

    public int getMapCount() {
        return maps.size();
    }

    public record Point(
            double x,
            double y,
            double z,
            float yaw,
            float pitch
    ) {
    }

    public record Bounds(
            int minX,
            int minY,
            int minZ,
            int maxX,
            int maxY,
            int maxZ
    ) {

        public boolean contains(Bounds other) {
            return other.minX() >= minX
                    && other.minY() >= minY
                    && other.minZ() >= minZ
                    && other.maxX() <= maxX
                    && other.maxY() <= maxY
                    && other.maxZ() <= maxZ;
        }
    }

    public record ZoneDefinition(
            String id,
            Bounds bounds
    ) {
    }

    public record MapDefinition(
            String id,
            String displayName,
            String schematic,
            Bounds bounds,
            Point entry,
            Map<String, ZoneDefinition> zones,
            List<Point> wardenSpawns,
            Point finalChest,
            Point extraction,
            List<Point> safeSpawns
    ) {

        public MapDefinition {
            zones = Collections.unmodifiableMap(
                    new LinkedHashMap<>(zones)
            );

            wardenSpawns = List.copyOf(wardenSpawns);
            safeSpawns = List.copyOf(safeSpawns);
        }
    }
}
