package com.example.SpeechToText.dto;

// JSON acknowledgement returned when graceful shutdown is accepted.
public record ShutdownResponse(String message) {
}
