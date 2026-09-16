package com.example.SpeechToText.service;

import org.springframework.http.HttpStatus;

//A safe, user-facing failure from a speech-to-text provider.

public class SpeechToTextException extends RuntimeException {

    private final HttpStatus status;

    public SpeechToTextException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public SpeechToTextException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
