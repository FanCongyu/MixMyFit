package com.fan.mixmyfit.domain;

public enum ClothingStatus implements DbEnum {
    DRAFT("draft"),
    READY("ready");

    private final String dbValue;

    ClothingStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    @Override
    public String dbValue() {
        return dbValue;
    }

    static ClothingStatus fromDbValue(String value) {
        return EnumConverter.fromDbValue(ClothingStatus.class, value);
    }
}
