package com.fan.mixmyfit.tag;

import com.fan.mixmyfit.domain.ClothingTag;
import com.fan.mixmyfit.domain.OutfitTag;

record TagResponse(Long tagId, String name, String kind) {
    static TagResponse fromClothing(ClothingTag tag) {
        return new TagResponse(tag.getClothingTagId(), tag.getName(), "clothing");
    }

    static TagResponse fromOutfit(OutfitTag tag) {
        return new TagResponse(tag.getOutfitTagId(), tag.getName(), "outfit");
    }
}
