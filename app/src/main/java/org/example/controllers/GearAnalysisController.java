package org.example.controllers;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;

import org.example.App;
import org.example.UserConfig;
import org.example.services.GearAnalysisService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class GearAnalysisController {

    private final GearAnalysisService gearAnalysisService = new GearAnalysisService(); // ✅ Create an instance

    
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Gear Analysis API is running!");
    }

    @PostMapping("/analyze")
    public ResponseEntity<?> analyzeGearUsage(
        @RequestParam("file") MultipartFile file,
        @RequestParam(value = "bigChainring", required = false, defaultValue = "0") Integer bigChainring,
        @RequestParam(value = "smallChainring", required = false, defaultValue = "0") Integer smallChainring,
        @RequestParam("cassette") String cassette,
        @RequestParam(value = "minCadence", required = false) Integer minCadence,
        @RequestParam(value = "minPower", required = false) Integer minPower,
        @RequestParam(value = "oneBySetup", required = false) String oneBySetup
    ) {
        File convertedFile = null; // Declare here so it's accessible in try & finally
        try {
            convertedFile = convertMultipartFile(file);
            Boolean oneBySetupBoolean = "true".equalsIgnoreCase(oneBySetup);
            UserConfig userConfig = new UserConfig(minCadence, minPower, cassette, bigChainring, smallChainring, oneBySetupBoolean);
            Map<String, Object> analysisResult = App.analyzeFile(convertedFile, userConfig);
            return ResponseEntity.ok(analysisResult);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(Map.of("error", "File processing failed"));
        } finally {
            // Delete the temporary file immediately after processing
            if (convertedFile != null && convertedFile.exists()) {
                try {
                    Files.deleteIfExists(convertedFile.toPath());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
    



    @GetMapping("/test")
    public String testApi() {
        return "Gear Analysis API is working! ";
    }

    private File convertMultipartFile(MultipartFile file) throws Exception {
        Path tempFile = Files.createTempFile("upload_", file.getOriginalFilename());
        Files.copy(file.getInputStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);
        File finalFile = tempFile.toFile();
    
        // ✅ Delete file when Java exits
       // finalFile.deleteOnExit(); ??
    
        return finalFile;
    }
    
}