package com.example.SpeechToText.concurrency;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import com.example.SpeechToText.service.SpeechToTextService;
import com.example.SpeechToText.service.TranscriptionResult;

// Full HTTP regression test for the rubric requirement of more than 200 overlapping blocking requests.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "openai.api.key=test-key",
        "spring.threads.virtual.enabled=true"
})
@Import(ConcurrentHttpLoadTest.BlockingStubConfiguration.class)
class ConcurrentHttpLoadTest {
    // Deliberately greater than 200 to satisfy the assignment concurrency
    // criterion.
    private static final int REQUEST_COUNT = 220;

    @LocalServerPort
    private int port;

    // Used AI for this test
    @Test
    void handlesMoreThanTwoHundredOverlappingBlockingRequestsWithoutLosingStats() throws Exception {
        long started = System.nanoTime();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            HttpClient client = HttpClient.newBuilder()
                    .executor(executor)
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();

            List<CompletableFuture<Integer>> requests = new ArrayList<>();

            // Launch all requests asynchronously so their 250 ms blocking periods overlap
            for (int i = 0; i < REQUEST_COUNT; i++) {
                HttpRequest request = multipartRequest(i);
                requests.add(CompletableFuture.supplyAsync(() -> {
                    try {
                        return client.send(request, HttpResponse.BodyHandlers.ofString()).statusCode();
                    } catch (Exception exception) {
                        throw new RuntimeException(exception);
                    }
                }, executor));
            }

            for (CompletableFuture<Integer> request : requests) {
                assertEquals(200, request.join());
            }

            long elapsedMs = (System.nanoTime() - started) / 1_000_000L;
            assertTrue(elapsedMs < 15_000,
                    "220 concurrent requests took too long: " + elapsedMs + " ms");

            // Every stub response reports 3 input + 2 output tokens, so after 220
            // requests the totals must be exactly 660 and 440.
            HttpRequest statsRequest = HttpRequest.newBuilder()
                    .uri(URI.create("http://127.0.0.1:" + port + "/api/v1/global/stats"))
                    .GET()
                    .build();

            HttpResponse<String> statsResponse = client.send(
                    statsRequest,
                    HttpResponse.BodyHandlers.ofString());

            assertEquals(200, statsResponse.statusCode());
            assertTrue(statsResponse.body().contains("\"inputTokens\":660"));
            assertTrue(statsResponse.body().contains("\"outputTokens\":440"));
        }
    }

    // Builds a multipart request manually so the test exercises the real embedded
    // HTTP server.
    private HttpRequest multipartRequest(int requestNumber) {
        String boundary = "Boundary" + UUID.randomUUID();
        String prefix = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"audio\"; filename=\"test-"
                + requestNumber + ".webm\"\r\n"
                + "Content-Type: audio/webm\r\n\r\n";
        String suffix = "\r\n--" + boundary + "--\r\n";

        byte[] audio = ("test-audio-" + requestNumber).getBytes(StandardCharsets.UTF_8);
        byte[] prefixBytes = prefix.getBytes(StandardCharsets.UTF_8);
        byte[] suffixBytes = suffix.getBytes(StandardCharsets.UTF_8);
        byte[] body = new byte[prefixBytes.length + audio.length + suffixBytes.length];

        System.arraycopy(prefixBytes, 0, body, 0, prefixBytes.length);
        System.arraycopy(audio, 0, body, prefixBytes.length, audio.length);
        System.arraycopy(suffixBytes, 0, body, prefixBytes.length + audio.length, suffixBytes.length);

        return HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/api/v1/transcribe"))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class BlockingStubConfiguration {

        // @Primary makes this blocking stub replace the real OpenAI service in this
        // test context.
        @Bean
        @Primary
        SpeechToTextService blockingSpeechToTextService() {
            return audioFile -> {
                if (audioFile == null || audioFile.isEmpty()) {
                    throw new IllegalArgumentException("Audio file is empty");
                }

                try {
                    Thread.sleep(250L);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Stub transcription interrupted", exception);
                }

                return new TranscriptionResult("stub transcription", 3L, 2L);
            };
        }
    }
}
