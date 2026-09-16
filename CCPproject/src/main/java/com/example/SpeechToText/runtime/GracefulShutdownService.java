package com.example.SpeechToText.runtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Service;

// Closes Spring shortly after the HTTP acknowledgement has been returned. 
@Service
public class GracefulShutdownService {

    private static final Logger log = LoggerFactory.getLogger(GracefulShutdownService.class);

    private final ConfigurableApplicationContext applicationContext;

    public GracefulShutdownService(ConfigurableApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public void shutdownAfterResponse() {
        // Use a separate lightweight virtual thread so the controller can return first.
        Thread.ofVirtual().name("api-graceful-shutdown").start(() -> {
            try {
                // Small delay so the accepted response can leave the server before shutdown
                // starts.
                Thread.sleep(500L);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }

            log.info("Graceful shutdown sequence starting");
            applicationContext.close();
        });
    }
}
