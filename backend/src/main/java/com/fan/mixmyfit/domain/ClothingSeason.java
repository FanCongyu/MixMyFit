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
@Table(name = "clothing_seasons")
public class ClothingSeason {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "clothing_season_id")
    private Long clothingSeasonId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clothing_id", nullable = false)
    private Clothing clothing;

    @Column(nullable = false, length = 20)
    private Season season;

    protected ClothingSeason() {
    }

    public ClothingSeason(Clothing clothing, Season season) {
        this.clothing = clothing;
        this.season = season;
    }

    public Long getClothingSeasonId() {
        return clothingSeasonId;
    }

    public Clothing getClothing() {
        return clothing;
    }

    public Season getSeason() {
        return season;
    }
}
