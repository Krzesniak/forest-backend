package pl.krzesniak.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import pl.krzesniak.model.enums.ForestFireState;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FireParameter {

    private boolean isBeingBurned;
    private boolean isBeingExtinguished;
    private double fieldPercentageDestroyed;
    private ForestFireState forestFireState;
    private String forestFireBurnedColor;
    private double fireSpeedSpreed;

    public FireParameter createCopy() {
        return new FireParameter(isBeingBurned, isBeingExtinguished, fieldPercentageDestroyed, forestFireState, forestFireBurnedColor, fireSpeedSpreed);
    }
}
