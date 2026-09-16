package com.example.SpeechToText.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.SpeechToText.runtime.GlobalStatsService;
import com.example.SpeechToText.service.SpeechToTextException;
import com.example.SpeechToText.service.SpeechToTextService;
import com.example.SpeechToText.service.TranscriptionResult;

@RestController
public class TranscriptionController {

    private static final Logger log = LoggerFactory.getLogger(TranscriptionController.class);

    private final SpeechToTextService speechToTextService;
    private final GlobalStatsService globalStatsService;

    public TranscriptionController(
            SpeechToTextService speechToTextService,
            GlobalStatsService globalStatsService) {

        this.speechToTextService = speechToTextService;
        this.globalStatsService = globalStatsService;
    }

    @PostMapping("/api/v1/transcribe")
    public ResponseEntity<String> transcribe(
            @RequestParam("audio") MultipartFile audioFile) {

        try {
            TranscriptionResult result = speechToTextService.transcribe(audioFile);

            // Only successful provider calls contribute to the global token counters.
            globalStatsService.addTokenUsage(result.inputTokens(), result.outputTokens());
            return ResponseEntity.ok(result.text());

        } catch (IllegalArgumentException exception) {
            // Invalid client input is a 400 response.
            return ResponseEntity
                    .badRequest()
                    .body(exception.getMessage());

        } catch (SpeechToTextException exception) {
            // Network failures are converted to safe status message
            log.warn("Transcription request failed status={} reason={}",
                    exception.status().value(),
                    exception.getMessage());

            return ResponseEntity
                    .status(exception.status())
                    .body(exception.getMessage());

        } catch (Exception exception) {
            // Final safety net so unexpected implementation details are not returned to the
            // browser.
            log.error("Unexpected transcription failure type={}",
                    exception.getClass().getSimpleName());

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unexpected transcription failure");
        }
    }
}
