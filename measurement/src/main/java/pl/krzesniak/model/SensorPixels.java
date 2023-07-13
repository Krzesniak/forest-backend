package pl.krzesniak.model;

import java.util.List;

public record SensorPixels(String id, List<ForestPixel> pixels) {
}
