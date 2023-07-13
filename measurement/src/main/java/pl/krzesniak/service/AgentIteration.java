package pl.krzesniak.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.krzesniak.exception.AgentNotFoundException;
import pl.krzesniak.model.ForestPixel;
import pl.krzesniak.model.agents.ManagingAgent;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class AgentIteration
{
    private final ForestPixelHelper forestPixelHelper;
    private final TestableFieldsGrouperByLocation testableFieldsGrouperByLocation;
    private final FireGroupFinder fireGroupFinder;
    private final AgentDashboard agentDashboard;
    public void agentIteration(ForestPixel[][] board) {
        forestPixelHelper.setBoard(board);
        boolean areAnalyzedFieldsChanged = analyzeFieldsByAnalystAgent();
        if (areAnalyzedFieldsChanged) {
            assignPixelsToManagingAgent(agentDashboard.getAnalystAgent().getNewBurnedForestPixels(), agentDashboard.getAnalystAgent().getNewDangerousForestPixels());
            groupFieldsByManagingAgents();
            Set<ForestPixel> testableFields = markFieldsToBecomeTestable();
            testableFieldsGrouperByLocation.setBoard(board);
            Map<String, Set<ForestPixel>> idGroupToTestableFields = testableFieldsGrouperByLocation.groupFieldsByNeighbours(testableFields);
        }
        Set<ForestPixel> newlyFoundBurningPixels = agentDashboard.getManagingAgents().stream()
                .flatMap(managingAgent -> managingAgent.dangerousFieldGrouper.getBurningFields().stream())
                .collect(Collectors.toSet());

        //   if(!isFire(newlyFoundBurningPixels)) return;
        extinguishFire(board, newlyFoundBurningPixels);
        agentDashboard.getAnalystAgent().updatePreviousAnalyzedFields();
    }

    private void extinguishFire(ForestPixel[][] board, Set<ForestPixel> newlyFoundBurningPixels) {
        Map<String, Set<ForestPixel>> allConnectedBurningPixels = fireGroupFinder.findAllConnectedBurningPixels(newlyFoundBurningPixels);
        System.out.println("XD");
    }

    private boolean isFire(Set<ForestPixel> burningPixels) {
        return !burningPixels.isEmpty();
    }

    private Set<ForestPixel> markFieldsToBecomeTestable() {
        var managingAgents = agentDashboard.getManagingAgents();
        Set<ForestPixel> testableFields = managingAgents.stream()
                .flatMap(managingAgent -> Stream.of(managingAgent.dangerousFieldGrouper.getDangerousFieldsCauseOfNeighbours(),
                        managingAgent.dangerousFieldGrouper.getDangerousFields()))
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
        testableFields.forEach(pixel -> pixel.getAgentParameters().setTestable(true));
        return testableFields;
    }

    private void assignPixelsToManagingAgent(Map<String, List<ForestPixel>> newBurnedForestPixels, Map<String, List<ForestPixel>> newDangerousForestPixels) {
        var managingAgents = agentDashboard.getManagingAgents();
        newBurnedForestPixels.forEach((key, value) -> {
            List<String> managingAgentIds = managingAgents.stream().map(ManagingAgent::getId).toList();
            String id = forestPixelHelper.chooseFromThePixelListTheClosestPixelToGivenPixel(managingAgentIds, key);
            ManagingAgent managingAgent = findManagingAgentById(id).orElseThrow(() -> new AgentNotFoundException("Agent not found with id: " + id));
            managingAgent.dangerousFieldGrouper.getBurningFields().addAll(value);
        });
        newDangerousForestPixels.forEach((key, value) -> {
            List<String> managingAgentIds = managingAgents.stream().map(ManagingAgent::getId).toList();
            String id = forestPixelHelper.chooseFromThePixelListTheClosestPixelToGivenPixel(managingAgentIds, key);
            ManagingAgent managingAgent = findManagingAgentById(id).orElseThrow(() -> new AgentNotFoundException("Agent not found with id: " + id));
            managingAgent.dangerousFieldGrouper.getDangerousFields().addAll(value);
        });
    }

    private void groupFieldsByManagingAgents() {
        var managingAgents = agentDashboard.getManagingAgents();
        managingAgents.forEach(ManagingAgent::groupFieldsByDangerousDegree);
    }

    private boolean analyzeFieldsByAnalystAgent() {
        agentDashboard.getSensorAgents().stream()
                .map(sensorAgent -> sensorAgent.updateForestFields(forestPixelHelper))
                .forEach(sensorPixels -> agentDashboard.getAnalystAgent().analyzeForestFields(sensorPixels.pixels(), sensorPixels.id()));
        return agentDashboard.getAnalystAgent().arePixelsChanged();
    }

    Optional<ManagingAgent> findManagingAgentById(String id) {
        return agentDashboard.getManagingAgents().stream()
                .filter(managingAgent -> managingAgent.getId().equals(id))
                .findAny();
    }
}
