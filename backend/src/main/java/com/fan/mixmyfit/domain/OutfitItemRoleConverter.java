package com.fan.mixmyfit.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
class OutfitItemRoleConverter implements AttributeConverter<OutfitItemRole, String> {
    @Override
    public String convertToDatabaseColumn(OutfitItemRole attribute) {
        return attribute == null ? null : attribute.dbValue();
    }

    @Override
    public OutfitItemRole convertToEntityAttribute(String dbData) {
        return OutfitItemRole.fromDbValue(dbData);
    }
}
