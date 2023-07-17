package pl.krzesniak.service.resources;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import pl.krzesniak.model.ForestPixel;

import java.util.Set;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class FireResourceMetadata {

    private String id;
    private int firefightersCount;
    private int optimalFireFighterCount;
    private double dangerousValue;
    private AdditionalResourceNeeded additionalResourceNeeded;
    private Set<ForestPixel> burningPixels;

    public boolean isBeingExtinguished() {
        return additionalResourceNeeded == AdditionalResourceNeeded.NO || additionalResourceNeeded == AdditionalResourceNeeded.YES;
    }

}
