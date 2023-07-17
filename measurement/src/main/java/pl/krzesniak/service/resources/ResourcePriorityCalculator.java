package pl.krzesniak.service.resources;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record ResourcePriorityCalculator(Map<String, Double> fireZoneIdToPriority,
                                         Map<String, Integer> fireZoneIdToNeededAdditionalResources) {

    public static ResourcePriorityCalculator calculatePriorityAndAdditionalResources(List<FireResourceMetadata> resourceMetadata,
                                                                                     Map<String, List<FireResourceMetadata>> previousResourceMetadata) {
        return new ResourcePriorityCalculator(calculatePriorityForFireZones(resourceMetadata), calculateNeededAdditionalResource(resourceMetadata, previousResourceMetadata));
    }


    public static Map<String, Double> calculatePriorityForFireZones(List<FireResourceMetadata> resourceMetadata) {
        var overallDangerousValue = calculateOverallDangerousValue(resourceMetadata);
        return resourceMetadata.stream()
                .collect(Collectors.toMap(FireResourceMetadata::getId,
                        resource -> resource.getDangerousValue() / overallDangerousValue));

    }

    public static double calculateOverallDangerousValue(List<FireResourceMetadata> resourceMetadata) {
        return resourceMetadata.stream()
                .map(FireResourceMetadata::getDangerousValue)
                .mapToDouble(val -> val)
                .sum();
    }

    private static int calculateNeededAdditionalResource(FireResourceMetadata resource, Map<String, List<FireResourceMetadata>> previousResourceMetadata) {
        int lastFirefightersCount = getLastFirefightersCount(resource, previousResourceMetadata);
        return resource.getOptimalFireFighterCount() - lastFirefightersCount;
    }

    public static Map<String, Integer> calculateNeededAdditionalResource(List<FireResourceMetadata> resourceMetadata, Map<String, List<FireResourceMetadata>> previousResourceMetadata) {
        return resourceMetadata.stream()
                .collect(Collectors.toMap(FireResourceMetadata::getId, entry -> calculateNeededAdditionalResource(entry, previousResourceMetadata)));
    }

    private static int getLastFirefightersCount(FireResourceMetadata resource, Map<String, List<FireResourceMetadata>> previousResourceMetadata) {
        List<FireResourceMetadata> resourceMetadata = previousResourceMetadata.get(resource.getId());
        if (resourceMetadata == null || resourceMetadata.isEmpty()) return 0;
        var lastResourceMetadata = resourceMetadata.get(resourceMetadata.size() - 1);
        return lastResourceMetadata.getFirefightersCount();
    }
}
