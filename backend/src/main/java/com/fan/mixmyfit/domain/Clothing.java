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
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "clothes")
public class Clothing {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "clothing_id")
    private Long clothingId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(length = 120)
    private String name;

    @Column(length = 80)
    private String color;

    @Column(name = "image_path", nullable = false, length = 500)
    private String imagePath;

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(nullable = false, length = 20)
    private ClothingStatus status;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    protected Clothing() {
    }

    private Clothing(
            User user,
            Category category,
            String name,
            String color,
            String imagePath,
            String originalFilename,
            String contentType,
            long fileSize,
            ClothingStatus status) {
        this.user = user;
        this.category = category;
        this.name = name;
        this.color = color;
        this.imagePath = imagePath;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.status = status;
    }

    public static Clothing draft(
            User user,
            String imagePath,
            String originalFilename,
            String contentType,
            long fileSize) {
        return new Clothing(user, null, null, null, imagePath, originalFilename, contentType, fileSize, ClothingStatus.DRAFT);
    }

    public static Clothing ready(
            User user,
            Category category,
            String name,
            String color,
            String imagePath,
            String originalFilename,
            String contentType,
            long fileSize) {
        return new Clothing(user, category, name, color, imagePath, originalFilename, contentType, fileSize, ClothingStatus.READY);
    }

    public Long getClothingId() {
        return clothingId;
    }

    public User getUser() {
        return user;
    }

    public Category getCategory() {
        return category;
    }

    public String getName() {
        return name;
    }

    public String getColor() {
        return color;
    }

    public String getImagePath() {
        return imagePath;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getContentType() {
        return contentType;
    }

    public long getFileSize() {
        return fileSize;
    }

    public ClothingStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void updateMetadata(Category category, String name, String color) {
        this.category = category;
        this.name = name;
        this.color = color;
        this.status = category == null ? ClothingStatus.DRAFT : ClothingStatus.READY;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Clothing clothing)) {
            return false;
        }
        return clothingId != null && Objects.equals(clothingId, clothing.clothingId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
