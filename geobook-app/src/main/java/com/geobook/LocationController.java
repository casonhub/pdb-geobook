package com.geobook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/locations")
public class LocationController {

    @Autowired
    private LocationRepository locationRepository;
    
    @Autowired
    private ChapterRepository chapterRepository;
    
    @Autowired
    private SpatialService spatialService;

    private static final Logger logger = LoggerFactory.getLogger(LocationController.class);

    @GetMapping
    public String listLocations(Model model) {
        List<Location> locations = locationRepository.findAll();
        model.addAttribute("locations", locations.stream().map(LocationDto::from).collect(Collectors.toList()));
        return "locations";
    }

    @GetMapping("/new")
    public String newLocationForm(Model model) {
        model.addAttribute("location", new Location());
        model.addAttribute("chapters", chapterRepository.findAll());
        return "location-form";
    }

    @PostMapping
    public String createLocation(@ModelAttribute Location location) {
        logger.info("Creating location: {}", location);
        
        // Save location without spatial data first
        Location savedLocation = locationRepository.save(location);
        logger.info("Saved location: {}", savedLocation);
        
        // Update spatial data if coordinates are provided
        if (location.getLatitude() != null && location.getLongitude() != null) {
            logger.info("Creating spatial data for lat: {}, lng: {}", location.getLatitude(), location.getLongitude());
            try {
                spatialService.updateLocationSpatialData(savedLocation.getLocationId(), 
                                                        location.getLatitude(), location.getLongitude());
                logger.info("Updated location with spatial data");
            } catch (Exception e) {
                logger.error("Failed to update spatial data", e);
                // Continue without spatial data
            }
        }
        
        return "redirect:/locations";
    }

    @GetMapping("/{id}/edit")
    public String editLocationForm(@PathVariable Long id, Model model) {
        Location location = locationRepository.findById(id).orElseThrow();
        model.addAttribute("location", location);
        model.addAttribute("chapters", chapterRepository.findAll());
        return "location-form";
    }

    @PostMapping("/{id}")
    public String updateLocation(@PathVariable Long id, @ModelAttribute Location location) {
        location.setLocationId(id);
        
        // Save location without spatial data first
        Location savedLocation = locationRepository.save(location);
        
        // Update spatial data if coordinates are provided
        if (location.getLatitude() != null && location.getLongitude() != null) {
            try {
                spatialService.updateLocationSpatialData(id, location.getLatitude(), location.getLongitude());
            } catch (Exception e) {
                logger.error("Failed to update spatial data for location {}", id, e);
                // Continue without spatial data
            }
        }
        
        return "redirect:/locations";
    }

    @PostMapping("/{id}/delete")
    public String deleteLocation(@PathVariable Long id) {
        locationRepository.deleteById(id);
        return "redirect:/locations";
    }

    @GetMapping("/spatial-test")
    @ResponseBody
    public String spatialTest(@RequestParam(required = false) Double lat, 
                             @RequestParam(required = false) Double lng) {
        if (lat != null && lng != null) {
            return "Spatial data: " + spatialService.createSpatialData(lat, lng);
        }
        return "Usage: /locations/spatial-test?lat=50.0&lng=14.0";
    }
}
