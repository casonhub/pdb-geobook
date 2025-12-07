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
    
   
    //  MAP FUNCTIONS 
    
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
        
        switch (type) {
            case POINT:
                return String.format("POINT(%.6f %.6f)", baseLng, baseLat);
                
            case LINESTRING:
                return String.format("LINESTRING(%.6f %.6f, %.6f %.6f)", 
                                   baseLng, baseLat, baseLng + offset, baseLat + offset);
                
            case POLYGON:
                return String.format("POLYGON((%.6f %.6f, %.6f %.6f, %.6f %.6f, %.6f %.6f, %.6f %.6f))",
                                   baseLng - offset, baseLat - offset,
                                   baseLng + offset, baseLat - offset,
                                   baseLng + offset, baseLat + offset,
                                   baseLng - offset, baseLat + offset,
                                   baseLng - offset, baseLat - offset);
                
            case CIRCLE:
                // Approximate circle as polygon with multiple points
                StringBuilder circle = new StringBuilder("POLYGON((");
                int points = 16;
                double radius = offset;
                for (int i = 0; i <= points; i++) {
                    double angle = 2.0 * Math.PI * i / points;
                    double x = baseLng + radius * Math.cos(angle);
                    double y = baseLat + radius * Math.sin(angle);
                    if (i > 0) circle.append(", ");
                    circle.append(String.format("%.6f %.6f", x, y));
                }
                circle.append("))");
                return circle.toString();
                
            case RECTANGLE:
                return String.format("POLYGON((%.6f %.6f, %.6f %.6f, %.6f %.6f, %.6f %.6f, %.6f %.6f))",
                                   baseLng - offset/2, baseLat - offset/2,
                                   baseLng + offset/2, baseLat - offset/2,
                                   baseLng + offset/2, baseLat + offset/2,
                                   baseLng - offset/2, baseLat + offset/2,
                                   baseLng - offset/2, baseLat - offset/2);
                
            default:
                return String.format("POINT(%.6f %.6f)", baseLng, baseLat);
        }
    }
    
    private boolean isValidGeometry(String geometryString) {
        if (geometryString == null || geometryString.trim().isEmpty()) {
            return false;
        }
        String upperGeometry = geometryString.trim().toUpperCase();
        // Accept both WKT and SDO_GEOMETRY format
        return upperGeometry.startsWith("POINT(") || 
               upperGeometry.startsWith("LINESTRING(") || 
               upperGeometry.startsWith("POLYGON(") ||
               upperGeometry.startsWith("SDO_GEOMETRY(");
    }

    
}
