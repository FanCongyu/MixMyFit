package com.fan.mixmyfit.outfit;

import java.util.List;

record OutfitCreateRequest(
        String title,
        String note,
        List<String> seasons,
        List<Long> tagIds,
        List<OutfitItemRequest> items) {
}
