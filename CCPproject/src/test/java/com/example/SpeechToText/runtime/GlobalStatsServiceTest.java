package com.example.SpeechToText.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

// Includes a race-condition regression test to prove token counters stay exact under concurrency.
class GlobalStatsServiceTest {

    @Test
    void countersRemainExactUnderConcurrentUpdates() throws Exception {
        GlobalStatsService stats = new GlobalStatsService();
        int operations = 2_000;

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<?>> futures = new ArrayList<>();

            for (int i = 0; i < operations; i++) {
                futures.add(executor.submit(() -> stats.addTokenUsage(3L, 2L)));
            }

            for (Future<?> future : futures) {
                future.get();
            }
        }

        assertEquals(6_000L, stats.inputTokens());
        assertEquals(4_000L, stats.outputTokens());
    }

    @Test
    void negativeTokenCountsAreRejected() {
        GlobalStatsService stats = new GlobalStatsService();

        assertThrows(IllegalArgumentException.class, () -> stats.addTokenUsage(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> stats.addTokenUsage(0, -1));
        assertEquals(0L, stats.inputTokens());
        assertEquals(0L, stats.outputTokens());
    }
}
