package pl.krzesniak.service;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pl.krzesniak.model.ForestPixel;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Data
public class FireGroupFinder {

    private final ForestPixelHelper forestPixelHelper;

    private List<ForestPixel> pixelsVisited;
    private Map<String, Set<ForestPixel>> idSensorToBurnedFields = new HashMap<>();
    private Map<String, Set<ForestPixel>> idSensorToAlreadyExtinguishFields = new HashMap<>();
    private Set<ForestPixel> foundBurningPixels = new HashSet<>();

    public Map<String, Set<ForestPixel>> findAllConnectedBurningPixels(Set<ForestPixel> pixels) {
        pixels.forEach(this::findAllConnectedBurningPixels);
        return idSensorToBurnedFields;
    }
    public Set<ForestPixel> findAllConnectedBurningPixels(ForestPixel pixel) {
        pixelsVisited = new ArrayList<>();
        foundBurningPixels = new HashSet<>();
        foundBurningPixels.add(pixel);
        recursiveFindingConnectedBurningPixels(pixel);
        String foundIdSensorOfExistingFire = idSensorToBurnedFields.entrySet()
                .stream()
                .filter(entry -> entry.getValue().stream().anyMatch(forestPixel -> foundBurningPixels.contains(forestPixel)))
                .map(Map.Entry::getKey)
                .findAny()
                .orElse(pixel.getId());
        idSensorToBurnedFields.put(foundIdSensorOfExistingFire, foundBurningPixels);
        idSensorToAlreadyExtinguishFields.putIfAbsent(foundIdSensorOfExistingFire, new HashSet<>());
        return foundBurningPixels;
    }

    private void recursiveFindingConnectedBurningPixels(ForestPixel pixel) {
        pixelsVisited.add(pixel);
        Set<ForestPixel> foundNewBurnedFields = forestPixelHelper.
                createSurroundingsForPixel(pixel.getId())
                .stream()
                .flatMap(forestPixel -> forestPixelHelper.
                        createSurroundingsForPixel(pixel.getId())
                        .stream())
                .filter(forestPixel -> forestPixel.isBeingBurned() || forestPixel.isBeingExtinguish())
                .filter(forestPixel -> !pixelsVisited.contains(forestPixel))
                .collect(Collectors.toSet());
        if (foundNewBurnedFields.isEmpty()) return;
        foundBurningPixels.addAll(foundNewBurnedFields);
        foundNewBurnedFields.forEach(this::recursiveFindingConnectedBurningPixels);
    }
}
