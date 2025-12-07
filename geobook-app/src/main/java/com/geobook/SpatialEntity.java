package com.geobook;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;

@Entity
@Table(name = "spatial_entities")
public class SpatialEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "spatial_entities_seq")
    @SequenceGenerator(name = "spatial_entities_seq", sequenceName = "spatial_entities_seq", allocationSize = 1)
    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "name")
    private String name;

    @Column(name = "entity_type")
    @Enumerated(EnumType.STRING)
    private SpatialEntityType entityType;

    @Column(name = "geometry")
    private String geometry; // Flexible geometry storage - works with both VARCHAR2 and SDO_GEOMETRY

    @Column(name = "description")
    private String description;

    @Column(name = "color")
    private String color;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Column(name = "updated_date")
    private LocalDateTime updatedDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id")
    private Chapter chapter;

    // Constructors
    public SpatialEntity() {
        this.createdDate = LocalDateTime.now();
        this.updatedDate = LocalDateTime.now();
    }

    public SpatialEntity(String name, SpatialEntityType entityType, String geometry) {
        this();
        this.name = name;
        this.entityType = entityType;
        this.geometry = geometry;
    }

    // Update methods
    public void updateGeometry(String newGeometry) {
        this.geometry = newGeometry;
        this.updatedDate = LocalDateTime.now();
    }

    public void updateName(String newName) {
        this.name = newName;
        this.updatedDate = LocalDateTime.now();
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
        this.updatedDate = LocalDateTime.now();
    }

    public SpatialEntityType getEntityType() {
        return entityType;
    }

    public void setEntityType(SpatialEntityType entityType) {
        this.entityType = entityType;
    }

    public String getGeometry() {
        return geometry;
    }

    public void setGeometry(String geometry) {
        this.geometry = geometry;
        this.updatedDate = LocalDateTime.now();
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

    public Chapter getChapter() {
        return chapter;
    }

    public void setChapter(Chapter chapter) {
        this.chapter = chapter;
    }

    // Enum for spatial entity types
    public enum SpatialEntityType {
        POINT,
        LINESTRING,
        POLYGON,
        CIRCLE,
        RECTANGLE
    }
}
