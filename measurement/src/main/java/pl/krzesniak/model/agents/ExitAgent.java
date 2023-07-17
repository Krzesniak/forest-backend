package pl.krzesniak.model.agents;

import pl.krzesniak.model.ForestPixel;
import pl.krzesniak.model.enums.ForestFireState;
import pl.krzesniak.service.PixelMathCalculator;

import java.util.Collection;
import java.util.HashMap;
import java.util.Set;

public class ExitAgent extends Agent {

    public ExitAgent(String id) {
        this.id = id;
    }
    public void setFieldsToDefaultValues(Collection<Set<ForestPixel>> values) {
        values.stream()
                .flatMap(Collection::stream)
                .forEach(pixel -> {
                    pixel.getFireParameter().setBeingExtinguished(false);
                    pixel.getFireParameter().setBeingBurned(false);
                    pixel.getFireParameter().setForestFireState(ForestFireState.NONE);
                });

    }
}
