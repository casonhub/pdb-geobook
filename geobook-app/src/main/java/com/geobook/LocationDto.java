package com.geobook;

public class LocationDto {
    private Long locationId;
    private String placeName;
    private Double latitude;
    private Double longitude;
    private String chapterTitle;
    private String bookTitle;
    private String spatialData;
    private String chapterDescription; // added to hold chapter location description

    // Constructor
    public LocationDto(Long locationId, String placeName, Double latitude, Double longitude, 
                       String chapterTitle, String bookTitle, String spatialData, String chapterDescription) {
        this.locationId = locationId;
        this.placeName = placeName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.chapterTitle = chapterTitle;
        this.bookTitle = bookTitle;
        this.spatialData = spatialData;
        this.chapterDescription = chapterDescription;
    }

    // Static factory method
    public static LocationDto from(Location location) {
        return new LocationDto(
            location.getLocationId(),
            location.getPlaceName(),
            location.getLatitude(),
            location.getLongitude(),
            location.getChapter() != null ? location.getChapter().getTitle() : null,
            location.getChapter() != null && location.getChapter().getBook() != null ? 
                location.getChapter().getBook().getTitle() : null,
            location.getSpatialData(),
            location.getChapter() != null ? location.getChapter().getLocationDescription() : null
        );
    }

    // Getters
    public Long getLocationId() { return locationId; }
    public String getPlaceName() { return placeName; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
    public String getChapterTitle() { return chapterTitle; }
    public String getBookTitle() { return bookTitle; }
    public String getSpatialData() { return spatialData; }
    public String getChapterDescription() { return chapterDescription; }

    // Setters
    public void setLocationId(Long locationId) { this.locationId = locationId; }
    public void setPlaceName(String placeName) { this.placeName = placeName; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public void setChapterTitle(String chapterTitle) { this.chapterTitle = chapterTitle; }
    public void setBookTitle(String bookTitle) { this.bookTitle = bookTitle; }
    public void setSpatialData(String spatialData) { this.spatialData = spatialData; }
    public void setChapterDescription(String chapterDescription) { this.chapterDescription = chapterDescription; }
}
