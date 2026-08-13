package com.fan.mixmyfit.clothing;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import java.util.List;

class ClothingBatchRequest {
    private List<Long> clothingIds;
    private Long categoryId;
    private String color;
    private List<String> seasons;
    private List<Long> addTagIds;
    private List<Long> removeTagIds;
    private boolean categoryIdProvided;
    private boolean colorProvided;

    List<Long> clothingIds() {
        return clothingIds;
    }

    Long categoryId() {
        return categoryId;
    }

    String color() {
        return color;
    }

    List<String> seasons() {
        return seasons;
    }

    List<Long> addTagIds() {
        return addTagIds;
    }

    List<Long> removeTagIds() {
        return removeTagIds;
    }

    @JsonIgnore
    boolean hasCategoryId() {
        return categoryIdProvided;
    }

    @JsonIgnore
    boolean hasColor() {
        return colorProvided;
    }

    @JsonSetter
    void setClothingIds(List<Long> clothingIds) {
        this.clothingIds = clothingIds;
    }

    @JsonSetter
    void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
        this.categoryIdProvided = true;
    }

    @JsonSetter
    void setColor(String color) {
        this.color = color;
        this.colorProvided = true;
    }

    @JsonSetter
    void setSeasons(List<String> seasons) {
        this.seasons = seasons;
    }

    @JsonSetter
    void setAddTagIds(List<Long> addTagIds) {
        this.addTagIds = addTagIds;
    }

    @JsonSetter
    void setRemoveTagIds(List<Long> removeTagIds) {
        this.removeTagIds = removeTagIds;
    }
}
