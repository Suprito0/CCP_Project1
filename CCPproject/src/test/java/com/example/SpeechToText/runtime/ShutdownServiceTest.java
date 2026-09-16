package com.example.SpeechToText.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

// Sends many shutdown attempts concurrently and verifies exactly one is accepted.
class ShutdownServiceTest {

    @Test
    void onlyOneConcurrentShutdownRequestIsAccepted() throws Exception {
        GracefulShutdownService graceful = mock(GracefulShutdownService.class);
        ShutdownService service = new ShutdownService(graceful);
        int attempts = 100;
        int accepted = 0;

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<Boolean>> futures = new ArrayList<>();
            for (int i = 0; i < attempts; i++) {
                futures.add(executor.submit(service::requestShutdown));
            }

            for (Future<Boolean> future : futures) {
                if (future.get()) {
                    accepted++;
                }
            }
        }

        assertEquals(1, accepted);
        verify(graceful, times(1)).shutdownAfterResponse();
    }
}
