package com.example.SpeechToText.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

// Contains Spring configuration used to communicate with the OpenAI REST API.
@Configuration
public class OpenAiConfig {

    @Bean
    public RestClient openAiRestClient(

            RestClient.Builder builder,

            // The base URL comes from application.properties
            @Value("${openai.api.base-url}") String baseUrl) {

        return builder
                .baseUrl(baseUrl)
                .build();
    }
}
