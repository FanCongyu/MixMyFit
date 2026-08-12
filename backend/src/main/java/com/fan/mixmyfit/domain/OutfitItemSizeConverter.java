package com.fan.mixmyfit.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
class OutfitItemSizeConverter implements AttributeConverter<OutfitItemSize, String> {
    @Override
    public String convertToDatabaseColumn(OutfitItemSize attribute) {
        return attribute == null ? null : attribute.dbValue();
    }

    @Override
    public OutfitItemSize convertToEntityAttribute(String dbData) {
        return OutfitItemSize.fromDbValue(dbData);
    }
}
