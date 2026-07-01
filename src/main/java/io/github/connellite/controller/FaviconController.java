package io.github.connellite.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.Duration;

@Controller
public class FaviconController {

    @GetMapping("favicon.ico")
    public ResponseEntity<ClassPathResource> favicon() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7)))
                .contentType(MediaType.parseMediaType("image/svg+xml"))
                .body(new ClassPathResource("static/favicon.svg"));
    }
}
