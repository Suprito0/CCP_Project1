package com.example.SpeechToText.controller;

import java.time.Instant;

import static org.hamcrest.Matchers.hasSize;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.example.SpeechToText.dto.UptimeResponse;
import com.example.SpeechToText.runtime.GlobalStatsService;
import com.example.SpeechToText.runtime.ServerLifecycleService;
import com.example.SpeechToText.runtime.ShutdownService;

// Verifies the administration/statistics endpoints match the supplied YAML exactly.
class SystemControllerTest {

        private ServerLifecycleService lifecycleService;
        private GlobalStatsService statsService;
        private ShutdownService shutdownService;
        private MockMvc mockMvc;

        @BeforeEach
        void setUp() {
                lifecycleService = mock(ServerLifecycleService.class);
                statsService = mock(GlobalStatsService.class);
                shutdownService = mock(ShutdownService.class);

                SystemController controller = new SystemController(
                                lifecycleService,
                                statsService,
                                shutdownService);

                mockMvc = MockMvcBuilders
                                .standaloneSetup(controller)
                                .setControllerAdvice(new ApiExceptionHandler())
                                .build();
        }

        @Test
        void uptimeEndpointMatchesYamlShape() throws Exception {
                when(lifecycleService.uptime()).thenReturn(new UptimeResponse(
                                Instant.parse("2026-07-14T01:15:30Z"),
                                Instant.parse("2026-07-14T03:45:30.500Z"),
                                9000.5));

                mockMvc.perform(get("/api/v1/admin/uptime"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.utcServerStart").value("2026-07-14T01:15:30Z"))
                                .andExpect(jsonPath("$.utcNow").value("2026-07-14T03:45:30.500Z"))
                                .andExpect(jsonPath("$.serverUptimeSeconds").value(9000.5))
                                .andExpect(jsonPath("$.*", hasSize(3)));
        }

        @Test
        void globalStatsEndpointMatchesYamlShape() throws Exception {
                when(statsService.inputTokens()).thenReturn(18_432L);
                when(statsService.outputTokens()).thenReturn(4_096L);

                mockMvc.perform(get("/api/v1/global/stats"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.inputTokens").value(18_432L))
                                .andExpect(jsonPath("$.outputTokens").value(4_096L))
                                .andExpect(jsonPath("$.*", hasSize(2)));
        }

        @Test
        void firstShutdownRequestReturnsExactAcceptedShape() throws Exception {
                when(shutdownService.requestShutdown()).thenReturn(true);

                mockMvc.perform(post("/api/v1/admin/shutdown"))
                                .andExpect(status().isAccepted())
                                .andExpect(jsonPath("$.message").value("Graceful shutdown requested."))
                                .andExpect(jsonPath("$.*", hasSize(1)));
        }

        @Test
        void repeatedShutdownRequestReturnsExactConflictShape() throws Exception {
                when(shutdownService.requestShutdown()).thenReturn(false);

                mockMvc.perform(post("/api/v1/admin/shutdown"))
                                .andExpect(status().isConflict())
                                .andExpect(jsonPath("$.timestamp").isString())
                                .andExpect(jsonPath("$.status").value(409))
                                .andExpect(jsonPath("$.error").value("Conflict"))
                                .andExpect(jsonPath("$.message")
                                                .value("Graceful shutdown is already in progress."))
                                .andExpect(jsonPath("$.path").value("/api/v1/admin/shutdown"))
                                .andExpect(jsonPath("$.*", hasSize(5)));
        }

        @Test
        void unexpectedAdminFailureUsesExactInternalServerErrorShape() throws Exception {
                when(lifecycleService.uptime()).thenThrow(new IllegalStateException("test failure"));

                mockMvc.perform(get("/api/v1/admin/uptime"))
                                .andExpect(status().isInternalServerError())
                                .andExpect(jsonPath("$.timestamp").isString())
                                .andExpect(jsonPath("$.status").value(500))
                                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                                .andExpect(jsonPath("$.message")
                                                .value("An unexpected server error occurred."))
                                .andExpect(jsonPath("$.path").value("/api/v1/admin/uptime"))
                                .andExpect(jsonPath("$.*", hasSize(5)));
        }

        @Test
        void oldAdministrationAliasesAreNotExposed() throws Exception {
                String[] removedPaths = {
                                "/api/v1/uptime",
                                "/api/v1/status",
                                "/api/v1/statistics",
                                "/api/v1/stats",
                                "/api/v1/runtime",
                                "/api/v1/health",
                                "/api/v1/admin/status",
                                "/api/v1/admin/stats"
                };

                for (String path : removedPaths) {
                        mockMvc.perform(get(path))
                                        .andExpect(status().isNotFound());
                }
        }
}
