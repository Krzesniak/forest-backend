package pl.krzesniak.service.resources;

import org.springframework.stereotype.Service;
import pl.krzesniak.exception.ForestPixelNotFoundException;
import pl.krzesniak.model.ForestPixel;
import pl.krzesniak.model.agents.FireControllerAgent;
import pl.krzesniak.model.agents.FirefighterAgent;
import pl.krzesniak.model.enums.ForestFireState;
import pl.krzesniak.service.AgentDashboard;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.function.Predicate.not;
import static pl.krzesniak.service.resources.AdditionalResourceNeeded.*;

@Service
public class FireResourceAllocator {

    public static final int MINIMUM_VALUE_OF_REALLOCATION = 4;
    private final Map<ForestFireState, Integer> fireStateToFirefightersCount;
    private final Map<String, List<FireResourceMetadata>> idToFireInformation;
    private final AgentDashboard agentDashboard;

    public FireResourceAllocator(AgentDashboard agentDashboard) {
        fireStateToFirefightersCount = Map.ofEntries(
                new AbstractMap.SimpleEntry<>(ForestFireState.NONE, 0),
                new AbstractMap.SimpleEntry<>(ForestFireState.DESTROYED, 0),
                new AbstractMap.SimpleEntry<>(ForestFireState.LOW, 1),
                new AbstractMap.SimpleEntry<>(ForestFireState.MEDIUM, 2),
                new AbstractMap.SimpleEntry<>(ForestFireState.HIGH, 3),
                new AbstractMap.SimpleEntry<>(ForestFireState.EXTREME, 4)
        );
        this.agentDashboard = agentDashboard;
        this.idToFireInformation = new HashMap<>();
    }


    public HashMap<String, FireResourceMetadata> computeFireAllocation(Map<String, Set<ForestPixel>> burningPixels) {
        List<FireResourceMetadata> updatedFireResourceMetadata = burningPixels.entrySet()
                .stream()
                .map(this::createFireResourceMetadata)
                .toList();

        Set<String> idsNeededMoreResource = updatedFireResourceMetadata.stream()
                .filter(resource -> resource.getAdditionalResourceNeeded() == YES)
                .map(FireResourceMetadata::getId)
                .collect(Collectors.toSet());

        List<FirefighterAgent> freeFirefighters = getFreeFirefighters();
        int freeFirefightersCount = freeFirefighters.size();

        if (!idsNeededMoreResource.isEmpty()) {

            List<FireResourceMetadata> updatedFireResourceMetadataOnlyBeingExtinguished = updatedFireResourceMetadata.stream()
                    .filter(FireResourceMetadata::isBeingExtinguished)
                    .collect(Collectors.toList());

            double allDangerousValue = updatedFireResourceMetadataOnlyBeingExtinguished.stream()
                    .map(FireResourceMetadata::getDangerousValue)
                    .mapToDouble(val -> val)
                    .sum();

            Map<String, Double> idToPriority = updatedFireResourceMetadataOnlyBeingExtinguished.stream()
                    .collect(Collectors.toMap(FireResourceMetadata::getId, resource -> resource.getDangerousValue() / allDangerousValue));

            Map<String, Integer> idToNeededAdditionalResources = updatedFireResourceMetadataOnlyBeingExtinguished.stream()
                    .collect(Collectors.toMap(FireResourceMetadata::getId, this::calculateNeededAdditionalResource));

            idToPriority.entrySet()
                    .stream()
                    .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                    .forEach(entry -> {
                        int additionalNeededResourceCount = idToNeededAdditionalResources.get(entry.getKey());
                        int availableResource = (int) (freeFirefightersCount * entry.getValue());
                        int addedResourceCount = Math.min(availableResource, additionalNeededResourceCount);

                        for (int i = 0; i < addedResourceCount && !freeFirefighters.isEmpty(); i++) {
                            FirefighterAgent firefighterAgent = freeFirefighters.get(0);
                            firefighterAgent.setBusy(true);
                            freeFirefighters.remove(0);
                        }
                        FireResourceMetadata fireResourceMetadata = updatedFireResourceMetadataOnlyBeingExtinguished.stream()
                                .filter(resource -> resource.getId().equals(entry.getKey()))
                                .peek(resource -> {
                                    int firefightersCount = getLastFirefightersCount(resource) + addedResourceCount;
                                    resource.setFirefightersCount(firefightersCount);
                                    if (firefightersCount == resource.getOptimalFireFighterCount())
                                        resource.setAdditionalResourceNeeded(NO);
                                    else resource.setAdditionalResourceNeeded(YES);
                                })
                                .findFirst()
                                .orElseThrow(ForestPixelNotFoundException::new);


                    });

            updatedFireResourceMetadata.stream()
                    .filter(resource -> resource.getAdditionalResourceNeeded() == YES)
                    .sorted(Comparator.comparing(FireResourceMetadata::getDangerousValue).reversed())
                    .limit(1)
                    .findFirst()
                    .ifPresent(fireResourceMetadata -> {
                        int firefightersCount = fireResourceMetadata.getFirefightersCount() + getFreeFirefighters().size();
                        fireResourceMetadata.setFirefightersCount(firefightersCount);
                        if (firefightersCount == fireResourceMetadata.getOptimalFireFighterCount())
                            fireResourceMetadata.setAdditionalResourceNeeded(NO);
                        else fireResourceMetadata.setAdditionalResourceNeeded(YES);
                        freeFirefighters.forEach(firefighterAgent -> firefighterAgent.setBusy(true));
                    });

        }
        addFireResourceMetadataOnlyForZonesBeingExtinguished(updatedFireResourceMetadata);
        long freeFireControllerAgentCount = calculateFreeFireControllerAgentCount();

        long newFireZonesCount = updatedFireResourceMetadata.stream()
                .filter(not(FireResourceMetadata::isBeingExtinguished))
                .count();
        if (freeFireControllerAgentCount <= 0 || newFireZonesCount == 0) return getTheLatestFireResourceData();

        double allDangerousValue = updatedFireResourceMetadata.stream()
                .filter(not(FireResourceMetadata::isBeingExtinguished))
                .map(FireResourceMetadata::getDangerousValue)
                .mapToDouble(val -> val)
                .sum();

        Map<String, Double> idToPriority = updatedFireResourceMetadata.stream()
                .filter(not(FireResourceMetadata::isBeingExtinguished))
                .collect(Collectors.toMap(FireResourceMetadata::getId, resource -> resource.getDangerousValue() / allDangerousValue));

        Map<String, Integer> idToNeededAdditionalResources = updatedFireResourceMetadata.stream()
                .filter(not(FireResourceMetadata::isBeingExtinguished))
                .collect(Collectors.toMap(FireResourceMetadata::getId, FireResourceMetadata::getOptimalFireFighterCount));

        idToPriority.entrySet()
                .stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(freeFireControllerAgentCount)
                .forEach(entry -> {
                    int additionalNeededResourceCount = idToNeededAdditionalResources.get(entry.getKey());
                    int availableResource = (int) (freeFirefightersCount * entry.getValue());
                    int addedResourceCount = Math.min(availableResource, additionalNeededResourceCount);

                    for (int i = 0; i < addedResourceCount && !freeFirefighters.isEmpty(); i++) {
                        FirefighterAgent firefighterAgent = freeFirefighters.get(0);
                        firefighterAgent.setBusy(true);
                        freeFirefighters.remove(0);
                    }
                    FireResourceMetadata fireResourceMetadata = updatedFireResourceMetadata.stream()
                            .filter(resource -> resource.getId().equals(entry.getKey()))
                            .peek(resource -> {
                                int firefightersCount = getLastFirefightersCount(resource) + addedResourceCount;
                                resource.setFirefightersCount(firefightersCount);
                                if (firefightersCount == resource.getOptimalFireFighterCount())
                                    resource.setAdditionalResourceNeeded(NO);
                                else resource.setAdditionalResourceNeeded(YES);
                            })
                            .findFirst()
                            .orElseThrow(ForestPixelNotFoundException::new);
                    this.idToFireInformation.put(entry.getKey(), new ArrayList<>((Collections.singletonList(fireResourceMetadata))));
                    this.agentDashboard.getFireControllerAgents()
                            .stream()
                            .filter(not(FireControllerAgent::isBusy))
                            .findFirst()
                            .ifPresent(agent -> agent.setBusy(true));
                });

        updatedFireResourceMetadata.stream()
                .filter(resource -> resource.getAdditionalResourceNeeded() == YES)
                .sorted(Comparator.comparing(FireResourceMetadata::getDangerousValue).reversed())
                .limit(1)
                .findFirst()
                .ifPresent(fireResourceMetadata -> {
                    int firefightersCount = fireResourceMetadata.getFirefightersCount() + getFreeFirefighters().size();
                    fireResourceMetadata.setFirefightersCount(firefightersCount);
                    if (firefightersCount == fireResourceMetadata.getOptimalFireFighterCount())
                        fireResourceMetadata.setAdditionalResourceNeeded(NO);
                    else fireResourceMetadata.setAdditionalResourceNeeded(YES);
                    freeFirefighters.forEach(firefighterAgent -> firefighterAgent.setBusy(true));
                });

        return getTheLatestFireResourceData();

    }

    private void addFireResourceMetadataOnlyForZonesBeingExtinguished(List<FireResourceMetadata> fireResourceMetadata) {
        fireResourceMetadata.stream()
                .filter(FireResourceMetadata::isBeingExtinguished)
                .forEach(metadata -> idToFireInformation.get(metadata.getId()).add(metadata));
    }

    private HashMap<String, FireResourceMetadata> getTheLatestFireResourceData() {
        return idToFireInformation.entrySet()
                .stream()
                .collect(HashMap::new, (map, entry) -> map.put(entry.getKey(), entry.getValue().get(entry.getValue().size() - 1)),
                        HashMap::putAll);
    }

    private int calculateNeededAdditionalResource(FireResourceMetadata resource) {
        int lastFirefightersCount = getLastFirefightersCount(resource);
        return resource.getOptimalFireFighterCount() - lastFirefightersCount;
    }

    private int getLastFirefightersCount(FireResourceMetadata resource) {
        List<FireResourceMetadata> resourceMetadata = idToFireInformation.get(resource.getId());
        if (resourceMetadata == null || resourceMetadata.isEmpty()) return 0;
        var lastResourceMetadata = resourceMetadata.get(resourceMetadata.size() - 1);
        return lastResourceMetadata.getFirefightersCount();
    }

    private List<FirefighterAgent> getFreeFirefighters() {
        return agentDashboard.getFirefighterAgents()
                .stream()
                .filter(not(FirefighterAgent::isBusy))
                .collect(Collectors.toList());
    }

    private FireResourceMetadata createFireResourceMetadata(Map.Entry<String, Set<ForestPixel>> entry) {
        double fireSpeed = calculateFireSpeed(entry.getValue());
        int optimalFirefightersCount = calculateOptimalFirefightersCount(entry.getValue());
        AdditionalResourceNeeded additionalResourceNeeded = areNeededAdditionalResources(idToFireInformation.get(entry.getKey()), optimalFirefightersCount);
        var previouslySetFireFightersCount = getPreviouslySetFireFightersCount(idToFireInformation.get(entry.getKey()));
        return new FireResourceMetadata(entry.getKey(), previouslySetFireFightersCount, optimalFirefightersCount, fireSpeed, additionalResourceNeeded);
    }

    public int getPreviouslySetFireFightersCount(List<FireResourceMetadata> statistics) {
        if (statistics == null || statistics.isEmpty()) return 0;
        return statistics.get(statistics.size() - 1).getFirefightersCount();
    }

    public AdditionalResourceNeeded areNeededAdditionalResources(List<FireResourceMetadata> statistics, int optimalFirefightersCount) {
        if (statistics == null || statistics.isEmpty()) return START;
        AdditionalResourceNeeded lastAdditionalResourceNeeded = statistics.get(statistics.size() - 1).getAdditionalResourceNeeded();
        if (statistics.size() < MINIMUM_VALUE_OF_REALLOCATION) {
            return lastAdditionalResourceNeeded;
        }
        if (lastAdditionalResourceNeeded == YES) return YES;

        List<Integer> lastFourOptimalFireFighterStatistics = statistics.subList(statistics.size() - 4, statistics.size())
                .stream()
                .map(FireResourceMetadata::getOptimalFireFighterCount)
                .collect(Collectors.toList());
        int lastFirefightersCount = statistics.get(statistics.size() - 1).getFirefightersCount();
        if (lastFirefightersCount > 20 && lastFirefightersCount * 0.3 <= optimalFirefightersCount) return YES;
        lastFourOptimalFireFighterStatistics.add(optimalFirefightersCount);
        if (IntStream.range(0, lastFourOptimalFireFighterStatistics.size() - 1)
                .allMatch(index -> lastFourOptimalFireFighterStatistics.get(index + 1) > lastFourOptimalFireFighterStatistics.get(index)))
            return YES;
        return NO;
    }

    private double calculateFireSpeed(Set<ForestPixel> burningPixels) {
        return burningPixels
                .stream()
                .map(pixel -> pixel.getFireParameter().getFireSpeedSpreed())
                .mapToDouble(value -> value)
                .sum();
    }

    public int calculateOptimalFirefightersCount(Set<ForestPixel> burningPixels) {
        return burningPixels.stream()
                .map(pixel -> pixel.getFireParameter().getForestFireState())
                .map(fireStateToFirefightersCount::get)
                .mapToInt(value -> value)
                .sum();
    }


    public long calculateFreeFireControllerAgentCount() {
        return agentDashboard.getFireControllerAgents()
                .stream()
                .filter(not(FireControllerAgent::isBusy))
                .count();
    }


    //TODO check availability of FireControllerAgent
    //TODO check limit of firefgihters


}
