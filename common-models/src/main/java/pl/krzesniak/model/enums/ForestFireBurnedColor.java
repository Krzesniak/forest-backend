package pl.krzesniak.model.enums;

public enum ForestFireBurnedColor {
    LEVEL0("#a6f2a8"), LEVEL1("#64e866"), LEVEL2("#64e866"), LEVEL3("#55da5a"), LEVEL4("#45cd4d"), LEVEL5("#34c041"),
    LEVEL6("#1fb335"), LEVEL7("#00a728"), LEVEL8("#009a1a"), LEVEL9("#008e07"), LEVEL10("#008100"),
    LEVEL11("#007500"), LEVEL12("#006900"), LEVEL13("#005e00"), LEVEL14("#005200"), LEVEL15("#004600"),
    LEVEL16("#003b00"), LEVEL17("#003200"), LEVEL18("#002a00"), LEVEL19("#002500"), LEVEL20("#000000"),
    LEVEL21("#052bec");

    private final String rgbColorValue;

    ForestFireBurnedColor(String rgbColorValue) {
        this.rgbColorValue = rgbColorValue;
    }

    public String getRgbColorValue() {
        return rgbColorValue;
    }

    public static String convertBurnedFieldPercentageToColorValue(int value) {
        if (value < 0) return LEVEL21.rgbColorValue;
        else if(value ==0 ) return LEVEL0.rgbColorValue;
        else if (value <= 5) return LEVEL1.rgbColorValue;
        else if (value <= 10) return LEVEL2.rgbColorValue;
        else if (value <= 15) return LEVEL3.rgbColorValue;
        else if (value <= 20) return LEVEL4.rgbColorValue;
        else if (value <= 25) return LEVEL5.rgbColorValue;
        else if (value <= 30) return LEVEL6.rgbColorValue;
        else if (value <= 35) return LEVEL7.rgbColorValue;
        else if (value <= 40) return LEVEL8.rgbColorValue;
        else if (value <= 45) return LEVEL9.rgbColorValue;
        else if (value <= 50) return LEVEL10.rgbColorValue;
        else if (value <= 55) return LEVEL11.rgbColorValue;
        else if (value <= 60) return LEVEL12.rgbColorValue;
        else if (value <= 65) return LEVEL13.rgbColorValue;
        else if (value <= 70) return LEVEL14.rgbColorValue;
        else if (value <= 75) return LEVEL15.rgbColorValue;
        else if (value <= 80) return LEVEL16.rgbColorValue;
        else if (value <= 85) return LEVEL17.rgbColorValue;
        else if (value <= 90) return LEVEL18.rgbColorValue;
        else if (value <= 95) return LEVEL19.rgbColorValue;
        else return LEVEL20.rgbColorValue;
    }
}
