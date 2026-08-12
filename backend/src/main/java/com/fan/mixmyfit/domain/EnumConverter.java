package com.fan.mixmyfit.domain;

final class EnumConverter {
    private EnumConverter() {
    }

    static <E extends Enum<E> & DbEnum> E fromDbValue(Class<E> enumType, String value) {
        if (value == null) {
            return null;
        }
        for (E candidate : enumType.getEnumConstants()) {
            if (candidate.dbValue().equals(value)) {
                return candidate;
            }
        }
        throw new IllegalArgumentException("Unknown " + enumType.getSimpleName() + " value: " + value);
    }
}
