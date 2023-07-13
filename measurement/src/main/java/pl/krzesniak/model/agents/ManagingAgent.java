package pl.krzesniak.model.agents;

import lombok.Data;
import pl.krzesniak.model.ForestPixel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ManagingAgent extends Agent {


    public static final int DANGEROUS_FOREST_VALUE = 40;
    public static final int MIN_FOREST_VALUE_FOR_TESTERS = 22;
    public static final int MIN_NUMBER_OF_DANGEROUS_NEIGHBOURS_FIELDS = 2;
    public DangerousFieldGrouper dangerousFieldGrouper;

    public ManagingAgent(String id) {
        this.id = id;
        dangerousFieldGrouper = new DangerousFieldGrouper();
    }

    public void groupFieldsByDangerousDegree() {
        var dangerousPixels = dangerousFieldGrouper.getDangerousFields();
        dangerousFieldGrouper.setDangerousFields(findDangerousFields(dangerousPixels));
        dangerousFieldGrouper.setDangerousFieldsCauseOfNeighbours(findDangerousFieldsCauseOfNeighbours(dangerousPixels));
        dangerousFieldGrouper.setNonDangerousFields(findNonDangerousFields(dangerousPixels));
    }

    private List<ForestPixel> findDangerousFields(List<ForestPixel> pixels) {
        return pixels.stream()
                .filter(pixel -> pixel.getForestFireIndexValue() > DANGEROUS_FOREST_VALUE)
                .collect(Collectors.toList());
    }

    private List<ForestPixel> findDangerousFieldsCauseOfNeighbours(List<ForestPixel> dangerousPixels) {
        return dangerousPixels.stream()
                .filter(pixel -> pixel.getForestFireIndexValue() < DANGEROUS_FOREST_VALUE &&
                        pixel.getForestFireIndexValue() > MIN_FOREST_VALUE_FOR_TESTERS)
                .filter(pixel ->
                        this.calculateNumberOfNeighbours(pixel, dangerousPixels) > MIN_NUMBER_OF_DANGEROUS_NEIGHBOURS_FIELDS)
                .collect(Collectors.toList());
    }

    private List<ForestPixel> findNonDangerousFields(List<ForestPixel> dangerousPixels) {
        return dangerousPixels.stream()
                .filter(pixel -> !dangerousFieldGrouper.getDangerousFields().contains(pixel))
                .filter(pixel -> !dangerousFieldGrouper.getDangerousFieldsCauseOfNeighbours().contains(pixel))
                .collect(Collectors.toList());
    }

    public int calculateNumberOfNeighbours(ForestPixel pixel, List<ForestPixel> pixels) {
        int numberOfNeighbours = 0;
        String[] split = pixel.getId().split(":");
        Integer[] rowAndColumnId = Arrays.stream(split)
                .map(Integer::parseInt)
                .toArray(Integer[]::new);
        for (int i = rowAndColumnId[0] - 1; i <= rowAndColumnId[0] + 1; i++) {
            for (int j = rowAndColumnId[1] - 1; j <= rowAndColumnId[1] + 1; j++) {
                String id = i + ":" + j;
                if (pixel.getId().equals(id)) continue;
                boolean hasNeighbours = pixels.stream()
                        .map(ForestPixel::getId)
                        .anyMatch(ids -> ids.equals(id));
                if (hasNeighbours) numberOfNeighbours++;
            }
        }
        return numberOfNeighbours;
    }

    @Data
    public static class DangerousFieldGrouper {
        private List<ForestPixel> dangerousFields;
        private List<ForestPixel> dangerousFieldsCauseOfNeighbours;
        private List<ForestPixel> nonDangerousFields;
        private List<ForestPixel> burningFields;

        public DangerousFieldGrouper(){
            dangerousFields = new ArrayList<>();
            dangerousFieldsCauseOfNeighbours = new ArrayList<>();
            nonDangerousFields = new ArrayList<>();
            burningFields = new ArrayList<>();
        }
    }

}
