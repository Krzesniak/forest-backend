package pl.krzesniak.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pl.krzesniak.model.agents.FireControllerAgent;
import pl.krzesniak.model.agents.FirefighterAgent;
import pl.krzesniak.model.agents.ManagingAgent;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AgentFinder {

    private final AgentDashboard agentDashboard;

    Optional<ManagingAgent> findManagingAgentById(String id) {
        return agentDashboard.getManagingAgents().stream()
                .filter(managingAgent -> managingAgent.getId().equals(id))
                .findAny();
    }

    Optional<FireControllerAgent> findFireControllerAgentByFireZoneId(String id) {
        return agentDashboard.getFireControllerAgents().stream()
                .filter(fireControllerAgent -> fireControllerAgent.getFireZoneId().equals(id))
                .findAny();
    }

    List<FirefighterAgent> findFireFighterAssignedByFireControllerAgentId(String fireControllerAgentId) {
        return agentDashboard.getFirefighterAgents()
                .stream()
                .filter(firefighterAgent -> firefighterAgent.getFireZoneId().equals(fireControllerAgentId))
                .toList();
    }
}
