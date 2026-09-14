package com.landhub.land;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

@Controller
public class LandUploadController {

    private final Path uploadDirectory = Paths.get("uploads", "lands").toAbsolutePath().normalize();

    @GetMapping("/uploads/lands/{filename:.+}")
    public ResponseEntity<Resource> serveLandImage(@PathVariable String filename) throws MalformedURLException {
        Path imagePath = uploadDirectory.resolve(filename).normalize();

        if (!imagePath.startsWith(uploadDirectory) || !Files.exists(imagePath)) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new UrlResource(imagePath.toUri());
        return ResponseEntity.ok()
                .contentType(resolveMediaType(filename))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + imagePath.getFileName() + "\"")
                .body(resource);
    }

    private MediaType resolveMediaType(String filename) {
        String lowerName = filename.toLowerCase(Locale.ROOT);

        if (lowerName.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        }

        if (lowerName.endsWith(".webp")) {
            return MediaType.parseMediaType("image/webp");
        }

        return MediaType.IMAGE_JPEG;
    }
}
