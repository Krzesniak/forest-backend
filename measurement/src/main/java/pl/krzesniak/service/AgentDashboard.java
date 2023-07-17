package pl.krzesniak.service;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pl.krzesniak.dto.AgentResourcesRequest;
import pl.krzesniak.model.agents.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.function.Predicate.not;

@Component
@RequiredArgsConstructor
@Slf4j
@Data
public class AgentDashboard {

    private final AgentCreator agentCreator;

    private List<SensorAgent> sensorAgents;
    private List<FireControllerAgent> fireControllerAgents;
    private List<FirefighterAgent> firefighterAgents;
    private List<ManagingAgent> managingAgents;
    private List<TesterAgent> testerAgents;
    private AnalystAgent analystAgent;
    private ExitAgent exitAgent;

    public void locateAgents(AgentResourcesRequest agentResourcesRequest,
                             Set<String> sensorAgentIds, Set<String> managingAgentsIds) {
        sensorAgents = agentCreator.createSensorAgents(sensorAgentIds);
        managingAgents = agentCreator.createManagingAgents(managingAgentsIds);
        testerAgents = agentCreator.createTesterAgents(agentResourcesRequest.testerAgents());
        fireControllerAgents = agentCreator.createFireControllerAgents(agentResourcesRequest.fireControllerAgents());
        firefighterAgents = agentCreator.createFireFighterAgents(agentResourcesRequest.firefighterAgents());
        analystAgent = agentCreator.createAnalystAgent();
        exitAgent = agentCreator.createExitAgent();

    }

    public void clearAgents() {
        sensorAgents = new ArrayList<>();
        fireControllerAgents = new ArrayList<>();
        firefighterAgents = new ArrayList<>();
        managingAgents = new ArrayList<>();
        testerAgents = new ArrayList<>();
    }

    public List<FirefighterAgent> getFreeFirefighters() {
        return firefighterAgents.stream()
                .filter(not(FirefighterAgent::isBusy))
                .collect(Collectors.toList());
    }

    public int getFreeFirefightersCount() {
        return (int) firefighterAgents.stream()
                .filter(not(FirefighterAgent::isBusy))
                .count();
    }

    public int calculateFreeFireControllerAgentCount() {
        return (int) fireControllerAgents
                .stream()
                .filter(not(FireControllerAgent::isBusy))
                .count();
    }

    public void assignFireControllerAgentToFireZone(String fireZoneId) {
        fireControllerAgents
                .stream()
                .filter(not(FireControllerAgent::isBusy))
                .findFirst()
                .ifPresent(agent -> {
                    agent.setBusy(true);
                    agent.setFireZoneId(fireZoneId);
                });
    }

    public void assignFirefightersToFireZone(int count, String fireZoneId) {
        firefighterAgents
                .stream()
                .filter(not(FirefighterAgent::isBusy))
                .limit(count)
                .forEach(firefighterAgent -> {
                    firefighterAgent.setBusy(true);
                    firefighterAgent.setFireZoneId(fireZoneId);
                    firefighterAgent.setCurrentExtinguishPixelId("");
                });
    }

    public void clearResourceAfterExtinguishingAllFields(Set<String> zoneIds) {
        zoneIds.forEach(zoneID -> {
                    freeUpFirefighters(zoneID);
                    freeUpFireControllerAgents(zoneID);
                });

    }

    public void freeUpFirefighters(String id) {
        firefighterAgents.stream()
                .filter(firefighterAgent -> firefighterAgent.getFireZoneId().equals(id))
                .forEach(firefighterAgent -> {
                    firefighterAgent.setBusy(false);
                    firefighterAgent.setFireZoneId("");
                    firefighterAgent.setCurrentExtinguishPixelId("");
                });
    }

    public void freeUpFireControllerAgents(String id) {
        fireControllerAgents.stream()
                .filter(firefighterAgent -> firefighterAgent.getFireZoneId().equals(id))
                .forEach(firefighterAgent -> {
                    firefighterAgent.setBusy(false);
                    firefighterAgent.setFireZoneId("");
                    firefighterAgent.setFirefighters(new ArrayList<>());
                });
    }

    public void moveFireFighterFromOneZoneToAnotherOne(Map.Entry<String, String> removedZoneByJoiningZone) {
        firefighterAgents.stream().
                filter(firefighterAgent -> firefighterAgent.getFireZoneId().equals(removedZoneByJoiningZone.getKey()))
                .forEach(firefighterAgent -> firefighterAgent.setFireZoneId(removedZoneByJoiningZone.getValue()));
    }
}
