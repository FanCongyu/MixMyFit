package com.fan.mixmyfit.clothing;

import com.fan.mixmyfit.domain.Clothing;

public record ClothingUploadResponse(
        Long clothingId,
        String status,
        String imageUrl,
        String originalFilename,
        String contentType,
        long fileSize) {
    static ClothingUploadResponse from(Clothing clothing) {
        Long clothingId = clothing.getClothingId();
        return new ClothingUploadResponse(
                clothingId,
                clothing.getStatus().dbValue(),
                "/api/clothes/" + clothingId + "/image",
                clothing.getOriginalFilename(),
                clothing.getContentType(),
                clothing.getFileSize());
    }
}
