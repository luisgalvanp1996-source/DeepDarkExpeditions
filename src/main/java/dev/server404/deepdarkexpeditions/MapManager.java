
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

        // Validar que las cinco zonas estén dentro del mapa.
        if (bounds != null) {

            for (ZoneDefinition zone : zones.values()) {

                if (!bounds.contains(zone.bounds())) {
                    errors.add(
                            "La zona " + zone.id()
                                    + " está fuera de los límites del mapa."
                    );
                }
            }

            // Validar puntos individuales.
            validatePointInsideBounds(
                    bounds,
                    entry,
                    "entry",
                    errors
            );

            validatePointInsideBounds(
                    bounds,
                    finalChest,
                    "final-chest",
                    errors
            );

            validatePointInsideBounds(
                    bounds,
                    extraction,
                    "extraction",
                    errors
            );

            // Validar apariciones de Wardens.
            for (int i = 0; i < wardenSpawns.size(); i++) {

                validatePointInsideBounds(
                        bounds,
                        wardenSpawns.get(i),
                        "warden-spawns[" + i + "]",
                        errors
                );
            }

            // Validar puntos seguros.
            for (int i = 0; i < safeSpawns.size(); i++) {

                validatePointInsideBounds(
                        bounds,
                        safeSpawns.get(i),
                        "safe-spawns[" + i + "]",
                        errors
                );
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

    /**
     * Comprueba que un punto esté dentro de los límites del mapa.
     */
    private void validatePointInsideBounds(
            Bounds bounds,
            Point point,
            String label,
            List<String> errors
    ) {

        if (bounds == null || point == null) {
            return;
        }

        if (!bounds.contains(point)) {
            errors.add(
                    "El punto " + label
                            + " está fuera de los límites del mapa. "
                            + "Coordenadas: "
                            + point.x() + ", "
                            + point.y() + ", "
                            + point.z()
            );
        }
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

        if (!isFiniteNumber(section, "x")
                || !isFiniteNumber(section, "y")
                || !isFiniteNumber(section, "z")) {

            errors.add(
                    "Coordenadas x, y, z inválidas en " + path + "."
            );
            return null;
        }

        double x = section.getDouble("x");
        double y = section.getDouble("y");
        double z = section.getDouble("z");

        if (!isFiniteNumber(section, "yaw", true)
                || !isFiniteNumber(section, "pitch", true)) {

            errors.add(
                    "Orientación yaw/pitch inválida en " + path + "."
            );
            return null;
        }

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

        if (!isIntegerNumber(min, "x")
                || !isIntegerNumber(min, "y")
                || !isIntegerNumber(min, "z")
                || !isIntegerNumber(max, "x")
                || !isIntegerNumber(max, "y")
                || !isIntegerNumber(max, "z")) {

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

    private boolean isFiniteNumber(
            ConfigurationSection section,
            String path
    ) {
        return isFiniteNumber(section, path, false);
    }

    private boolean isFiniteNumber(
            ConfigurationSection section,
            String path,
            boolean optional
    ) {

        Object value = section.get(path);

        if (value == null && optional) {
            return true;
        }

        if (!(value instanceof Number number)) {
            return false;
        }

        return Double.isFinite(number.doubleValue());
    }

    private boolean isIntegerNumber(
            ConfigurationSection section,
            String path
    ) {

        Object value = section.get(path);

        if (!(value instanceof Number number)) {
            return false;
        }

        double decimal = number.doubleValue();

        return Double.isFinite(decimal)
                && decimal == Math.floor(decimal)
                && decimal >= Integer.MIN_VALUE
                && decimal <= Integer.MAX_VALUE;
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

        public boolean contains(Point point) {
            return point.x() >= minX
                    && point.x() < (maxX + 1.0)
                    && point.y() >= minY
                    && point.y() < (maxY + 1.0)
                    && point.z() >= minZ
                    && point.z() < (maxZ + 1.0);
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
