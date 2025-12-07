package com.geobook;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.util.List;
import oracle.sql.STRUCT;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import org.hibernate.annotations.Type;

@Entity
@Table(name = "locations")
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "location_seq")
    @SequenceGenerator(name = "location_seq", sequenceName = "LOCATIONS_SEQ", allocationSize = 1)
    @Column(name = "location_id")
    private Long locationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id", nullable = false)
    private Chapter chapter;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "place_name")
    private String placeName;

    @Type(SdoGeometryType.class)
    @Column(name = "spatial_data")
    private String spatialData; 

    @OneToMany(mappedBy = "location", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Multimedia> multimedia;

    // Getters and Setters
    public Long getLocationId() {
        return locationId;
    }

    public void setLocationId(Long locationId) {
        this.locationId = locationId;
    }

    public Chapter getChapter() {
        return chapter;
    }

    public void setChapter(Chapter chapter) {
        this.chapter = chapter;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getPlaceName() {
        return placeName;
    }

    public void setPlaceName(String placeName) {
        this.placeName = placeName;
    }

    public String getSpatialData() {
        return spatialData;
    }

    public void setSpatialData(String spatialData) {
        this.spatialData = spatialData;
    }

    public List<Multimedia> getMultimedia() {
        return multimedia;
    }

    public void setMultimedia(List<Multimedia> multimedia) {
        this.multimedia = multimedia;
    }

    @Transient // Not persisted directly
    public STRUCT getSpatialDataAsGeometry(DataSource dataSource) throws SQLException {
        if (spatialData == null) return null;
        Connection conn = dataSource.getConnection();
        try {
            // Convert WKT string to SDO_GEOMETRY 
            var stmt = conn.prepareStatement(sql);
            stmt.setString(1, spatialData);
            var rs = stmt.executeQuery();
            if (rs.next()) {
                return (STRUCT) rs.getObject(1);
            }
        } finally {
            conn.close();
        }
        return null;
    }

       @Transient
    public void setSpatialDataFromGeometry(STRUCT geometry, DataSource dataSource) throws SQLException {
        if (geometry == null) {
            this.spatialData = null;
            return;
        }
        Connection conn = dataSource.getConnection();
        try {
            // Convert SDO_GEOMETRY to WKT string use Oracle spatial functions
            String sql = "SELECT SDO_UTIL.TO_WKTGEOMETRY(?) FROM DUAL";
            var stmt = conn.prepareStatement(sql);
            stmt.setObject(1, geometry);
            var rs = stmt.executeQuery();
            if (rs.next()) {
                this.spatialData = rs.getString(1);
            }
        } finally {
            conn.close();
        }
    }
}
