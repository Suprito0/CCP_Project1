package com.example.SpeechToText.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.example.SpeechToText.runtime.GlobalStatsService;
import com.example.SpeechToText.service.SpeechToTextException;
import com.example.SpeechToText.service.SpeechToTextService;
import com.example.SpeechToText.service.TranscriptionResult;

// Tests controller behaviour with a stubbed SpeechToTextService, so no real API call is made.
class TranscriptionControllerTest {

        private SpeechToTextService speechToTextService;
        private GlobalStatsService stats;
        private MockMvc mockMvc;

        @BeforeEach
        void setUp() {
                speechToTextService = org.mockito.Mockito.mock(SpeechToTextService.class);
                stats = new GlobalStatsService();

                TranscriptionController controller = new TranscriptionController(
                                speechToTextService,
                                stats);

                mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        }

        @Test
        void returnsTextAndCountsTokensExactlyOnceOnSuccess() throws Exception {
                when(speechToTextService.transcribe(any()))
                                .thenReturn(new TranscriptionResult("hello from test", 14L, 45L));

                mockMvc.perform(multipart("/api/v1/transcribe").file(audio()))
                                .andExpect(status().isOk())
                                .andExpect(content().string("hello from test"));

                assertEquals(14L, stats.inputTokens());
                assertEquals(45L, stats.outputTokens());
        }

        @Test
        void invalidAudioReturnsBadRequestWithoutChangingStats() throws Exception {
                when(speechToTextService.transcribe(any()))
                                .thenThrow(new IllegalArgumentException("Audio file is empty"));

                MockMultipartFile emptyAudio = new MockMultipartFile(
                                "audio",
                                "recording.webm",
                                "audio/webm",
                                new byte[0]);

                mockMvc.perform(multipart("/api/v1/transcribe").file(emptyAudio))
                                .andExpect(status().isBadRequest())
                                .andExpect(content().string("Audio file is empty"));

                assertEquals(0L, stats.inputTokens());
                assertEquals(0L, stats.outputTokens());
        }

        @Test
        void providerFailureReturnsProviderStatusWithoutChangingStats() throws Exception {
                when(speechToTextService.transcribe(any()))
                                .thenThrow(new SpeechToTextException(
                                                HttpStatus.BAD_GATEWAY,
                                                "Cloud transcription service rejected the request"));

                mockMvc.perform(multipart("/api/v1/transcribe").file(audio()))
                                .andExpect(status().isBadGateway())
                                .andExpect(content().string("Cloud transcription service rejected the request"));

                assertEquals(0L, stats.inputTokens());
                assertEquals(0L, stats.outputTokens());
        }

        @Test
        void legacyTranscribeAliasIsNotExposed() throws Exception {
                mockMvc.perform(multipart("/transcribe").file(audio()))
                                .andExpect(status().isNotFound());
        }

        private MockMultipartFile audio() {
                return new MockMultipartFile(
                                "audio",
                                "recording.webm",
                                "audio/webm",
                                "fake-audio".getBytes());
        }
}
