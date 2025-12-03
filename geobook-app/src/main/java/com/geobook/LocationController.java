package com.geobook;

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
        // Save location first to get ID
        Location savedLocation = locationRepository.save(location);
        
        // Update spatial data if coordinates are provided
        if (location.getLatitude() != null && location.getLongitude() != null) {
            String spatialData = spatialService.createSpatialData(location.getLatitude(), location.getLongitude());
            savedLocation.setSpatialData(spatialData);
            locationRepository.save(savedLocation);
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
        Location savedLocation = locationRepository.save(location);
        
        // Update spatial data if coordinates are provided
        if (location.getLatitude() != null && location.getLongitude() != null) {
            spatialService.updateLocationSpatialData(id, location.getLatitude(), location.getLongitude());
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
