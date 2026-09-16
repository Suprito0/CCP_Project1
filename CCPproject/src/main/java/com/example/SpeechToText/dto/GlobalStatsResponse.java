package com.example.SpeechToText.dto;

// JSON response for cumulative OpenAI token usage since this server process started.
public record GlobalStatsResponse(
                long inputTokens,
                long outputTokens) {
}
