package pl.krzesniak.service;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pl.krzesniak.dto.AgentResourcesRequest;
import pl.krzesniak.exception.AgentNotFoundException;
import pl.krzesniak.model.ForestPixel;
import pl.krzesniak.model.agents.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    }

    public void clearAgents() {
        sensorAgents = new ArrayList<>();
        fireControllerAgents = new ArrayList<>();
        firefighterAgents = new ArrayList<>();
        managingAgents = new ArrayList<>();
        testerAgents = new ArrayList<>();
    }

}
