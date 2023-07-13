package pl.krzesniak.model.agents;

import lombok.Data;

@Data
public class FireControllerAgent extends Agent {

    private boolean isBusy = false;
    public FireControllerAgent(String id) {
        this.id = id;
    }
}
