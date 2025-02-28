package com.bostonidentity.samlbox.controller;

import com.bostonidentity.samlbox.service.LogService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
@RequiredArgsConstructor
public class LogController {
    private static final Logger logger = LoggerFactory.getLogger(LogController.class);

    private final LogService logService;

    @GetMapping("/keycloak-log")
    public String viewLogs(
            @RequestParam(defaultValue = "keycloak.log") String filename,
            @RequestParam(defaultValue = "1000") int lines,
            @RequestParam("entityId") String entityId,
            Model model
    ) {
        try {
            LogService.LogFileContent logContent = logService.getLastLines(filename, lines);
            model.addAttribute("logContent", logContent);
            model.addAttribute("entityId", entityId);
        } catch (Exception e) {
            model.addAttribute("error", "Error loading logs: " + e.getMessage());
            logger.error("Log view error", e);
        }
        return "keycloak-log";
    }

    @GetMapping("/logs/data")
    @ResponseBody
    public ResponseEntity<?> getLogData(
            @RequestParam(defaultValue = "keycloak.log") String filenam,
            @RequestParam(defaultValue = "1000") int lines) {
        try {
            LogService.LogFileContent logContent = logService.getLastLines(filenam, lines);
            return ResponseEntity.ok(logContent);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
