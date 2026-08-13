package com.fan.mixmyfit.outfit;

record OutfitItemRequest(
        Long clothingId,
        String role,
        String slot,
        Integer positionX,
        Integer positionY,
        String size,
        Integer zIndex) {
}
