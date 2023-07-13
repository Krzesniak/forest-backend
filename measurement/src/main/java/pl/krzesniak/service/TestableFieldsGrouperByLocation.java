package pl.krzesniak.service;

import lombok.Data;
import org.springframework.stereotype.Component;
import pl.krzesniak.model.ForestPixel;

import java.util.*;
import java.util.stream.Collectors;

@Component
@Data
public class TestableFieldsGrouperByLocation {

    private final ForestPixelHelper forestPixelHelper;
    private ForestPixel[][] board;
    private Set<ForestPixel> pixelsVisited;
    private Set<ForestPixel> pixelsGroupedByNeighbours;
    private Map<String, Set<ForestPixel>> idGroupToTestableFields;

    public TestableFieldsGrouperByLocation(ForestPixelHelper forestPixelHelper) {
        this.forestPixelHelper = forestPixelHelper;
        this.idGroupToTestableFields = new HashMap<>();
        pixelsVisited = new HashSet<>();
        pixelsGroupedByNeighbours = new HashSet<>();
    }

    public Map<String, Set<ForestPixel>> groupFieldsByNeighbours(Set<ForestPixel> pixels) {
        idGroupToTestableFields =  idGroupToTestableFields.entrySet()
                .stream()
                .map(entry -> {
                    pixelsGroupedByNeighbours = new HashSet<>();
                    entry.getValue()
                            .forEach(forestPixel -> recursiveFindingConnectedPixels(pixels, forestPixel));
                    return new AbstractMap.SimpleEntry<>(entry.getKey(), pixelsGroupedByNeighbours);
                })
                .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));
        for (ForestPixel pixel : pixels) {
            if (pixelsVisited.contains(pixel)) continue;
            pixelsGroupedByNeighbours = new HashSet<>();
            recursiveFindingConnectedPixels(pixels, pixel);
            idGroupToTestableFields.put(pixel.getId(), pixelsGroupedByNeighbours);
        }
        return idGroupToTestableFields;
    }

    private void recursiveFindingConnectedPixels(Set<ForestPixel> pixels, ForestPixel pixel) {
        pixelsGroupedByNeighbours.add(pixel);
        pixelsVisited.add(pixel);
        forestPixelHelper.
                createTestingSurroundingsForPixel(pixel.getId())
                .stream()
                .filter(pixels::contains)
                .filter(forestPixel -> !pixelsVisited.contains(forestPixel))
                .forEach(lambdaPixel -> recursiveFindingConnectedPixels(pixels, lambdaPixel));
    }
}
