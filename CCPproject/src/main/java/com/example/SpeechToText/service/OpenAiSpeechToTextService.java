package com.example.SpeechToText.service;

import java.io.IOException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.multipart.MultipartFile;

// Calls the assignment-required OpenAI speech-to-text endpoint. 
@Service
public class OpenAiSpeechToTextService implements SpeechToTextService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiSpeechToTextService.class);

    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    // Spring injects the configured RestClient plus values from
    // application.properties.
    public OpenAiSpeechToTextService(
            RestClient openAiRestClient,
            @Value("${openai.api.key:}") String apiKey,
            @Value("${openai.api.model:gpt-4o-mini-transcribe}") String model) {

        this.restClient = openAiRestClient;
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public TranscriptionResult transcribe(MultipartFile audioFile) {
        // Validate locally first so bad uploads never consume a cloud API request.
        AudioFileSupport.validate(audioFile);

        if (apiKey == null || apiKey.isBlank()) {
            throw new SpeechToTextException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "OPENAI_API_KEY is not configured");
        }

        String mimeType = AudioFileSupport.normaliseMimeType(audioFile);
        String filename = AudioFileSupport.safeFilename(audioFile, mimeType);

        try {
            byte[] audioBytes = audioFile.getBytes();

            ByteArrayResource audioResource = new ByteArrayResource(audioBytes) {
                @Override
                public String getFilename() {
                    return filename;
                }
            };

            HttpHeaders fileHeaders = new HttpHeaders();
            fileHeaders.setContentType(MediaType.parseMediaType(mimeType));

            MultiValueMap<String, Object> multipart = new LinkedMultiValueMap<>();
            multipart.add("model", model);
            multipart.add("response_format", "json");
            multipart.add("file", new HttpEntity<>(audioResource, fileHeaders));

            long started = System.nanoTime();

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient
                    .post()
                    .uri("/audio/transcriptions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(multipart)
                    .retrieve()
                    .body(Map.class);

            long elapsedMs = (System.nanoTime() - started) / 1_000_000L;
            log.info("OpenAI transcription completed bytes={} mime={} elapsedMs={}",
                    audioFile.getSize(), mimeType, elapsedMs);

            if (response == null) {
                throw new SpeechToTextException(
                        HttpStatus.BAD_GATEWAY,
                        "Cloud transcription service returned no response");
            }

            Object textValue = response.get("text");
            if (!(textValue instanceof String text) || text.isBlank()) {
                throw new SpeechToTextException(
                        HttpStatus.BAD_GATEWAY,
                        "Cloud transcription service returned no transcription");
            }

            // Token usage is required so /api/v1/global/stats can report exact totals.
            Object usageValue = response.get("usage");
            if (!(usageValue instanceof Map<?, ?> usage)) {
                throw new SpeechToTextException(
                        HttpStatus.BAD_GATEWAY,
                        "Cloud transcription service returned no token usage");
            }

            long inputTokens = requiredNonNegativeLong(usage.get("input_tokens"), "input_tokens");
            long outputTokens = requiredNonNegativeLong(usage.get("output_tokens"), "output_tokens");

            return new TranscriptionResult(text.trim(), inputTokens, outputTokens);

        } catch (RestClientResponseException exception) {
            // Never log the upstream response body because it may contain provider details.
            log.warn("OpenAI transcription rejected status={}", exception.getStatusCode());
            throw new SpeechToTextException(
                    HttpStatus.BAD_GATEWAY,
                    "Cloud transcription service rejected the request",
                    exception);

        } catch (ResourceAccessException exception) {
            log.warn("OpenAI transcription network failure type={}",
                    exception.getClass().getSimpleName());
            throw new SpeechToTextException(
                    HttpStatus.GATEWAY_TIMEOUT,
                    "Cloud transcription service could not be reached",
                    exception);

        } catch (IOException exception) {
            throw new SpeechToTextException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not read uploaded audio",
                    exception);
        }
    }

    // Safely validates numeric token fields before they are added to global
    // counters.
    private long requiredNonNegativeLong(Object value, String fieldName) {
        if (!(value instanceof Number number)) {
            throw new SpeechToTextException(
                    HttpStatus.BAD_GATEWAY,
                    "Cloud transcription service returned invalid " + fieldName);
        }

        long result = number.longValue();
        if (result < 0) {
            throw new SpeechToTextException(
                    HttpStatus.BAD_GATEWAY,
                    "Cloud transcription service returned invalid " + fieldName);
        }

        return result;
    }
}
