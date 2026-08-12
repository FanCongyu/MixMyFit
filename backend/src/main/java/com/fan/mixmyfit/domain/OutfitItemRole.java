package com.fan.mixmyfit.domain;

public enum OutfitItemRole implements DbEnum {
    MAIN_SLOT("main_slot"),
    ACCESSORY_OVERLAY("accessory_overlay");

    private final String dbValue;

    OutfitItemRole(String dbValue) {
        this.dbValue = dbValue;
    }

    @Override
    public String dbValue() {
        return dbValue;
    }

    static OutfitItemRole fromDbValue(String value) {
        return EnumConverter.fromDbValue(OutfitItemRole.class, value);
    }
}
