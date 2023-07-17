package pl.krzesniak.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.krzesniak.exception.AgentNotFoundException;
import pl.krzesniak.model.ForestPixel;
import pl.krzesniak.model.agents.FireControllerAgent;
import pl.krzesniak.model.agents.ManagingAgent;
import pl.krzesniak.service.resources.AdditionalResourceNeeded;
import pl.krzesniak.service.resources.FireResourceAllocator;
import pl.krzesniak.service.resources.FireResourceMetadata;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Getter
public class AgentIteration {
    private final ForestPixelHelper forestPixelHelper;
    private final TestableFieldsGrouperByLocation testableFieldsGrouperByLocation;
    private final FireGroupFinder fireGroupFinder;
    private final AgentDashboard agentDashboard;
    private final FireResourceAllocator fireResourceAllocator;
    private final AgentFinder agentFinder;

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


    //TODO it is newlyBurned to we need to update existing using board field
    void extinguishFire(ForestPixel[][] board, Set<ForestPixel> newlyFoundBurningPixels) {
        fireGroupFinder.updateCurrentlyExtinguishedFields();
        Map<String, Set<ForestPixel>> allConnectedBurningPixels = fireGroupFinder.findAllConnectedBurningPixels(newlyFoundBurningPixels);
        if (!fireGroupFinder.getRemovedZoneByZoneToJoin().isEmpty()) moveConnectedZones();
        allConnectedBurningPixels = clearExtinguishedFireZones(allConnectedBurningPixels);
        Map<String, FireResourceMetadata> firefightersAllocation = fireResourceAllocator.computeFireAllocation(allConnectedBurningPixels);
        assignFireZoneIdToFireControllerAgents(firefightersAllocation);
    }

    private void moveConnectedZones() {
        Map<String, List<FireResourceMetadata>> idToFireInformation = fireResourceAllocator.getIdToFireInformation();
        fireGroupFinder.getRemovedZoneByZoneToJoin()
                .entrySet()
                .forEach(removedZoneByJoiningZone -> {
                    int previouslySetFireFightersCount = fireResourceAllocator.getPreviouslySetFireFightersCount(idToFireInformation.get(removedZoneByJoiningZone.getKey()));
                    List<FireResourceMetadata> fireResourceMetadata = idToFireInformation.get(removedZoneByJoiningZone.getValue());
                    FireResourceMetadata lastFireResourceMetadata = fireResourceMetadata.get(fireResourceMetadata.size() - 1);
                    lastFireResourceMetadata.setFirefightersCount(lastFireResourceMetadata.getFirefightersCount() + previouslySetFireFightersCount);
                    lastFireResourceMetadata.setAdditionalResourceNeeded(AdditionalResourceNeeded.YES);
                    agentDashboard.moveFireFighterFromOneZoneToAnotherOne(removedZoneByJoiningZone);
                    agentDashboard.freeUpFireControllerAgents(removedZoneByJoiningZone.getKey());
                });
    }

    private Map<String, Set<ForestPixel>> clearExtinguishedFireZones(Map<String, Set<ForestPixel>> allConnectedBurningPixels) {
        Set<String> nonBurningZoneIds = allConnectedBurningPixels.entrySet()
                .stream()
                .filter(entry -> entry.getValue().isEmpty())
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        Map<String, Set<ForestPixel>> zoneIdByAllExtinguishedPixels = fireGroupFinder.getAndClearPixelsByZoneId(nonBurningZoneIds);
        agentDashboard.clearResourceAfterExtinguishingAllFields(zoneIdByAllExtinguishedPixels.keySet());
        agentDashboard.getExitAgent().setFieldsToDefaultValues(zoneIdByAllExtinguishedPixels.values());
        return allConnectedBurningPixels.entrySet()
                .stream()
                .filter(entry -> !entry.getValue().isEmpty())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    }

    private void assignFireZoneIdToFireControllerAgents(Map<String, FireResourceMetadata> firefightersAllocation) {
        firefightersAllocation.forEach((key, value) -> {
            FireControllerAgent fireControllerAgent = agentFinder.findFireControllerAgentByFireZoneId(key)
                    .orElseThrow(() -> new AgentNotFoundException("Agent with id not found: " + key));
            fireControllerAgent.assignFirefighters(value, agentFinder.findFireFighterAssignedByFireControllerAgentId(key));
        });
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
            ManagingAgent managingAgent = agentFinder.findManagingAgentById(id)
                    .orElseThrow(() -> new AgentNotFoundException("Agent not found with id: " + id));
            managingAgent.dangerousFieldGrouper.getBurningFields().addAll(value);
        });
        newDangerousForestPixels.forEach((key, value) -> {
            List<String> managingAgentIds = managingAgents.stream().map(ManagingAgent::getId).toList();
            String id = forestPixelHelper.chooseFromThePixelListTheClosestPixelToGivenPixel(managingAgentIds, key);
            ManagingAgent managingAgent = agentFinder.findManagingAgentById(id)
                    .orElseThrow(() -> new AgentNotFoundException("Agent not found with id: " + id));
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

}
