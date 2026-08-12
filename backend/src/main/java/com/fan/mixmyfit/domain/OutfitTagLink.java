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
@Table(name = "outfit_tag_links")
public class OutfitTagLink {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "outfit_tag_link_id")
    private Long outfitTagLinkId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "outfit_id", nullable = false)
    private Outfit outfit;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "outfit_tag_id", nullable = false)
    private OutfitTag outfitTag;

    protected OutfitTagLink() {
    }

    public OutfitTagLink(Outfit outfit, OutfitTag outfitTag) {
        this.outfit = outfit;
        this.outfitTag = outfitTag;
    }

    public Long getOutfitTagLinkId() {
        return outfitTagLinkId;
    }

    public Outfit getOutfit() {
        return outfit;
    }

    public OutfitTag getOutfitTag() {
        return outfitTag;
    }
}
