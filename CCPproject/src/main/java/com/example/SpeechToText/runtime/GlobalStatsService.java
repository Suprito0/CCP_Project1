package com.example.SpeechToText.runtime;

import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Service;

// Process-lifetime OpenAI token counters. 
@Service
public class GlobalStatsService {

    private final AtomicLong inputTokens = new AtomicLong();
    private final AtomicLong outputTokens = new AtomicLong();

    // Atomically adds one successful transcription's token usage to the totals.
    public void addTokenUsage(long input, long output) {
        if (input < 0 || output < 0) {
            throw new IllegalArgumentException("Token counts must be non-negative");
        }

        inputTokens.addAndGet(input);
        outputTokens.addAndGet(output);
    }

    // AtomicLong#get safely reads the latest value across threads.
    public long inputTokens() {
        return inputTokens.get();
    }

    public long outputTokens() {
        return outputTokens.get();
    }
}
