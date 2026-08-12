package com.fan.mixmyfit.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "outfit_items")
public class OutfitItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "outfit_item_id")
    private Long outfitItemId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumns({
            @JoinColumn(name = "outfit_id", referencedColumnName = "outfit_id", nullable = false, insertable = false, updatable = false),
            @JoinColumn(name = "user_id", referencedColumnName = "user_id", nullable = false, insertable = false, updatable = false)
    })
    private Outfit outfit;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumns({
            @JoinColumn(name = "clothing_id", referencedColumnName = "clothing_id", nullable = false, insertable = false, updatable = false),
            @JoinColumn(name = "user_id", referencedColumnName = "user_id", nullable = false, insertable = false, updatable = false)
    })
    private Clothing clothing;

    @Column(name = "outfit_id", nullable = false)
    private Long outfitId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "clothing_id", nullable = false)
    private Long clothingId;

    @Column(nullable = false, length = 30)
    private OutfitItemRole role;

    @Column(length = 20)
    private OutfitSlot slot;

    @Column(name = "position_x")
    private Integer positionX;

    @Column(name = "position_y")
    private Integer positionY;

    @Column(length = 20)
    private OutfitItemSize size;

    @Column(name = "z_index")
    private Integer zIndex;

    protected OutfitItem() {
    }

    private OutfitItem(
            Outfit outfit,
            Long userId,
            Long clothingId,
            OutfitItemRole role,
            OutfitSlot slot,
            Integer positionX,
            Integer positionY,
            OutfitItemSize size,
            Integer zIndex) {
        this.outfit = outfit;
        this.outfitId = outfit.getOutfitId();
        this.userId = userId;
        this.clothingId = clothingId;
        this.role = role;
        this.slot = slot;
        this.positionX = positionX;
        this.positionY = positionY;
        this.size = size;
        this.zIndex = zIndex;
    }

    public static OutfitItem mainSlot(Outfit outfit, User user, Long clothingId, OutfitSlot slot) {
        return new OutfitItem(outfit, user.getUserId(), clothingId, OutfitItemRole.MAIN_SLOT, slot, null, null, null, null);
    }

    public static OutfitItem accessory(
            Outfit outfit,
            User user,
            Long clothingId,
            int positionX,
            int positionY,
            OutfitItemSize size,
            int zIndex) {
        return new OutfitItem(outfit, user.getUserId(), clothingId, OutfitItemRole.ACCESSORY_OVERLAY, null, positionX, positionY, size, zIndex);
    }

    public Long getOutfitItemId() {
        return outfitItemId;
    }

    public Outfit getOutfit() {
        return outfit;
    }

    public Long getOutfitId() {
        return outfitId;
    }

    public Clothing getClothing() {
        return clothing;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getClothingId() {
        return clothingId;
    }

    public OutfitItemRole getRole() {
        return role;
    }

    public OutfitSlot getSlot() {
        return slot;
    }

    public Integer getPositionX() {
        return positionX;
    }

    public Integer getPositionY() {
        return positionY;
    }

    public OutfitItemSize getSize() {
        return size;
    }

    public Integer getZIndex() {
        return zIndex;
    }
}
