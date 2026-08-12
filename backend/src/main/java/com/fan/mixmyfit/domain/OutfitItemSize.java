package com.fan.mixmyfit.domain;

public enum OutfitItemSize implements DbEnum {
    SMALL("small"),
    MEDIUM("medium"),
    LARGE("large");

    private final String dbValue;

    OutfitItemSize(String dbValue) {
        this.dbValue = dbValue;
    }

    @Override
    public String dbValue() {
        return dbValue;
    }

    static OutfitItemSize fromDbValue(String value) {
        return EnumConverter.fromDbValue(OutfitItemSize.class, value);
    }
}
