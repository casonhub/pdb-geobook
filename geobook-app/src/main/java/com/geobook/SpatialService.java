package com.geobook;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Service
public class SpatialService {

    @Autowired
    private DataSource dataSource;

    /**
     * Return WKT representation of an SDO point (for testing). Uses SDO_GEOMETRY(...).GET_WKT()
     */
    public String createSpatialData(double latitude, double longitude) {
        String sql = "SELECT MDSYS.SDO_GEOMETRY(2001, 8307, MDSYS.SDO_POINT_TYPE(?, ?, NULL), NULL, NULL).GET_WKT() as wkt FROM dual";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDouble(1, longitude);
            ps.setDouble(2, latitude);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("wkt");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            // Fallback to simple WKT format
            return String.format("POINT(%f %f)", longitude, latitude);
        }
        return null;
    }

    /**
     * Update spatial_data column with a proper SDO_GEOMETRY object for the given location.
     * Uses SDO_POINT_TYPE(longitude, latitude).
     */
    public boolean updateLocationSpatialData(Long locationId, double latitude, double longitude) {
        String sql = "UPDATE locations SET spatial_data = MDSYS.SDO_GEOMETRY(2001, 8307, MDSYS.SDO_POINT_TYPE(?, ?, NULL), NULL, NULL) WHERE location_id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDouble(1, longitude);
            ps.setDouble(2, latitude);
            ps.setLong(3, locationId);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            // fallback: try storing WKT string into spatial_data (only as last resort)
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement("UPDATE locations SET spatial_data = ? WHERE location_id = ?")) {
                ps.setString(1, String.format("POINT(%f %f)", longitude, latitude));
                ps.setLong(2, locationId);
                return ps.executeUpdate() > 0;
            } catch (SQLException e2) {
                e2.printStackTrace();
                return false;
            }
        }
    }

    /**
     * Helper: attempt to register geometry metadata and create spatial index.
     * This requires privileges; errors are caught and logged.
     */
    public void ensureSpatialMetadataAndIndex() {
        String insertMeta = "BEGIN " +
                "  INSERT INTO user_sdo_geom_metadata (table_name, column_name, diminfo, srid) VALUES ( 'LOCATIONS', 'SPATIAL_DATA', " +
                "    MDSYS.SDO_DIM_ARRAY( MDSYS.SDO_DIM_ELEMENT('LONG', -180, 180, 0.5), MDSYS.SDO_DIM_ELEMENT('LAT', -90, 90, 0.5) ), 8307 );" +
                " EXCEPTION WHEN OTHERS THEN NULL; END;";

        String createIndex = "BEGIN " +
                "  EXECUTE IMMEDIATE 'CREATE INDEX LOCATIONS_SDO_IDX ON LOCATIONS(SPATIAL_DATA) INDEXTYPE IS MDSYS.SPATIAL_INDEX';" +
                " EXCEPTION WHEN OTHERS THEN NULL; END;";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps1 = conn.prepareStatement(insertMeta);
             PreparedStatement ps2 = conn.prepareStatement(createIndex)) {
            ps1.execute();
            ps2.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Find locations inside a polygon WKT using SDO_INSIDE for advanced queries.
     */
    public String findLocationsWithinPolygon(String wktPolygon) {
        String sql = "SELECT location_id, place_name FROM locations WHERE SDO_INSIDE(spatial_data, SDO_GEOMETRY(?, 8307)) = 'TRUE'";

        StringBuilder result = new StringBuilder();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, wktPolygon);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.append("Location ID: ").append(rs.getLong("location_id"))
                          .append(", Name: ").append(rs.getString("place_name"))
                          .append("\n");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result.toString();
    }
}
