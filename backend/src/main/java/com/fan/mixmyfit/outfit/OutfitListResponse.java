package com.fan.mixmyfit.outfit;

import java.util.List;

record OutfitListResponse(
        List<OutfitSummaryResponse> items,
        int page,
        int size,
        long total) {
}
