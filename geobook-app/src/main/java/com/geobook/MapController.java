package com.geobook;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/map")
public class MapController {

    private final LocationRepository locationRepository;

    public MapController(LocationRepository locationRepository) {
        this.locationRepository = locationRepository;
    }

    @GetMapping
    public String showMap(Model model) {
        List<Location> locations = locationRepository.findAll();
        model.addAttribute("locations", locations.stream().map(LocationDto::from).collect(Collectors.toList()));
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
        return "map";
    }
}
