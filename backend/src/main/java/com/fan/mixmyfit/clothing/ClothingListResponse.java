package com.fan.mixmyfit.clothing;

import java.util.List;

record ClothingListResponse(List<ClothingResponse> items, int page, int size, long total) {
}
