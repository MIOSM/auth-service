package MIOSM.auth_service.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

@Slf4j
@RestController
@RequestMapping("/auth/public/api/images")
@CrossOrigin(origins = "http://localhost:4200")
public class ImageProxyController {

    @GetMapping("/proxy")
    public ResponseEntity<InputStreamResource> proxyImage(@RequestParam String url) {
        try {
            log.info("Proxying image request for URL: {}", url);

            if (!url.startsWith("http://localhost:9000/user-images/")) {
                log.warn("Invalid URL requested: {}", url);
                return ResponseEntity.badRequest().build();
            }

            URL imageUrl = new URL(url);
            URLConnection connection = imageUrl.openConnection();
            InputStream inputStream = connection.getInputStream();
            
            String contentType = connection.getContentType();
            if (contentType == null) {
                contentType = "image/jpeg"; 
            }

            HttpHeaders headers = new HttpHeaders();
            headers.add("Access-Control-Allow-Origin", "http://localhost:4200");
            headers.add("Access-Control-Allow-Methods", "GET");
            headers.add("Access-Control-Allow-Headers", "*");
            headers.setContentType(MediaType.parseMediaType(contentType));

            log.info("Successfully proxied image: {}", url);
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(new InputStreamResource(inputStream));

        } catch (Exception e) {
            log.error("Failed to proxy image: {} - Error: {}", url, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
}
