package com.example.SpeechToText.service;

// Successful cloud transcription plus the provider token usage for that request.
public record TranscriptionResult(
                String text,
                long inputTokens,
                long outputTokens) {
}
