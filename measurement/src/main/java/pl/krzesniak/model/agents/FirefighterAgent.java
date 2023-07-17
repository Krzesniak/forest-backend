package pl.krzesniak.model.agents;

import lombok.Data;

@Data
public class FirefighterAgent extends Agent{

    private boolean isBusy = false;
    private String fireZoneId = "";
    private String currentExtinguishPixelId = "";
    public FirefighterAgent(String id) {
        this.id = id;
    }

}
