package com.landhub.land;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "lands")
public class Land {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Title is required")
    @Size(max = 150, message = "Title must be 150 characters or fewer")
    @Column(nullable = false, length = 150)
    private String title;

    @NotBlank(message = "Description is required")
    @Size(min = 40, max = 3000, message = "Description must be between 40 and 3000 characters")
    @Column(nullable = false, length = 3000)
    private String description;

    @NotBlank(message = "Location is required")
    @Size(max = 120, message = "Location must be 120 characters or fewer")
    @Column(nullable = false, length = 120)
    private String location;

    @Size(max = 255, message = "Address must be 255 characters or fewer")
    @Column(length = 255)
    private String address;

    @NotBlank(message = "District is required")
    @Size(max = 120, message = "District must be 120 characters or fewer")
    @Column(nullable = false, length = 120)
    private String district;

    @NotNull(message = "Price is required")
    @Positive(message = "Price must be greater than zero")
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal price;

    @NotNull(message = "Land size is required")
    @Positive(message = "Land size must be greater than zero")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal size;

    @NotBlank(message = "Size unit is required")
    @Size(max = 40, message = "Size unit must be 40 characters or fewer")
    @Column(nullable = false, length = 40)
    private String sizeUnit;

    @NotNull(message = "Land type is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private LandType landType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private LandStatus status;

    private boolean roadAccess;

    private boolean waterAvailable;

    private boolean electricityAvailable;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "land", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC, id ASC")
    private List<LandImage> images = new ArrayList<>();

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;

        if (status == null) {
            status = LandStatus.PENDING;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Transient
    public String getCoverImageUrl() {
        return images.stream()
                .filter(LandImage::isCoverImage)
                .findFirst()
                .or(() -> images.stream().findFirst())
                .map(LandImage::getImageUrl)
                .orElse("https://images.unsplash.com/photo-1500382017468-9049fed747ef?auto=format&fit=crop&w=900&q=80");
    }

    @Transient
    public boolean isBookable() {
        return status == LandStatus.AVAILABLE;
    }

    public void addImage(LandImage image) {
        images.add(image);
        image.setLand(this);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getSize() {
        return size;
    }

    public void setSize(BigDecimal size) {
        this.size = size;
    }

    public String getSizeUnit() {
        return sizeUnit;
    }

    public void setSizeUnit(String sizeUnit) {
        this.sizeUnit = sizeUnit;
    }

    public LandType getLandType() {
        return landType;
    }

    public void setLandType(LandType landType) {
        this.landType = landType;
    }

    public LandStatus getStatus() {
        return status;
    }

    public void setStatus(LandStatus status) {
        this.status = status;
    }

    public boolean isRoadAccess() {
        return roadAccess;
    }

    public void setRoadAccess(boolean roadAccess) {
        this.roadAccess = roadAccess;
    }

    public boolean isWaterAvailable() {
        return waterAvailable;
    }

    public void setWaterAvailable(boolean waterAvailable) {
        this.waterAvailable = waterAvailable;
    }

    public boolean isElectricityAvailable() {
        return electricityAvailable;
    }

    public void setElectricityAvailable(boolean electricityAvailable) {
        this.electricityAvailable = electricityAvailable;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<LandImage> getImages() {
        return images;
    }

    public void setImages(List<LandImage> images) {
        this.images = images;
    }
}
