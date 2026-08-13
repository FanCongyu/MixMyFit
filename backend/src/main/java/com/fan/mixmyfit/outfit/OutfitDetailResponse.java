package com.fan.mixmyfit.outfit;

import java.util.List;

record OutfitDetailResponse(
        Long outfitId,
        String title,
        String note,
        List<String> seasons,
        List<OutfitTagResponse> tags,
        List<OutfitItemResponse> items) {
}
