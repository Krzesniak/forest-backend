package pl.krzesniak.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pl.krzesniak.dto.ForestPixelRequest;
import pl.krzesniak.dto.TerrainBoundaries;
import pl.krzesniak.dto.TerrainGeneratorRequest;
import pl.krzesniak.model.AgentParameters;
import pl.krzesniak.model.FireParameter;
import pl.krzesniak.model.ForestPixel;
import pl.krzesniak.model.Wind;
import pl.krzesniak.model.enums.ForestFireBurnedColor;
import pl.krzesniak.model.enums.ForestFireIndex;
import pl.krzesniak.model.enums.ForestFireState;
import pl.krzesniak.model.enums.Terrain;


@Service
public class BoardGeneratorService {

    @Value("${board.width}")
    public int WIDTH;

    @Value("${board.height}")
    public int HEIGHT;
    public static final double PERLIN_NOISE_INTERVAL_VALUES = 1.7;
    public static final double MIN_PERLIN_NOISE_INTERVAL_VALUE = -0.85;

    private TerrainBoundaries terrainBoundaries;

    public BoardGeneratorService() {
        terrainBoundaries = new TerrainBoundaries(-0.2, 0.45, 2, -0.31);
    }


    public ForestPixel[][] createBoard(TerrainGeneratorRequest terrainGeneratorRequest) {
        long seed = terrainGeneratorRequest.seed();
        double frequency = 1.0 / terrainGeneratorRequest.frequency();
        setTerrainProbability(terrainGeneratorRequest.terrainProbability());
        ForestPixel[][] pixels = new ForestPixel[WIDTH][HEIGHT];
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                double value = PerlinNoiseCreator.noise3_ImproveXY(seed, x * frequency, y * frequency, 0.0);
                pixels[y][x] = new ForestPixel();
                pixels[y][x].setId(y + ":" + x);
                pixels[y][x].setFireParameter(new FireParameter(false, false, 0,
                        ForestFireState.NONE, ForestFireBurnedColor.convertBurnedFieldPercentageToColorValue(0), 0));
                Terrain terrain = convertToTerrain(value);
                pixels[y][x].setAgentParameters(new AgentParameters(false, false, false, false));
                pixels[y][x].setForestFireIndex(ForestFireIndex.NONE);
                pixels[y][x].setForestFireIndexValue(0);
                pixels[y][x].setTerrain(terrain);
            }
        }
        return pixels;
    }

    private Terrain convertToTerrain(double value) {
        if (value <= terrainBoundaries.water()) {
            return Terrain.WATER;
        } else if (value <= terrainBoundaries.sand()) {
            return Terrain.SAND;
        } else if (value <= terrainBoundaries.forestConiferous()) {
            return Terrain.FOREST_DECIDUOUS;
        }
        return Terrain.FOREST_CONIFEROUS;
    }

    private void setTerrainProbability(TerrainBoundaries terrainProbability) {
        double sumValues = terrainProbability.forestDeciduous() + terrainProbability.water() +
                terrainProbability.sand() + terrainProbability.forestConiferous();
        double interval = PERLIN_NOISE_INTERVAL_VALUES / sumValues;
        double waterBoundaries = MIN_PERLIN_NOISE_INTERVAL_VALUE + terrainProbability.water() * interval;
        double sandBoundaries = waterBoundaries + terrainProbability.sand() * interval;
        double forestConiferous = sandBoundaries + terrainProbability.forestConiferous() * interval;
        terrainBoundaries = new TerrainBoundaries(sandBoundaries, forestConiferous, 2.0, waterBoundaries);

    }

    public ForestPixel[][] createForestFireIndex(ForestPixel[][] board, ForestPixelRequest forestPixelRequest) {
        for (ForestPixel[] forestPixels : board) {
            for (int j = 0; j < board[0].length; j++) {
                forestPixels[j].setTemperature(forestPixelRequest.temperature());
                forestPixels[j].setWind(new Wind(forestPixelRequest.windDirection(), forestPixelRequest.windStrength()));
                forestPixels[j].setHumidity(forestPixelRequest.humidity());
                forestPixels[j].applyProbability();
            }
        }
        return board;
    }

}
