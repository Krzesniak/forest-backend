package pl.krzesniak.service;

import org.springframework.stereotype.Component;
import pl.krzesniak.model.ForestPixel;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

@Component
public class PixelMathCalculator {

    public int getMinRange(int range, int windowSize) {
        return IntStream.iterate(range, r -> r - 1)
                .limit(Math.round(windowSize / 2.0))
                .filter(number -> number >= 0)
                .min()
                .orElse(0);
    }

    public int getMaxRange(int range, int windowSize, int maxRange) {
        return IntStream.iterate(range, r -> r + 1)
                .limit(Math.round(windowSize / 2.0))
                .filter(number -> number <= maxRange)
                .max()
                .orElse(range);
    }

    public Map<String, List<ForestPixel>> differentiateMapValuesFromListValues(
            Map<String, List<ForestPixel>> forestPixelMap, List<ForestPixel> previous) {
        Map<String, List<ForestPixel>> filteredPixels = forestPixelMap.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> entry.getValue()
                                .stream()
                                .filter(pixel -> !previous.contains(pixel))
                                .collect(Collectors.toList())
                ));
        filteredPixels.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        return filteredPixels;
    }

    public Integer[] retrieveRowAndColumnNumberFromId(String id) {
        return Arrays.stream(id.split(":")).map(Integer::parseInt).toArray(Integer[]::new);
    }

    public double calculateDistanceBetweenPixels(String id1, String id2) {
        Integer[] coordinates1 = retrieveRowAndColumnNumberFromId(id1);
        Integer[] coordinates2 = retrieveRowAndColumnNumberFromId(id2);
        return sqrt(pow(coordinates1[0] - coordinates2[0], 2) + pow(coordinates1[1] - coordinates2[1], 2));
    }

    public boolean isBorderPixel(String id, Set<String> pixelId) {
        return  !pixelId.containsAll(createBorderForPixel(id));
    }

    public Set<String> createBorderForPixel(String id) {
        return Set.of(
                getPixelFromCurrentPixelLocation(id, 1,0),
                getPixelFromCurrentPixelLocation(id, 0,1),
                getPixelFromCurrentPixelLocation(id, -1,0),
                getPixelFromCurrentPixelLocation(id, 0,-1)
        );

    }

    public String getPixelFromCurrentPixelLocation(String pixelId, int rowTransition, int columnTransition) {
        Integer[] rowAndColumnNumber = retrieveRowAndColumnNumberFromId(pixelId);
        rowAndColumnNumber[0] += rowTransition;
        rowAndColumnNumber[1] += columnTransition;
        return rowAndColumnNumber[0] + ":" + rowAndColumnNumber[1];
    }
}
