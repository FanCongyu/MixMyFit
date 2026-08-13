package com.fan.mixmyfit.clothing;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import java.util.List;

class ClothingUpdateRequest {
    private Long categoryId;
    private String name;
    private String color;
    private List<String> seasons;
    private List<Long> tagIds;
    private boolean categoryIdProvided;
    private boolean nameProvided;
    private boolean colorProvided;

    Long categoryId() {
        return categoryId;
    }

    String name() {
        return name;
    }

    String color() {
        return color;
    }

    List<String> seasons() {
        return seasons;
    }

    List<Long> tagIds() {
        return tagIds;
    }

    @JsonIgnore
    boolean hasCategoryId() {
        return categoryIdProvided;
    }

    @JsonIgnore
    boolean hasName() {
        return nameProvided;
    }

    @JsonIgnore
    boolean hasColor() {
        return colorProvided;
    }

    @JsonSetter
    void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
        this.categoryIdProvided = true;
    }

    @JsonSetter
    void setName(String name) {
        this.name = name;
        this.nameProvided = true;
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
    void setTagIds(List<Long> tagIds) {
        this.tagIds = tagIds;
    }
}
