package com.fan.mixmyfit.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
class CategoryTypeConverter implements AttributeConverter<CategoryType, String> {
    @Override
    public String convertToDatabaseColumn(CategoryType attribute) {
        return attribute == null ? null : attribute.dbValue();
    }

    @Override
    public CategoryType convertToEntityAttribute(String dbData) {
        return CategoryType.fromDbValue(dbData);
    }
}
