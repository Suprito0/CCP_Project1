package com.example.SpeechToText.dto;

import java.time.Instant;

// JSON response containing the UTC server start time, current time, and uptime.
public record UptimeResponse(
                Instant utcServerStart,
                Instant utcNow,
                double serverUptimeSeconds) {
}
