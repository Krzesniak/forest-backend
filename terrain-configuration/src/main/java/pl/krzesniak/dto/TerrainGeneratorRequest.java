package pl.krzesniak.dto;

public record TerrainGeneratorRequest(long seed, long frequency, TerrainBoundaries terrainProbability) {
}
