package com.example.SpeechToText.runtime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.example.SpeechToText.dto.UptimeResponse;

// Confirms uptime uses a valid UTC start/current time and never becomes negative.
class ServerLifecycleServiceTest {

    @Test
    void uptimeUsesUtcInstantsAndNeverReturnsNegativeSeconds() {
        ServerLifecycleService service = new ServerLifecycleService();

        UptimeResponse response = service.uptime();

        assertFalse(response.utcNow().isBefore(response.utcServerStart()));
        assertTrue(response.serverUptimeSeconds() >= 0.0);
    }
}
