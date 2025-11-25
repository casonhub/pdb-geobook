package com.geobook;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/map")
public class MapController {

    private final LocationRepository locationRepository;

    public MapController(LocationRepository locationRepository) {
        this.locationRepository = locationRepository;
    }

    @GetMapping
    public String showMap(Model model) {
        model.addAttribute("locations", locationRepository.findAll());
        return "map";
    }
}
