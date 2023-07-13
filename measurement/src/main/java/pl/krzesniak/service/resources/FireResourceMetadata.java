package pl.krzesniak.service.resources;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

@AllArgsConstructor
@NoArgsConstructor
@Data
public  class FireResourceMetadata {

    private String id;
    private  int firefightersCount;
    private  int optimalFireFighterCount;
    private  double dangerousValue;
    private AdditionalResourceNeeded additionalResourceNeeded;

    public boolean isBeingExtinguished () {
        return additionalResourceNeeded == AdditionalResourceNeeded.NO || additionalResourceNeeded == AdditionalResourceNeeded.YES;
    }




}
