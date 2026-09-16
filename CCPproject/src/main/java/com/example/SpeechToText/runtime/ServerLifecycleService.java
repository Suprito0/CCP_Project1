package com.example.SpeechToText.runtime;

import java.time.Duration;
import java.time.Instant;

import org.springframework.stereotype.Service;

import com.example.SpeechToText.dto.UptimeResponse;

// Captures this server process start time and calculates UTC uptime responses.
@Service
public class ServerLifecycleService {

    private final Instant utcServerStart = Instant.now();

    public Instant utcServerStart() {
        return utcServerStart;
    }

    public UptimeResponse uptime() {
        // Uptime is calculated at request time so utcNow is current for every response.
        Instant utcNow = Instant.now();
        double seconds = Duration.between(utcServerStart, utcNow).toNanos()
                / 1_000_000_000.0;

        return new UptimeResponse(
                utcServerStart,
                utcNow,
                Math.max(0.0, seconds));
    }
}
