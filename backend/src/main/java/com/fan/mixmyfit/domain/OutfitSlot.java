package com.fan.mixmyfit.domain;

public enum OutfitSlot implements DbEnum {
    TOP("top"),
    BOTTOM("bottom"),
    SHOES("shoes"),
    HAT("hat");

    private final String dbValue;

    OutfitSlot(String dbValue) {
        this.dbValue = dbValue;
    }

    @Override
    public String dbValue() {
        return dbValue;
    }

    static OutfitSlot fromDbValue(String value) {
        return EnumConverter.fromDbValue(OutfitSlot.class, value);
    }
}
