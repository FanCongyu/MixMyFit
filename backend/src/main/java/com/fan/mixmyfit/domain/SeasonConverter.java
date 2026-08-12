package com.fan.mixmyfit.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
class SeasonConverter implements AttributeConverter<Season, String> {
    @Override
    public String convertToDatabaseColumn(Season attribute) {
        return attribute == null ? null : attribute.dbValue();
    }

    @Override
    public Season convertToEntityAttribute(String dbData) {
        return Season.fromDbValue(dbData);
    }
}
