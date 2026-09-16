package com.example.SpeechToText.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import static org.springframework.test.web.client.ExpectedCount.once;
import org.springframework.test.web.client.MockRestServiceServer;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import org.springframework.web.client.RestClient;

// Uses MockRestServiceServer so OpenAI request/response handling can be tested without network/API cost.
class OpenAiSpeechToTextServiceTest {

        private MockRestServiceServer server;
        private OpenAiSpeechToTextService service;

        @BeforeEach
        void setUp() {
                RestClient.Builder builder = RestClient.builder()
                                .baseUrl("https://api.openai.test/v1");

                server = MockRestServiceServer.bindTo(builder).build();
                service = new OpenAiSpeechToTextService(
                                builder.build(),
                                "test-key",
                                "gpt-4o-mini-transcribe");
        }

        @Test
        void parsesTranscriptionAndTokenUsage() {
                server.expect(once(), requestTo("https://api.openai.test/v1/audio/transcriptions"))
                                .andExpect(method(HttpMethod.POST))
                                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-key"))
                                .andRespond(withSuccess("""
                                                {
                                                  "text": "test transcription",
                                                  "usage": {
                                                    "type": "tokens",
                                                    "input_tokens": 14,
                                                    "output_tokens": 45,
                                                    "total_tokens": 59
                                                  }
                                                }
                                                """, MediaType.APPLICATION_JSON));

                TranscriptionResult result = service.transcribe(audio());

                assertEquals("test transcription", result.text());
                assertEquals(14L, result.inputTokens());
                assertEquals(45L, result.outputTokens());
                server.verify();
        }

        @Test
        void rejectsSuccessfulResponseWithoutCompleteTokenUsage() {
                server.expect(once(), requestTo("https://api.openai.test/v1/audio/transcriptions"))
                                .andExpect(method(HttpMethod.POST))
                                .andRespond(withSuccess("""
                                                {
                                                  "text": "hello",
                                                  "usage": {
                                                    "type": "tokens",
                                                    "input_tokens": 14
                                                  }
                                                }
                                                """, MediaType.APPLICATION_JSON));

                SpeechToTextException exception = assertThrows(
                                SpeechToTextException.class,
                                () -> service.transcribe(audio()));

                assertEquals(HttpStatus.BAD_GATEWAY, exception.status());
                server.verify();
        }

        @Test
        void convertsProviderServerErrorToSafeBadGatewayError() {
                server.expect(once(), requestTo("https://api.openai.test/v1/audio/transcriptions"))
                                .andExpect(method(HttpMethod.POST))
                                .andRespond(withServerError());

                SpeechToTextException exception = assertThrows(
                                SpeechToTextException.class,
                                () -> service.transcribe(audio()));

                assertEquals(HttpStatus.BAD_GATEWAY, exception.status());
                server.verify();
        }

        @Test
        void refusesToCallProviderWhenApiKeyIsMissing() {
                OpenAiSpeechToTextService noKeyService = new OpenAiSpeechToTextService(
                                RestClient.builder().baseUrl("https://api.openai.test/v1").build(),
                                "",
                                "gpt-4o-mini-transcribe");

                SpeechToTextException exception = assertThrows(
                                SpeechToTextException.class,
                                () -> noKeyService.transcribe(audio()));

                assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.status());
        }

        private MockMultipartFile audio() {
                return new MockMultipartFile(
                                "audio",
                                "recording.webm",
                                "audio/webm;codecs=opus",
                                "fake-audio".getBytes());
        }
}
