package com.geobook;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDate;

@Entity
@Table(name = "multimedia")
public class Multimedia {

    @Id
    @SequenceGenerator(
        name = "multimedia_seq",
        sequenceName = "MULTIMEDIA_SEQ",
        allocationSize = 1
    )
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        generator = "multimedia_seq"
    )
    @Column(name = "multimedia_id")
    private Long multimediaId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    @JsonIgnore
    private Location location;

    @Column(name = "file_type")
    private String fileType;

    @Column(name = "file_path")
    private String filePath;

//    @Lob
//    @Column(name = "description")
//    private String description;
@Column(name = "DESCRIPTION")
private String description;

    @Column(name = "upload_date")
    private LocalDate uploadDate;

    @Column(name = "is_active")
    private Character isActive;

    @Column(name = "thumbnail_path")
    private String thumbnailPath;

    // NO ORDImage fields here - they're managed by native SQL

    // Getters and Setters (ONLY for the fields above)
    public Long getMultimediaId() {
        return multimediaId;
    }

    public void setMultimediaId(Long multimediaId) {
        this.multimediaId = multimediaId;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(LocalDate uploadDate) {
        this.uploadDate = uploadDate;
    }

    public Character getIsActive() {
        return isActive;
    }

    public void setIsActive(Character isActive) {
        this.isActive = isActive;
    }

    public String getThumbnailPath() {
        return thumbnailPath;
    }

    public void setThumbnailPath(String thumbnailPath) {
        this.thumbnailPath = thumbnailPath;
    }

    // Constructors
    public Multimedia() {}

    public Multimedia(Long multimediaId, Location location, String fileType, 
                     String filePath, String description, LocalDate uploadDate, 
                     Character isActive, String thumbnailPath) {
        this.multimediaId = multimediaId;
        this.location = location;
        this.fileType = fileType;
        this.filePath = filePath;
        this.description = description;
        this.uploadDate = uploadDate;
        this.isActive = isActive;
        this.thumbnailPath = thumbnailPath;
    }
}