package com.example.SpeechToText.controller;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.SpeechToText.dto.ErrorResponse;
import com.example.SpeechToText.dto.GlobalStatsResponse;
import com.example.SpeechToText.dto.ShutdownResponse;
import com.example.SpeechToText.dto.UptimeResponse;
import com.example.SpeechToText.runtime.GlobalStatsService;
import com.example.SpeechToText.runtime.ServerLifecycleService;
import com.example.SpeechToText.runtime.ShutdownService;

import jakarta.servlet.http.HttpServletRequest;

// Endpoints defined by the YAML. 
@RestController
@RequestMapping("/api/v1")
public class SystemController {

    private final ServerLifecycleService lifecycleService;
    private final GlobalStatsService globalStatsService;
    private final ShutdownService shutdownService;

    public SystemController(
            ServerLifecycleService lifecycleService,
            GlobalStatsService globalStatsService,
            ShutdownService shutdownService) {

        this.lifecycleService = lifecycleService;
        this.globalStatsService = globalStatsService;
        this.shutdownService = shutdownService;
    }

    // GET /api/v1/admin/uptime
    @GetMapping("/admin/uptime")
    public UptimeResponse uptime() {
        return lifecycleService.uptime();
    }

    // GET /api/v1/global/stats
    @GetMapping("/global/stats")
    public GlobalStatsResponse globalStats() {
        return new GlobalStatsResponse(
                globalStatsService.inputTokens(),
                globalStatsService.outputTokens());
    }

    // POST /api/v1/admin/shutdown
    @PostMapping("/admin/shutdown")
    public ResponseEntity<?> shutdown(HttpServletRequest request) {
        if (shutdownService.requestShutdown()) {
            return ResponseEntity
                    .status(HttpStatus.ACCEPTED)
                    .body(new ShutdownResponse("Graceful shutdown requested."));
        }

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(
                        Instant.now(),
                        409,
                        "Conflict",
                        "Graceful shutdown is already in progress.",
                        request.getRequestURI()));
    }
}
