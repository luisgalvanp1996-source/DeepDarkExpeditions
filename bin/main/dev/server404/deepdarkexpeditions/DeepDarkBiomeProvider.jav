
package dev.server404.deepdarkexpeditions;

import org.bukkit.block.Biome;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.WorldInfo;

import java.util.List;

public final class DeepDarkBiomeProvider extends BiomeProvider {

    @Override
    public Biome getBiome(
            WorldInfo worldInfo,
            int x,
            int y,
            int z
    ) {
        return Biome.DEEP_DARK;
    }

    @Override
    public List<Biome> getBiomes(WorldInfo worldInfo) {
        return List.of(Biome.DEEP_DARK);
    }
}
