package com.geobook;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Repository
public interface SpatialEntityRepository extends JpaRepository<SpatialEntity, Long> {

    // Find entities by type
    List<SpatialEntity> findByEntityType(SpatialEntity.SpatialEntityType entityType);
    
    // Find entities by type string
    @Query("SELECT e FROM SpatialEntity e WHERE e.entityType = :entityType")
    List<SpatialEntity> findByEntityType(@Param("entityType") String entityType);

    // Basic spatial functionality that works with H2
    List<SpatialEntity> findByNameContainingIgnoreCase(String name);
    
  
}
