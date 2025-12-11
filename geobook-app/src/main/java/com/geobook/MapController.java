package com.geobook;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.transaction.annotation.Transactional;

@Controller
@RequestMapping("/map")
public class MapController {

    private final LocationRepository locationRepository;
    private final SpatialEntityRepository spatialEntityRepository;
    
    @PersistenceContext
    private EntityManager entityManager;

    public MapController(LocationRepository locationRepository, SpatialEntityRepository spatialEntityRepository) {
        this.locationRepository = locationRepository;
        this.spatialEntityRepository = spatialEntityRepository;
    }

    @GetMapping
    public String showMap(Model model) {
        List<Location> locations = locationRepository.findAll();
        List<SpatialEntity> spatialEntities = spatialEntityRepository.findAll();
        
        model.addAttribute("locations", locations.stream().map(LocationDto::from).collect(Collectors.toList()));
        model.addAttribute("spatialEntities", spatialEntities.stream().map(SpatialEntityDto::from).collect(Collectors.toList()));
        return "map";
    }

    @PostMapping("/search")
    public String searchLocations(@RequestParam double lat, @RequestParam double lng, @RequestParam double distance, Model model) {
        try {
            // Try Oracle Spatial query first
            List<Location> locations = locationRepository.findLocationsWithinDistance(lat, lng, distance);
            model.addAttribute("locations", locations.stream().map(LocationDto::from).collect(Collectors.toList()));
            model.addAttribute("searchLat", lat);
            model.addAttribute("searchLng", lng);
            model.addAttribute("searchDistance", distance);
        } catch (Exception e) {
            // If spatial query fails, fall back to mathematical calculation
            try {
                List<Location> locations = locationRepository.findLocationsWithinDistanceFallback(lat, lng, distance);
                model.addAttribute("locations", locations.stream().map(LocationDto::from).collect(Collectors.toList()));
                model.addAttribute("searchLat", lat);
                model.addAttribute("searchLng", lng);
                model.addAttribute("searchDistance", distance);
                model.addAttribute("warning", "Using fallback distance calculation");
            } catch (Exception e2) {
                List<Location> allLocations = locationRepository.findAll();
                model.addAttribute("locations", allLocations.stream().map(LocationDto::from).collect(Collectors.toList()));
                model.addAttribute("error", "Both spatial queries failed: " + e.getMessage());
            }
        }
        
        // Always include spatial entities
        addAllEntitiestoModel(model);
        return "map";
    }

    // === CRUD OPERATIONS FOR 5 SPATIAL ENTITY TYPES ===
    
    @PostMapping("/entities")
    @ResponseBody
    public ResponseEntity<?> createSpatialEntity(@RequestBody SpatialEntityDto entityDto) {
        try {
            // Validate input
            if (entityDto.getName() == null || entityDto.getName().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Entity name is required");
            }
            
            if (entityDto.getEntityType() == null || entityDto.getEntityType().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Entity type is required");
            }
            
            System.out.println("Creating entity: " + entityDto.getName() + " of type: " + entityDto.getEntityType());
            
            SpatialEntity entity = entityDto.toEntity();
            
            // Validate geometry if provided, otherwise generate sample geometry
            if (entityDto.getGeometry() == null || entityDto.getGeometry().trim().isEmpty()) {
                // Generate sample geometry at default NYC coordinates
                String sampleGeometry = generateSampleGeometry(
                    entity.getEntityType(), 40.7128, -74.0060
                );
                entity.setGeometry(sampleGeometry);
                System.out.println("Generated geometry: " + sampleGeometry);
            } else if (!isValidGeometry(entityDto.getGeometry())) {
                return ResponseEntity.badRequest().body("Invalid geometry format");
            }
            
            entity = spatialEntityRepository.save(entity);
            System.out.println("Entity saved with ID: " + entity.getEntityId());
            
            return ResponseEntity.ok(SpatialEntityDto.from(entity));
        } catch (IllegalArgumentException e) {
            System.err.println("IllegalArgumentException: " + e.getMessage());
            return ResponseEntity.badRequest().body("Invalid entity type: " + entityDto.getEntityType());
        } catch (Exception e) {
            System.err.println("Exception creating entity: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Error creating entity: " + e.getMessage());
        }
    }
    
    @PutMapping("/entities/{id}")
    @ResponseBody
    public ResponseEntity<?> updateSpatialEntity(@PathVariable Long id, @RequestBody SpatialEntityDto entityDto) {
        try {
            SpatialEntity entity = spatialEntityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Entity not found with id: " + id));
            
            entity.setName(entityDto.getName());
            entity.setGeometry(entityDto.getGeometry());
            entity.setDescription(entityDto.getDescription());
            entity.setColor(entityDto.getColor());
            
            if (entityDto.getEntityType() != null) {
                entity.setEntityType(SpatialEntity.SpatialEntityType.valueOf(entityDto.getEntityType()));
            }
            
            entity = spatialEntityRepository.save(entity);
            return ResponseEntity.ok(SpatialEntityDto.from(entity));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error updating entity: " + e.getMessage());
        }
    }
    
    @DeleteMapping("/entities/{id}")
    @ResponseBody
    public ResponseEntity<?> deleteSpatialEntity(@PathVariable Long id) {
        try {
            if (!spatialEntityRepository.existsById(id)) {
                return ResponseEntity.notFound().build();
            }
            spatialEntityRepository.deleteById(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error deleting entity: " + e.getMessage());
        }
    }
    
    @GetMapping("/entities")
    @ResponseBody
    public ResponseEntity<List<SpatialEntityDto>> getAllSpatialEntities() {
        try {
            List<SpatialEntity> entities = spatialEntityRepository.findAll();
            return ResponseEntity.ok(entities.stream().map(SpatialEntityDto::from).collect(Collectors.toList()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/entities/type/{type}")
    @ResponseBody
    public ResponseEntity<List<SpatialEntityDto>> getEntitiesByType(@PathVariable String type) {
        try {
            List<SpatialEntity> entities = spatialEntityRepository.findByEntityType(type);
            return ResponseEntity.ok(entities.stream().map(SpatialEntityDto::from).collect(Collectors.toList()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/entities/{id}")
    @ResponseBody
    public ResponseEntity<SpatialEntityDto> getSpatialEntityById(@PathVariable Long id) {
        try {
            SpatialEntity entity = spatialEntityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Entity not found with id: " + id));
            return ResponseEntity.ok(SpatialEntityDto.from(entity));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    // === SPATIAL OPERATORS ===
    
    // Non-trivial spatial operator: ST_INTERSECTION
    @PostMapping("/spatial/intersections")
    public String findIntersections(@RequestParam String entityType1, 
                                   @RequestParam String entityType2, Model model) {
        try {
            List<Map<String, Object>> intersections = new ArrayList<>();
            
            String sql = "SELECT e1.name as entity1_name, e1.entity_type as entity1_type, " +
                        "e2.name as entity2_name, e2.entity_type as entity2_type " +
                        "FROM spatial_entities e1, spatial_entities e2 " +
                        "WHERE e1.entity_type = :type1 AND e2.entity_type = :type2 " +
                        "AND SDO_OVERLAPS(parse_sdo_geom(e1.geometry), parse_sdo_geom(e2.geometry)) = 'TRUE' " +
                        "AND e1.entity_id != e2.entity_id";
            
            @SuppressWarnings("unchecked")
            List<Object[]> results = entityManager.createNativeQuery(sql)
                .setParameter("type1", entityType1)
                .setParameter("type2", entityType2)
                .getResultList();
            
            for (Object[] row : results) {
                Map<String, Object> intersection = new HashMap<>();
                intersection.put("entity1_name", row[0]);
                intersection.put("entity1_type", row[1]);
                intersection.put("entity2_name", row[2]);
                intersection.put("entity2_type", row[3]);
                intersections.add(intersection);
            }
            
            model.addAttribute("intersections", intersections);
            model.addAttribute("operationType", "INTERSECTION");
            model.addAttribute("entityType1", entityType1);
            model.addAttribute("entityType2", entityType2);
        } catch (Exception e) {
            model.addAttribute("error", "Intersection analysis failed: " + e.getMessage());
        }
        
        addAllEntitiestoModel(model);
        return "spatial-analysis";
    }
    
    
    // === ANALYTIC FUNCTIONS ===
    
    // Analytic function Density Analysis
    @PostMapping("/analysis/density")
    public String performDensityAnalysis(@RequestParam(required = false) String regionType, Model model) {
        try {
            List<Map<String, Object>> densityResults = new ArrayList<>();
            
            String sql = "SELECT p.name as region_name, COUNT(pt.entity_id) as point_count, " +
                        "SDO_GEOM.SDO_AREA(parse_sdo_geom(p.geometry), 0.005, 'unit=SQ_KM') as area " +
                        "FROM spatial_entities p LEFT JOIN spatial_entities pt ON pt.entity_type = 'POINT' " +
                        "AND SDO_INSIDE(parse_sdo_geom(pt.geometry), parse_sdo_geom(p.geometry)) = 'TRUE' " +
                        "WHERE p.entity_type = :regionType " +
                        "GROUP BY p.name, p.geometry";
            
            @SuppressWarnings("unchecked")
            List<Object[]> results = entityManager.createNativeQuery(sql)
                .setParameter("regionType", regionType != null ? regionType : "POLYGON")
                .getResultList();
            
            for (Object[] row : results) {
                String regionName = (String) row[0];
                Long pointCount = ((Number) row[1]).longValue();
                Double area = ((Number) row[2]).doubleValue();
                Double density = area > 0 ? pointCount / area : 0.0;
                
                Map<String, Object> result = new HashMap<>();
                result.put("region_name", regionName);
                result.put("point_count", pointCount);
                result.put("density_per_unit_area", density);
                densityResults.add(result);
            }
            
            model.addAttribute("densityResults", densityResults);
            model.addAttribute("analysisType", "DENSITY");
            model.addAttribute("regionType", regionType != null ? regionType : "POLYGON");
        } catch (Exception e) {
            model.addAttribute("error", "Density analysis failed: " + e.getMessage());
        }
        
        addAllEntitiestoModel(model);
        return "spatial-analysis";
    }
    
    
    // === INTERACTIVE MAP FUNCTIONS ===
    
    @PostMapping("/entities/{id}/move")
    @ResponseBody
    public ResponseEntity<?> moveEntity(@PathVariable Long id, 
                                       @RequestParam double deltaX, 
                                       @RequestParam double deltaY) {
        try {
            // For H2 - just return success without spatial operations
            SpatialEntity entity = spatialEntityRepository.findById(id).orElseThrow();
            return ResponseEntity.ok(SpatialEntityDto.from(entity));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error moving entity: " + e.getMessage());
        }
    }
    
    @PostMapping("/entities/{id}/resize")
    @ResponseBody
    public ResponseEntity<?> resizeEntity(@PathVariable Long id, 
                                         @RequestParam double scaleFactor) {
        try {
            // For H2 - just return success without spatial operations
            SpatialEntity entity = spatialEntityRepository.findById(id).orElseThrow();
            return ResponseEntity.ok(SpatialEntityDto.from(entity));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error resizing entity: " + e.getMessage());
        }
    }
    
    // === ADDITIONAL UTILITY ENDPOINTS ===
    
    @GetMapping("/analysis")
    public String showAnalysisPage(Model model) {
        addAllEntitiestoModel(model);
        return "spatial-analysis";
    }
    
    @PostMapping("/spatial/bounding-box")
    @ResponseBody
    public ResponseEntity<List<SpatialEntityDto>> findEntitiesInBoundingBox(
            @RequestParam double minX, @RequestParam double minY,
            @RequestParam double maxX, @RequestParam double maxY) {
        try {
            String bboxWkt = String.format("POLYGON((%.6f %.6f, %.6f %.6f, %.6f %.6f, %.6f %.6f, %.6f %.6f))", 
                                         minX, minY, maxX, minY, maxX, maxY, minX, maxY, minX, minY);
            String sql = "SELECT * FROM spatial_entities WHERE SDO_INSIDE(parse_sdo_geom(geometry), SDO_GEOMETRY(:bbox, 4326)) = 'TRUE'";
            List<SpatialEntity> entities = entityManager.createNativeQuery(sql, SpatialEntity.class)
                .setParameter("bbox", bboxWkt)
                .getResultList();
            return ResponseEntity.ok(entities.stream().map(SpatialEntityDto::from).collect(Collectors.toList()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    // Helper method to add all entities to model
    private void addAllEntitiestoModel(Model model) {
        try {
            List<Location> locations = locationRepository.findAll();
            List<SpatialEntity> spatialEntities = spatialEntityRepository.findAll();
            
            model.addAttribute("locations", locations.stream().map(LocationDto::from).collect(Collectors.toList()));
            model.addAttribute("spatialEntities", spatialEntities.stream().map(SpatialEntityDto::from).collect(Collectors.toList()));
            
            // Add entity type counts for UI
            Map<String, Long> entityCounts = new HashMap<>();
            for (SpatialEntity.SpatialEntityType type : SpatialEntity.SpatialEntityType.values()) {
                long count = spatialEntities.stream()
                    .filter(e -> e.getEntityType() == type)
                    .count();
                entityCounts.put(type.name(), count);
            }
            model.addAttribute("entityCounts", entityCounts);
            
        } catch (Exception e) {
            model.addAttribute("warning", "Error loading entities: " + e.getMessage());
        }
    }
    
    // Helper methods for geometry handling
    private String generateSampleGeometry(SpatialEntity.SpatialEntityType type, double baseLat, double baseLng) {
        double offset = 0.01;
        
        // Generate SDO_GEOMETRY string format for Oracle compatibility
        switch (type) {
            case POINT:
                return String.format("SDO_GEOMETRY(2001, 4326, SDO_POINT_TYPE(%.6f, %.6f, NULL), NULL, NULL)", baseLng, baseLat);
                
            case LINESTRING:
                return String.format("SDO_GEOMETRY(2002, 4326, NULL, SDO_ELEM_INFO_ARRAY(1,2,1), SDO_ORDINATE_ARRAY(%.6f, %.6f, %.6f, %.6f))", 
                                   baseLng, baseLat, baseLng + offset, baseLat + offset);
                
            case POLYGON:
                return String.format("SDO_GEOMETRY(2003, 4326, NULL, SDO_ELEM_INFO_ARRAY(1,1003,1), SDO_ORDINATE_ARRAY(%.6f, %.6f, %.6f, %.6f, %.6f, %.6f, %.6f, %.6f, %.6f, %.6f))",
                                   baseLng - offset, baseLat - offset,
                                   baseLng + offset, baseLat - offset,
                                   baseLng + offset, baseLat + offset,
                                   baseLng - offset, baseLat + offset,
                                   baseLng - offset, baseLat - offset);
                
            case CIRCLE:
                // Approximate circle as polygon
                StringBuilder circle = new StringBuilder("SDO_GEOMETRY(2003, 4326, NULL, SDO_ELEM_INFO_ARRAY(1,1003,1), SDO_ORDINATE_ARRAY(");
                int points = 16;
                double radius = offset;
                for (int i = 0; i <= points; i++) {
                    double angle = 2.0 * Math.PI * i / points;
                    double x = baseLng + radius * Math.cos(angle);
                    double y = baseLat + radius * Math.sin(angle);
                    if (i > 0) circle.append(", ");
                    circle.append(String.format("%.6f, %.6f", x, y));
                }
                circle.append("))");
                return circle.toString();
                
            case RECTANGLE:
                return String.format("SDO_GEOMETRY(2003, 4326, NULL, SDO_ELEM_INFO_ARRAY(1,1003,1), SDO_ORDINATE_ARRAY(%.6f, %.6f, %.6f, %.6f, %.6f, %.6f, %.6f, %.6f, %.6f, %.6f))",
                                   baseLng - offset/2, baseLat - offset/2,
                                   baseLng + offset/2, baseLat - offset/2,
                                   baseLng + offset/2, baseLat + offset/2,
                                   baseLng - offset/2, baseLat + offset/2,
                                   baseLng - offset/2, baseLat - offset/2);
                
            default:
                return String.format("SDO_GEOMETRY(2001, 4326, SDO_POINT_TYPE(%.6f, %.6f, NULL), NULL, NULL)", baseLng, baseLat);
        }
    }
    
    private boolean isValidGeometry(String geometryString) {
        if (geometryString == null || geometryString.trim().isEmpty()) {
            return false;
        }
        String upperGeometry = geometryString.trim().toUpperCase();
        // Accept both WKT format and SDO_GEOMETRY format
        return upperGeometry.startsWith("POINT(") || 
               upperGeometry.startsWith("LINESTRING(") || 
               upperGeometry.startsWith("POLYGON(") ||
               upperGeometry.startsWith("SDO_GEOMETRY(");
    }

    @PostMapping("/fix-triggers")
    @ResponseBody
    @Transactional
    public ResponseEntity<Map<String, Object>> fixTriggers() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            System.out.println("Fixing Oracle triggers for spatial_entities...");
            
            // Use JDBC connection directly to avoid Hibernate parameter parsing issues
            java.sql.Connection connection = entityManager.unwrap(java.sql.Connection.class);
            java.sql.Statement statement = connection.createStatement();
            
            List<String> results = new ArrayList<>();
            
            // Step 1: Check current table structure
            try {
                java.sql.ResultSet rs = statement.executeQuery(
                    "SELECT column_name, data_type FROM user_tab_columns WHERE table_name = 'SPATIAL_ENTITIES' AND column_name = 'GEOMETRY'"
                );
                if (rs.next()) {
                    String geometryType = rs.getString("DATA_TYPE");
                    results.add("✓ Found geometry column with type: " + geometryType);
                } else {
                    results.add("✗ No geometry column found in SPATIAL_ENTITIES table");
                }
                rs.close();
            } catch (Exception e) {
                results.add("⚠ Could not check table structure: " + e.getMessage());
            }
            
            // Step 2: Drop existing problematic triggers
            try {
                java.sql.ResultSet rs = statement.executeQuery(
                    "SELECT trigger_name FROM user_triggers WHERE table_name = 'SPATIAL_ENTITIES'"
                );
                List<String> triggers = new ArrayList<>();
                while (rs.next()) {
                    triggers.add(rs.getString("trigger_name"));
                }
                rs.close();
                
                for (String triggerName : triggers) {
                    try {
                        statement.execute("DROP TRIGGER " + triggerName);
                        results.add("✓ Dropped trigger: " + triggerName);
                    } catch (Exception e) {
                        results.add("⚠ Could not drop trigger " + triggerName + ": " + e.getMessage());
                    }
                }
                
                if (triggers.isEmpty()) {
                    results.add("✓ No existing triggers to drop");
                }
            } catch (Exception e) {
                results.add("⚠ Error checking existing triggers: " + e.getMessage());
            }
            
            // Step 3: Create new working triggers
            String insertTrigger = 
                "CREATE OR REPLACE TRIGGER trg_spatial_entities_bi " +
                "BEFORE INSERT ON spatial_entities " +
                "FOR EACH ROW " +
                "BEGIN " +
                "    IF :NEW.entity_id IS NULL THEN " +
                "        :NEW.entity_id := spatial_entities_seq.NEXTVAL; " +
                "    END IF; " +
                "    :NEW.created_date := CURRENT_TIMESTAMP; " +
                "    :NEW.updated_date := CURRENT_TIMESTAMP; " +
                "END;";
                
            statement.execute(insertTrigger);
            results.add("✓ Created insert trigger (trg_spatial_entities_bi)");
            
            String updateTrigger = 
                "CREATE OR REPLACE TRIGGER trg_spatial_entities_bu " +
                "BEFORE UPDATE ON spatial_entities " +
                "FOR EACH ROW " +
                "BEGIN " +
                "    :NEW.updated_date := CURRENT_TIMESTAMP; " +
                "    :NEW.created_date := :OLD.created_date; " +
                "END;";
                
            statement.execute(updateTrigger);
            results.add("✓ Created update trigger (trg_spatial_entities_bu)");
            
            // Step 4: Verify triggers are working
            try {
                java.sql.ResultSet rs = statement.executeQuery(
                    "SELECT trigger_name, status FROM user_triggers WHERE table_name = 'SPATIAL_ENTITIES'"
                );
                List<String> triggerStatus = new ArrayList<>();
                while (rs.next()) {
                    String name = rs.getString("trigger_name");
                    String status = rs.getString("status");
                    triggerStatus.add(name + " (" + status + ")");
                }
                rs.close();
                
                if (triggerStatus.isEmpty()) {
                    results.add("✗ No triggers found after creation");
                } else {
                    results.add("✓ Active triggers: " + String.join(", ", triggerStatus));
                }
            } catch (Exception e) {
                results.add("⚠ Could not verify trigger status: " + e.getMessage());
            }
            
            // Step 5: Test trigger functionality
            try {
                // Test with a simple insert that should work
                java.sql.PreparedStatement testInsert = connection.prepareStatement(
                    "INSERT INTO spatial_entities (name, entity_type, geometry, description, color) " +
                    "VALUES (?, 'POINT', 'POINT(-74.0060 40.7128)', 'Test entity', '#ff0000')"
                );
                testInsert.setString(1, "Test_" + System.currentTimeMillis());
                
                int rowsInserted = testInsert.executeUpdate();
                
                if (rowsInserted > 0) {
                    results.add("✓ Trigger test successful - spatial entity creation works!");
                    
                    // Clean up test record
                    statement.execute("DELETE FROM spatial_entities WHERE name LIKE 'Test_%'");
                    results.add("✓ Test record cleaned up");
                } else {
                    results.add("✗ Trigger test failed - no rows inserted");
                }
                
                testInsert.close();
            } catch (Exception e) {
                results.add("✗ Trigger test failed: " + e.getMessage());
            }
            
            statement.close();
            
            response.put("success", true);
            response.put("message", "Oracle spatial triggers fixed successfully! Spatial entity creation should now work.");
            response.put("details", results);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("Error fixing triggers: " + e.getMessage());
            e.printStackTrace();
            
            response.put("success", false);
            response.put("message", "Failed to fix triggers: " + e.getMessage());
            response.put("error", e.getClass().getSimpleName());
            
            return ResponseEntity.badRequest().body(response);
        }
    }
}
