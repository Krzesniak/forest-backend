package pl.krzesniak.model.agents;


import pl.krzesniak.model.ForestPixel;
import pl.krzesniak.model.SensorPixels;
import pl.krzesniak.service.ForestPixelHelper;

import java.util.List;

public class SensorAgent extends Agent {

    private List<ForestPixel> forestPixels;
    public SensorAgent(String id) {
        this.id = id;
    }

    public SensorPixels updateForestFields(ForestPixelHelper forestPixelHelper) {
        forestPixels = forestPixelHelper.createSurroundingsForPixel(id);
        return new SensorPixels(id, forestPixels);
    }
}
