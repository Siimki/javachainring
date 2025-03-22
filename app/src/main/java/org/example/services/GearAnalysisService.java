package org.example.services;

import java.io.File;
import java.util.Map;

import org.example.App;
import org.example.UserConfig;

public class GearAnalysisService {
    public Map<String, Object> processFitFile(File fitFile, UserConfig userConfig) {
        try {
            return App.analyzeFile(fitFile, userConfig);
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("error", "Failed to process the FIT file");
        }
    }
}
    

