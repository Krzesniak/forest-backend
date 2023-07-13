package pl.krzesniak.service;

import org.springframework.stereotype.Component;
import pl.krzesniak.model.agents.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@Component
public class AgentCreator {

    public List<SensorAgent> createSensorAgents(Set<String> sensorAgentIds) {
        return sensorAgentIds.stream()
                .map(SensorAgent::new)
                .toList();
    }
    public List<ManagingAgent> createManagingAgents(Set<String> sensorAgentIds) {
        return sensorAgentIds.stream()
                .map(ManagingAgent::new)
                .toList();
    }

    public List<TesterAgent> createTesterAgents(long number) {
        return Stream.iterate(1, n -> n + 1)
                .limit(number)
                .map(Object::toString)
                .map(TesterAgent::new)
                .toList();
    }
    public List<FireControllerAgent> createFireControllerAgents(long number) {
        return Stream.iterate(1, n -> n + 1)
                .limit(number)
                .map(Object::toString)
                .map(FireControllerAgent::new)
                .toList();
    }

    public List<FirefighterAgent> createFireFighterAgents(long number) {
        return Stream.iterate(1, n -> n + 1)
                .limit(number)
                .map(Object::toString)
                .map(FirefighterAgent::new)
                .toList();
    }

    public AnalystAgent createAnalystAgent() {
        return new AnalystAgent("1");
    }
}
