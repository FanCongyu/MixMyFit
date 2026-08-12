package com.fan.mixmyfit.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "clothing_tag_links")
public class ClothingTagLink {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "clothing_tag_link_id")
    private Long clothingTagLinkId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clothing_id", nullable = false)
    private Clothing clothing;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clothing_tag_id", nullable = false)
    private ClothingTag clothingTag;

    protected ClothingTagLink() {
    }

    public ClothingTagLink(Clothing clothing, ClothingTag clothingTag) {
        this.clothing = clothing;
        this.clothingTag = clothingTag;
    }

    public Long getClothingTagLinkId() {
        return clothingTagLinkId;
    }

    public Clothing getClothing() {
        return clothing;
    }

    public ClothingTag getClothingTag() {
        return clothingTag;
    }
}
