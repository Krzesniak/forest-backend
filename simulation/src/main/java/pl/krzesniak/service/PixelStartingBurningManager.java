package pl.krzesniak.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pl.krzesniak.model.ForestPixel;
import pl.krzesniak.model.enums.ForestFireBurnedColor;
import pl.krzesniak.model.enums.Terrain;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PixelStartingBurningManager {

    public static final int MIN_FIELD_DAMAGE_DESTROYED = 20;
    private final FireCalculator fireCalculator;
    public static final double SELF_BURNING_COEFFICIENT = 400.0;
    public static final double NEIGHBOUR_FIRE_COEFFICIENT = 0.20;

    public void setPixelValueToBeBurned(ForestPixel pixel) {
        pixel.getFireParameter().setBeingBurned(true);
        pixel.getFireParameter().setFieldPercentageDestroyed(1);
        pixel.getFireParameter().setForestFireBurnedColor(ForestFireBurnedColor.LEVEL1.getRgbColorValue());
        double fireSpeed = fireCalculator.calculateOwnFireSpreed(pixel);
        pixel.getFireParameter().setFireSpeed(fireSpeed);
        pixel.getFireParameter().setForestFireState(fireCalculator.convertToForestFireState(fireSpeed));
    }

    public boolean isPixelStartingBurning(ForestPixel forestPixel, List<ForestPixel> neighbours) {
        if (forestPixel.getTerrain() == Terrain.SAND || forestPixel.getTerrain() == Terrain.WATER) return false;
        double probability = calculateProbabilityOfPixelBurning(forestPixel, neighbours);
        return Math.random() * (0.85) + 0.15 <= probability;
    }

    public double calculateProbabilityOfPixelBurning(ForestPixel forestPixel, List<ForestPixel> neighbours) {
        var probabilityOfPixelBurningItself = calculateProbabilityOfPixelBurningItself(forestPixel);
        var probabilityOfPixelBurningBasedOnNeighbours = calculateProbabilityOfPixelBurningBasedOnNeighbours(neighbours);
        return probabilityOfPixelBurningItself + probabilityOfPixelBurningBasedOnNeighbours * NEIGHBOUR_FIRE_COEFFICIENT;
    }


    public double calculateProbabilityOfPixelBurningBasedOnNeighbours(List<ForestPixel> neighbours) {
        return neighbours.stream()
                .filter(ForestPixel::isBeingBurned)
                .filter(this::canNeighbourWidespreadFire)
                .map(this::calculateProbabilityOfPixelBurning)
                .mapToDouble(value -> value)
                .sum();
    }

    public double calculateProbabilityOfPixelBurningItself(ForestPixel forestPixel) {
        return forestPixel.getForestFireIndexValue() / SELF_BURNING_COEFFICIENT;
    }

    public double calculateProbabilityOfPixelBurning(ForestPixel forestPixel) {
        return forestPixel.getFireParameter().getFireSpeed() * 0.2;
    }

    public boolean canNeighbourWidespreadFire(ForestPixel pixel) {
        return pixel.getFireParameter().getFieldPercentageDestroyed() > MIN_FIELD_DAMAGE_DESTROYED
                || pixel.getFireParameter().getFireSpeed() > 4.5;
    }
}
