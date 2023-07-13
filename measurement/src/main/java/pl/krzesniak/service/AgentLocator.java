package pl.krzesniak.service;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.krzesniak.dto.AgentResourcesRequest;
import pl.krzesniak.model.ForestPixel;
import pl.krzesniak.exception.ForestPixelNotFoundException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AgentLocator {

    private final AgentDashboard agentDashboard;
    private final ForestPixelHelper forestPixelHelper;
    public static final int MIN_FIRE_VALUE_TO_BECOME_TESTABLE = 40;


    public ForestPixel[][] locateAgents(AgentResourcesRequest agentResourcesRequest) {
        agentDashboard.clearAgents();
        forestPixelHelper.setBoard(agentResourcesRequest.board());
        Set<String> sensorAgentIds = new HashSet<>();
        Set<String> managingAgentsIds = new HashSet<>();
        Arrays.stream(agentResourcesRequest.board())
                .flatMap(Arrays::stream)
                .forEach(forestPixel -> {
                    ForestPixel foundForestPixel = forestPixelHelper.findForestPixelById(forestPixel.getId())
                            .orElseThrow(ForestPixelNotFoundException::new);
                    if (forestPixel.getAgentParameters().isHasSensor()) {
                        foundForestPixel.getAgentParameters().setHasSensor(true);
                        sensorAgentIds.add(forestPixel.getId());
                        recursiveSetVisibleNeighborhoodPixels(forestPixel);
                    }
                    if (forestPixel.getAgentParameters().isCenter()) {
                        foundForestPixel.getAgentParameters().setCenter(true);
                        managingAgentsIds.add(foundForestPixel.getId());
                    }
                });
        agentDashboard.locateAgents(agentResourcesRequest, sensorAgentIds, managingAgentsIds);
        return agentResourcesRequest.board();
    }

    void recursiveSetVisibleNeighborhoodPixels(ForestPixel forestPixel) {
        List<ForestPixel> pixelsNeighborhood = forestPixelHelper.createSurroundingsForPixel(forestPixel.getId());
        pixelsNeighborhood.forEach(pixel -> pixel.getAgentParameters().setVisible(true));
        pixelsNeighborhood.stream()
                .map(ForestPixel::getId)
                .flatMap(pixelId -> forestPixelHelper.createTestingSurroundingsForPixel(pixelId).stream())
                .filter(pixel -> pixel.getForestFireIndexValue() > MIN_FIRE_VALUE_TO_BECOME_TESTABLE)
                .forEach(pixel -> pixel.getAgentParameters().setTestable(true));
    }
}
