package com.geobook;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

@Repository
public interface LocationRepository extends JpaRepository<Location, Long> {
    // Oracle Spatial query using SDO_WITHIN_DISTANCE
    @Query(value = "SELECT * FROM locations WHERE spatial_data IS NOT NULL AND " +
                   "SDO_WITHIN_DISTANCE(spatial_data, MDSYS.SDO_GEOMETRY(2001, 4326, MDSYS.SDO_POINT_TYPE(?2, ?1, NULL), NULL, NULL), 'distance=' || ?3 || ' unit=KM') = 'TRUE'", nativeQuery = true)
    List<Location> findLocationsWithinDistance(double lat, double lng, double distanceKm);
    
    // Fallback query using mathematical calculation
    @Query("SELECT l FROM Location l WHERE " +
           "6371 * acos(cos(radians(?1)) * cos(radians(l.latitude)) * cos(radians(l.longitude) - radians(?2)) + sin(radians(?1)) * sin(radians(l.latitude))) <= ?3")
    List<Location> findLocationsWithinDistanceFallback(double lat, double lng, double distanceKm);
}
