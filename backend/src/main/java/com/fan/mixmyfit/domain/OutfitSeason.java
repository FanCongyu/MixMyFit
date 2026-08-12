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
@Table(name = "outfit_seasons")
public class OutfitSeason {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "outfit_season_id")
    private Long outfitSeasonId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "outfit_id", nullable = false)
    private Outfit outfit;

    @Column(nullable = false, length = 20)
    private Season season;

    protected OutfitSeason() {
    }

    public OutfitSeason(Outfit outfit, Season season) {
        this.outfit = outfit;
        this.season = season;
    }

    public Long getOutfitSeasonId() {
        return outfitSeasonId;
    }

    public Outfit getOutfit() {
        return outfit;
    }

    public Season getSeason() {
        return season;
    }
}
