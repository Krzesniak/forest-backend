package pl.krzesniak.service.resources;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.krzesniak.model.ForestPixel;
import pl.krzesniak.service.AgentDashboard;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class ResourceAllocator {

    private final AgentDashboard agentDashboard;

    public void calculateFireDanger(Map<String, Set<ForestPixel>> idToBurnedPixels) {
        idToBurnedPixels.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> calculateFireDanger(entry.getValue())))
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue())
                .limit(agentDashboard.getFirefighterAgents().size())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));


    }

    public double calculateFireDanger(Set<ForestPixel> burnedPixels) {
        return burnedPixels.stream()
                .map(forestPixel -> forestPixel.getFireParameter().getFireSpeedSpreed())
                .mapToDouble(value -> value)
                .sum();
    }
}
