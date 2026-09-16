package com.example.SpeechToText.runtime;

import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.stereotype.Service;

// Ensures only the first shutdown request can be accepted. 
@Service
public class ShutdownService {

    private final GracefulShutdownService gracefulShutdownService;
    private final AtomicBoolean shutdownRequested = new AtomicBoolean(false);

    public ShutdownService(GracefulShutdownService gracefulShutdownService) {
        this.gracefulShutdownService = gracefulShutdownService;
    }

    public boolean requestShutdown() {

        // compareAndSet changes false -> true atomically for exactly one caller.
        if (!shutdownRequested.compareAndSet(false, true)) {
            return false;
        }

        gracefulShutdownService.shutdownAfterResponse();
        return true;
    }
}
