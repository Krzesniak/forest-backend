package pl.krzesniak.service;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import pl.krzesniak.exception.ForestPixelCalculationException;
import pl.krzesniak.model.ForestPixel;

import java.util.*;


@Data
public class ForestPixelHelper {

    private ForestPixel[][] board;

    private final PixelMathCalculator pixelMathCalculator;

    private final int boardWidth;

    private final int boardHeight;

    private final int boardWindowSize;

    private final int boardWindowTestingSize;

    public ForestPixelHelper(int boardWidth, int boardHeight, int boardWindowSize, int boardWindowTestingSize) {
        this.pixelMathCalculator = new PixelMathCalculator();
        this.boardWidth = boardWidth;
        this.boardHeight = boardHeight;
        this.boardWindowSize = boardWindowSize;
        this.boardWindowTestingSize = boardWindowTestingSize;
    }

    public Optional<ForestPixel> findForestPixelById(String id) {
        return Arrays.stream(this.board)
                .flatMap(Arrays::stream)
                .filter(forestPixel -> forestPixel.getId().equals(id))
                .findFirst();
    }

    public List<ForestPixel> createSurroundingsForPixel(String id) {
        return createSurroundings(id, boardWindowSize);
    }

    public List<ForestPixel> createTestingSurroundingsForPixel(String id) {
        return createSurroundings(id, boardWindowTestingSize);
    }

    public List<ForestPixel> createSurroundings(String id, int windowSize) {
        Integer[] rowAndColumn = pixelMathCalculator.retrieveRowAndColumnNumberFromId(id);
        int rowNumber = rowAndColumn[0];
        int columnNumber = rowAndColumn[1];
        List<ForestPixel> forestPixels = new ArrayList<>();
        int maxColumnRange = pixelMathCalculator.getMaxRange(columnNumber, windowSize, boardWidth-1);
        int minColumnRange = pixelMathCalculator.getMinRange(columnNumber, windowSize);
        int maxRowRange = pixelMathCalculator.getMaxRange(rowNumber, windowSize, boardHeight-1);
        int minRowRange = pixelMathCalculator.getMinRange(rowNumber, windowSize);
        for (int i = minRowRange; i <= maxRowRange; i++) {
            forestPixels.addAll(Arrays.asList(board[i]).subList(minColumnRange, maxColumnRange + 1));
        }
        return forestPixels;
    }

    public String chooseFromThePixelListTheClosestPixelToGivenPixel(List<String> ids, String givenPixelId) {
        return ids.stream()
                .map(id -> new PixelAndDistance(id, pixelMathCalculator.calculateDistanceBetweenPixels(id, givenPixelId)))
                .min(Comparator.comparing(PixelAndDistance::distance))
                .map(PixelAndDistance::id)
                .orElseThrow( () -> new ForestPixelCalculationException("There is not value"));
    }

    public record PixelAndDistance(String id, double distance) {};

}
