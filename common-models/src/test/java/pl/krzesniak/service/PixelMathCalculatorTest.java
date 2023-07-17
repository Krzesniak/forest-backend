package pl.krzesniak.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PixelMathCalculatorTest {

    PixelMathCalculator pixelMathCalculator = new PixelMathCalculator();
    String pixelId;
    Set<String> pixelsId;

    @BeforeEach
    void setUp () {
        pixelId = "5:5";
        pixelsId = new HashSet<>(Set.of("5:6", "4:5", "5:5", "6:5", "5:4"));
    }

    @Test
    void isBorderPixel_WhenIsBorderPixel() {
        boolean result = pixelMathCalculator.isBorderPixel(pixelId, pixelsId);

        assertFalse(result);

    }
    @Test
    void isBorderPixel_WhenLeftOneIsMissing() {
        pixelsId.remove("5:4");

        boolean result = pixelMathCalculator.isBorderPixel(pixelId, pixelsId);

        assertTrue(result);

    }

    @Test
    void isBorderPixel_WhenRightOneIsMissing() {
        pixelsId.remove("5:6");

        boolean result = pixelMathCalculator.isBorderPixel(pixelId, pixelsId);

        assertTrue(result);

    }

    @Test
    void isBorderPixel_WhenUpperOneIsMissing() {
        pixelsId.remove("4:5");

        boolean result = pixelMathCalculator.isBorderPixel(pixelId, pixelsId);

        assertTrue(result);

    }

    @Test
    void isBorderPixel_WhenDownOneIsMissing() {
        pixelsId.remove("6:5");

        boolean result = pixelMathCalculator.isBorderPixel(pixelId, pixelsId);

        assertTrue(result);

    }

}
