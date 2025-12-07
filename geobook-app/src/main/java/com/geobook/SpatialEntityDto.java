package com.geobook;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;

public class SpatialEntityDto {
    
    private Long entityId;
    private String name;
    private String entityType;
    private String geometry;
    private String description;
    private String color;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdDate;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedDate;
    
    private Long chapterId;

    // Constructors
    public SpatialEntityDto() {}

    public SpatialEntityDto(Long entityId, String name, String entityType, String geometry) {
        this.entityId = entityId;
        this.name = name;
        this.entityType = entityType;
        this.geometry = geometry;
    }

    // convert from entity
    public static SpatialEntityDto from(SpatialEntity entity) {
        SpatialEntityDto dto = new SpatialEntityDto();
        dto.setEntityId(entity.getEntityId());
        dto.setName(entity.getName());
        dto.setEntityType(entity.getEntityType() != null ? entity.getEntityType().name() : null);
        dto.setGeometry(entity.getGeometry());
        dto.setDescription(entity.getDescription());
        dto.setColor(entity.getColor());
        dto.setCreatedDate(entity.getCreatedDate());
        dto.setUpdatedDate(entity.getUpdatedDate());
        dto.setChapterId(entity.getChapter() != null ? entity.getChapter().getChapterId() : null);
        return dto;
    }

    // Convert to entity
    public SpatialEntity toEntity() {
        SpatialEntity entity = new SpatialEntity();
        entity.setName(this.name);
        entity.setEntityType(this.entityType != null ? SpatialEntity.SpatialEntityType.valueOf(this.entityType) : null);
        entity.setGeometry(this.geometry);
        entity.setDescription(this.description);
        entity.setColor(this.color);
        return entity;
    }

    // Getters and Setters
    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getGeometry() {
        return geometry;
    }

    public void setGeometry(String geometry) {
        this.geometry = geometry;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public LocalDateTime getUpdatedDate() {
        return updatedDate;
    }

    public void setUpdatedDate(LocalDateTime updatedDate) {
        this.updatedDate = updatedDate;
    }

    public Long getChapterId() {
        return chapterId;
    }

    public void setChapterId(Long chapterId) {
        this.chapterId = chapterId;
    }
}
