package com.example.SpeechToText;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// Starts the complete Spring context to catch missing beans/configuration before deployment.
@SpringBootTest(properties = "openai.api.key=test-key")
class SpeechToTextApplicationTests {

    @Test
    void contextLoads() {
    }
}
