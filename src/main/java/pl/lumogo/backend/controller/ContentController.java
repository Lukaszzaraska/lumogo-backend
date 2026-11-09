package pl.lumogo.backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Duration;

/** Treści statyczne z src/main/resources/content (edycja bez zmiany kodu). */
@RestController
@RequestMapping("/api/content")
@Tag(name = "Content", description = "FAQ i polityka prywatności wyświetlane w aplikacji")
public class ContentController {

    private final JsonNode faq;
    private final JsonNode privacy;

    public ContentController(ObjectMapper objectMapper) {
        this.faq = read(objectMapper, "content/faq.json");
        this.privacy = read(objectMapper, "content/privacy.json");
    }

    @GetMapping("/faq")
    @Operation(summary = "Lista pytań i odpowiedzi: [{ question, answer }]")
    public ResponseEntity<JsonNode> faq() {
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofHours(1))).body(faq);
    }

    @GetMapping("/privacy")
    @Operation(summary = "Polityka prywatności: { lastUpdated, sections: [{ title, content }] }")
    public ResponseEntity<JsonNode> privacy() {
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofHours(1))).body(privacy);
    }

    private static JsonNode read(ObjectMapper objectMapper, String path) {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            return objectMapper.readTree(in);
        } catch (IOException ex) {
            throw new UncheckedIOException("Brak pliku " + path, ex);
        }
    }
}
