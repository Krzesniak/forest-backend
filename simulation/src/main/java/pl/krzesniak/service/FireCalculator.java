package pl.krzesniak.service;

import org.springframework.stereotype.Service;
import pl.krzesniak.model.ForestPixel;
import pl.krzesniak.model.enums.ForestFireBurnedColor;
import pl.krzesniak.model.enums.ForestFireState;

import java.util.List;

import static pl.krzesniak.model.enums.ForestFireState.*;


@Service
public class FireCalculator {

    public static final double A_COEFFICIENT = 0.03;
    public static final double B_COEFFICIENT = 0.05;
    public static final double C_COEFFICIENT = 0.01;
    public static final double D_COEFFICIENT = 0.03;
    public static final double NEIGHBOUR_FIRE_COEFFICIENT = 0.15;
    public static final double NEXT_ITERATION_FIRE_COEFFICIENT = 0.10;


    public double calculateFirePixelDamage(ForestPixel forestPixel, List<ForestPixel> neighbours) {

        double ownForestFireSpread = calculateOwnFireSpreed(forestPixel);
        double neighbourForestFireSpread = NEIGHBOUR_FIRE_COEFFICIENT * calculateFireSpreedForNeighbours(neighbours);
        double currentIterationFireSpread = ownForestFireSpread + neighbourForestFireSpread;
        if (!forestPixel.isBeingBurned()) return currentIterationFireSpread;
        return forestPixel.getFireParameter().getFireSpeedSpreed() + NEXT_ITERATION_FIRE_COEFFICIENT * currentIterationFireSpread;
    }

    public double calculateOwnFireSpreed(ForestPixel pixel) {
        double W = (int) Math.pow((pixel.getWind().speed() / 0.836), 2.0 / 3);
        double R0 = A_COEFFICIENT * pixel.getTemperature() + B_COEFFICIENT * W + C_COEFFICIENT * (100 - pixel.getHumidity()) - D_COEFFICIENT;
        double K_PHI = Math.exp((0.1783 * pixel.getWind().speed() * 0.342));
        double K0 = Math.exp((pixel.getForestFireIndexValue() - 2) * (1.6 - 0.1) / (50 - 2) + 0.1);
        double TK = 0.4;
        return R0 * K_PHI * K0 * TK;
    }

    public double calculateFireSpreedForNeighbours(List<ForestPixel> neighbours) {
        return neighbours.stream()
                .filter(ForestPixel::isBeingBurned)
                .map(this::calculateOwnFireSpreed)
                .mapToDouble(value -> value)
                .sum();
    }

    public ForestFireState convertToForestFireState(double fireSpeed) {
        if(fireSpeed == 0) return NONE;
        else if(fireSpeed <= 1.25) return LOW;
        else if(fireSpeed <= 2.5) return MEDIUM;
        else if(fireSpeed <= 5.0) return HIGH;
        else return EXTREME;
    }

    public void updatePixelBurning(ForestPixel pixel, double firePixelDamage) {
        pixel.getFireParameter().setFireSpeedSpreed(firePixelDamage);
        pixel.getFireParameter().setFieldPercentageDestroyed(pixel.getFireParameter().getFieldPercentageDestroyed() + firePixelDamage);
        pixel.getFireParameter().setForestFireState(convertToForestFireState(firePixelDamage));
        pixel.getFireParameter().setForestFireBurnedColor(ForestFireBurnedColor.convertBurnedFieldPercentageToColorValue(
                (int) pixel.getFireParameter().getFieldPercentageDestroyed()));

        if(pixel.getFireParameter().getFieldPercentageDestroyed() >= 100.0) {
            pixel.getFireParameter().setForestFireState(DESTROYED);
        }
    }

  //  public void setPixelAsDestroyed()
}
