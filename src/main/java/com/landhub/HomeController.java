package com.landhub;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/lands")
    public String lands() {
        return "lands";
    }

    @GetMapping("/lands/{id}")
    public String landDetails() {
        return "land-details";
    }
}
