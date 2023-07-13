package pl.krzesniak.service;

import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pl.krzesniak.client.MeasurementClient;
import pl.krzesniak.client.SimulationClient;
import pl.krzesniak.dto.AgentResourcesRequest;
import pl.krzesniak.dto.ForestPixelRequest;
import pl.krzesniak.dto.TerrainGeneratorRequest;
import pl.krzesniak.exception.ForestPixelNotFoundException;
import pl.krzesniak.model.ForestPixel;
import pl.krzesniak.service.BoardGeneratorService;

import java.util.Arrays;
import java.util.Optional;

@Component
@Data
@RequiredArgsConstructor
public class BoardTemporaryHolder {

    private final BoardGeneratorService boardGenerator;
    private final MeasurementClient measurementClient;
    private final SimulationClient simulationClient;

    @Getter
    private ForestPixel[][] board;
    public static final int MIN_FIRE_VALUE_TO_BECOME_TESTABLE = 40;


    public ForestPixel[][] createTerrain(TerrainGeneratorRequest terrainGeneratorRequest) {
        board = boardGenerator.createBoard(terrainGeneratorRequest);
        return board;
    }

    public ForestPixel[][] applyForestFireIndex(ForestPixelRequest forestPixelRequest) {
        return boardGenerator.createForestFireIndex(board, forestPixelRequest);
    }

    public ForestPixel[][] locateAgents(AgentResourcesRequest agentResourcesRequest) {
        clearPreviouslyLocatedAgents(agentResourcesRequest.board());
        var locatedBoard = measurementClient.locateAgents(agentResourcesRequest);
        Arrays.stream(locatedBoard)
                .flatMap(Arrays::stream)
                .forEach(pixel -> {
                    var foundPixel = findForestPixelById(pixel.getId())
                            .orElseThrow(ForestPixelNotFoundException::new);
                    foundPixel.setAgentParameters(pixel.getAgentParameters());
                });
        return board;
    }

    private void clearPreviouslyLocatedAgents(ForestPixel[][] board) {
        for (ForestPixel[] forestPixels : board) {
            for (ForestPixel forestPixel : forestPixels) {
                forestPixel.getAgentParameters().setVisible(false);
                forestPixel.getAgentParameters().setTestable(false);
            }
        }
    }

    public ForestPixel[][] addFires(ForestPixel[][] forestPixels) {
        Arrays.stream(forestPixels)
                .flatMap(Arrays::stream)
                .forEach(forestPixel -> {
                    ForestPixel foundForestPixel = findForestPixelById(forestPixel.getId())
                            .orElseThrow(ForestPixelNotFoundException::new);
                    foundForestPixel.resetFireParameter();
                    foundForestPixel.setBasicFireParameter(forestPixel.getFireParameter().getForestFireState());
                });
        return this.board;
    }

    public Optional<ForestPixel> findForestPixelById(String id) {
        return Arrays.stream(this.board)
                .flatMap(Arrays::stream)
                .filter(forestPixel -> forestPixel.getId().equals(id))
                .findFirst();
    }

    public void exchangeBoard() {
        simulationClient.sendBoard(board);
    }
}
