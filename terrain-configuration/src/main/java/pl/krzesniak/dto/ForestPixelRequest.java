package pl.krzesniak.dto;

public record ForestPixelRequest(long temperature, long humidity, long pressure, long windDirection,
                                 long windStrength) {
}

