package com.example.SpeechToText;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Marks this class as the entry point for Spring Boot and enables component scanning
@SpringBootApplication
public class SpeechToTextApplication {

	public static void main(String[] args) {
		// Starts Spring, creates all managed beans, and launches the embedded web
		// server.
		SpringApplication.run(SpeechToTextApplication.class, args);
	}

}
