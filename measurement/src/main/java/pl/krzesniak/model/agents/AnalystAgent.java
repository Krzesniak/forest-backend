package pl.krzesniak.model.agents;


import pl.krzesniak.model.ForestPixel;
import pl.krzesniak.service.PixelMathCalculator;

import java.util.*;
import java.util.stream.Collectors;

public class AnalystAgent extends Agent {

    public final static double DANGEROUS_INDEX_VALUE = 16;
    private final PixelMathCalculator pixelMathCalculator;

    private Map<String, List<ForestPixel>> dangerousForestPixels;
    private Map<String, List<ForestPixel>> burningPixels;
    private List<ForestPixel> previousBurningPixels = new ArrayList<>();
    private List<ForestPixel> previousDangerousFields = new ArrayList<>();


    public AnalystAgent(String id) {
        this.id = id;
        dangerousForestPixels = new HashMap<>();
        burningPixels = new HashMap<>();
        pixelMathCalculator = new PixelMathCalculator();
    }

    public void analyzeForestFields(List<ForestPixel> pixels, String id) {
        findDangerousPixels(pixels, id);
        findBurningPixels(pixels, id);
    }

    private void findDangerousPixels(List<ForestPixel> pixels, String id) {
        List<ForestPixel> filteredPixels = pixels.stream()
                .filter(pixel -> pixel.getForestFireIndexValue() > DANGEROUS_INDEX_VALUE)
                .collect(Collectors.toList());
        if (!filteredPixels.isEmpty()) dangerousForestPixels.put(id, filteredPixels);
    }

    private void findBurningPixels(List<ForestPixel> pixels, String id) {
        List<ForestPixel> burningPixelsList = pixels.stream()
                .filter(ForestPixel::isBeingBurned)
                .collect(Collectors.toList());
        if (!burningPixelsList.isEmpty()) burningPixels.put(id, burningPixelsList);
    }

    public boolean arePixelsChanged() {
        return areBurnedFieldsChanged() || areDangerousFieldsChanged();
    }

    private boolean areDangerousFieldsChanged() {
        return dangerousForestPixels.values()
                .stream()
                .flatMap(Collection::stream)
                .anyMatch(forestPixel -> !previousDangerousFields.contains(forestPixel));
    }

    private boolean areBurnedFieldsChanged() {
        return burningPixels.values()
                .stream()
                .flatMap(Collection::stream)
                .anyMatch(forestPixel -> !previousBurningPixels.contains(forestPixel));
    }

    public void updatePreviousAnalyzedFields() {
        setPreviousBurningPixels();
        setPreviousDangerousFields();
    }
    private void setPreviousBurningPixels() {
        previousBurningPixels = burningPixels.entrySet().stream()
                .flatMap(entry -> entry.getValue().stream())
                .collect(Collectors.toList());
    }

    private void setPreviousDangerousFields() {
        previousDangerousFields = dangerousForestPixels.values()
                .stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }
    public List<ForestPixel> getDangerousPixelsList() {
        return getPixelsListFromMap(dangerousForestPixels);
    }
    public List<ForestPixel> getBurningPixelsList() {
        return getPixelsListFromMap(burningPixels);
    }

    public List<ForestPixel> getPixelsListFromMap(Map<String, List<ForestPixel>> idToForestPixels) {
        return idToForestPixels.values()
                .stream()
                .flatMap(Collection::stream)
                .toList();
    }

    public Map<String, List<ForestPixel>> getNewBurnedForestPixels() {
        return pixelMathCalculator.differentiateMapValuesFromListValues(burningPixels, previousBurningPixels);
    }

    public Map<String, List<ForestPixel>> getNewDangerousForestPixels() {
        return pixelMathCalculator.differentiateMapValuesFromListValues(dangerousForestPixels, previousDangerousFields);
    }
}
