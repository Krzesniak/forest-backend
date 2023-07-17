package pl.krzesniak.service;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pl.krzesniak.exception.ForestPixelNotFoundException;
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
    private Set<ForestPixel> foundBurningPixels = new HashSet<>();
    private Map<String, String> removedZoneByZoneToJoin;

    public Map<String, Set<ForestPixel>> findAllConnectedBurningPixels(Set<ForestPixel> pixels) {
        pixels.forEach(this::findAllConnectedBurningPixels);
        removeDuplicatesInCaseOfCombiningZones();
        return getOnlyCurrentlyBurnedPixels();
    }

    private void removeDuplicatesInCaseOfCombiningZones() {
        removedZoneByZoneToJoin = new HashMap<>();

        // Remove keys where other keys contain all their values
        idSensorToBurnedFields.entrySet().forEach(entry1 -> {
            String removedKey = entry1.getKey();
            if (idSensorToBurnedFields.entrySet().stream()
                    .filter(entry2 -> entry1 != entry2)
                    .anyMatch(entry2 -> entry2.getValue().containsAll(entry1.getValue()))) {
                String containingKey = idSensorToBurnedFields.entrySet().stream()
                        .filter(entry2 -> entry1 != entry2 && entry2.getValue().containsAll(entry1.getValue()))
                        .map(Map.Entry::getKey)
                        .findFirst()
                        .orElse("");
                removedZoneByZoneToJoin.put(removedKey, containingKey);
            }
        });

        // Remove the keys from the map
        removedZoneByZoneToJoin.keySet().forEach(idSensorToBurnedFields::remove);

    }

    public void findAllConnectedBurningPixels(ForestPixel pixel) {
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
    }

    private void recursiveFindingConnectedBurningPixels(ForestPixel pixel) {
        pixelsVisited.add(pixel);
        Set<ForestPixel> foundNewBurnedFields = forestPixelHelper.
                createTestingSurroundingsForPixel(pixel.getId())
                .stream()
                .flatMap(forestPixel -> forestPixelHelper.
                        createTestingSurroundingsForPixel(pixel.getId())
                        .stream())
                .filter(forestPixel -> forestPixel.isBeingBurned() || forestPixel.isBeingExtinguish())
                .filter(forestPixel -> !pixelsVisited.contains(forestPixel))
                .collect(Collectors.toSet());
        if (foundNewBurnedFields.isEmpty()) return;
        foundBurningPixels.addAll(foundNewBurnedFields);
        foundNewBurnedFields.forEach(this::recursiveFindingConnectedBurningPixels);
    }

    public Map<String, Set<ForestPixel>> getOnlyCurrentlyBurnedPixels() {
        return this.idSensorToBurnedFields.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().stream().filter(ForestPixel::isBeingBurned).collect(Collectors.toSet())));

    }

    public Map<String, Set<ForestPixel>> getAndClearPixelsByZoneId(Set<String> zoneId) {
        var pixels = idSensorToBurnedFields.entrySet()
                .stream()
                .filter(entry -> zoneId.contains(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        zoneId.forEach(id -> idSensorToBurnedFields.remove(id));
        return pixels;
    }

    public void updateCurrentlyExtinguishedFields() {
        idSensorToBurnedFields = idSensorToBurnedFields.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue()
                        .stream()
                        .map(pixel -> forestPixelHelper.findForestPixelById(pixel.getId()).orElseThrow(ForestPixelNotFoundException::new))
                        .collect(Collectors.toSet())));
    }
}
