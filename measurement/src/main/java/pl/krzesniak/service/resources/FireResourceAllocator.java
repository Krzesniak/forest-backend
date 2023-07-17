package pl.krzesniak.service.resources;

import lombok.Data;
import org.springframework.stereotype.Service;
import pl.krzesniak.exception.ForestPixelNotFoundException;
import pl.krzesniak.model.ForestPixel;
import pl.krzesniak.model.enums.ForestFireState;
import pl.krzesniak.service.AgentDashboard;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.function.Predicate.not;
import static pl.krzesniak.service.resources.AdditionalResourceNeeded.*;

@Service
@Data
public class FireResourceAllocator {

    public static final int MINIMUM_VALUE_OF_REALLOCATION = 4;
    private final Map<ForestFireState, Integer> fireStateToFirefightersCount;
    private Map<String, List<FireResourceMetadata>> idToFireInformation;
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


    public Map<String, FireResourceMetadata> computeFireAllocation(Map<String, Set<ForestPixel>> burningPixels) {
        List<FireResourceMetadata> updatedFireResourceMetadata = createFireResourceData(burningPixels);
        clearExtinguishedZoneIds(updatedFireResourceMetadata);
        int freeFirefightersCount = agentDashboard.getFreeFirefightersCount();
        int freeFireControllerAgentCount = agentDashboard.calculateFreeFireControllerAgentCount();

        if (isNeededMoreResourceForAnyCurrentlyExtinguishedFireZone(updatedFireResourceMetadata)) {

            List<FireResourceMetadata> updatedFireResourceMetadataOnlyBeingExtinguished = getFireResourceOnlyBeingExtinguished(updatedFireResourceMetadata);
            var priorityCalculator = ResourcePriorityCalculator.
                    calculatePriorityAndAdditionalResources(updatedFireResourceMetadataOnlyBeingExtinguished, idToFireInformation);

            priorityCalculator.fireZoneIdToPriority().entrySet()
                    .stream()
                    .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                    .forEach(entry -> {
                        int addedResourceCount = Math.max(0,calculateAddedFirefightersCount(freeFirefightersCount, priorityCalculator, entry));
                        agentDashboard.assignFirefightersToFireZone(addedResourceCount, entry.getKey());

                        updatedFireResourceMetadataOnlyBeingExtinguished.stream()
                                .filter(resource -> resource.getId().equals(entry.getKey()))
                                .forEach(resource -> allocateFireFightersCount(getLastFirefightersCount(resource), addedResourceCount, resource));
                    });

            assignTheRestOfRemainingResourcesToTheMostDangerousFireZone(updatedFireResourceMetadata);
        }
        addFireResourceMetadataOnlyForZonesBeingExtinguished(updatedFireResourceMetadata);
        List<FireResourceMetadata> fireResourceMetadataNewFireZones = getFireResourceForNewFireZones(updatedFireResourceMetadata);

        if (!isFreeFireControllerAgentAndNewFireZones(freeFireControllerAgentCount, fireResourceMetadataNewFireZones)) return getTheLatestFireResourceData();
        var priorityCalculator = ResourcePriorityCalculator.calculatePriorityAndAdditionalResources(fireResourceMetadataNewFireZones, idToFireInformation);

        priorityCalculator.fireZoneIdToPriority().entrySet()
                .stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(freeFireControllerAgentCount)
                .forEach(entry -> {
                    int addedResourceCount = Math.max(0, calculateAddedFirefightersCount(freeFirefightersCount, priorityCalculator, entry));
                    agentDashboard.assignFirefightersToFireZone(addedResourceCount, entry.getKey());
                    FireResourceMetadata fireResourceMetadata = updatedFireResourceMetadata.stream()
                            .filter(resource -> resource.getId().equals(entry.getKey()))
                            .peek(resource -> allocateFireFightersCount(getLastFirefightersCount(resource), addedResourceCount, resource))
                            .findFirst()
                            .orElseThrow(ForestPixelNotFoundException::new);

                    this.idToFireInformation.put(entry.getKey(), new ArrayList<>((Collections.singletonList(fireResourceMetadata))));
                    agentDashboard.assignFireControllerAgentToFireZone(entry.getKey());
                });

        assignTheRestOfRemainingResourcesToTheMostDangerousFireZone(updatedFireResourceMetadata);
        return getTheLatestFireResourceData();

    }

    private void clearExtinguishedZoneIds(List<FireResourceMetadata> updatedFireResourceMetadata) {
        Set<String> currentlyExtinguishedZonesId = updatedFireResourceMetadata.stream()
                .map(FireResourceMetadata::getId)
                .collect(Collectors.toSet());
        idToFireInformation = idToFireInformation.entrySet()
                .stream()
                .filter(entry -> currentlyExtinguishedZonesId.contains(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static List<FireResourceMetadata> getFireResourceForNewFireZones(List<FireResourceMetadata> updatedFireResourceMetadata) {
        return updatedFireResourceMetadata.stream()
                .filter(not(FireResourceMetadata::isBeingExtinguished))
                .toList();
    }

    private static List<FireResourceMetadata> getFireResourceOnlyBeingExtinguished(List<FireResourceMetadata> updatedFireResourceMetadata) {
        return updatedFireResourceMetadata.stream()
                .filter(FireResourceMetadata::isBeingExtinguished)
                .toList();
    }

    private static int calculateAddedFirefightersCount(int freeFirefightersCount, ResourcePriorityCalculator resourcePriorityCalculator, Map.Entry<String, Double> entry) {
        int additionalNeededResourceCount = resourcePriorityCalculator.fireZoneIdToNeededAdditionalResources().get(entry.getKey());
        int availableResource = (int) (freeFirefightersCount * entry.getValue());
        return Math.min(availableResource, additionalNeededResourceCount);
    }

    private static boolean isFreeFireControllerAgentAndNewFireZones(long freeFireControllerAgentCount, List<FireResourceMetadata> fireResourceMetadataNewFireZones) {
        return freeFireControllerAgentCount > 0 && fireResourceMetadataNewFireZones.size() != 0;
    }

    private void allocateFireFightersCount(int fireFightersCount, long addedResourceCount, FireResourceMetadata resourceMetadata) {
        int firefightersCount = (int) (fireFightersCount + addedResourceCount);
        resourceMetadata.setFirefightersCount(firefightersCount);
        if (firefightersCount >= resourceMetadata.getOptimalFireFighterCount())
            resourceMetadata.setAdditionalResourceNeeded(NO);
        else resourceMetadata.setAdditionalResourceNeeded(YES);
    }

    private void assignTheRestOfRemainingResourcesToTheMostDangerousFireZone(List<FireResourceMetadata> updatedFireResourceMetadata) {
        updatedFireResourceMetadata.stream()
                .filter(resource -> resource.getAdditionalResourceNeeded() == YES)
                .sorted(Comparator.comparing(FireResourceMetadata::getDangerousValue).reversed())
                .limit(1)
                .findFirst()
                .ifPresent(fireResourceMetadata -> {
                    allocateFireFightersCount(fireResourceMetadata.getFirefightersCount(), agentDashboard.getFreeFirefightersCount(), fireResourceMetadata);
                    agentDashboard.assignFirefightersToFireZone(agentDashboard.getFreeFirefightersCount(), fireResourceMetadata.getId());
                });
    }

    private boolean isNeededMoreResourceForAnyCurrentlyExtinguishedFireZone(List<FireResourceMetadata> updatedFireResourceMetadata) {
        return updatedFireResourceMetadata.stream()
                .anyMatch(resource -> resource.getAdditionalResourceNeeded() == YES);
    }

    private List<FireResourceMetadata> createFireResourceData(Map<String, Set<ForestPixel>> burningPixels) {
        return burningPixels.entrySet()
                .stream()
                .map(this::createFireResourceMetadata)
                .toList();
    }

    private FireResourceMetadata createFireResourceMetadata(Map.Entry<String, Set<ForestPixel>> entry) {
        double fireSpeed = calculateFireSpeed(entry.getValue());
        int optimalFirefightersCount = calculateOptimalFirefightersCount(entry.getValue());
        AdditionalResourceNeeded additionalResourceNeeded = areNeededAdditionalResources(idToFireInformation.get(entry.getKey()), optimalFirefightersCount);
        var previouslySetFireFightersCount = getPreviouslySetFireFightersCount(idToFireInformation.get(entry.getKey()));
        return new FireResourceMetadata(entry.getKey(), previouslySetFireFightersCount, optimalFirefightersCount, fireSpeed, additionalResourceNeeded, entry.getValue());
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


    private int getLastFirefightersCount(FireResourceMetadata resource) {
        List<FireResourceMetadata> resourceMetadata = idToFireInformation.get(resource.getId());
        if (resourceMetadata == null || resourceMetadata.isEmpty()) return 0;
        var lastResourceMetadata = resourceMetadata.get(resourceMetadata.size() - 1);
        return lastResourceMetadata.getFirefightersCount();
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
                .map(pixel -> pixel.getFireParameter().getFireSpeed())
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


}
