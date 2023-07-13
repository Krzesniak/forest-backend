package pl.krzesniak.model;

import lombok.*;
import pl.krzesniak.model.enums.ForestFireBurnedColor;
import pl.krzesniak.model.enums.ForestFireIndex;
import pl.krzesniak.model.enums.ForestFireState;
import pl.krzesniak.model.enums.Terrain;

import java.util.Random;
import java.util.stream.Stream;

import static java.lang.Math.exp;
import static java.lang.Math.log;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ForestPixel {

    private String id;
    private Terrain terrain;
    private double forestFireIndexValue;
    private ForestFireIndex forestFireIndex;
    private double temperature;
    private double humidity;
    private Wind wind;
    private AgentParameters agentParameters = new AgentParameters(false, false, false, false);
    private FireParameter fireParameter;

    public void applyProbability() {
        if (terrain == Terrain.SAND || terrain == Terrain.WATER) return;
        Random rand = new Random();
        int maximumProbabilityValue = 20;
        Double[] probabilities = Stream.iterate(0, n -> n + 1).map(element -> (double)
                ((rand.nextInt(maximumProbabilityValue * 2)) - maximumProbabilityValue) / 100.0).limit(4).toArray(Double[]::new);
        temperature += temperature * probabilities[0];
        humidity += temperature * probabilities[1];
        double newWindDirection = wind.direction() + wind.direction() * probabilities[2];
        double newWindStrength = wind.speed() + wind.speed() * probabilities[3];
        wind = new Wind(newWindDirection, newWindStrength);
        calculateForestFireIndexValue();
    }

    public void calculateForestFireIndexValue() {
        double droughtFactor = Math.max(getRandomDroughtFactor(), 4);
        double exponent = -0.45 + 0.987 * log(droughtFactor) - 0.0345 * getHumidity() + 0.0338 * getTemperature() + 0.0234 * getWind().speed();
        this.forestFireIndexValue = 2 * exp(exponent);
        this.forestFireIndex = converForestFireValueToForestFireIndex(this.forestFireIndexValue);
    }

    public void resetFireParameter() {
        this.fireParameter = new FireParameter(false, false, 0, ForestFireState.NONE,
                ForestFireBurnedColor.convertBurnedFieldPercentageToColorValue(0), 0);
    }

    public void setBasicFireParameter(ForestFireState forestFireState) {
        if (terrain == Terrain.SAND || terrain == Terrain.WATER) return;
        switch (forestFireState) {
            case NONE, DESTROYED ->
                    this.fireParameter = new FireParameter(false, false, convertEnumToFieldDestroyed(forestFireState), forestFireState,
                            ForestFireBurnedColor.convertBurnedFieldPercentageToColorValue(convertEnumToFieldDestroyed(forestFireState)),0);
            case LOW, MEDIUM, HIGH, EXTREME ->
                    this.fireParameter = new FireParameter(true, false, convertEnumToFieldDestroyed(forestFireState), forestFireState,
                            ForestFireBurnedColor.convertBurnedFieldPercentageToColorValue(convertEnumToFieldDestroyed(forestFireState)),0);
        }
    }

    public int convertEnumToFieldDestroyed(ForestFireState state) {
        switch (state) {
            case NONE -> {
                return 0;
            }
            case LOW -> {
                return 1;
            }
            case MEDIUM -> {
                return 26;
            }
            case HIGH -> {
                return 51;
            }
            case EXTREME -> {
                return 76;
            }
            case DESTROYED -> {
                return 100;
            }
        }
        return 101;
    }

    private int getRandomDroughtFactor() {
        Random ran = new Random();
        if (ran.nextDouble() < .1) {
            return ran.nextInt(50) + 140;
        } else if (ran.nextDouble() < .1 + .3) {
            return ran.nextInt(50) + 60;
        } else return ran.nextInt(50);
    }

    private ForestFireIndex converForestFireValueToForestFireIndex(double value) {
        if (value == 0) return ForestFireIndex.NONE;
        if (value < 5) return ForestFireIndex.LOW;
        else if (value < 12) return ForestFireIndex.MODERATE;
        else if (value < 25) return ForestFireIndex.HIGH;
        else if (value < 50) return ForestFireIndex.VERY_HIGH;
        else return ForestFireIndex.EXTREME;
    }

    public ForestPixel createCopy() {
        return ForestPixel.builder()
                .id(this.id)
                .terrain(this.terrain)
                .forestFireIndex(this.forestFireIndex)
                .forestFireIndexValue(this.forestFireIndexValue)
                .temperature(this.temperature)
                .humidity(this.humidity)
                .wind(new Wind(this.wind.direction(), this.wind.speed()))
                .agentParameters(this.agentParameters.createCopy())
                .fireParameter(fireParameter.createCopy())
                .build();
    }

    public boolean isBeingBurned(){
        return this.fireParameter.getForestFireState() != ForestFireState.NONE;
    }

    public boolean isNotDestroyed() {
        return this.fireParameter.getForestFireState() != ForestFireState.DESTROYED;
    }

    @Override
    public int hashCode(){
        return 31 * id.hashCode();
    }

    @Override
    public boolean equals(Object o){
        if (!(o instanceof ForestPixel))
            return false;
        ForestPixel fp = (ForestPixel) o;
        return fp.id.equals(id);
    }

    public boolean isBeingExtinguish() {
        return this.fireParameter.isBeingExtinguished();
    }
}
