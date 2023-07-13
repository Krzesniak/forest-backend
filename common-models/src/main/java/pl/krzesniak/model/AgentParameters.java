package pl.krzesniak.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AgentParameters {
    private boolean isVisible;
    private boolean isTestable;
    private boolean isCenter;
    private boolean hasSensor;


    public AgentParameters createCopy(){
        return new AgentParameters(this.isVisible, this.isTestable, this.isCenter, this.hasSensor);
    }
}
