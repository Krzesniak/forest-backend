package pl.krzesniak.model.agents;

import lombok.Data;
import pl.krzesniak.model.ForestPixel;
import pl.krzesniak.service.PixelMathCalculator;
import pl.krzesniak.service.resources.FireResourceMetadata;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Data
public class FireControllerAgent extends Agent {

    private boolean isBusy = false;
    private String fireZoneId = "";
    private List<FirefighterAgent> firefighters;
    private PixelMathCalculator pixelMathCalculator = new PixelMathCalculator();

    public FireControllerAgent(String id) {
        this.id = id;
    }

    public void assignFirefighters(FireResourceMetadata resourceMetadata, List<FirefighterAgent> firefighters) {
        this.firefighters = firefighters;
        clearPreviousFirefighterAssignments(firefighters);
        Set<String> forestPixelsId = getForestPixelsId(resourceMetadata);
        var borderPixelsId = findBorderPixelsId(forestPixelsId);
        Set<ForestPixel> borderPixels = getForestPixels(resourceMetadata, borderPixelsId);
        double calculatedOverallFireDanger = calculateOverallFireDanger(borderPixels);
        Map<String, Double> forestPixelIdByPriority = groupPixelIdToPriority(borderPixels, calculatedOverallFireDanger);
        assignFirefighterToPixel(resourceMetadata, forestPixelIdByPriority);
        assignTheRestOfRemainingResourcesToTheMostDangerousPixel(firefighters, forestPixelIdByPriority);

    }

    private void clearPreviousFirefighterAssignments(List<FirefighterAgent> firefighters) {
        firefighters.forEach(firefighterAgent -> firefighterAgent.setCurrentExtinguishPixelId(""));
    }

    private void assignFirefighterToPixel(FireResourceMetadata resourceMetadata, Map<String, Double> forestPixelIdByPriority) {
        forestPixelIdByPriority.entrySet()
                .stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .forEach(entry -> {
                    int assignedFirefightersCount = (int) (entry.getValue() * resourceMetadata.getFirefightersCount());
                    if (assignedFirefightersCount > 0) markFieldAsExtinguished(entry.getKey(), resourceMetadata);
                    assignFireFightersToGivenPixel(assignedFirefightersCount, entry.getKey());
                });
    }

    private void markFieldAsExtinguished(String pixelId, FireResourceMetadata resourceMetadata) {
        resourceMetadata.getBurningPixels()
                .stream()
                .filter(pixel -> pixel.getId().equals(pixelId))
                .findFirst()
                .ifPresent(pixel -> pixel.getFireParameter().setBeingExtinguished(true));

    }

    private Map<String, Double> groupPixelIdToPriority(Set<ForestPixel> borderPixels, double calculatedOverallFireDanger) {
        return borderPixels.stream()
                .collect(Collectors.toMap(ForestPixel::getId, pixel -> pixel.getFireParameter().getFireSpeed() / calculatedOverallFireDanger));
    }

    private double calculateOverallFireDanger(Set<ForestPixel> borderPixels) {
        return borderPixels.stream()
                .map(forestPixel -> forestPixel.getFireParameter().getFireSpeed())
                .mapToDouble(value -> value)
                .sum();
    }

    private Set<ForestPixel> getForestPixels(FireResourceMetadata resourceMetadata, List<String> borderPixelsId) {
        return resourceMetadata.getBurningPixels()
                .stream()
                .filter(pixel -> borderPixelsId.contains(pixel.getId()))
                .collect(Collectors.toSet());
    }

    private Set<String> getForestPixelsId(FireResourceMetadata resourceMetadata) {
        return resourceMetadata.getBurningPixels()
                .stream()
                .map(ForestPixel::getId)
                .collect(Collectors.toSet());
    }

    private void assignTheRestOfRemainingResourcesToTheMostDangerousPixel(List<FirefighterAgent> firefighters, Map<String, Double> forestPixelIdByPriority) {
        var unAssignedFirefighters = firefighters.stream().filter(firefighterAgent -> firefighterAgent.getCurrentExtinguishPixelId().equals("")).collect(Collectors.toList());
        AtomicInteger unAssignedFirefightersCount = new AtomicInteger(unAssignedFirefighters.size());
        forestPixelIdByPriority.entrySet()
                .stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .forEach(entry -> {
                    if(unAssignedFirefightersCount.get() > 0) {
                        FirefighterAgent firefighterAgent = unAssignedFirefighters.get(0);
                        firefighterAgent.setCurrentExtinguishPixelId(entry.getKey());
                        unAssignedFirefighters.remove(firefighterAgent);
                        unAssignedFirefightersCount.getAndDecrement();
                    }
                });
    }

    private List<String> findBorderPixelsId(Set<String> forestPixelsId) {
        return forestPixelsId.stream()
                .filter(pixelId -> pixelMathCalculator.isBorderPixel(pixelId, forestPixelsId))
                .toList();
    }

    public void assignFireFightersToGivenPixel(int count, String pixelId) {
        this.firefighters.stream()
                .filter(firefighterAgent -> firefighterAgent.getCurrentExtinguishPixelId().equals(""))
                .limit(count)
                .forEach(firefighterAgent -> firefighterAgent.setCurrentExtinguishPixelId(pixelId));

    }


}
