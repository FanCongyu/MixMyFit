package com.fan.mixmyfit.domain;

public enum CategoryType implements DbEnum {
    FIXED("fixed"),
    CUSTOM("custom");

    private final String dbValue;

    CategoryType(String dbValue) {
        this.dbValue = dbValue;
    }

    @Override
    public String dbValue() {
        return dbValue;
    }

    static CategoryType fromDbValue(String value) {
        return EnumConverter.fromDbValue(CategoryType.class, value);
    }
}
