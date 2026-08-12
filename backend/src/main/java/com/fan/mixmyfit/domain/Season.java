package com.fan.mixmyfit.domain;

public enum Season implements DbEnum {
    SPRING("spring"),
    SUMMER("summer"),
    AUTUMN("autumn"),
    WINTER("winter");

    private final String dbValue;

    Season(String dbValue) {
        this.dbValue = dbValue;
    }

    @Override
    public String dbValue() {
        return dbValue;
    }

    static Season fromDbValue(String value) {
        return EnumConverter.fromDbValue(Season.class, value);
    }
}
