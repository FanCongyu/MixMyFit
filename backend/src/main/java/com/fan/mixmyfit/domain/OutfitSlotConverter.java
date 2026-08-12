package com.fan.mixmyfit.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
class OutfitSlotConverter implements AttributeConverter<OutfitSlot, String> {
    @Override
    public String convertToDatabaseColumn(OutfitSlot attribute) {
        return attribute == null ? null : attribute.dbValue();
    }

    @Override
    public OutfitSlot convertToEntityAttribute(String dbData) {
        return OutfitSlot.fromDbValue(dbData);
    }
}
