package com.fan.mixmyfit.clothing;

import com.fan.mixmyfit.domain.Category;
import com.fan.mixmyfit.domain.Clothing;
import com.fan.mixmyfit.domain.ClothingSeason;
import com.fan.mixmyfit.domain.ClothingTag;
import java.util.List;

record ClothingResponse(
        Long clothingId,
        String status,
        String name,
        String color,
        CategoryItem category,
        List<String> seasons,
        List<TagItem> tags,
        String imageUrl,
        String originalFilename,
        String contentType,
        long fileSize) {
    static ClothingResponse from(Clothing clothing, List<ClothingSeason> seasons, List<ClothingTag> tags) {
        Long clothingId = clothing.getClothingId();
        return new ClothingResponse(
                clothingId,
                clothing.getStatus().dbValue(),
                clothing.getName(),
                clothing.getColor(),
                CategoryItem.from(clothing.getCategory()),
                seasons.stream().map(season -> season.getSeason().dbValue()).toList(),
                tags.stream().map(TagItem::from).toList(),
                "/api/clothes/" + clothingId + "/image",
                clothing.getOriginalFilename(),
                clothing.getContentType(),
                clothing.getFileSize());
    }

    record CategoryItem(Long categoryId, String name, String type) {
        static CategoryItem from(Category category) {
            if (category == null) {
                return null;
            }
            return new CategoryItem(category.getCategoryId(), category.getName(), category.getType().dbValue());
        }
    }

    record TagItem(Long tagId, String name) {
        static TagItem from(ClothingTag tag) {
            return new TagItem(tag.getClothingTagId(), tag.getName());
        }
    }
}
