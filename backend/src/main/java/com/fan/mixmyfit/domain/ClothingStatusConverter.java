package com.fan.mixmyfit.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
class ClothingStatusConverter implements AttributeConverter<ClothingStatus, String> {
    @Override
    public String convertToDatabaseColumn(ClothingStatus attribute) {
        return attribute == null ? null : attribute.dbValue();
    }

    @Override
    public ClothingStatus convertToEntityAttribute(String dbData) {
        return ClothingStatus.fromDbValue(dbData);
    }
}
