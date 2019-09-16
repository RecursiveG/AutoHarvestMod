package me.recursiveg.autoharvest;

public enum EnumHarvestMode {
    HARVEST,// Harvest mature crops using the tool in hand
    PLANT,  // Plant seeds in hand
    MOW,    // Harvest seeds & flowers
    FEED,   // Feed animals with materials in hand
    SHEAR,  // Shear the sheep if you have scissors in hand
    OFF;    // Turn off mod

    private static EnumHarvestMode[] vals = values();

    public static EnumHarvestMode nextMode(EnumHarvestMode mode) {
        return mode.nextMode();
    }

    public EnumHarvestMode nextMode() {
        return vals[(this.ordinal() + 1) % vals.length];
    }
}
