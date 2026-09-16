package com.example.SpeechToText.dto;

import java.time.Instant;

// Record used for the exact error object defined by the administration API YAML.
public record ErrorResponse(
                Instant timestamp,
                int status,
                String error,
                String message,
                String path) {
}
